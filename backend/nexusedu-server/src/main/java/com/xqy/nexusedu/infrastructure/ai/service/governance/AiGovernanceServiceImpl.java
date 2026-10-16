package com.xqy.nexusedu.infrastructure.ai.service.governance;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.application.ai.port.in.AiGovernanceService;
import com.xqy.nexusedu.application.system.port.in.SystemManagementService;
import com.xqy.nexusedu.common.dto.ManagementRows;
import com.xqy.nexusedu.common.dto.PageResult;
import com.xqy.nexusedu.common.exception.ManagementConflictException;
import com.xqy.nexusedu.common.exception.ManagementNotFoundException;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;
import com.xqy.nexusedu.infrastructure.ai.service.evaluation.AiConfiguration;

@Service
public class AiGovernanceServiceImpl implements AiGovernanceService {
    @Autowired private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    public AiGovernanceServiceImpl(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override public PageResult list(String resource,Map<String,String> filters,int current,int size,InternalCallContext caller) {
        authorization.authorize("ai:config:read",caller);
        String table=switch(resource) {case "versions"->"ai_resource_version";case "rollouts"->"ai_resource_rollout";case "policies"->"ai_resource_policy";case "usage"->"ai_resource_usage";default->throw new IllegalArgumentException("未知配置资源");};
        String where=" WHERE tenant_id=?";List<Object> args=new ArrayList<>(List.of(caller.tenantId()));
        if(filters!=null && filters.get("type")!=null && !filters.get("type").isBlank()) {where+=" AND resource_type=?";args.add(AiConfiguration.type(filters.get("type")));}
        if(filters!=null && filters.get("key")!=null && !filters.get("key").isBlank()) {where+=" AND resource_key=?";args.add(AiConfiguration.key(filters.get("key")));}
        current=Math.max(current,1);size=Math.clamp(size,1,200);
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM "+table+where,Long.class,args.toArray());
        args.add(size);args.add((long)(current-1)*size);
        var rows=ManagementRows.publicRows(jdbc.queryForList("SELECT * FROM "+table+where+" ORDER BY "+(resource.equals("versions")?"create_time DESC,id DESC":"resource_type,resource_key")+" LIMIT ? OFFSET ?",args.toArray()));
        rows.forEach(row->{if(row.containsKey("configJson"))row.put("config",AiConfiguration.object(row.remove("configJson")));row.replaceAll((key,value)->value instanceof BigDecimal?value.toString():value);});
        return new PageResult(rows,current,size,total==null?0:total);
    }
    @Override public Map<String,Object> draft(Map<String,Object> input,InternalCallContext caller) {
        authorization.authorize("ai:config:write",caller);
        String type=AiConfiguration.type(input.get("resourceType")),key=AiConfiguration.key(input.get("resourceKey"));
        if("*".equals(key))throw new IllegalArgumentException("版本不支持通配符");
        var config=AiConfiguration.validate(type,key,input.get("config"));
        Long id=IdWorker.getId();
        try {jdbc.update("INSERT INTO ai_resource_version(id,tenant_id,resource_type,resource_key,version,config_json,create_by) VALUES(?,?,?,?,?,?,?)",id,caller.tenantId(),type,key,ManagementRows.text(input.get("version"),32,true),JSONUtil.toJsonStr(config),caller.userId().toString());}
        catch(org.springframework.dao.DuplicateKeyException error) {throw new ManagementConflictException("该资源版本已存在，请使用新版本号");}
        return Map.of("id",id.toString(),"status","DRAFT");
    }
    @Override @Transactional public void transition(Long id,String action,String note,InternalCallContext caller) {
        authorization.authorize(Set.of("approve","reject").contains(action)?"ai:config:approve":"ai:config:write",caller);
        var version=owned(id,caller,true);
        String status=String.valueOf(version.get("status"));
        switch(action) {
            case "submit" -> {if(!"DRAFT".equals(status))throw new ManagementConflictException("只能提交草稿");jdbc.update("UPDATE ai_resource_version SET status='PENDING' WHERE id=?",id);}
            case "approve","reject" -> {
                if(!"PENDING".equals(status))throw new ManagementConflictException("只能审批待审版本");
                if(caller.userId().toString().equals(version.get("create_by")))throw new ManagementConflictException("版本作者不能审批自己的版本");
                jdbc.update("UPDATE ai_resource_version SET status=?,reviewed_by=?,review_note=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?",action.equals("approve")?"APPROVED":"REJECTED",caller.userId().toString(),ManagementRows.text(note,500,true),id);
            }
            default -> throw new IllegalArgumentException("未知审批操作");
        }
    }
    @Override @Transactional public void rollout(Map<String,Object> input,InternalCallContext caller) {
        authorization.authorize("ai:config:publish",caller);
        String type=AiConfiguration.type(input.get("resourceType")),key=AiConfiguration.key(input.get("resourceKey"));
        Long stable=ManagementRows.id(input.get("stableVersionId"));Long candidate=input.get("candidateVersionId")==null?null:ManagementRows.id(input.get("candidateVersionId"));
        requireRelease(stable,type,key,caller);if(candidate!=null)requireRelease(candidate,type,key,caller);
        int percent=AiConfiguration.integer(input.getOrDefault("canaryPercent",0),0,100);
        if(percent>0 && (candidate==null || candidate.equals(stable)))throw new IllegalArgumentException("灰度需要不同的候选版本");
        jdbc.update("INSERT INTO ai_resource_rollout(tenant_id,resource_type,resource_key,stable_version_id,candidate_version_id,canary_percent,modify_by) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE stable_version_id=VALUES(stable_version_id),candidate_version_id=VALUES(candidate_version_id),canary_percent=VALUES(canary_percent),modify_by=VALUES(modify_by)",caller.tenantId(),type,key,stable,candidate,percent,caller.userId().toString());
    }
    @Override @Transactional public void policy(Map<String,Object> input,InternalCallContext caller) {
        authorization.authorize("ai:policy:write",caller);
        String type=AiConfiguration.type(input.get("resourceType")),key=AiConfiguration.key(input.get("resourceKey"));
        int rpm=AiConfiguration.integer(input.getOrDefault("requestsPerMinute",0),0,1000000);
        BigDecimal budget=decimal(input.get("dailyBudget")),reserve=decimal(input.getOrDefault("reservePerCall","0"));
        if(reserve==null)throw new IllegalArgumentException("预占金额不能为空");
        if(budget!=null && (!type.equals("MODEL") || reserve.signum()<=0 || reserve.compareTo(budget)>0))throw new IllegalArgumentException("模型预算须配置正的单次预占金额且不超过预算");
        String currency=ManagementRows.text(input.getOrDefault("currency","USD"),8,true);
        if(!currency.matches("[A-Z]{3}"))throw new IllegalArgumentException("货币必须是三位大写代码");
        String permission=ManagementRows.text(input.get("permissionCode"),128,false);
        if(permission!=null && !permission.isBlank() && !permission.matches("[a-zA-Z0-9_:.-]+"))throw new IllegalArgumentException("无效权限标识");
        if(!(input.getOrDefault("enabled",true) instanceof Boolean))throw new IllegalArgumentException("enabled 必须为布尔值");
        var previous=jdbc.queryForList("SELECT currency FROM ai_resource_policy WHERE tenant_id=? AND resource_type=? AND resource_key=? FOR UPDATE",caller.tenantId(),type,key);
        if(!previous.isEmpty() && !currency.equals(previous.getFirst().get("currency")))throw new ManagementConflictException("已有策略不可更改货币，避免混合预算账本");
        jdbc.update("INSERT INTO ai_resource_policy(tenant_id,resource_type,resource_key,enabled,requests_per_minute,daily_budget,reserve_per_call,currency,permission_code,modify_by) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),requests_per_minute=VALUES(requests_per_minute),daily_budget=VALUES(daily_budget),reserve_per_call=VALUES(reserve_per_call),permission_code=VALUES(permission_code),modify_by=VALUES(modify_by)",caller.tenantId(),type,key,Boolean.FALSE.equals(input.get("enabled"))?0:1,rpm,budget,reserve,currency,permission,caller.userId().toString());
    }
    private BigDecimal decimal(Object value) {
        if(value==null || value.toString().isBlank())return null;
        try {BigDecimal number=new BigDecimal(value.toString());if(number.signum()<0 || number.scale()>12 || number.precision()-number.scale()>12)throw new ArithmeticException();return number;}
        catch(Exception error){throw new IllegalArgumentException("金额必须为非负十进制数，最多十二位整数及十二位小数");}
    }
    private void requireRelease(Long id,String type,String key,InternalCallContext caller) {
        var row=owned(id,caller,true);
        if(!"APPROVED".equals(row.get("status")) || !type.equals(row.get("resource_type")) || !key.equals(row.get("resource_key")))throw new ManagementConflictException("只能发布同一资源的已批准版本");
    }
    private Map<String,Object> owned(Long id,InternalCallContext caller,boolean lock) {
        var rows=jdbc.queryForList("SELECT * FROM ai_resource_version WHERE id=? AND tenant_id=?"+(lock?" FOR UPDATE":""),ManagementRows.id(id),caller.tenantId());
        if(rows.isEmpty())throw new ManagementNotFoundException();return rows.getFirst();
    }
}

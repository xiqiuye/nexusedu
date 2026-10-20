package com.xqy.nexusedu.ai.governance.infrastructure.governance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.shared.kernel.application.port.out.AuthorizationPolicy;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.observation.AiObservationService.Scope;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

/** Authoritative, transactional admission; unlike telemetry this must fail closed. */
@Service
public class AiAdmissionService {
    @Autowired private AuthorizationPolicy authorization;
    private final InternalCallSigner signer;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public AiAdmissionService(JdbcTemplate jdbc,PlatformTransactionManager manager,@Value("${nexusedu.internal-rpc.secret}")String secret) {this.jdbc=jdbc;this.transactions=new TransactionTemplate(manager);this.signer=new InternalCallSigner(secret);}
    public void model(Scope scope,String provider,String model,String callId,Map<String,Object> price) {
        String key=provider+":"+model;
        if(scope.runId()!=null) {
            var keys=jdbc.queryForList("SELECT resource_key FROM ai_execution_release WHERE tenant_id=? AND run_id=? AND resource_type='MODEL' AND JSON_UNQUOTE(JSON_EXTRACT(config_json,'$.provider'))=? AND JSON_UNQUOTE(JSON_EXTRACT(config_json,'$.modelName'))=? ORDER BY resource_key LIMIT 1",String.class,scope.tenantId(),scope.runId(),provider,model);
            if(!keys.isEmpty())key=keys.getFirst();
        }
        for(var policy:jdbc.queryForList("SELECT permission_code FROM ai_resource_policy WHERE tenant_id=? AND resource_type='MODEL' AND resource_key IN ('*',?)",scope.tenantId(),key)) {
            Object permission=policy.get("permission_code");if(permission==null || permission.toString().isBlank())continue;
            if(scope.userId()==null || scope.sessionId()==null)throw new ManagementAccessDeniedException();
            authorization.authorize(permission.toString(),signer.sign("nexusedu-web-bff",scope.tenantId(),scope.userId(),scope.sessionId(),scope.requestId()==null?callId:scope.requestId()));
        }
        admit(scope.tenantId(),"MODEL",key,callId,price);
    }
    public void tool(String tenant,String key) {admit(tenant,"TOOL",key,null,Map.of());}
    public void prompt(String tenant,String key) {admit(tenant,"PROMPT",key,null,Map.of());}
    private void admit(String tenant,String type,String key,String callId,Map<String,Object> price) {
        transactions.executeWithoutResult(tx -> {
            for(String target:List.of("*",key)) {
                var policies=jdbc.queryForList("SELECT * FROM ai_resource_policy WHERE tenant_id=? AND resource_type=? AND resource_key=? FOR UPDATE",tenant,type,target);
                if(policies.isEmpty())continue;
                var policy=policies.getFirst();
                if(((Number)policy.get("enabled")).intValue()!=1)throw new ManagementConflictException("AI 璧勬簮宸插仠鐢細"+target);
                String minute=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),day=LocalDateTime.now().toLocalDate().toString();
                int rpm=((Number)policy.get("requests_per_minute")).intValue();
                if(rpm>0) {
                    ensureWindow(tenant,type,target,minute);
                    long count=jdbc.queryForObject("SELECT request_count FROM ai_resource_usage WHERE tenant_id=? AND resource_type=? AND resource_key=? AND window_key=? FOR UPDATE",Long.class,tenant,type,target,minute);
                    if(count>=rpm)throw new ManagementConflictException("AI 璇锋眰棰戠巼宸茶揪涓婇檺锛岃绋嶅悗閲嶈瘯");
                    jdbc.update("UPDATE ai_resource_usage SET request_count=request_count+1 WHERE tenant_id=? AND resource_type=? AND resource_key=? AND window_key=?",tenant,type,target,minute);
                }
                BigDecimal budget=(BigDecimal)policy.get("daily_budget");
                if(budget!=null && callId!=null) {
                    if(price.get("input_price")==null || price.get("output_price")==null || !policy.get("currency").equals(price.get("currency")))throw new ManagementConflictException("棰勭畻鎺у埗闇€瑕佹湁鏁堜笖甯佺涓€鑷寸殑妯″瀷浠锋牸");
                    BigDecimal reserve=(BigDecimal)policy.get("reserve_per_call");
                    if(reserve.signum()<=0)throw new ManagementConflictException("棰勭畻棰勫崰閰嶇疆鏃犳晥");
                    ensureWindow(tenant,type,target,day);
                    BigDecimal charged=jdbc.queryForObject("SELECT charged_cost FROM ai_resource_usage WHERE tenant_id=? AND resource_type=? AND resource_key=? AND window_key=? FOR UPDATE",BigDecimal.class,tenant,type,target,day);
                    if(charged.add(reserve).compareTo(budget)>0)throw new ManagementConflictException("AI 褰撴棩棰勭畻涓嶈冻");
                    jdbc.update("UPDATE ai_resource_usage SET charged_cost=charged_cost+? WHERE tenant_id=? AND resource_type=? AND resource_key=? AND window_key=?",reserve,tenant,type,target,day);
                    jdbc.update("INSERT INTO ai_cost_reservation(call_id,tenant_id,resource_key,window_key,currency,reserved_cost) VALUES(?,?,?,?,?,?)",callId,tenant,target,day,policy.get("currency"),reserve);
                }
            }
        });
    }
    private void ensureWindow(String tenant,String type,String key,String window) {
        jdbc.update("INSERT INTO ai_resource_usage(tenant_id,resource_type,resource_key,window_key) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE window_key=window_key",tenant,type,key,window);
    }
    public void settle(String callId,BigDecimal amount,String currency) {
        if(amount==null || currency==null)return;
        transactions.executeWithoutResult(tx->{
            var reservations=jdbc.queryForList("SELECT * FROM ai_cost_reservation WHERE call_id=? AND settled=0 ORDER BY resource_key FOR UPDATE",callId);
            for(var row:reservations) {
                if(!currency.equals(row.get("currency")))continue;
                BigDecimal delta=amount.subtract((BigDecimal)row.get("reserved_cost"));
                jdbc.update("UPDATE ai_resource_usage SET charged_cost=charged_cost+? WHERE tenant_id=? AND resource_type='MODEL' AND resource_key=? AND window_key=?",delta,row.get("tenant_id"),row.get("resource_key"),row.get("window_key"));
                jdbc.update("UPDATE ai_cost_reservation SET settled=1 WHERE call_id=? AND resource_key=?",callId,row.get("resource_key"));
            }
        });
    }
}


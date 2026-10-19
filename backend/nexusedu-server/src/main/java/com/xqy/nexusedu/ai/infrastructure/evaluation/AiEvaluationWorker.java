package com.xqy.nexusedu.ai.infrastructure.evaluation;

import cn.hutool.json.JSONUtil;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.ai.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.application.port.in.AiEvaluationService;
import com.xqy.nexusedu.system.application.port.in.SystemManagementService;
import com.xqy.nexusedu.ai.application.service.chat.AiConversationLifecycle;
import com.xqy.nexusedu.ai.application.service.chat.ConversationApplicationService;
import com.xqy.nexusedu.ai.application.service.run.AiTaskRunService;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.security.rpc.InternalCallSigner;

/** Durable queue: claims/snapshots commit before submitting an idempotent workflow. */
@Service
public class AiEvaluationWorker {
    @Autowired private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final InternalCallSigner signer;
    private final AiEvaluationService evaluations;
    private final AiConversationLifecycle lifecycle;
    private final AiTaskRunService runs;
    private final ConversationApplicationService workflow;
    @Value("${nexusedu.ai.evaluation.case-timeout-ms:300000}") private long caseTimeoutMs=300000;
    public AiEvaluationWorker(JdbcTemplate jdbc,PlatformTransactionManager manager,AiEvaluationService evaluations,AiConversationLifecycle lifecycle,AiTaskRunService runs,ConversationApplicationService workflow,@Value("${nexusedu.internal-rpc.secret}")String secret) {
        this.jdbc=jdbc;this.transactions=new TransactionTemplate(manager);this.evaluations=evaluations;this.lifecycle=lifecycle;this.runs=runs;this.workflow=workflow;this.signer=new InternalCallSigner(secret);
    }
    private InternalCallContext caller(Map<String,Object> batch) {return signer.sign("nexusedu-web-bff",batch.get("tenant_id").toString(),Long.valueOf(batch.get("create_by").toString()),((Number)batch.get("session_id")).longValue(),UUID.randomUUID().toString());}
    @Scheduled(fixedDelayString="${nexusedu.ai.evaluation.poll-ms:5000}")
    public void tick() {
        schedules();
        for(int i=0;i<2;i++) {
            Long[] claimedId={null};
            Map<String,Object> claimed;
            try { claimed=transactions.execute(tx->{
                var rows=jdbc.queryForList("SELECT e.* FROM ai_eval_result e WHERE e.status='PENDING' ORDER BY e.create_time,e.id LIMIT 1 FOR UPDATE SKIP LOCKED");
                if(rows.isEmpty())return null;
                var item=rows.getFirst();var batch=jdbc.queryForMap("SELECT * FROM ai_eval_batch WHERE id=?",item.get("batch_id"));
                claimedId[0]=((Number)item.get("id")).longValue();
                    var caller=caller(batch);authorization.authorize("ai:evaluation:run",caller);
                    var sample=AiConfiguration.object(item.get("case_snapshot"));
                    var conversation=lifecycle.create("评测："+sample.get("caseKey"),caller);
                    var context=AiExecutionContext.create(Long.valueOf(conversation.get("id").toString()),sample.get("question").toString(),"eval:"+item.get("id"),caller.tenantId(),caller.userId(),caller.sessionId());
                    runs.createOrReuse(context,batch.get("model_alias").toString());
                    jdbc.update("UPDATE ai_task_run SET run_type='EVALUATION' WHERE run_id=?",context.getRunId());
                    if(batch.get("release_snapshot")!=null)for(Object release:(List<?>)AiConfiguration.json(batch.get("release_snapshot").toString())) {
                        var pinned=AiConfiguration.object(release);
                        jdbc.update("INSERT INTO ai_execution_release(tenant_id,run_id,resource_type,resource_key,version_id,config_json) VALUES(?,?,?,?,?,?)",context.getTenantId(),context.getRunId(),pinned.get("resourceType"),pinned.get("resourceKey"),pinned.get("versionId"),JSONUtil.toJsonStr(pinned.get("config")));
                    }
                    jdbc.update("UPDATE ai_eval_result SET status='RUNNING',execution_context=?,run_id=?,conversation_id=? WHERE id=?",JSONUtil.toJsonStr(context),context.getRunId(),context.getConversationId(),item.get("id"));
                    jdbc.update("UPDATE ai_eval_batch SET status='RUNNING' WHERE id=? AND status='PENDING'",batch.get("id"));
                    return item;
            }); }catch(Exception error) {
                if(claimedId[0]!=null)jdbc.update("UPDATE ai_eval_result SET status='FAILED',plan_correct=0,tool_correct=0,error_message=? WHERE id=? AND status='PENDING'","评测启动失败："+error.getClass().getSimpleName(),claimedId[0]);
                continue;
            }
            if(claimed==null)break;
        }
        // Claiming the underlying run is atomic, so replicas and crash retries cannot execute it twice.
        for(var item:jdbc.queryForList("SELECT e.*,b.model_alias FROM ai_eval_result e JOIN ai_eval_batch b ON b.id=e.batch_id WHERE e.status='RUNNING' ORDER BY e.create_time LIMIT 200")) {
            var run=jdbc.queryForMap("SELECT status,plan_json,start_time FROM ai_task_run WHERE run_id=? AND tenant_id=?",item.get("run_id"),item.get("tenant_id"));
            java.time.LocalDateTime started=run.get("start_time") instanceof java.sql.Timestamp timestamp?timestamp.toLocalDateTime():(java.time.LocalDateTime)run.get("start_time");
            if(!Set.of("SUCCEEDED","FAILED","CANCELLED").contains(run.get("status")) && started.plusNanos(caseTimeoutMs*1000000).isBefore(java.time.LocalDateTime.now())) {
                workflow.cancel(JSONUtil.toBean(item.get("execution_context").toString(),AiExecutionContext.class));
                continue;
            }
            if("PENDING".equals(run.get("status"))) {
                AiExecutionContext context=JSONUtil.toBean(item.get("execution_context").toString(),AiExecutionContext.class);
                workflow.start(context,item.get("model_alias").toString());
            } else if(Set.of("SUCCEEDED","FAILED","CANCELLED").contains(run.get("status")))score(item,run);
        }
        jdbc.update("UPDATE ai_eval_batch b SET b.status=IF(EXISTS(SELECT 1 FROM ai_eval_result e WHERE e.batch_id=b.id AND e.status='FAILED'),'FAILED','SUCCEEDED'),b.finish_time=CURRENT_TIMESTAMP WHERE b.status IN ('PENDING','RUNNING') AND NOT EXISTS(SELECT 1 FROM ai_eval_result e WHERE e.batch_id=b.id AND e.status IN ('PENDING','RUNNING'))");
    }
    @SuppressWarnings("unchecked")
    private void score(Map<String,Object> item,Map<String,Object> run) {
        var sample=AiConfiguration.object(item.get("case_snapshot"));String tenant=item.get("tenant_id").toString(),runId=item.get("run_id").toString();
        var answers=jdbc.queryForList("SELECT content FROM ai_chat_message WHERE tenant_id=? AND run_id=? AND role='assistant'",String.class,tenant,runId);
        String answer=answers.isEmpty()?null:answers.getFirst();
        var sourceJson=jdbc.queryForList("SELECT sources_json FROM ai_run_sources WHERE tenant_id=? AND run_id=?",String.class,tenant,runId);
        List<Map<String,Object>> sources=sourceJson.isEmpty()?List.of():(List<Map<String,Object>>)(Object)JSONUtil.toList(sourceJson.getFirst(),Map.class);
        var score=AiEvaluationScorer.score(sample,Objects.toString(run.get("plan_json"),"[]"),answer,sources);
        var costs=jdbc.queryForList("SELECT currency,SUM(estimated_cost) AS amount FROM ai_model_call_log WHERE tenant_id=? AND run_id=? AND estimated_cost IS NOT NULL GROUP BY currency",tenant,runId).stream().map(r->Map.of("currency",r.get("currency"),"amount",r.get("amount").toString())).toList();
        Integer unknown=jdbc.queryForObject("SELECT COUNT(*) FROM ai_model_call_log WHERE tenant_id=? AND run_id=? AND estimated_cost IS NULL",Integer.class,tenant,runId);
        jdbc.update("UPDATE ai_eval_result SET status=?,plan_correct=?,tool_correct=?,rag_hit=?,cited_count=?,correct_cited_count=?,refused=?,refusal_correct=?,costs_json=?,unknown_cost_calls=?,answer=?,error_message=? WHERE id=? AND status='RUNNING'",run.get("status").equals("SUCCEEDED")?"SUCCEEDED":"FAILED",score.planCorrect(),score.toolCorrect(),score.ragHit(),score.citedCount(),score.correctCitedCount(),score.refused(),score.refusalCorrect(),JSONUtil.toJsonStr(costs),unknown,answer,run.get("status").equals("SUCCEEDED")?null:"执行未成功："+run.get("status"),item.get("id"));
    }
    private void schedules() {
        var row=transactions.execute(tx->{
            var rows=jdbc.queryForList("SELECT * FROM ai_eval_schedule WHERE enabled=1 AND next_run_at<=CURRENT_TIMESTAMP ORDER BY next_run_at LIMIT 1 FOR UPDATE SKIP LOCKED");
            if(rows.isEmpty())return null;var schedule=rows.getFirst();
            jdbc.update("UPDATE ai_eval_schedule SET next_run_at=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL interval_hours HOUR) WHERE tenant_id=? AND dataset=?",schedule.get("tenant_id"),schedule.get("dataset"));return schedule;
        });
        if(row==null)return;
        try {
            var context=signer.sign("nexusedu-web-bff",row.get("tenant_id").toString(),((Number)row.get("user_id")).longValue(),((Number)row.get("session_id")).longValue(),UUID.randomUUID().toString());
            evaluations.run(Map.of("dataset",row.get("dataset")),context);
        }catch(Exception error) {
            jdbc.update("UPDATE ai_eval_schedule SET enabled=0,last_error=? WHERE tenant_id=? AND dataset=?","计划已暂停："+error.getClass().getSimpleName()+"。请重新登录检查权限并保存计划。",row.get("tenant_id"),row.get("dataset"));
        }
    }
}

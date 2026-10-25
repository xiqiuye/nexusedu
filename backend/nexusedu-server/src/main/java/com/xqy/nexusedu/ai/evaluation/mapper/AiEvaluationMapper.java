package com.xqy.nexusedu.ai.evaluation.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiEvaluationMapper {
    long count(@Param("resource") String resource, @Param("tenantId") String tenantId, @Param("dataset") String dataset, @Param("batchId") Long batchId);
    List<Map<String,Object>> list(@Param("resource") String resource, @Param("tenantId") String tenantId, @Param("dataset") String dataset, @Param("batchId") Long batchId, @Param("limit") int limit, @Param("offset") long offset);
    List<Map<String,Object>> releasesForRun(@Param("tenantId") String tenantId, @Param("runId") String runId);
    int insertCase(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("dataset") String dataset, @Param("caseKey") String caseKey, @Param("version") int version, @Param("question") String question, @Param("plan") String plan, @Param("tools") String tools, @Param("sources") String sources, @Param("refusal") int refusal, @Param("createBy") String createBy);
    int updateCaseEnabled(@Param("id") long id, @Param("tenantId") String tenantId, @Param("enabled") int enabled);
    List<Map<String,Object>> latestCases(@Param("tenantId") String tenantId, @Param("dataset") String dataset);
    int insertBatch(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("dataset") String dataset, @Param("alias") String alias, @Param("model") Long model, @Param("prompt") Long prompt, @Param("snapshot") String snapshot, @Param("createBy") String createBy, @Param("sessionId") Long sessionId);
    int insertResult(@Param("id") Long id, @Param("batchId") Long batchId, @Param("tenantId") String tenantId, @Param("caseId") Object caseId, @Param("snapshot") String snapshot);
    long validVersion(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key);
    Map<String,Object> resourceVersion(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key);
    long batchExists(@Param("id") long id, @Param("tenantId") String tenantId);
    Map<String,Object> summary(@Param("id") long id, @Param("tenantId") String tenantId);
    long unknownCosts(@Param("id") long id, @Param("tenantId") String tenantId);
    List<Map<String,Object>> costs(@Param("id") long id, @Param("tenantId") String tenantId);
    List<Map<String,Object>> releaseSummary(@Param("id") long id, @Param("tenantId") String tenantId);
    int upsertSchedule(@Param("tenantId") String tenantId, @Param("dataset") String dataset, @Param("hours") int hours, @Param("enabled") int enabled, @Param("userId") Long userId, @Param("sessionId") Long sessionId);
    Map<String,Object> claimPendingResult();
    Map<String,Object> batch(@Param("id") Object id);
    int markTaskRunEvaluation(@Param("runId") String runId);
    int insertRelease(@Param("tenantId") String tenantId, @Param("runId") String runId, @Param("type") Object type, @Param("key") Object key, @Param("version") Object version, @Param("config") String config);
    int markResultRunning(@Param("id") Object id, @Param("context") String context, @Param("runId") String runId, @Param("conversationId") Long conversationId);
    int markBatchRunning(@Param("id") Object id);
    int markPendingFailed(@Param("id") Long id, @Param("message") String message);
    List<Map<String,Object>> runningResults();
    Map<String,Object> run(@Param("runId") Object runId, @Param("tenantId") Object tenantId);
    int updateBatchStatuses();
    List<String> assistantAnswers(@Param("tenantId") String tenantId, @Param("runId") String runId);
    List<String> sourceJson(@Param("tenantId") String tenantId, @Param("runId") String runId);
    List<Map<String,Object>> modelCosts(@Param("tenantId") String tenantId, @Param("runId") String runId);
    long unknownModelCosts(@Param("tenantId") String tenantId, @Param("runId") String runId);
    int updateResult(@Param("status") Object status, @Param("plan") int plan, @Param("tools") int tools, @Param("rag") Integer rag, @Param("cited") int cited, @Param("correct") int correct, @Param("refused") Boolean refused, @Param("refusalCorrect") Boolean refusalCorrect, @Param("costs") String costs, @Param("unknown") long unknown, @Param("answer") String answer, @Param("error") String error, @Param("id") Object id);
    Map<String,Object> claimSchedule();
    int advanceSchedule(@Param("tenantId") Object tenantId, @Param("dataset") Object dataset);
    int pauseSchedule(@Param("message") String message, @Param("tenantId") Object tenantId, @Param("dataset") Object dataset);
}

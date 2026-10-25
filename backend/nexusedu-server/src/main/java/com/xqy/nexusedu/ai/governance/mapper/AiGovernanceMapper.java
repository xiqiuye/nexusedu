package com.xqy.nexusedu.ai.governance.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Database access for AI governance resources and execution release snapshots. */
@Mapper
public interface AiGovernanceMapper {
    long countResources(@Param("resource") String resource, @Param("tenantId") String tenantId,
                        @Param("type") String type, @Param("key") String key);

    List<Map<String, Object>> listResources(@Param("resource") String resource, @Param("tenantId") String tenantId,
                                            @Param("type") String type, @Param("key") String key,
                                            @Param("limit") int limit, @Param("offset") long offset);

    int insertVersion(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("type") String type,
                      @Param("key") String key, @Param("version") String version, @Param("configJson") String configJson,
                      @Param("createBy") String createBy);

    int updatePending(@Param("id") Long id);

    int updateReview(@Param("id") Long id, @Param("status") String status, @Param("reviewedBy") String reviewedBy,
                     @Param("reviewNote") String reviewNote);

    Map<String, Object> lockVersion(@Param("id") Long id, @Param("tenantId") String tenantId);

    int upsertRollout(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key,
                      @Param("stable") Long stable, @Param("candidate") Long candidate, @Param("percent") int percent,
                      @Param("modifyBy") String modifyBy);

    String selectPolicyCurrency(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key);

    int upsertPolicy(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key,
                     @Param("enabled") int enabled, @Param("rpm") int rpm, @Param("budget") BigDecimal budget,
                     @Param("reserve") BigDecimal reserve, @Param("currency") String currency,
                     @Param("permission") String permission, @Param("modifyBy") String modifyBy);

    List<Map<String, Object>> executionRelease(@Param("tenantId") String tenantId, @Param("runId") String runId,
                                               @Param("type") String type, @Param("key") String key);

    List<Map<String, Object>> rollout(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key);

    List<Map<String, Object>> approvedVersion(@Param("id") Object id, @Param("tenantId") String tenantId,
                                              @Param("type") String type, @Param("key") String key);

    int insertExecutionRelease(@Param("tenantId") String tenantId, @Param("runId") String runId,
                               @Param("type") String type, @Param("key") String key, @Param("version") long version,
                               @Param("configJson") String configJson);

    String executionReleaseConfig(@Param("tenantId") String tenantId, @Param("runId") String runId,
                                  @Param("type") String type, @Param("key") String key);

    List<Map<String, Object>> policies(@Param("tenantId") String tenantId, @Param("type") String type,
                                       @Param("key") String key);

    List<String> releasedModelKeys(@Param("tenantId") String tenantId, @Param("runId") String runId,
                                   @Param("provider") String provider, @Param("model") String model);
    List<Map<String,Object>> modelPolicies(@Param("tenantId") String tenantId, @Param("key") String key);
    List<Map<String,Object>> lockPolicies(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key);
    Long usageCount(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key, @Param("window") String window);
    int ensureUsage(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key, @Param("window") String window);
    int incrementUsage(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key, @Param("window") String window);
    BigDecimal chargedCost(@Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key, @Param("window") String window);
    int incrementCharged(@Param("amount") BigDecimal amount, @Param("tenantId") String tenantId, @Param("type") String type, @Param("key") String key, @Param("window") String window);
    int insertReservation(@Param("callId") String callId, @Param("tenantId") String tenantId, @Param("key") String key, @Param("window") String window, @Param("currency") String currency, @Param("amount") BigDecimal amount);
    List<Map<String,Object>> reservations(@Param("callId") String callId);
    int markSettled(@Param("callId") String callId, @Param("key") String key);
}

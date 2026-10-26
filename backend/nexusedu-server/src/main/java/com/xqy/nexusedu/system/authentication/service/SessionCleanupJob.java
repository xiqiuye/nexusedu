package com.xqy.nexusedu.system.authentication.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.system.authentication.service.SystemAuthServiceImpl;

@Component
public class SessionCleanupJob {
    private final SystemAuthServiceImpl service;

    public SessionCleanupJob(SystemAuthServiceImpl service) {
        this.service = service;
    }

    @Scheduled(cron = "${nexusedu.system.session-cleanup-cron:0 */10 * * * *}")
    public void clearExpiredSessions() {
        service.clearExpiredSessions();
    }
}

package com.xqy.nexusedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.xqy.nexusedu.ai.chat.mapper",
        "com.xqy.nexusedu.ai.evaluation.mapper",
        "com.xqy.nexusedu.ai.governance.mapper",
        "com.xqy.nexusedu.ai.model.mapper",
        "com.xqy.nexusedu.ai.operations.mapper",
        "com.xqy.nexusedu.ai.task.mapper",
        "com.xqy.nexusedu.ai.tools.mapper",
        "com.xqy.nexusedu.knowledge.catalog.mapper",
        "com.xqy.nexusedu.knowledge.ingestion.mapper",
        "com.xqy.nexusedu.student.profile.mapper",
        "com.xqy.nexusedu.system.administration.mapper",
        "com.xqy.nexusedu.system.authentication.mapper"
})
public class NexusEduApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusEduApplication.class, args);
    }
}

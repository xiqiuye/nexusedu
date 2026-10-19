package com.xqy.nexusedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.xqy.nexusedu.ai.infrastructure.persistence.mapper",
        "com.xqy.nexusedu.knowledge.infrastructure.persistence.mapper",
        "com.xqy.nexusedu.student.infrastructure.persistence.mapper",
        "com.xqy.nexusedu.system.infrastructure.persistence.mapper"
})
public class NexusEduApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusEduApplication.class, args);
    }
}

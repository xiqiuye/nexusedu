package com.xqy.nexusedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.xqy.nexusedu.infrastructure.ai.persistence.mapper",
        "com.xqy.nexusedu.infrastructure.knowledge.persistence.mapper",
        "com.xqy.nexusedu.infrastructure.student.persistence.mapper",
        "com.xqy.nexusedu.infrastructure.system.persistence.mapper"
})
public class NexusEduApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusEduApplication.class, args);
    }
}

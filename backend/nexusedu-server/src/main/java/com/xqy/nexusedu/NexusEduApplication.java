package com.xqy.nexusedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.xqy.nexusedu.ai",
        "com.xqy.nexusedu.knowledge",
        "com.xqy.nexusedu.student",
        "com.xqy.nexusedu.system"
})
public class NexusEduApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusEduApplication.class, args);
    }
}

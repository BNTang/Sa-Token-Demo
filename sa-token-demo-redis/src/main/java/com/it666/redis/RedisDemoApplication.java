package com.it666.redis;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisDemoApplication.class, args);
        System.out.println("Started: http://127.0.0.1:8085");
        System.out.println("Sa-Token config: " + SaManager.getConfig());
    }
}

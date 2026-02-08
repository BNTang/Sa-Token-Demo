package com.it666.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sa-Token 注解鉴权演示应用
 *
 * @author 程序员NEO
 */
@SpringBootApplication
public class AnnotationApplication {

    private static final Logger log = LoggerFactory.getLogger(AnnotationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AnnotationApplication.class, args);
        log.info("""
                ==========================================
                  Sa-Token 注解鉴权演示启动成功！
                  访问 http://localhost:8083 测试接口
                ==========================================
                """);
    }
}

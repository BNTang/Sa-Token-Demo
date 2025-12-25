package com.it666;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author BNTang
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("启动成功：http://127.0.0.1:8080");
        System.out.println("启动成功，Sa-Token 配置如下：" + SaManager.getConfig());
    }

}

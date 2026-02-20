package com.it666.session;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sa-Token Session 会话演示项目
 * <p>
 * 演示三种 Session 类型：
 * 1. Account-Session（账号会话）- 与账号id绑定，一个账号只有一个
 * 2. Token-Session（令牌会话）- 与token绑定，每次登录创建一个
 * 3. Custom-Session（自定义会话）- 自定义ID，灵活使用
 *
 * @author BNTang
 */
@SpringBootApplication
public class SessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionApplication.class, args);
        System.out.println("启动成功：http://127.0.0.1:8081");
        System.out.println("Sa-Token 配置如下：" + SaManager.getConfig());
    }

}

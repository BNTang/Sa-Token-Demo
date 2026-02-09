package com.it666.interceptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sa-Token 拦断器路由鉴权演示项目启动类
 * <p>
 * 演示功能：
 * 1. 路由匹配与排除规则
 * 2. 登录校验
 * 3. 角色校验
 * 4. 权限校验
 * 5. 连缀写法
 *
 * @author 程序员NEO
 */
@SpringBootApplication
public class InterceptorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterceptorApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("Sa-Token 拦断器路由鉴权演示启动成功！");
        System.out.println("访问地址: http://localhost:8083");
        System.out.println("========================================\n");
    }
}

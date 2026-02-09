package com.it666.interceptor.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦断器配置类
 * <p>
 * 演示知识点：
 * 1. 路由匹配规则 (match)
 * 2. 路由排除规则 (notMatch)
 * 3. 登录校验 (checkLogin)
 * 4. 角色校验 (checkRoleOr)
 * 5. 权限校验 (checkPermission)
 * 6. 连缀写法
 *
 * @author 程序员NEO
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(SaTokenConfigure.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦断器，定义详细认证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
            // ========== 1. 基础登录校验 ==========
            SaRouter
                    .match("/**")              // 拦截的 path 列表，可以写多个
                    .notMatch(                // 排除掉的 path 列表，可以写多个
                            "/auth/doLogin",
                            "/auth/register",
                            "/favicon.ico",
                            "/error"
                    )
                    .check(r -> StpUtil.checkLogin());  // 要执行的校验动作，可以写完整的 lambda 表达式

            // ========== 2. 角色校验 - 根据路由划分模块，不同模块需要不同角色 ==========
            // 开头的路由，必须具备 admin 角色或者 super-admin 角色才可以通过认证
            SaRouter.match("/admin/**", r -> StpUtil.checkRoleOr("admin", "super-admin"));

            // ========== 3. 权限校验 - 不同模块校验不同权限 ==========
            SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
            SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
            SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));
            SaRouter.match("/orders/**", r -> StpUtil.checkPermission("orders"));
            SaRouter.match("/notice/**", r -> StpUtil.checkPermission("notice"));
            SaRouter.match("/comment/**", r -> StpUtil.checkPermission("comment"));

            // ========== 4. 自定义逻辑 - 连缀写法演示 ==========
            // 方式一：使用 lambda 表达式
            SaRouter.match("/**", r -> logger.debug("Sa-Token 访问日志"));

            // 方式二：连缀写法 (推荐)
            SaRouter
                    .match("/**")
                    .check(r -> logger.debug("Sa-Token 连缀写法演示"));

        })).addPathPatterns("/**");
    }
}

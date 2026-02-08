package com.it666.annotation.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 注解鉴权配置类
 * <p>
 * 注册 Sa-Token 拦截器，开启注解鉴权功能
 * <p>
 * 注意：不注册拦截器的话，所有注解都不会生效！
 *
 * @author 程序员NEO
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 Sa-Token 注解鉴权
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }
}

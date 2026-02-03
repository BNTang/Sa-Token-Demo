package com.it666.kickout;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 踢人下线功能演示 - 启动类
 *
 * 文章参考：用户投诉账号异常登录，CTO 让我 5 分钟内解决
 *
 * @author BNTang
 */
@SpringBootApplication
public class KickoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(KickoutApplication.class, args);
        System.out.println("""

                ==========================================
                  踢人下线功能演示启动成功！
                  访问地址：http://127.0.0.1:8081
                ==========================================

                文章：用户投诉账号异常登录，CTO 让我 5 分钟内解决

                核心功能：
                  1. 强制注销 (logout) - 直接销毁用户的登录态
                  2. 踢人下线 (kickout) - 不删除 Token，打上「被踢」标记
                  3. 顶人下线 (replaced) - 新设备登录把旧设备顶掉

                Sa-Token 配置如下：""" + SaManager.getConfig());
    }
}

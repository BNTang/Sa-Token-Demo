package com.it666.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author BNTang
 */
@RestController
@RequestMapping("/acc/")
public class LoginController {

    // 1. 登录接口
    // 测试链接：http://localhost:8080/acc/doLogin?name=zhang&pwd=123456
    @RequestMapping("doLogin")
    public SaResult doLogin(String name, String pwd) {
        // 模拟数据库比对
        if ("zhang".equals(name) && "123456".equals(pwd)) {
            // 关键点：一行代码登录
            StpUtil.login(10001);
            return SaResult.ok("登录成功");
        }
        return SaResult.error("登录失败");
    }

    // 2. 查询登录状态
    // 测试链接：http://localhost:8080/acc/isLogin
    @RequestMapping("isLogin")
    public SaResult isLogin() {
        return SaResult.ok("是否登录：" + StpUtil.isLogin());
    }

    // 3. 查询 Token 详情
    // 测试链接：http://localhost:8080/acc/tokenInfo
    @RequestMapping("tokenInfo")
    public SaResult tokenInfo() {
        return SaResult.data(StpUtil.getTokenInfo());
    }

    // 4. 注销
    // 测试链接：http://localhost:8080/acc/logout
    @RequestMapping("logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok();
    }
}

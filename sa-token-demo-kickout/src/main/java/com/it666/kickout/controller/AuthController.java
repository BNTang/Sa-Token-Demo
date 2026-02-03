package com.it666.kickout.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 模拟用户登录
 *
 * @author BNTang
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 模拟用户数据库
    private static final Map<String, UserInfo> USER_DATABASE = new HashMap<>();

    static {
        USER_DATABASE.put("admin", new UserInfo(10001L, "admin", "123456"));
        USER_DATABASE.put("zhangsan", new UserInfo(10002L, "zhangsan", "123456"));
        USER_DATABASE.put("lisi", new UserInfo(10003L, "lisi", "123456"));
    }

    /**
     * 用户登录
     *
     * 测试：
     * curl http://localhost:8081/api/auth/login?username=admin&password=123456
     * curl http://localhost:8081/api/auth/login?username=zhangsan&password=123456
     */
    @PostMapping("/login")
    public SaResult login(@RequestParam String username, @RequestParam String password,
                          @RequestParam(defaultValue = "default") String device) {
        // 模拟数据库比对
        UserInfo user = USER_DATABASE.get(username);
        if (user == null) {
            return SaResult.error("用户不存在");
        }

        if (!user.password.equals(password)) {
            return SaResult.error("密码错误");
        }

        // 登录成功，使用指定设备标识
        StpUtil.login(user.userId, device);

        // 返回登录信息和 Token
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.userId);
        result.put("username", user.username);
        result.put("device", device);
        result.put("tokenName", StpUtil.getTokenName());
        result.put("tokenValue", StpUtil.getTokenValue());
        result.put("message", "登录成功");

        return SaResult.data(result);
    }

    /**
     * 检查登录状态
     *
     * 测试：
     * curl http://localhost:8081/api/auth/check
     */
    @GetMapping("/check")
    public SaResult checkLogin() {
        boolean isLogin = StpUtil.isLogin();

        Map<String, Object> result = new HashMap<>();
        result.put("isLogin", isLogin);

        if (isLogin) {
            result.put("userId", StpUtil.getLoginId());
            result.put("tokenValue", StpUtil.getTokenValue());
            result.put("tokenInfo", StpUtil.getTokenInfo());
        } else {
            result.put("message", "未登录");
        }

        return SaResult.data(result);
    }

    /**
     * 获取当前登录用户信息
     *
     * 测试：
     * curl http://localhost:8081/api/auth/info
     */
    @GetMapping("/info")
    public SaResult getUserInfo() {
        if (!StpUtil.isLogin()) {
            return SaResult.error("未登录");
        }

        Object loginId = StpUtil.getLoginId();
        Long userId = Long.valueOf(loginId.toString());

        UserInfo user = USER_DATABASE.values().stream()
                .filter(u -> u.userId.equals(userId))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return SaResult.error("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.userId);
        result.put("username", user.username);
        result.put("tokenValue", StpUtil.getTokenValue());
        result.put("tokenInfo", StpUtil.getTokenInfo());

        return SaResult.data(result);
    }

    /**
     * 用户主动注销
     *
     * 测试：
     * curl http://localhost:8081/api/auth/logout
     */
    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("注销成功");
    }

    /**
     * 模拟多设备登录
     * 用于测试同一账号在不同设备登录的场景
     *
     * 测试：
     * curl http://localhost:8081/api/auth/login/multi?userId=10001
     */
    @PostMapping("/login/multi")
    public SaResult loginMultiDevice(@RequestParam Long userId) {
        // 模拟同一账号在多个设备登录
        String[] devices = {"PC", "Mobile", "Tablet"};
        Map<String, String> tokens = new HashMap<>();

        for (String device : devices) {
            StpUtil.login(userId, device);
            tokens.put(device, StpUtil.getTokenValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("message", "已在多个设备登录");
        result.put("devices", tokens);

        return SaResult.data(result);
    }

    /**
     * 用户信息类
     */
    private static class UserInfo {
        Long userId;
        String username;
        String password;

        public UserInfo(Long userId, String username, String password) {
            this.userId = userId;
            this.username = username;
            this.password = password;
        }
    }
}

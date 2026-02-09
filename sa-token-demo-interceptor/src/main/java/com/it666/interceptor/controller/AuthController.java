package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 演示登录、登出、获取当前用户信息等功能
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 模拟用户数据库
     * <p>
     * key: username, value: 用户信息（包含角色和权限）
     */
    private static final Map<String, UserInfo> USER_DB = new HashMap<>();

    static {
        // 初始化测试用户数据

        // 普通用户 - 只有 user 权限
        UserInfo user = new UserInfo();
        user.setUsername("user");
        user.setPassword("123456");
        user.setRole("user");
        user.setPermissions(new String[]{"user"});
        USER_DB.put("user", user);

        // 管理员 - 有 admin 角色和 admin、user 权限
        UserInfo admin = new UserInfo();
        admin.setUsername("admin");
        admin.setPassword("123456");
        admin.setRole("admin");
        admin.setPermissions(new String[]{"admin", "user", "goods", "orders"});
        USER_DB.put("admin", admin);

        // 超级管理员 - 有 super-admin 角色和所有权限
        UserInfo superAdmin = new UserInfo();
        superAdmin.setUsername("super-admin");
        superAdmin.setPassword("123456");
        superAdmin.setRole("super-admin");
        superAdmin.setPermissions(new String[]{"admin", "user", "goods", "orders", "notice", "comment"});
        USER_DB.put("super-admin", superAdmin);

        // 商品管理员 - 有 admin 角色和 goods 权限
        UserInfo goodsAdmin = new UserInfo();
        goodsAdmin.setUsername("goods-admin");
        goodsAdmin.setPassword("123456");
        goodsAdmin.setRole("admin");
        goodsAdmin.setPermissions(new String[]{"goods"});
        USER_DB.put("goods-admin", goodsAdmin);
    }

    /**
     * 用户登录接口
     * <p>
     * 演示 Sa-Token 的登录功能
     *
     * @param loginReq 登录请求参数
     * @return 登录结果，包含 token
     */
    @PostMapping("/doLogin")
    public SaResult doLogin(@RequestBody LoginRequest loginReq) {
        // 1. 校验用户名和密码
        UserInfo userInfo = USER_DB.get(loginReq.getUsername());
        if (userInfo == null) {
            return SaResult.error("用户不存在");
        }
        if (!userInfo.getPassword().equals(loginReq.getPassword())) {
            return SaResult.error("密码错误");
        }

        // 2. 执行登录
        // 参数1：登录账号的 id，建议使用 *Long* 类型（数据库主键）
        // 参数2：登录账号的类型，用于多账号体系（可选）
        StpUtil.login(userInfo.getUsername());

        // 3. 将用户信息（角色、权限）存入 Session，供后续鉴权使用
        StpUtil.getSession().set("role", userInfo.getRole());
        StpUtil.getSession().set("permissions", userInfo.getPermissions());

        // 4. 返回 token
        String token = StpUtil.getTokenValue();
        return SaResult.ok("登录成功")
                .set("token", token)
                .set("username", userInfo.getUsername())
                .set("role", userInfo.getRole())
                .set("permissions", userInfo.getPermissions());
    }

    /**
     * 用户注册接口（模拟）
     * <p>
     * 此接口不需要登录校验，在 SaTokenConfigure 中已排除
     *
     * @param registerReq 注册请求参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public SaResult register(@RequestBody RegisterRequest registerReq) {
        // 实际项目中应该保存到数据库
        // 这里只是演示，不做实际注册操作
        return SaResult.ok("注册成功（演示）");
    }

    /**
     * 检查当前登录状态
     *
     * @return 登录状态
     */
    @GetMapping("/isLogin")
    public SaResult isLogin() {
        boolean isLogin = StpUtil.isLogin();
        return SaResult.ok()
                .set("isLogin", isLogin)
                .set("tokenValue", StpUtil.getTokenValue());
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/userInfo")
    public SaResult getUserInfo() {
        String loginId = (String) StpUtil.getLoginId();
        String role = (String) StpUtil.getSession().get("role");
        String[] permissions = (String[]) StpUtil.getSession().get("permissions");

        return SaResult.ok()
                .set("loginId", loginId)
                .set("role", role)
                .set("permissions", permissions);
    }

    /**
     * 退出登录
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("退出登录成功");
    }

    // ========== 内部类 ==========

    /**
     * 登录请求参数
     */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 注册请求参数
     */
    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
    }

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        private String username;
        private String password;
        private String role;
        private String[] permissions;
    }
}

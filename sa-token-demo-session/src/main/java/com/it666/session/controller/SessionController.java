package com.it666.session.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.it666.session.dto.DeviceInfo;
import com.it666.session.dto.LoginDTO;
import com.it666.session.entity.UserInfo;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Session 会话演示控制器
 * <p>
 * 演示 Sa-Token 三种 Session 类型的使用：
 * 1. Account-Session：账号会话，与账号id绑定，StpUtil.getSession()
 * 2. Token-Session：令牌会话，与token绑定，StpUtil.getTokenSession()
 * 3. Custom-Session：自定义会话，自定义ID，SaSession customSession = new SaSession("customId");
 *
 * @author BNTang
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    // ==================== 模拟数据库用户数据 ====================
    // 模拟用户信息（实际项目应从数据库查询）
    private UserInfo getMockUser(Long userId) {
        return UserInfo.builder()
                .userId(userId)
                .username("user_" + userId)
                .nickname("用户" + userId)
                .email("user" + userId + "@example.com")
                .phone("13800138000")
                .role("admin")
                .build();
    }

    /**
     * 获取 Session 中的所有数据
     */
    private Map<String, Object> getSessionData(SaSession session) {
        Map<String, Object> data = new HashMap<>();
        // 遍历 session 中的所有 key
        for (String key : session.keys()) {
            data.put(key, session.get(key));
        }
        return data;
    }

    // ==================== 1. 登录与 Session 存储 ====================

    /**
     * 登录接口 - 演示登录后存储用户信息到 Session
     * <p>
     * 测试链接：POST http://localhost:8081/session/login
     * Body: {"username":"admin","password":"123456","deviceType":"PC"}
     */
    @PostMapping("/login")
    public SaResult login(@RequestBody LoginDTO dto) {
        // 1. 模拟校验账号密码
        if (!"123456".equals(dto.getPassword())) {
            return SaResult.error("密码错误");
        }

        // 模拟根据用户名查询用户ID
        Long userId = 10001L;

        // 2. 执行登录
        StpUtil.login(userId);

        // 3. 存储用户信息到 Account-Session（与账号绑定）
        SaSession accountSession = StpUtil.getSession();
        UserInfo userInfo = getMockUser(userId);
        accountSession.set("userInfo", userInfo);

        // 4. 存储设备信息到 Token-Session（与令牌绑定）
        SaSession tokenSession = StpUtil.getTokenSession();
        DeviceInfo deviceInfo = DeviceInfo.builder()
                .deviceType(dto.getDeviceType())
                .loginIp("127.0.0.1")
                .loginTime(System.currentTimeMillis())
                .build();
        tokenSession.set("device", deviceInfo);

        return SaResult.ok("登录成功")
                .set("token", StpUtil.getTokenValue())
                .set("userId", userId);
    }

    // ==================== 2. Account-Session 演示 ====================

    /**
     * 获取 Account-Session 中的用户信息
     * <p>
     * Account-Session：与账号ID绑定，一个账号只有一个
     * 无论在哪里登录，都能获取到同一份 Account-Session 数据
     * <p>
     * 测试链接：GET http://localhost:8081/session/account/info
     */
    @GetMapping("/account/info")
    public SaResult getAccountSessionInfo() {
        // 获取当前登录用户的 Account-Session
        SaSession session = StpUtil.getSession();

        // 获取存储的用户信息
        UserInfo userInfo = (UserInfo) session.get("userInfo");

        return SaResult.data(userInfo)
                .set("sessionId", session.getId())
                .set("sessionType", "Account-Session");
    }

    /**
     * 更新 Account-Session 数据
     * <p>
     * 测试链接：PUT http://localhost:8081/session/account/nickname?nickname=新昵称
     */
    @PutMapping("/account/nickname")
    public SaResult updateNickname(@RequestParam String nickname) {
        SaSession session = StpUtil.getSession();
        UserInfo userInfo = (UserInfo) session.get("userInfo");
        if (userInfo != null) {
            userInfo.setNickname(nickname);
            session.set("userInfo", userInfo);
        }
        return SaResult.ok("昵称更新成功").set("newNickname", nickname);
    }

    // ==================== 3. Token-Session 演示 ====================

    /**
     * 获取 Token-Session 中的设备信息
     * <p>
     * Token-Session：与 Token 绑定，每次登录创建一个新的
     * 同一账号多设备登录时，每个设备有独立的 Token-Session
     * <p>
     * 测试链接：GET http://localhost:8081/session/token/device
     */
    @GetMapping("/token/device")
    public SaResult getTokenSessionInfo() {
        // 获取当前 Token 的 Session
        SaSession tokenSession = StpUtil.getTokenSession();

        // 获取存储的设备信息
        DeviceInfo deviceInfo = (DeviceInfo) tokenSession.get("device");

        return SaResult.data(deviceInfo)
                .set("sessionId", tokenSession.getId())
                .set("sessionType", "Token-Session")
                .set("currentToken", StpUtil.getTokenValue());
    }

    /**
     * 获取当前账号所有 Token-Session（多设备登录场景）
     * <p>
     * 测试链接：GET http://localhost:8081/session/token/list
     */
    @GetMapping("/token/list")
    public SaResult getTokenSessionList() {
        // 获取当前账号所有已登录的 token 列表
        List<String> tokenList = StpUtil.getTokenValueListByLoginId(StpUtil.getLoginIdDefaultNull());
        return SaResult.data(tokenList);
    }

    // ==================== 4. Custom-Session 演示 ====================

    /**
     * Custom-Session：自定义会话
     * <p>
     * 使用场景：
     * - 存储全局配置（如系统配置）
     * - 存储共享数据（如在线用户列表）
     * - 存储临时缓存数据
     * <p>
     * 测试链接：GET http://localhost:8081/session/custom/system-config
     */
    @GetMapping("/custom/system-config")
    public SaResult getCustomSession() {
        // 创建或获取自定义 Session（使用自定义 ID）
        String customSessionId = "system-config";
        SaSession customSession = StpUtil.getSessionBySessionId(customSessionId);

        // 获取配置（如果不存在则设置默认值）
        Object config = customSession.get("theme");
        if (config == null) {
            customSession.set("theme", "dark");
            customSession.set("language", "zh-CN");
            customSession.set("timezone", "Asia/Shanghai");
            config = "dark（默认值，首次访问设置）";
        }

        return SaResult.data(getSessionData(customSession))
                .set("sessionId", customSession.getId())
                .set("sessionType", "Custom-Session");
    }

    /**
     * 更新 Custom-Session 配置
     * <p>
     * 测试链接：PUT http://localhost:8081/session/custom/theme?theme=light
     */
    @PutMapping("/custom/theme")
    public SaResult updateCustomConfig(@RequestParam String theme) {
        String customSessionId = "system-config";
        SaSession customSession = StpUtil.getSessionBySessionId(customSessionId);
        customSession.set("theme", theme);

        return SaResult.ok("主题更新成功")
                .set("theme", theme)
                .set("allConfig", getSessionData(customSession));
    }

    // ==================== 5. Session 对比演示 ====================

    /**
     * 获取所有 Session 信息对比
     * <p>
     * 测试链接：GET http://localhost:8081/session/compare
     */
    @GetMapping("/compare")
    public SaResult compareSessions() {
        SaResult result = SaResult.ok();

        // Account-Session
        if (StpUtil.isLogin()) {
            SaSession accountSession = StpUtil.getSession();
            result.set("accountSession", getSessionData(accountSession));
            result.set("accountSessionId", accountSession.getId());
        }

        // Token-Session
        if (StpUtil.isLogin()) {
            SaSession tokenSession = StpUtil.getTokenSession();
            result.set("tokenSession", getSessionData(tokenSession));
            result.set("tokenSessionId", tokenSession.getId());
        }

        // Custom-Session
        SaSession customSession = StpUtil.getSessionBySessionId("system-config");
        result.set("customSession", getSessionData(customSession));
        result.set("customSessionId", customSession.getId());

        return result;
    }

    // ==================== 6. 登出与 Session 清理 ====================

    /**
     * 注销登录
     * <p>
     * 测试链接：POST http://localhost:8081/session/logout
     */
    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("注销成功");
    }

    /**
     * 检查登录状态
     * <p>
     * 测试链接：GET http://localhost:8081/session/isLogin
     */
    @GetMapping("/isLogin")
    public SaResult isLogin() {
        return SaResult.ok()
                .set("isLogin", StpUtil.isLogin())
                .set("loginId", StpUtil.isLogin() ? StpUtil.getLoginId() : null);
    }

}

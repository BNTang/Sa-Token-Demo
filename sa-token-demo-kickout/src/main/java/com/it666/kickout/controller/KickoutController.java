package com.it666.kickout.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 踢人下线功能控制器
 *
 * 文章参考：用户投诉账号异常登录，CTO 让我 5 分钟内解决
 *
 * 核心原理：找到目标用户的 Token → 让它失效
 *
 * @author BNTang
 */
@RestController
@RequestMapping("/api/kickout")
public class KickoutController {

    // =====================================================================
    //  方式一：强制注销 (logout)
    // =====================================================================
    //
    //  直接销毁用户的登录态，Token 被删除
    //
    //  适用场景：
    //  - 用户主动申请注销账号
    //  - 后台管理员封禁某用户
    //  - 密码被修改后，强制旧会话失效
    //
    //  用户再次访问时，系统会提示：Token 无效
    //
    // =====================================================================

    /**
     * 强制指定账号注销下线
     *
     * 测试：curl http://localhost:8081/api/kickout/logout?userId=10001
     */
    @GetMapping("/logout")
    public SaResult logout(@RequestParam Long userId) {
        StpUtil.logout(userId);
        return SaResult.ok("账号 " + userId + " 已被强制注销下线（Token 已删除）");
    }

    /**
     * 强制指定账号指定端注销下线
     *
     * 测试：curl http://localhost:8081/api/kickout/logout/device?userId=10001&device=PC
     */
    @GetMapping("/logout/device")
    public SaResult logoutByDevice(@RequestParam Long userId, @RequestParam String device) {
        StpUtil.logout(userId, device);
        return SaResult.ok("账号 " + userId + " 在 [" + device + "] 端已被强制注销下线");
    }

    /**
     * 强制指定 Token 注销下线
     *
     * 测试：curl http://localhost:8081/api/kickout/logout/token?token=xxxx-xxxx-xxxx
     */
    @GetMapping("/logout/token")
    public SaResult logoutByToken(@RequestParam String token) {
        StpUtil.logoutByTokenValue(token);
        return SaResult.ok("Token [" + token + "] 已被强制注销下线");
    }

    // =====================================================================
    //  方式二：踢人下线 (kickout)
    // =====================================================================
    //
    //  不会删除 Token，而是给它打上一个「被踢」的标记
    //
    //  适用场景：
    //  - 检测到异常登录行为，临时踢出
    //  - 需要让用户知道「你被踢了」而不是「Token 过期了」
    //  - 保留登录记录用于安全审计
    //
    //  用户再次访问时，系统会提示：Token 已被踢下线
    //
    // =====================================================================

    /**
     * 将指定账号踢下线
     *
     * 测试：curl http://localhost:8081/api/kickout/kickout?userId=10001
     */
    @GetMapping("/kickout")
    public SaResult kickout(@RequestParam Long userId) {
        StpUtil.kickout(userId);
        return SaResult.ok("账号 " + userId + " 已被踢下线（Token 保留但标记为被踢）");
    }

    /**
     * 将指定账号指定端踢下线
     *
     * 测试：curl http://localhost:8081/api/kickout/kickout/device?userId=10001&device=PC
     */
    @GetMapping("/kickout/device")
    public SaResult kickoutByDevice(@RequestParam Long userId, @RequestParam String device) {
        StpUtil.kickout(userId, device);
        return SaResult.ok("账号 " + userId + " 在 [" + device + "] 端已被踢下线");
    }

    /**
     * 将指定 Token 踢下线
     *
     * 测试：curl http://localhost:8081/api/kickout/kickout/token?token=xxxx-xxxx-xxxx
     */
    @GetMapping("/kickout/token")
    public SaResult kickoutByToken(@RequestParam String token) {
        StpUtil.kickoutByTokenValue(token);
        return SaResult.ok("Token [" + token + "] 已被踢下线");
    }

    // =====================================================================
    //  方式三：顶人下线 (replaced)
    // =====================================================================
    //
    //  通常发生在登录时，当同一账号在新设备登录，框架自动把旧设备顶掉
    //
    //  适用场景：
    //  - 实现"单点登录"效果（同一账号只能一处在线）
    //  - 模拟"新设备登录，旧设备被挤下去"的行为
    //
    //  一般情况下，你不需要手动调用这个方法，框架会自动处理
    //
    // =====================================================================

    /**
     * 将指定账号顶下线
     *
     * 测试：curl http://localhost:8081/api/kickout/replaced?userId=10001
     */
    @GetMapping("/replaced")
    public SaResult replaced(@RequestParam Long userId) {
        StpUtil.replaced(userId);
        return SaResult.ok("账号 " + userId + " 已被顶下线（模拟新设备登录）");
    }

    /**
     * 将指定账号指定端顶下线
     *
     * 测试：curl http://localhost:8081/api/kickout/replaced/device?userId=10001&device=PC
     */
    @GetMapping("/replaced/device")
    public SaResult replacedByDevice(@RequestParam Long userId, @RequestParam String device) {
        StpUtil.replaced(userId, device);
        return SaResult.ok("账号 " + userId + " 在 [" + device + "] 端已被顶下线");
    }

    /**
     * 将指定 Token 顶下线
     *
     * 测试：curl http://localhost:8081/api/kickout/replaced/token?token=xxxx-xxxx-xxxx
     */
    @GetMapping("/replaced/token")
    public SaResult replacedByToken(@RequestParam String token) {
        StpUtil.replacedByTokenValue(token);
        return SaResult.ok("Token [" + token + "] 已被顶下线");
    }

    // =====================================================================
    //  实战场景演示
    // =====================================================================

    /**
     * 场景1：用户修改密码后，强制所有设备下线（使用 logout）
     *
     * 业务逻辑：用户修改密码后，为了安全起见，强制所有设备的会话失效
     *
     * 测试：curl -X POST http://localhost:8081/api/kickout/scenario/password-change?userId=10001
     */
    @PostMapping("/scenario/password-change")
    public SaResult passwordChange(@RequestParam Long userId) {
        // 模拟：用户修改密码
        // ... 密码修改逻辑 ...

        // 强制所有设备下线
        StpUtil.logout(userId);

        return SaResult.ok()
                .set("message", "密码修改成功")
                .set("action", "所有设备已强制下线，请重新登录")
                .set("method", "logout - Token 已删除");
    }

    /**
     * 场景2：检测到异地登录，踢出当前会话（使用 kickout）
     *
     * 业务逻辑：检测到用户在异常地点登录，为了安全，踢出当前会话，但保留记录用于审计
     *
     * 测试：curl -X POST http://localhost:8081/api/kickout/scenario/abnormal-login?userId=10001&location=北京
     */
    @PostMapping("/scenario/abnormal-login")
    public SaResult abnormalLogin(@RequestParam Long userId, @RequestParam String location) {
        // 模拟：检测到异地登录
        String lastLocation = "上海";  // 假设上次登录地点
        if (!location.equals(lastLocation)) {
            // 检测到异地登录，踢出当前会话，但保留记录用于审计
            StpUtil.kickout(userId);

            return SaResult.ok()
                    .set("detected", "检测到异地登录")
                    .set("lastLocation", lastLocation)
                    .set("currentLocation", location)
                    .set("action", "账号已被踢下线，请联系管理员确认")
                    .set("method", "kickout - Token 保留但标记为被踢");
        }

        return SaResult.ok("登录正常");
    }

    /**
     * 场景3：管理员封禁用户（使用 logout）
     *
     * 业务逻辑：后台管理员封禁某用户，彻底清理其所有登录态
     *
     * 测试：curl -X POST http://localhost:8081/api/kickout/scenario/ban-user?userId=10001&reason=违规操作
     */
    @PostMapping("/scenario/ban-user")
    public SaResult banUser(@RequestParam Long userId, @RequestParam String reason) {
        // 模拟：管理员封禁用户
        // ... 封禁逻辑 ...

        // 彻底清理所有登录态
        StpUtil.logout(userId);

        return SaResult.ok()
                .set("message", "用户已被封禁")
                .set("userId", userId)
                .set("reason", reason)
                .set("action", "所有登录已失效")
                .set("method", "logout - Token 已删除");
    }

    /**
     * 场景4：用户投诉账号被盗，强制踢出（使用 kickout）
     *
     * 业务逻辑：用户反馈账号被盗，需要强制踢出所有会话，但保留记录用于安全审计
     *
     * 测试：curl -X POST http://localhost:8081/api/kickout/scenario/account-hijacked?userId=10001
     */
    @PostMapping("/scenario/account-hijacked")
    public SaResult accountHijacked(@RequestParam Long userId) {
        // 模拟：用户投诉账号被盗
        // ... 验证用户身份 ...

        // 强制踢出所有会话，保留记录用于审计
        StpUtil.kickout(userId);

        return SaResult.ok()
                .set("message", "账号被盗申诉已受理")
                .set("action", "所有会话已被踢出")
                .set("advice", "请立即修改密码并开启二次验证")
                .set("method", "kickout - Token 保留用于安全审计");
    }

    // =====================================================================
    //  辅助接口
    // =====================================================================

    /**
     * 获取当前会话的 Token 信息
     * 用于测试被踢后的状态
     *
     * 测试：curl http://localhost:8081/api/kickout/token/info
     */
    @GetMapping("/token/info")
    public SaResult tokenInfo() {
        return SaResult.data(StpUtil.getTokenInfo());
    }

    /**
     * 检查当前会话是否有效
     * 用于测试被踢后的状态
     *
     * 测试：curl http://localhost:8081/api/kickout/check
     */
    @GetMapping("/check")
    public SaResult checkLogin() {
        Map<String, Object> result = new HashMap<>();
        result.put("isLogin", StpUtil.isLogin());
        result.put("tokenValue", StpUtil.getTokenValue());
        result.put("loginId", StpUtil.getLoginIdDefaultNull());
        return SaResult.data(result);
    }

    /**
     * 获取三种方法的对比说明
     *
     * 测试：curl http://localhost:8081/api/kickout/comparison
     */
    @GetMapping("/comparison")
    public SaResult comparison() {
        Map<String, Object> comparison = new HashMap<>();

        Map<String, String> logout = new HashMap<>();
        logout.put("name", "强制注销 (logout)");
        logout.put("description", "直接删除 Token");
        logout.put("userPerception", "Token 无效");
        logout.put("typicalScenario", "账号封禁、密码修改");
        comparison.put("logout", logout);

        Map<String, String> kickout = new HashMap<>();
        kickout.put("name", "踢人下线 (kickout)");
        kickout.put("description", "打标记，保留 Token");
        kickout.put("userPerception", "Token 已被踢下线");
        kickout.put("typicalScenario", "异常登录检测、安全审计");
        comparison.put("kickout", kickout);

        Map<String, String> replaced = new HashMap<>();
        replaced.put("name", "顶人下线 (replaced)");
        replaced.put("description", "新设备登录把旧设备顶掉");
        replaced.put("userPerception", "账号在别处登录");
        replaced.put("typicalScenario", "单点登录、登录限制");
        comparison.put("replaced", replaced);

        comparison.put("summary", "logout 是「你没了」，kickout 是「你被请走了」");

        return SaResult.data(comparison);
    }
}

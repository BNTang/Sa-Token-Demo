package com.it666.annotation.controller;

import org.mindrot.jbcrypt.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证辅助控制器
 * <p>
 * 提供登录、设置角色权限等测试接口，方便测试各种注解
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String PERMISSION_LIST_KEY = "permissionList";

    /**
     * 登录接口
     * <p>
     * 用户名任意，密码任意，用于模拟登录
     */
    @RequestMapping("/login")
    public SaResult login(@RequestParam(defaultValue = "user") String username) {
        // 模拟登录，实际项目应该验证用户名密
        StpUtil.login(username);
        return SaResult.data("登录成功，用户：" + username + "，Token：" + StpUtil.getTokenValue());
    }

    /**
     * 登录为管理员
     */
    @RequestMapping("/loginAdmin")
    public SaResult loginAdmin() {
        StpUtil.login("admin");
        // 设置角色
        StpUtil.getSession().set("role", "admin");
        // 设置权限列表
        List<String> permissionList = new ArrayList<>();
        permissionList.add("user.add");
        permissionList.add("user.update");
        permissionList.add("user.delete");
        permissionList.add("user.all");
        permissionList.add("system.config");
        StpUtil.getSession().set(PERMISSION_LIST_KEY, permissionList);
        return SaResult.data("管理员登录成功，Token：" + StpUtil.getTokenValue());
    }

    /**
     * 登录为普通用户（有部分权限）
     */
    @RequestMapping("/loginUser")
    public SaResult loginUser() {
        StpUtil.login("user");
        // 设置角色
        StpUtil.getSession().set("role", "user");
        // 设置权限列表
        List<String> permissionList = new ArrayList<>();
        permissionList.add("user.add");
        permissionList.add("user.update");
        StpUtil.getSession().set(PERMISSION_LIST_KEY, permissionList);
        return SaResult.data("普通用户登录成功，Token：" + StpUtil.getTokenValue());
    }

    /**
     * 登录为超级管理员
     */
    @RequestMapping("/loginSuperAdmin")
    public SaResult loginSuperAdmin() {
        StpUtil.login("super-admin");
        // 设置角色
        StpUtil.getSession().set("role", "super-admin");
        // 设置所有权限
        List<String> permissionList = new ArrayList<>();
        permissionList.add("*");
        StpUtil.getSession().set(PERMISSION_LIST_KEY, permissionList);
        return SaResult.data("超级管理员登录成功，Token：" + StpUtil.getTokenValue());
    }

    /**
     * 获取当前登录用户信息
     */
    @RequestMapping("/getInfo")
    public SaResult getInfo() {
        if (!StpUtil.isLogin()) {
            return SaResult.error("未登录");
        }
        return SaResult.ok()
                .set("loginId", StpUtil.getLoginIdAsString())
                .set("token", StpUtil.getTokenValue())
                .set("role", StpUtil.getSession().get("role"))
                .set("permissions", StpUtil.getSession().get(PERMISSION_LIST_KEY));
    }

    /**
     * 登出
     */
    @RequestMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.data("登出成功");
    }

    /**
     * 开启二级认证
     * <p>
     * 用于测试 @SaCheckSafe 注解
     * 场景：在修改密码等敏感操作前，需要先调用此接口进行二次验证
     */
    @RequestMapping("/openSafe")
    public SaResult openSafe() {
        StpUtil.openSafe(3600);
        return SaResult.data("二级认证通过，现在可以进行敏感操作了");
    }

    /**
     * 关闭二级认证
     */
    @RequestMapping("/closeSafe")
    public SaResult closeSafe() {
        StpUtil.closeSafe();
        return SaResult.data("二级认证已关闭");
    }

    /**
     * 检查二级认证状态
     */
    @RequestMapping("/checkSafe")
    public SaResult checkSafe() {
        boolean isOpen = StpUtil.isSafe("update-password");
        return SaResult.data("二级认证状态：" + (isOpen ? "已开启" : "未开启"));
    }

    /**
     * 封禁账号的指定服务
     * <p>
     * 用于测试 @SaCheckDisable 注解
     * <p>
     * 场景：用户违规后，封禁其评论、发帖等服务，但不影响其他功能
     */
    @RequestMapping("/disableService")
    public SaResult disableService(@RequestParam String service) {
        // 封禁当前用户的指定服务，永久封禁（-1表示永久）
        // 注意：Sa-Token 1.44.0 版本中，disable 方法需要指定 loginId 和服务类型及时长
        // 这里简化演示，实际使用需要根据具体业务调整
        Object loginId = StpUtil.getLoginId();
        StpUtil.disable(loginId, service, -1);
        return SaResult.data("已封禁服务：" + service);
    }

    /**
     * 解封账号的指定服务
     */
    @RequestMapping("/untieDisableService")
    public SaResult untieDisableService(@RequestParam String service) {
        StpUtil.untieDisable(service);
        return SaResult.data("已解封服务：" + service);
    }

    /**
     * 检查服务是否被封禁
     */
    @RequestMapping("/checkDisable")
    public SaResult checkDisable(@RequestParam String service) {
        boolean isDisabled = StpUtil.isDisable(service);
        return SaResult.data("服务 " + service + " 状态：" + (isDisabled ? "已封禁" : "正常"));
    }

    /**
     * 演示 BCrypt 密码加密
     * <p>
     * 使用 jBCrypt 库进行 BCrypt 加密
     */
    @RequestMapping("/encrypt")
    public SaResult encrypt(@RequestParam String password) {
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        return SaResult.data("加密后的密码：" + hashed);
    }

    /**
     * 演示 BCrypt 密码验证
     */
    @RequestMapping("/verify")
    public SaResult verify(@RequestParam String password, @RequestParam String hashed) {
        boolean matches = BCrypt.checkpw(password, hashed);
        return SaResult.data("密码验证结果：" + (matches ? "正确" : "错误"));
    }
}

package com.it666.annotation.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import cn.dev33.satoken.annotation.SaCheckHttpBasic;
import cn.dev33.satoken.annotation.SaCheckHttpDigest;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaCheckSafe;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token 基本注解演示控制器
 * <p>
 * 演示 Sa-Token 提供的常用注解：
 * - @SaCheckLogin - 登录校验
 * - @SaCheckRole - 角色校验
 * - @SaCheckPermission - 权限校验
 * - @SaCheckSafe - 二级认证校验
 * - @SaCheckHttpBasic - HttpBasic 认证
 * - @SaCheckHttpDigest - HttpDigest 认证
 * - @SaCheckDisable - 账号服务封禁校验
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/basic")
public class BasicAnnotationController {

    /**
     * 登录校验 - 只有登录后才能访问
     * 访问示例：先调用 /auth/login 登录，再访问此接口
     */
    @SaCheckLogin
    @RequestMapping("/info")
    public SaResult info() {
        return SaResult.data("查询用户信息成功，当前登录用户ID：" + StpUtil.getLoginIdAsString());
    }

    /**
     * 角色校验 - 必须具有 super-admin 角色才能访问
     * 测试方式：登录后通过 StpUtil.loginId(10001).setRole("super-admin") 设置角色
     */
    @SaCheckRole("super-admin")
    @RequestMapping("/add")
    public SaResult add() {
        return SaResult.data("用户增加成功");
    }

    /**
     * 权限校验 - 必须具有 user-add 权限才能访问
     */
    @SaCheckPermission("user-add")
    @RequestMapping("/userAdd")
    public SaResult userAdd() {
        return SaResult.data("用户增加成功");
    }

    /**
     * 二级认证校验 - 在修改密码等敏感操作前需要再次验证身份
     * 使用方式：先调用 /auth/openSafe 验证，然后再访问此接口
     */
    @SaCheckSafe
    @RequestMapping("/updatePwd")
    public SaResult updatePwd() {
        return SaResult.data("修改密码成功");
    }

    /**
     * Http Basic 认证 - 适合内部管理接口
     * 测试方式：使用 Postman，选择 Basic Auth，输入用户名 sa，密码 123456
     */
    @SaCheckHttpBasic(account = "sa:123456")
    @RequestMapping("/dashboard")
    public SaResult dashboard() {
        return SaResult.data("管理后台数据");
    }

    /**
     * Http Digest 认证 - 比Basic更安全的认证方式
     */
    @SaCheckHttpDigest(value = "sa:123456")
    @RequestMapping("/report")
    public SaResult report() {
        return SaResult.data("报表数据");
    }

    /**
     * 校验账号是否被封禁了 comment 服务
     * 如果账号的 comment 服务被封禁，则无法发表评论
     */
    @SaCheckDisable("comment")
    @RequestMapping("/send")
    public SaResult send() {
        return SaResult.data("发表评论成功");
    }
}

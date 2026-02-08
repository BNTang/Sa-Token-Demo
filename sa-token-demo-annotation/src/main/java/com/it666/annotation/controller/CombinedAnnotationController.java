package com.it666.annotation.controller;

import cn.dev33.satoken.annotation.SaCheckHttpBasic;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckOr;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaCheckSafe;
import cn.dev33.satoken.annotation.SaCheckDisable;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token 组合注解演示控制器
 * <p>
 * 演示：
 * - {@code @SaIgnore}：忽略校验（白名单）
 * - {@code @SaCheckOr}：最灵活的组合校验（满足任意条件即可）
 * - 类级别注解 + 方法级别 {@code @SaIgnore}
 *
 * @author 程序员NEO
 */
@SaCheckLogin  // 整个Controller都需要登录
@RestController
@RequestMapping("/combined")
public class CombinedAnnotationController {

    /**
     * {@code @SaIgnore} 白名单示例
     * <p>
     * 虽然 Controller 类上加了 {@code @SaCheckLogin}，但这个方法可以不登录访问
     * <p>
     * 适用场景：健康检查、公开查询、获取接口文档等
     * <p>
     * 注意：{@code @SaIgnore} 优先级最高，即使同时写了 {@code @SaCheckLogin} 和 {@code @SaIgnore}，结果也是不鉴权
     */
    @SaIgnore
    @RequestMapping("/health")
    public SaResult health() {
        return SaResult.data("系统健康检查 - 无需登录");
    }

    /**
     * 正常需要登录的接口
     */
    @RequestMapping("/normal")
    public SaResult normal() {
        return SaResult.data("正常接口 - 需要登录");
    }

    /**
     * {@code @SaCheckOr} 组合校验示例
     * <p>
     * 满足以下任意一个条件即可访问（OR 逻辑）：
     * - 已登录
     * - 具有 admin 角色
     * - 具有 user.add 权限
     * - 通过了二级认证（safe）
     * - 通过了 HttpBasic 认证
     * - 账号的 submit-orders 服务未被封禁
     * <p>
     * 这是 Sa-Token 最灵活的组合校验方式
     */
    @SaCheckOr(
            login = @SaCheckLogin,
            role = @SaCheckRole("admin"),
            safe = @SaCheckSafe("update-password"),
            httpBasic = @SaCheckHttpBasic(account = "sa:123456"),
            disable = @SaCheckDisable("submit-orders")
    )
    @RequestMapping("/test")
    public SaResult test() {
        return SaResult.data("访问成功 - 满足了多种校验方式中的任意一种");
    }

    /**
     * 多账号体系组合校验示例
     * <p>
     * 场景：项目有多套账号体系（如后台账号和用户账号分开维护）
     * 后台登录了能访问，普通用户登录了也能访问，两套账号体系任一通过即可
     * <p>
     * 注意：这里使用了 login 数组，表示多种登录类型
     */
    @SaIgnore  // 忽略类级别的 @SaCheckLogin
    @SaCheckOr(
            login = {@SaCheckLogin(type = "login"), @SaCheckLogin(type = "user")}
    )
    @RequestMapping("/multiAccount")
    public SaResult multiAccount() {
        return SaResult.data("访问成功 - 后台账号或用户账号任一登录即可");
    }

    /**
     * 叠加多个注解实现 AND 效果
     * <p>
     * 以下条件必须同时满足：
     * - 已登录（Controller 类级别）
     * - 具有 admin 角色
     * <p>
     * 这是 AND 逻辑，与 {@code @SaCheckOr} 的 OR 逻辑形成对比
     */
    @SaCheckRole("admin")
    @RequestMapping("/adminOnly")
    public SaResult adminOnly() {
        return SaResult.data("管理员专属接口 - 需要登录 + admin 角色");
    }

    /**
     * 实际业务场景示例
     * <p>
     * 修改密码操作：
     * - 必须登录
     * - 必须通过二级认证（再次验证身份）
     */
    @SaCheckSafe
    @RequestMapping("/changePassword")
    public SaResult changePassword() {
        // 实际的修改密码逻辑...
        return SaResult.data("密码修改成功");
    }
}

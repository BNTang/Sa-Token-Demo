package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员模块控制器
 * <p>
 * 访问此模块的接口需要：
 * 1. admin 或 super-admin 角色
 * 2. admin 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * 管理员首页
     * <p>
     * 需要 admin 或 super-admin 角色
     * 需要 admin 权限
     *
     * @return 管理员首页信息
     */
    @GetMapping("/dashboard")
    public SaResult dashboard() {
        return SaResult.ok("欢迎进入管理员后台")
                .set("statistics", new Object(){
                    public final int userCount = 1000;
                    public final int orderCount = 500;
                    public final int goodsCount = 200;
                })
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 系统设置
     * <p>
     * 需要 admin 或 super-admin 角色
     * 需要 admin 权限
     *
     * @return 系统设置信息
     */
    @GetMapping("/settings")
    public SaResult getSettings() {
        return SaResult.ok("获取系统设置成功")
                .set("systemName", "Sa-Token 演示系统")
                .set("version", "1.0.0")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 更新系统设置
     * <p>
     * 需要 admin 或 super-admin 角色
     * 需要 admin 权限
     *
     * @return 更新结果
     */
    @PutMapping("/settings")
    public SaResult updateSettings() {
        return SaResult.ok("更新系统设置成功")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 用户管理
     * <p>
     * 需要 admin 或 super-admin 角色
     * 需要 admin 权限
     *
     * @return 用户管理信息
     */
    @GetMapping("/users")
    public SaResult manageUsers() {
        return SaResult.ok("获取用户管理数据成功")
                .set("operator", StpUtil.getLoginId());
    }
}

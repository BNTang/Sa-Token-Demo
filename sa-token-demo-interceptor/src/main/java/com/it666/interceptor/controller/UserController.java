package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户模块控制器
 * <p>
 * 访问此模块的接口需要 user 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 获取用户列表
     * <p>
     * 需要 user 权限
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public SaResult getUserList() {
        List<String> users = new ArrayList<>();
        users.add("user");
        users.add("admin");
        users.add("super-admin");
        users.add("goods-admin");

        return SaResult.ok("获取用户列表成功")
                .set("users", users)
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 获取用户详情
     * <p>
     * 需要 user 权限
     *
     * @param username 用户名
     * @return 用户详情
     */
    @GetMapping("/info/{username}")
    public SaResult getUserInfo(@PathVariable String username) {
        return SaResult.ok("获取用户详情成功")
                .set("username", username)
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 创建用户
     * <p>
     * 需要 user 权限
     *
     * @return 创建结果
     */
    @PostMapping("/create")
    public SaResult createUser() {
        return SaResult.ok("创建用户成功")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 更新用户
     * <p>
     * 需要 user 权限
     *
     * @return 更新结果
     */
    @PutMapping("/update")
    public SaResult updateUser() {
        return SaResult.ok("更新用户成功")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 删除用户
     * <p>
     * 需要 user 权限
     *
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public SaResult deleteUser() {
        return SaResult.ok("删除用户成功")
                .set("operator", StpUtil.getLoginId());
    }
}

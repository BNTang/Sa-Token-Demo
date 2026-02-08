package com.it666.annotation.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token 高级注解演示控制器
 * <p>
 * 演示高级用法：
 * - OR 模式：满足任一条件即可
 * - AND 模式：必须同时满足所有条件（默认）
 * - orRole：角色和权限混合校验
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/advanced")
public class AdvancedAnnotationController {

    /**
     * OR 模式权限校验
     * <p>
     * 满足以下任意一个权限即可访问：
     * - user-add
     * - user-all
     * - user-delete
     * <p>
     * 适用场景：有 user-add 或 user-all 权限的人都能新增用户
     */
    @RequestMapping("/atJurOr")
    @SaCheckPermission(value = {"user-add", "user-all", "user-delete"}, mode = SaMode.OR)
    public SaResult atJurOr() {
        return SaResult.data("用户信息 - OR模式：满足任一权限即可");
    }

    /**
     * AND 模式权限校验
     * <p>
     * 必须同时具备以下所有权限才能访问：
     * - user-add
     * - user-update
     * <p>
     * 适用场景：需要同时拥有多个权限才能进行的操作
     */
    @RequestMapping("/atJurAnd")
    @SaCheckPermission(value = {"user-add", "user-update"}, mode = SaMode.AND)
    public SaResult atJurAnd() {
        return SaResult.data("用户信息 - AND模式：必须同时具备所有权限");
    }

    /**
     * 角色和权限混合校验（orRole）
     * <p>
     * 先校验权限，没过的话再看角色，有一个满足就放行
     * <p>
     * 场景：普通用户需要 user.add 权限才能操作，但 admin 角色直接放行
     */
    @RequestMapping("/userAdd")
    @SaCheckPermission(value = "user.add", orRole = "admin")
    public SaResult userAdd() {
        return SaResult.data("用户增加成功 - 有 user.add 权限或 admin 角色均可");
    }

    /**
     * orRole 数组写法 - 有其中一个角色就行
     * <p>
     * 满足以下任意一个角色即可：
     * - admin
     * - manager
     * - staff
     */
    @RequestMapping("/multiRoleOr")
    @SaCheckPermission(value = "user.delete", orRole = {"admin", "manager", "staff"})
    public SaResult multiRoleOr() {
        return SaResult.data("用户删除成功 - 有 admin/manager/staff 任一角色均可");
    }

    /**
     * orRole 字符串写法 - 必须同时具备所有角色
     * <p>
     * 注意：orRole = {"admin, manager, staff"} 是一个字符串里用逗号分隔
     * 意思变成了必须同时具备这三个角色
     * <p>
     * 这种写法不太直观，但确实能覆盖到"必须同时拥有多个角色"的场景
     */
    @RequestMapping("/multiRoleAnd")
    @SaCheckPermission(value = "system.config", orRole = {"admin, manager, staff"})
    public SaResult multiRoleAnd() {
        return SaResult.data("系统配置成功 - 必须同时具备 admin/manager/staff 三个角色");
    }

    /**
     * 叠加多个注解实现 AND 效果
     * <p>
     * 以下三个注解必须同时满足：
     * - 已登录
     * - 具有 admin 角色
     * - 具有 user.add 权限
     * <p>
     * 这是天然的 AND 效果，不需要额外的注解
     */
    @SaCheckPermission(value = "user.add")
    @SaCheckRole("admin")
    @RequestMapping("/multiCheckAnd")
    public SaResult multiCheckAnd() {
        return SaResult.data("操作成功 - 需要同时满足登录+admin角色+user.add权限");
    }
}

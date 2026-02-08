package com.it666.annotation.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限认证接口实现
 * <p>
 * 用于告诉 Sa-Token 如何获取用户的角色和权限信息
 *
 * @author 程序员NEO
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回指定账号所拥有的权限码集合
     *
     * @param loginId   账号id
     * @param loginType 登录类型
     * @return 权限码集合
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 从 Session 中获取权限列表
        Object permissionObj = StpUtil.getSession().get("permissionList");
        if (permissionObj instanceof List) {
            return (List<String>) permissionObj;
        }
        return new ArrayList<>();
    }

    /**
     * 返回指定账号所拥有的角色标识集合
     *
     * @param loginId   账号id
     * @param loginType 登录类型
     * @return 角色集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 从 Session 中获取角色
        List<String> roleList = new ArrayList<>();
        String role = (String) StpUtil.getSession().get("role");
        if (role != null) {
            roleList.add(role);
        }
        return roleList;
    }
}

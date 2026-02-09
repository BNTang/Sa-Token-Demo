package com.it666.interceptor.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sa-Token 权限验证接口实现
 * <p>
 * 此类用于返回指定用户的角色和权限列表
 * Sa-Token 会在进行角色和权限校验时调用此接口
 *
 * @author 程序员NEO
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回指定 loginId 拥有的权限码集合
     *
     * @param loginId 账号id
     * @param loginType 账号类型
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 从 Session 中获取权限列表
        // 注意：这里需要从 Sa-Token 的 Session 中获取，因为我们在登录时已经将权限存入 Session
        String[] permissions = (String[]) cn.dev33.satoken.stp.StpUtil
                .getSessionByLoginId(loginId)
                .get("permissions");

        if (permissions == null) {
            return new ArrayList<>();
        }

        return Arrays.asList(permissions);
    }

    /**
     * 返回指定 loginId 拥有的角色标识集合
     *
     * @param loginId 账号id
     * @param loginType 账号类型
     * @return 角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 从 Session 中获取角色
        String role = (String) cn.dev33.satoken.stp.StpUtil
                .getSessionByLoginId(loginId)
                .get("role");

        if (role == null) {
            return new ArrayList<>();
        }

        List<String> roles = new ArrayList<>();
        roles.add(role);
        return roles;
    }
}

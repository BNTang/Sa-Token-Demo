package com.it666.session.dto;

import lombok.Data;

/**
 * 登录请求参数
 *
 * @author BNTang
 */
@Data
public class LoginDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 设备类型（PC、APP、H5等）
     */
    private String deviceType;

}

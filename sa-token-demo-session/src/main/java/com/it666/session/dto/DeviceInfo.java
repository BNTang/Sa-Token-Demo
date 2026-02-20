package com.it666.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备信息
 *
 * @author BNTang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    private Long loginTime;

}

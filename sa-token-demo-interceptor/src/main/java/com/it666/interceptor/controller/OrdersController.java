package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

/**
 * 订单模块控制器
 * <p>
 * 访问此模块的接口需要 orders 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/orders")
public class OrdersController {

    /**
     * 获取订单列表
     * <p>
     * 需要 orders 权限
     *
     * @return 订单列表
     */
    @GetMapping("/list")
    public SaResult getOrderList() {
        return SaResult.ok("获取订单列表成功")
                .set("orders", new Object[]{
                        new Object(){public final String id = "ORD001"; public final String amount = "7999.00";},
                        new Object(){public final String id = "ORD002"; public final String amount = "12999.00";}
                })
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 获取订单详情
     * <p>
     * 需要 orders 权限
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/info/{orderId}")
    public SaResult getOrderInfo(@PathVariable String orderId) {
        return SaResult.ok("获取订单详情成功")
                .set("orderId", orderId)
                .set("amount", "7999.00")
                .set("status", "已支付")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 创建订单
     * <p>
     * 需要 orders 权限
     *
     * @return 创建结果
     */
    @PostMapping("/create")
    public SaResult createOrder() {
        return SaResult.ok("创建订单成功")
                .set("orderId", "ORD" + System.currentTimeMillis())
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 取消订单
     * <p>
     * 需要 orders 权限
     *
     * @return 取消结果
     */
    @PutMapping("/cancel/{orderId}")
    public SaResult cancelOrder(@PathVariable String orderId) {
        return SaResult.ok("取消订单成功")
                .set("orderId", orderId)
                .set("operator", StpUtil.getLoginId());
    }
}

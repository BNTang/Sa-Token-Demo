package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品模块控制器
 * <p>
 * 访问此模块的接口需要 goods 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    /**
     * 获取商品列表
     * <p>
     * 需要 goods 权限
     *
     * @return 商品列表
     */
    @GetMapping("/list")
    public SaResult getGoodsList() {
        List<String> goods = new ArrayList<>();
        goods.add("iPhone 15 Pro");
        goods.add("MacBook Pro");
        goods.add("AirPods Pro");
        goods.add("iPad Pro");

        return SaResult.ok("获取商品列表成功")
                .set("goods", goods)
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 获取商品详情
     * <p>
     * 需要 goods 权限
     *
     * @param goodsId 商品ID
     * @return 商品详情
     */
    @GetMapping("/info/{goodsId}")
    public SaResult getGoodsInfo(@PathVariable String goodsId) {
        return SaResult.ok("获取商品详情成功")
                .set("goodsId", goodsId)
                .set("name", "iPhone 15 Pro")
                .set("price", 7999)
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 添加商品
     * <p>
     * 需要 goods 权限
     *
     * @return 添加结果
     */
    @PostMapping("/add")
    public SaResult addGoods() {
        return SaResult.ok("添加商品成功")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 更新商品
     * <p>
     * 需要 goods 权限
     *
     * @return 更新结果
     */
    @PutMapping("/update")
    public SaResult updateGoods() {
        return SaResult.ok("更新商品成功")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 删除商品
     * <p>
     * 需要 goods 权限
     *
     * @return 删除结果
     */
    @DeleteMapping("/delete/{goodsId}")
    public SaResult deleteGoods(@PathVariable String goodsId) {
        return SaResult.ok("删除商品成功")
                .set("goodsId", goodsId)
                .set("operator", StpUtil.getLoginId());
    }
}

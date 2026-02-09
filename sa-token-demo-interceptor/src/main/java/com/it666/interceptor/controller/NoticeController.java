package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

/**
 * 通知模块控制器
 * <p>
 * 访问此模块的接口需要 notice 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {

    /**
     * 获取通知列表
     * <p>
     * 需要 notice 权限
     *
     * @return 通知列表
     */
    @GetMapping("/list")
    public SaResult getNoticeList() {
        return SaResult.ok("获取通知列表成功")
                .set("notices", new Object[]{
                        new Object(){public final String title = "系统维护通知"; public final String content = "系统将于今晚22:00进行维护";},
                        new Object(){public final String title = "新功能上线"; public final String content = "Sa-Token 拦断器功能已上线";}
                })
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 获取通知详情
     * <p>
     * 需要 notice 权限
     *
     * @param noticeId 通知ID
     * @return 通知详情
     */
    @GetMapping("/info/{noticeId}")
    public SaResult getNoticeInfo(@PathVariable String noticeId) {
        return SaResult.ok("获取通知详情成功")
                .set("noticeId", noticeId)
                .set("title", "系统维护通知")
                .set("content", "系统将于今晚22:00进行维护，预计持续2小时")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 发布通知
     * <p>
     * 需要 notice 权限
     *
     * @return 发布结果
     */
    @PostMapping("/publish")
    public SaResult publishNotice() {
        return SaResult.ok("发布通知成功")
                .set("noticeId", "NOTICE" + System.currentTimeMillis())
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 删除通知
     * <p>
     * 需要 notice 权限
     *
     * @return 删除结果
     */
    @DeleteMapping("/delete/{noticeId}")
    public SaResult deleteNotice(@PathVariable String noticeId) {
        return SaResult.ok("删除通知成功")
                .set("noticeId", noticeId)
                .set("operator", StpUtil.getLoginId());
    }
}

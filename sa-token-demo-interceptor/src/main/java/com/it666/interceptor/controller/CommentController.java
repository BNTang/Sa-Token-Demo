package com.it666.interceptor.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.*;

/**
 * 评论模块控制器
 * <p>
 * 访问此模块的接口需要 comment 权限
 *
 * @author 程序员NEO
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    /**
     * 获取评论列表
     * <p>
     * 需要 comment 权限
     *
     * @return 评论列表
     */
    @GetMapping("/list")
    public SaResult getCommentList() {
        return SaResult.ok("获取评论列表成功")
                .set("comments", new Object[]{
                        new Object(){public final String user = "user"; public final String content = "非常好的产品！";},
                        new Object(){public final String user = "admin"; public final String content = "功能很完善";}
                })
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 获取评论详情
     * <p>
     * 需要 comment 权限
     *
     * @param commentId 评论ID
     * @return 评论详情
     */
    @GetMapping("/info/{commentId}")
    public SaResult getCommentInfo(@PathVariable String commentId) {
        return SaResult.ok("获取评论详情成功")
                .set("commentId", commentId)
                .set("content", "非常好的产品！")
                .set("user", "user")
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 添加评论
     * <p>
     * 需要 comment 权限
     *
     * @return 添加结果
     */
    @PostMapping("/add")
    public SaResult addComment() {
        return SaResult.ok("添加评论成功")
                .set("commentId", "COMMENT" + System.currentTimeMillis())
                .set("operator", StpUtil.getLoginId());
    }

    /**
     * 删除评论
     * <p>
     * 需要 comment 权限
     *
     * @return 删除结果
     */
    @DeleteMapping("/delete/{commentId}")
    public SaResult deleteComment(@PathVariable String commentId) {
        return SaResult.ok("删除评论成功")
                .set("commentId", commentId)
                .set("operator", StpUtil.getLoginId());
    }
}

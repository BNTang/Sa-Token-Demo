package com.it666.redis.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/redis-demo")
public class RedisDemoController {

    private final StringRedisTemplate redisTemplate;

    @Value("${demo.node-id:${spring.application.name}:${server.port}}")
    private String nodeId;

    @Value("${demo.storage-mode:auto}")
    private String storageMode;

    @Value("${demo.serializer:json}")
    private String serializerMode;

    public RedisDemoController(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/login")
    public SaResult login(@RequestParam(defaultValue = "10001") long userId,
                          @RequestParam(defaultValue = "123456") String password) {
        if (!"123456".equals(password)) {
            return SaResult.error("password error");
        }

        StpUtil.login(userId);
        SaSession session = StpUtil.getSession();
        if (session.get("createdAt") == null) {
            session.set("createdAt", Instant.now().toString());
        }
        session.set("nickname", "user-" + userId);
        session.set("lastLoginNode", nodeId);
        session.set("lastLoginAt", Instant.now().toString());

        return SaResult.ok("login success")
                .set("nodeId", nodeId)
                .set("loginId", userId)
                .set("token", StpUtil.getTokenValue())
                .set("daoClass", SaManager.getSaTokenDao().getClass().getName());
    }

    @GetMapping("/me")
    public SaResult me() {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getSession();

        return SaResult.ok()
                .set("nodeId", nodeId)
                .set("loginId", StpUtil.getLoginId())
                .set("token", StpUtil.getTokenValue())
                .set("sessionId", session.getId())
                .set("sessionData", readSessionData(session));
    }

    @PutMapping("/nickname")
    public SaResult updateNickname(@RequestParam String nickname) {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getSession();
        session.set("nickname", nickname);
        session.set("lastEditNode", nodeId);
        session.set("lastEditAt", Instant.now().toString());
        return SaResult.ok("nickname updated").set("nickname", nickname);
    }

    @GetMapping("/storage")
    public SaResult storage() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nodeId", nodeId);
        data.put("storageMode", storageMode);
        data.put("serializerMode", serializerMode);
        data.put("daoClass", SaManager.getSaTokenDao().getClass().getName());
        data.put("serializerClass", SaManager.getSaSerializerTemplate().getClass().getName());
        data.put("redis", redisCheck());
        return SaResult.data(data);
    }

    @GetMapping("/session-by-login-id")
    public SaResult sessionByLoginId(@RequestParam(defaultValue = "10001") long loginId) {
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return SaResult.error("session not found for loginId=" + loginId);
        }
        return SaResult.ok()
                .set("nodeId", nodeId)
                .set("loginId", loginId)
                .set("sessionId", session.getId())
                .set("sessionData", readSessionData(session));
    }

    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("logout success").set("nodeId", nodeId);
    }

    @GetMapping("/is-login")
    public SaResult isLogin() {
        return SaResult.ok()
                .set("nodeId", nodeId)
                .set("isLogin", StpUtil.isLogin())
                .set("loginId", StpUtil.isLogin() ? StpUtil.getLoginId() : null);
    }

    private Map<String, Object> readSessionData(SaSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : session.keys()) {
            data.put(key, session.get(key));
        }
        return data;
    }

    private Map<String, Object> redisCheck() {
        Map<String, Object> redisInfo = new LinkedHashMap<>();
        if (redisTemplate == null) {
            redisInfo.put("status", "redis template missing");
            return redisInfo;
        }

        try (RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            redisInfo.put("ping", connection.ping());
        } catch (Exception ex) {
            redisInfo.put("pingError", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return redisInfo;
        }

        String key = "satoken:demo:redis-check:" + nodeId;
        String value = Instant.now().toString();
        redisTemplate.opsForValue().set(key, value, 120, TimeUnit.SECONDS);
        redisInfo.put("writeKey", key);
        redisInfo.put("readValue", redisTemplate.opsForValue().get(key));
        return redisInfo;
    }
}

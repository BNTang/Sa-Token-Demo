package com.it666.nocookie.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.it666.nocookie.dto.LoginRequest;
import com.it666.nocookie.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/doLogin")
    public SaResult doLogin(@RequestBody LoginRequest request) {
        AuthService.DemoUser user = authService.verifyCredentials(request.getUsername(), request.getPassword());
        StpUtil.login(user.userId());

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        Map<String, Object> tokenData = new LinkedHashMap<>();
        tokenData.put("tokenName", tokenInfo.getTokenName());
        tokenData.put("tokenValue", tokenInfo.getTokenValue());
        tokenData.put("loginId", user.userId());
        tokenData.put("username", user.username());
        tokenData.put("expiresIn", tokenInfo.getTokenTimeout());

        return SaResult.data(tokenData);
    }

    @SaCheckLogin
    @GetMapping("/info")
    public SaResult info() {
        long loginId = StpUtil.getLoginIdAsLong();
        return SaResult.data(authService.getUserInfo(loginId));
    }

    @SaCheckLogin
    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("logout success");
    }

}

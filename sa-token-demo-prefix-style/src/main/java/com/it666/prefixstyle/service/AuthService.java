package com.it666.prefixstyle.service;

import com.it666.prefixstyle.dto.UserInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final Map<String, DemoUser> userByName = new HashMap<>();
    private final Map<Long, DemoUser> userById = new HashMap<>();

    public AuthService() {
        addUser(new DemoUser(10001L, "neo", "123456", "Neo", List.of("user")));
        addUser(new DemoUser(10002L, "trinity", "123456", "Trinity", List.of("user")));
    }

    public DemoUser verifyCredentials(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("username and password must not be empty");
        }
        DemoUser user = userByName.get(username.trim());
        if (user == null || !user.password().equals(password)) {
            throw new IllegalArgumentException("username or password is invalid");
        }
        return user;
    }

    public UserInfoResponse getUserInfo(long loginId) {
        DemoUser user = userById.get(loginId);
        if (user == null) {
            throw new IllegalArgumentException("user not found by loginId: " + loginId);
        }

        UserInfoResponse info = new UserInfoResponse();
        info.setUserId(user.userId());
        info.setUsername(user.username());
        info.setDisplayName(user.displayName());
        info.setRoles(user.roles());
        return info;
    }

    private void addUser(DemoUser user) {
        userByName.put(user.username(), user);
        userById.put(user.userId(), user);
    }

    public record DemoUser(long userId, String username, String password, String displayName, List<String> roles) {
    }

}

package com.it666.prefixstyle.config;

import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.util.SaFoxUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = "demo", name = "custom-token-generator", havingValue = "true")
public class CustomTokenStrategyConfig {

    // This switch proves that changing the generator still does not change Bearer submission rules.
    @PostConstruct
    public void rewriteSaStrategy() {
        SaStrategy.instance.createToken = (loginId, loginType) -> SaFoxUtil.getRandomString(60);
    }

}

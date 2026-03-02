package com.it666.redis.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.serializer.impl.SaSerializerTemplateForJdkUseBase64;
import cn.dev33.satoken.serializer.impl.SaSerializerTemplateForJdkUseHex;
import cn.dev33.satoken.serializer.impl.SaSerializerTemplateForJdkUseISO_8859_1;
import cn.dev33.satoken.serializer.impl.SaSerializerTemplateForJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Locale;

@Component
public class SaTokenComponentRewriteConfig {

    @Value("${demo.storage-mode:auto}")
    private String storageMode;

    @Value("${demo.serializer:json}")
    private String serializerMode;

    @PostConstruct
    public void rewriteComponents() {
        if ("memory".equalsIgnoreCase(storageMode)) {
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
        }

        String mode = serializerMode.toLowerCase(Locale.ROOT);
        switch (mode) {
            case "jdk-base64":
                SaManager.setSaSerializerTemplate(new SaSerializerTemplateForJdkUseBase64());
                break;
            case "jdk-hex":
                SaManager.setSaSerializerTemplate(new SaSerializerTemplateForJdkUseHex());
                break;
            case "jdk-iso-8859-1":
                SaManager.setSaSerializerTemplate(new SaSerializerTemplateForJdkUseISO_8859_1());
                break;
            default:
                SaManager.setSaSerializerTemplate(new SaSerializerTemplateForJson());
                break;
        }
    }
}

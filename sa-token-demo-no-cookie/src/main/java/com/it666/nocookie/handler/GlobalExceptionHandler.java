package com.it666.nocookie.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.util.SaResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<SaResult> handleNotLogin(NotLoginException ex) {
        SaResult body = SaResult.error("401 Unauthorized: token missing, invalid, or expired")
                .set("notLoginType", ex.getType());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SaResult> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(SaResult.error(ex.getMessage()));
    }

}

package com.dailydiet.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseResult<?> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResponseResult.error(400, msg);
    }

    @ExceptionHandler(BindException.class)
    public ResponseResult<?> handleBind(BindException e) {
        return ResponseResult.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<?> handleAll(Exception e) {
        log.error("系统异常", e);
        return ResponseResult.error(500, "系统内部错误");
    }
}

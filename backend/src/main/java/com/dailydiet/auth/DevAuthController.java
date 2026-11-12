package com.dailydiet.auth;

import com.dailydiet.common.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发调试登录：仅 dev profile 注册，直接为测试用户签发 JWT，
 * 供浏览器 / H5 环境调试（微信小程序环境走 {@link AuthController#wxLogin}）。
 * 生产环境该 Bean 不存在，接口返回 404。
 */
@RestController
@RequestMapping("/api/auth")
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthController {

    private static final String DEV_OPENID = "dev_test_openid";

    private final WxLoginService wxLoginService;

    @PostMapping("/dev-login")
    public ResponseResult<LoginResponse> devLogin() {
        return ResponseResult.success(wxLoginService.loginByOpenid(DEV_OPENID));
    }
}

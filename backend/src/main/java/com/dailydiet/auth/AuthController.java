package com.dailydiet.auth;

import com.dailydiet.common.ResponseResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WxLoginService wxLoginService;

    @PostMapping("/wx-login")
    public ResponseResult<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ResponseResult.success(wxLoginService.login(request.getCode()));
    }
}

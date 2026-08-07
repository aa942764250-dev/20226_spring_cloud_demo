package com.example.workbench.controller;

import com.example.common.result.Result;
import com.example.workbench.context.UserContext;
import com.example.workbench.dto.LoginRequest;
import com.example.workbench.service.AuthService;
import com.example.workbench.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/wb/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/info")
    public Result<LoginVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(authService.getUserInfo(userId));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        UserContext.clear();
        return Result.ok();
    }
}

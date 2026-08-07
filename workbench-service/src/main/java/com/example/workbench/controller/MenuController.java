package com.example.workbench.controller;

import com.example.common.result.Result;
import com.example.workbench.context.UserContext;
import com.example.workbench.service.AuthService;
import com.example.workbench.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作台菜单接口。
 * 前端经网关 /api/wb/menu 调用，返回当前用户的菜单列表。
 */
@RestController
@RequestMapping("/wb/menu")
@RequiredArgsConstructor
public class MenuController {

    private final AuthService authService;

    @GetMapping
    public Result<List<MenuVO>> getMenu() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(authService.getMenusByUserId(userId));
    }
}

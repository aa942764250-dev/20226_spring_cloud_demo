package com.example.service.controller;

import com.example.api.entity.User;
import com.example.common.result.Result;
import com.example.service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<User> create(@RequestBody User user) {
        userService.create(user);
        return Result.success(user);
    }

    @PutMapping
    public Result<User> update(@RequestBody User user) {
        userService.update(user);
        return Result.success(user);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
}
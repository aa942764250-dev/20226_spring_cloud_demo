package com.example.api.feign;

import com.example.api.entity.User;
import com.example.api.fallback.UserFeignFallback;
import com.example.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "springcloud-service", path = "/user", fallbackFactory = UserFeignFallback.class)
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<User> getById(@PathVariable("id") Long id);

    @PostMapping
    Result<User> create(@RequestBody User user);

    @PutMapping
    Result<User> update(@RequestBody User user);

    @DeleteMapping("/{id}")
    Result<Void> delete(@PathVariable("id") Long id);
}
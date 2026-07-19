package com.example.api.fallback;

import com.example.api.entity.User;
import com.example.api.feign.UserFeignClient;
import com.example.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignFallback implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("UserFeignClient 调用失败: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<User> getById(Long id) {
                return Result.fail("用户服务不可用 - getById");
            }

            @Override
            public Result<User> create(User user) {
                return Result.fail("用户服务不可用 - create");
            }

            @Override
            public Result<User> update(User user) {
                return Result.fail("用户服务不可用 - update");
            }

            @Override
            public Result<Void> delete(Long id) {
                return Result.fail("用户服务不可用 - delete");
            }
        };
    }
}
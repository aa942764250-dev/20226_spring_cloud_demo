package com.example.service.config;

import com.example.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/datasource")
@Profile({"dev", "master"})
public class DataSourceController {

    private static final List<String> VALID_KEYS = Arrays.asList("dev", "master");

    @GetMapping("/current")
    public Result<Map<String, Object>> current() {
        String key = DynamicDataSource.getKey();
        Map<String, Object> info = new HashMap<>();
        info.put("current", key != null ? key : "dev");
        info.put("available", VALID_KEYS);
        return Result.success(info);
    }

    @GetMapping("/switch")
    public Result<String> switchDataSource(@RequestParam String key) {
        if (!VALID_KEYS.contains(key)) {
            return Result.fail("unsupported datasource: " + key + ", available: " + VALID_KEYS);
        }
        String old = DynamicDataSource.getKey();
        DynamicDataSource.setKey(key);
        log.info("datasource switch: {} -> {}", old, key);
        return Result.success("switched to " + key);
    }
}
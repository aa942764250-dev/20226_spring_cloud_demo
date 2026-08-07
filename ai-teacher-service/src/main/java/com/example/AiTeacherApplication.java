package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI英语教师工作台 独立微服务
 * 从 springcloud-service 内 god-service 中抽离，独立部署、独立注册到 Nacos。
 * 复用：springcloud-api（实体/VO/DTO）、网关 JWT 校验、远程 MySQL/Nacos。
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.example.service.dao")
public class AiTeacherApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTeacherApplication.class, args);
    }
}

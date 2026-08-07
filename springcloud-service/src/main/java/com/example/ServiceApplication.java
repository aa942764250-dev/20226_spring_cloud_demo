package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableAsync
@MapperScan({"com.example.**.dao", "com.example.service.encryptlite.executor"})
public class ServiceApplication {

    public static void main(String[] args) {
        // s测试
        System.out.println("s测试");
        
        SpringApplication.run(ServiceApplication.class, args);
    }
}
package com.example.service.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@Profile({"dev", "master"})
public class DynamicDataSourceConfig {

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String devUrl;

    @Value("${spring.datasource.username}")
    private String devUsername;

    @Value("${spring.datasource.password}")
    private String devPassword;

    @Value("${datasource.master.url:${spring.datasource.url}}")
    private String masterUrl;

    @Value("${datasource.master.username:${spring.datasource.username}}")
    private String masterUsername;

    @Value("${datasource.master.password:${spring.datasource.password}}")
    private String masterPassword;

    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        DynamicDataSource routing = new DynamicDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();

        DataSource devDs = buildDataSource("dev", devUrl, devUsername, devPassword);
        targetDataSources.put("dev", devDs);

        DataSource masterDs = buildDataSource("master", masterUrl, masterUsername, masterPassword);
        targetDataSources.put("master", masterDs);

        routing.setTargetDataSources(targetDataSources);

        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        if ("master".equals(activeProfile)) {
            routing.setDefaultTargetDataSource(masterDs);
        } else {
            routing.setDefaultTargetDataSource(devDs);
        }
        DynamicDataSource.setKey(activeProfile);

        log.info("【动态数据源】初始化完成，默认数据源: {}, dev={}, master={}", activeProfile, devUrl, masterUrl);

        return routing;
    }

    private DataSource buildDataSource(String name, String url, String username, String password) {
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
        ds.setPoolName(name + "-pool");
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        return ds;
    }
}
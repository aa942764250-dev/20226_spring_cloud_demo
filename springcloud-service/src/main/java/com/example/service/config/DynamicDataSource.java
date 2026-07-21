package com.example.service.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    private static volatile String currentKey = "dev";

    public static void setKey(String key) {
        currentKey = key;
    }

    public static String getKey() {
        return currentKey;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return currentKey;
    }
}
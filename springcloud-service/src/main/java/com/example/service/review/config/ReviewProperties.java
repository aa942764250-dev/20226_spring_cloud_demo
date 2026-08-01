package com.example.service.review.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {
    private String pythonPath = "python";
    private String kbScriptPath;
    private String kbServerUrl = "http://127.0.0.1:9876";
    private int searchTimeoutSeconds = 30;
    private boolean enabled = true;
}
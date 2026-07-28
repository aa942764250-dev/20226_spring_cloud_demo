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
    private int searchTimeoutSeconds = 60;
    private boolean enabled = true;
}
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

    /**
     * 知识库后端模式：ima | local | both（默认 both = 双查合并、按 dedupKey 去重）
     */
    private String kbMode = "both";

    /**
     * IMA 知识库 OpenAPI 凭证（ima.qq.com/agent-interface 生成）。
     * 远程部署务必通过环境变量注入，禁止写入代码或仓库。
     */
    private String imaClientId;
    private String imaApiKey;
    private String imaKnowledgeId;
}
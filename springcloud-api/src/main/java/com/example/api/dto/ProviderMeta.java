package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 提供方元信息（供前端下拉联动使用）
 */
@Data
public class ProviderMeta implements Serializable {
    /** 提供方编码 */
    private String provider;
    /** 提供方展示名 */
    private String label;
    /** 可选模型列表 */
    private List<ModelOption> models;

    @Data
    public static class ModelOption implements Serializable {
        private String value;
        private String label;

        public ModelOption() {
        }

        public ModelOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}

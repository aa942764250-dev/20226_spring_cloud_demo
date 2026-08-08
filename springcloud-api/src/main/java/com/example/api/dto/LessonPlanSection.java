package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 教案段落（标题 + 内容）
 */
@Data
public class LessonPlanSection implements Serializable {
    /** 段落标题，如：学情印象回顾 / 针对性训练目标 */
    private String title;
    /** 段落正文（Markdown） */
    private String content;
}

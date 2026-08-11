package com.example.service.aiteacher.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 教材原文图片服务（替代原 Python 图片服务）。
 * 浏览器 <img> 无法携带 JWT，故该路径已在网关白名单放行。
 */
@RestController
@RequestMapping("/ai-teacher/textbook")
public class TextbookImageController {

    @Value("${textbook.image-base-dir:D:/Workspace/Project_010_英语知识库/data/教材原文}")
    private String imageBaseDir;

    private static final Map<String, MediaType> CONTENT_TYPES = new HashMap<String, MediaType>();
    static {
        CONTENT_TYPES.put("jpg", MediaType.IMAGE_JPEG);
        CONTENT_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
        CONTENT_TYPES.put("png", MediaType.IMAGE_PNG);
        CONTENT_TYPES.put("webp", MediaType.parseMediaType("image/webp"));
        CONTENT_TYPES.put("gif", MediaType.IMAGE_GIF);
    }

    @GetMapping("/image/{dir}/{file}")
    public ResponseEntity<byte[]> getImage(@PathVariable String dir, @PathVariable String file) {
        // 第一道防线：拒绝含穿越字符的路径分量（Java 8 兼容，不依赖正则）
        if (isUnsafe(dir) || isUnsafe(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path base = Paths.get(imageBaseDir).toRealPath();
            Path target = base.resolve(dir).resolve(file).toRealPath();

            // 确保目标仍位于 base 目录内，杜绝目录穿越
            if (!target.startsWith(base) || !Files.isRegularFile(target)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String ext = getExtension(file).toLowerCase();
            MediaType contentType = CONTENT_TYPES.get(ext);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            byte[] bytes = Files.readAllBytes(target);
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private boolean isUnsafe(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        return s.contains("..") || s.contains("/") || s.contains("\\")
                || s.contains(File.separator) || s.indexOf('\u0000') >= 0;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return fileName.substring(idx + 1);
    }
}

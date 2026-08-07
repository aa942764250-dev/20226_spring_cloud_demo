package com.example.workbench.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "workbench.menu")
public class WorkbenchProperties {

    private List<MenuGroup> groups = new ArrayList<>();

    @Data
    public static class MenuGroup {
        private String section;
        private List<MenuItem> items = new ArrayList<>();
    }

    @Data
    public static class MenuItem {
        private String to;
        private String label;
        private String icon;
        private Boolean coming = false;
    }
}

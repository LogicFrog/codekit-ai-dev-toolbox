package org.itfjnu.codekit.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "codekit")
public class CodeKitProperties {
    
    private Watch watch = new Watch();
    
    private Fs fs = new Fs();

    @Data
    public static class Watch {
        private boolean enabled = true;
        private String path;
        private long interval = 3000;
    }

    @Data
    public static class Fs {
        private String workspaceRoot;
    }
}

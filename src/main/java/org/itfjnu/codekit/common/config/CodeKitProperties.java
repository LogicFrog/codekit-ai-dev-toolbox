package org.itfjnu.codekit.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "codekit")
public class CodeKitProperties {
    /**
     * 文件监听配置
     */
    private Watch watch = new Watch();

    @Data
    public static class Watch {
        /**
         * 监听是否启用（默认 true，方便一键关闭）
         */
        private boolean enabled = true;

        /**
         * 监听的根目录路径（必填）
         * 示例：/Users/annu/Documents/project
         */
        private String path;
        
        /**
         * 轮询间隔（毫秒），默认 3000ms
         */
        private long interval = 3000;
    }
}

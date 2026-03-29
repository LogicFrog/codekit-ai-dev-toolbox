package org.itfjnu.codekit.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.itfjnu.codekit.code.filesystem.watcher.ProjectFileListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileWatcherConfig {

    private final CodeKitProperties properties;
    private final ProjectFileListener listener;

    @Bean(destroyMethod = "stop")
    // 只有当 codekit.watch.enabled=true 时才创建这个 Bean
    @ConditionalOnProperty(prefix = "codekit.watch", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FileAlterationMonitor fileAlterationMonitor() throws Exception {
        String watchPath = properties.getWatch().getPath();

        // 1. 校验路径是否配置
        if (watchPath == null || watchPath.trim().isEmpty()) {
            log.warn("⚠️ [File Watcher] 未配置监听路径 (codekit.watch.path)，文件监听功能已跳过。");
            return null; // 或者抛出异常，看你策略
        }

        File rootDir = new File(watchPath);
        // 2. 校验路径是否存在
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.error("❌ [File Watcher] 配置的路径不存在或不是目录: {}", watchPath);
            return null;
        }

        // 3. 配置并启动
        long interval = properties.getWatch().getInterval();
        FileAlterationObserver observer = new FileAlterationObserver(rootDir);
        observer.addListener(listener);

        FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
        monitor.start();

        log.info("✅ [File Watcher] 服务已启动 | 监听目录: {} | 轮询间隔: {}ms", watchPath, interval);
        return monitor;
    }
}

package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.service.AISettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class AISettingsServiceImpl implements AISettingsService {

    @Value("${ai.settings.store-file:./data/ai-settings.json}")
    private String storeFile;

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Object fileLock = new Object();

    @PostConstruct
    public void init() {
        loadFromDisk();
    }

    @Override
    public Double getTemperature() {
        return aiProperties.getTemperature();
    }

    @Override
    public Double setTemperature(Double value) {
        synchronized (fileLock) {
            aiProperties.setTemperature(value);
            flushToDiskSafe();
        }
        return aiProperties.getTemperature();
    }

    private void loadFromDisk() {
        try {
            Path path = Paths.get(storeFile);
            if (!Files.exists(path)) {
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                return;
            }
            AISettingsSnapshot snapshot = objectMapper.readValue(bytes, AISettingsSnapshot.class);
            if (snapshot.getTemperature() != null) {
                aiProperties.setTemperature(snapshot.getTemperature());
                log.info("已从本地配置恢复 temperature={}", snapshot.getTemperature());
            }
        } catch (Exception e) {
            log.warn("读取 AI 设置失败，继续使用内存配置: {}", e.getMessage());
        }
    }

    private void flushToDiskSafe() {
        try {
            Path path = Paths.get(storeFile);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            AISettingsSnapshot snapshot = new AISettingsSnapshot();
            snapshot.setTemperature(aiProperties.getTemperature());

            Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), snapshot);

            try {
                Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("写入 AI 设置失败: {}", e.getMessage(), e);
        }
    }

    @Data
    private static class AISettingsSnapshot {
        private Double temperature;
    }
}

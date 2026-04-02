package org.itfjnu.codekit.code.filesystem.support;

import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ScanTaskTracker {

    private final Map<String, String> scanStatusMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanProgressMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanSuccessCountMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanSkipCountMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanFailedCountMap = new ConcurrentHashMap<>();

    public boolean isRunning(String scanDir) {
        return "RUNNING".equals(scanStatusMap.get(normalize(scanDir)));
    }

    public void start(String scanDir) {
        String rootDir = normalize(scanDir);
        scanProgressMap.put(rootDir, new AtomicInteger(0));
        scanSuccessCountMap.put(rootDir, new AtomicInteger(0));
        scanSkipCountMap.put(rootDir, new AtomicInteger(0));
        scanFailedCountMap.put(rootDir, new AtomicInteger(0));
        scanStatusMap.put(rootDir, "RUNNING");
    }

    public void markProcessed(String scanDir) {
        counter(scanProgressMap, scanDir).incrementAndGet();
    }

    public void markSuccess(String scanDir) {
        counter(scanSuccessCountMap, scanDir).incrementAndGet();
    }

    public void markSkip(String scanDir) {
        counter(scanSkipCountMap, scanDir).incrementAndGet();
    }

    public void markFailed(String scanDir) {
        counter(scanFailedCountMap, scanDir).incrementAndGet();
    }

    public void complete(String scanDir) {
        scanStatusMap.put(normalize(scanDir), "COMPLETED");
    }

    public void fail(String scanDir) {
        scanStatusMap.put(normalize(scanDir), "FAILED");
    }

    public int getProcessedCount(String scanDir) {
        return counter(scanProgressMap, scanDir).get();
    }

    public ScanStatusDTO getStatus(String scanDir) {
        String rootDir = normalize(scanDir);
        String status = scanStatusMap.getOrDefault(rootDir, "IDLE");
        AtomicInteger processed = scanProgressMap.get(rootDir);
        AtomicInteger success = scanSuccessCountMap.get(rootDir);
        AtomicInteger skip = scanSkipCountMap.get(rootDir);
        AtomicInteger failed = scanFailedCountMap.get(rootDir);

        return ScanStatusDTO.builder()
                .status(status)
                .processedCount(readCount(processed))
                .successCount(readCount(success))
                .skipCount(readCount(skip))
                .failedCount(readCount(failed))
                .message(buildStatusMessage(status, processed, success, skip, failed))
                .build();
    }

    private AtomicInteger counter(Map<String, AtomicInteger> counterMap, String scanDir) {
        return counterMap.computeIfAbsent(normalize(scanDir), key -> new AtomicInteger(0));
    }

    private String normalize(String scanDir) {
        return new File(scanDir).getAbsolutePath();
    }

    private int readCount(AtomicInteger counter) {
        return counter == null ? 0 : counter.get();
    }

    private String buildStatusMessage(String status, AtomicInteger progress,
                                      AtomicInteger successCount, AtomicInteger skipCount,
                                      AtomicInteger failedCount) {
        if ("RUNNING".equals(status)) {
            return String.format("扫描中... 已处理：%d, 成功：%d, 跳过：%d, 失败：%d",
                    readCount(progress), readCount(successCount), readCount(skipCount), readCount(failedCount));
        }
        if ("COMPLETED".equals(status)) {
            return String.format("扫描完成！成功：%d, 跳过：%d",
                    readCount(successCount), readCount(skipCount));
        }
        if ("FAILED".equals(status)) {
            return String.format("扫描失败！失败数：%d", readCount(failedCount));
        }
        return "空闲";
    }
}

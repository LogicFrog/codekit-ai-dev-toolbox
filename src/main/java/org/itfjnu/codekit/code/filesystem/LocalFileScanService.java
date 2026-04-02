package org.itfjnu.codekit.code.filesystem;

import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.itfjnu.codekit.code.filesystem.support.CodeFileProcessor;
import org.itfjnu.codekit.code.filesystem.support.ScanTaskTracker;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

@Service
@RequiredArgsConstructor
public class LocalFileScanService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileScanService.class);

    private final CodeSnippetService codeSnippetService;
    private final CodeFileProcessor codeFileProcessor;
    private final ScanTaskTracker scanTaskTracker;

    // 使用 JDK 21 虚拟线程执行器：为每个扫描任务分配轻量级虚拟线程
    private final ExecutorService scanExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 对外提供的异步扫描入口方法
     * @param scanDir 要扫描的根目录
     */
    public Boolean scanLocalCodeDir(String scanDir) {
        File rootDir = new File(scanDir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new BusinessException(ErrorCode.DIRECTORY_NOT_FOUND, "扫描目录不存在或不是有效目录: " + scanDir);
        }

        String absolutePath = rootDir.getAbsolutePath();

        if (scanTaskTracker.isRunning(absolutePath)) {
            throw new BusinessException(ErrorCode.CODE_SCAN_RUNNING, "该目录正在扫描中，请勿重复操作");
        }

        scanExecutor.submit(() -> runScanTask(rootDir, absolutePath));
        return true;
    }

    private void runScanTask(File rootDir, String absolutePath) {
        scanTaskTracker.start(absolutePath);
        Phaser phaser = new Phaser(1);
        try {
            log.info("【异步任务启动】开始并发扫描目录: {}", absolutePath);
            cleanupNonExistentFiles(absolutePath);
            recursiveScanDir(rootDir, absolutePath, phaser);
            phaser.arriveAndAwaitAdvance();
            scanTaskTracker.complete(absolutePath);
            log.info("【异步任务完成】并发扫描目录成功: {}, 共处理 {} 个文件",
                    absolutePath, scanTaskTracker.getProcessedCount(absolutePath));
        } catch (Exception e) {
            scanTaskTracker.fail(absolutePath);
            log.error("【异步任务失败】扫描过程发生异常: {}", absolutePath, e);
        } finally {
            phaser.forceTermination();
        }
    }

    /**
     * 清理指定目录下已不存在于磁盘的文件记录
     */
    private void cleanupNonExistentFiles(String scanDir) {
        log.info("【清理任务】开始清理不存在的文件: {}", scanDir);
        List<String> allPaths = codeSnippetService.getAllFilePaths();
        int count = 0;
        for (String path : allPaths) {
            if (path.startsWith(scanDir)) {
                File file = new File(path);
                if (!file.exists()) {
                    codeSnippetService.deleteByFilePath(path);
                    count++;
                }
            }
        }
        log.info("【清理任务】清理完成，共移除 {} 条记录", count);
    }

    /**
     * 查询指定目录的扫描状态（返回结构化 DTO）
     */
    public ScanStatusDTO getScanStatus(String scanDir) {
        return scanTaskTracker.getStatus(scanDir);
    }

    /**
     * 按文件路径导入单个文件 (保持同步，因为单文件导入通常很快)
     * @param filePath 文件绝对路径
     * @param languageType 开发语言类型（可选）
     * @param tag 标签（可选）
     * @return 导入的代码片段实体
     */
    public CodeSnippet importSingleFile(String filePath, String languageType, String tag) {
        return importSingleFile(filePath, languageType, tag, null);
    }

    public CodeSnippet importSingleFile(String filePath, String languageType, String tag, Long categoryId) {
        File codeFile = new File(filePath);
        if (!codeFile.exists() || !codeFile.isFile()) {
            throw new BusinessException(ErrorCode.CODE_FILE_NOT_EXISTS, "文件不存在或不是有效文件: " + filePath);
        }

        return codeFileProcessor.processFile(codeFile, null, languageType, tag, categoryId);
    }

    /**
     * 递归扫描目录核心方法 (支持并发)
     */
    private void recursiveScanDir(File dir, String rootDir, Phaser phaser) {
        File[] files = dir.listFiles();
        
        // 防御式编程：防止无权限或 IO 错误导致的 null 指针
        if (files == null) {
            log.warn("无法列出目录内容（可能无权限）: {}", dir.getAbsolutePath());
            return;
        }

        for (File file : files) {
            // 排除指定目录
            if (file.isDirectory() && isExcludeDir(file.getName())) {
                continue;
            }
            // 递归扫描子目录 (提交到虚拟线程池)
            if (file.isDirectory()) {
                phaser.register();
                scanExecutor.submit(() -> {
                    try {
                        recursiveScanDir(file, rootDir, phaser);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
                continue;
            }
            // 处理代码文件 (提交到虚拟线程池)
            if (file.isFile() && isSupportedCodeFile(file.getName())) {
                phaser.register();
                scanExecutor.submit(() -> {
                    try {
                        codeFileProcessor.processFile(file, rootDir, null, null, null);
                    } catch (Exception e) {
                        log.error("扫描文件失败：{}", file.getAbsolutePath(), e);
                        scanTaskTracker.markFailed(rootDir);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
            }
        }
    }

    /**
     * 判断是否为排除的目录
     */
    private boolean isExcludeDir(String dirName) {
        for (String exclude : FileScanConstant.EXCLUDE_DIR) {
            if (exclude.equals(dirName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为支持的代码文件
     */
    private boolean isSupportedCodeFile(String fileName) {
        for (String exclude : FileScanConstant.EXCLUDE_FILE) {
            if (fileName.endsWith(exclude)) {
                return false;
            }
        }
        for (String suffix : FileScanConstant.SUPPORTED_SUFFIX) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

}

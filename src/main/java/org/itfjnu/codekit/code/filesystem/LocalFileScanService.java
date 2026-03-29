package org.itfjnu.codekit.code.filesystem;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.service.CodeManagerService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LocalFileScanService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileScanService.class);

    private final CodeManagerService codeManagerService;

    // 使用 JDK 21 虚拟线程执行器：为每个扫描任务分配轻量级虚拟线程
    private final ExecutorService scanExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 扫描状态缓存：Key 为扫描目录，Value 为状态 (IDLE / RUNNING / COMPLETED / FAILED)
    private final Map<String, String> scanStatusMap = new ConcurrentHashMap<>();
    
    // 扫描进度缓存：Key 为扫描目录，Value 为已处理文件数
    private final Map<String, AtomicInteger> scanProgressMap = new ConcurrentHashMap<>();

    // 最大读取文件大小限制 (1MB)，防止 OOM
    private static final long MAX_FILE_SIZE = 1024 * 1024;

    // 扫描进度统计
    private final Map<String, AtomicInteger> scanSuccessCountMap =  new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanSkipCountMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> scanFailedCountMap = new ConcurrentHashMap<>();

    /**
     * 核心方法：处理单个文件（统一入口）
     * 目录扫描、单文件导入、文件监听都调用这个方法
     * 
     * @param codeFile 文件对象
     * @param rootDir 根目录（用于进度统计）
     * @param languageType 用户指定的语言类型（可选）
     * @param tag 用户指定的标签（可选）
     * @return 处理结果（CodeSnippet 或 null）
     */
    private CodeSnippet processFile(File codeFile, String rootDir, String languageType, String tag) {
        String filePath = codeFile.getAbsolutePath();
        
        // 更新进度计数
        if (rootDir != null && scanProgressMap.containsKey(rootDir)) {
            scanProgressMap.get(rootDir).incrementAndGet();
        }

        // OOM 防护：大文件跳过
        if (codeFile.length() > MAX_FILE_SIZE) {
            log.warn("【文件处理】文件过大，跳过读取：{} ({} KB)", filePath, codeFile.length() / 1024);
            updateScanStats(rootDir, false, true);  // 跳过
            return null;
        }

        try {
            // 1. 计算文件 MD5
            String fileMd5 = calculateFileMd5(codeFile);
            if (fileMd5 == null) {
                updateScanStats(rootDir, false, true);  // 跳过
                return null;
            }

            // 2. 检查数据库中是否存在相同路径或相同 MD5 的记录
            CodeSnippet existingByPath = codeManagerService.getCodeSnippetByPath(filePath);
            CodeSnippet existingByMd5 = codeManagerService.getCodeSnippetByMd5(fileMd5);

            if (existingByPath != null) {
                // 路径已存在
                if (fileMd5.equals(existingByPath.getFileMd5())) {
                    // MD5 没变，无需处理
                    log.debug("【文件处理】文件内容未变，跳过：{}", filePath);
                    updateScanStats(rootDir, false, true);  // 跳过
                    return existingByPath;
                } else {
                    // MD5 变了，更新内容
                    log.info("【文件处理】文件内容已更新：{}", filePath);
                    existingByPath.setCodeContent(new String(Files.readAllBytes(Paths.get(filePath))));
                    existingByPath.setFileMd5(fileMd5);
                    
                    // 结构化解析
                    List<String> importList = new ArrayList<>();
                    deepAnalyzeAndParse(existingByPath, importList);
                    
                    CodeSnippet savedSnippet = codeManagerService.saveOrUpdateCodeSnippet(existingByPath);
                    
                    // 修正依赖保存：使用 savedSnippet.getId()
                    if (!importList.isEmpty()) {
                        // 先删除旧依赖，再保存新依赖
                        codeManagerService.saveDependencies(savedSnippet.getId(), importList);
                    }
                    
                    updateScanStats(rootDir, true, false);  // 成功
                    return savedSnippet;
                }
            }

            if (existingByMd5 != null) {
                // MD5 已存在但路径不同 -> 说明文件被移动了
                log.info("【文件处理】检测到文件移动：{} -> {}", existingByMd5.getFilePath(), filePath);
                existingByMd5.setFilePath(filePath);
                existingByMd5.setFileName(codeFile.getName());
                
                CodeSnippet savedSnippet = codeManagerService.saveOrUpdateCodeSnippet(existingByMd5);
                updateScanStats(rootDir, true, false);  // 成功
                return savedSnippet;
            }

            // 3. 全新文件，直接入库
            log.info("【文件处理】新文件：{}", filePath);
            String codeContent = new String(Files.readAllBytes(Paths.get(filePath)));
            String fileName = codeFile.getName();

            // 如果未指定语言，则根据后缀自动判断
            if (languageType == null || languageType.isEmpty()) {
                languageType = getLanguageTypeBySuffix(fileName);
            }

            CodeSnippet snippet = new CodeSnippet();
            snippet.setFilePath(filePath);
            snippet.setFileName(fileName);
            snippet.setCodeContent(codeContent);
            snippet.setLanguageType(languageType);
            snippet.setFileMd5(fileMd5);

            // 处理用户指定的标签
            if (tag != null && !tag.isEmpty()) {
                for (String t : tag.split(",")) {
                    snippet.getTags().add(t.trim());
                }
            }

            // 结构化解析
            List<String> importList = new ArrayList<>();
            deepAnalyzeAndParse(snippet, importList);

            // 如果解析后没有任何标签，才添加"未分类"
            if (snippet.getTags().isEmpty()) {
                snippet.getTags().add("未分类");
            }

            // 保存 Snippet 获得 ID
            CodeSnippet savedSnippet = codeManagerService.saveOrUpdateCodeSnippet(snippet);

            // 保存依赖关系（使用 savedSnippet.getId()）
            if (!importList.isEmpty()) {
                codeManagerService.saveDependencies(savedSnippet.getId(), importList);
            }
            
            updateScanStats(rootDir, true, false);  // 成功
            return savedSnippet;
            
        } catch (IOException e) {
            log.error("【文件处理】读取文件失败：{}，原因：{}", filePath, e.getMessage(), e);
            updateScanStats(rootDir, false, false);  // 失败
            return null;
        }
    }

    /**
     * 统一的文件解析方法（根据语言类型自动选择解析器）
     */
    private void deepAnalyzeAndParse(CodeSnippet snippet, List<String> importList) {
        String languageType = snippet.getLanguageType();
        
        if ("Java".equalsIgnoreCase(languageType)) {
            deepAnalyzeJavaFile(snippet, importList);
        } else if ("Python".equalsIgnoreCase(languageType)) {
            deepAnalyzePythonFile(snippet, importList);
        } else if ("JavaScript/TypeScript".equalsIgnoreCase(languageType)) {
            deepAnalyzeJavaScriptFile(snippet, importList);
        }
    }

    /**
     * 更新扫描统计信息
     */
    private void updateScanStats(String rootDir, boolean success, boolean skip) {
        if (rootDir == null) return;
        
        if (skip) {
            scanSkipCountMap.computeIfAbsent(rootDir, k -> new AtomicInteger(0)).incrementAndGet();
        } else if (success) {
            scanSuccessCountMap.computeIfAbsent(rootDir, k -> new AtomicInteger(0)).incrementAndGet();
        } else {
            scanFailedCountMap.computeIfAbsent(rootDir, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    /**
     * 对外提供的异步扫描入口方法
     * @param scanDir 要扫描的根目录
     */
    public Boolean scanLocalCodeDir(String scanDir) {
        File rootDir = new File(scanDir);
        // 1. 同步校验路径合法性
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new BusinessException(ErrorCode.DIRECTORY_NOT_FOUND, "扫描目录不存在或不是有效目录: " + scanDir);
        }

        String absolutePath = rootDir.getAbsolutePath();
        
        // 2. 检查该路径是否正在扫描中，避免重复触发
        if ("RUNNING".equals(scanStatusMap.get(absolutePath))) {
            throw new BusinessException(ErrorCode.CODE_SCAN_RUNNING, "该目录正在扫描中，请勿重复操作");
        }

        // 3. 异步启动扫描任务
        scanExecutor.submit(() -> {
            // 初始化进度和统计
            scanProgressMap.put(absolutePath, new AtomicInteger(0));
            scanSuccessCountMap.put(absolutePath, new AtomicInteger(0));
            scanSkipCountMap.put(absolutePath, new AtomicInteger(0));
            scanFailedCountMap.put(absolutePath, new AtomicInteger(0));
            
            // 使用 Phaser 来追踪所有并发子任务的完成情况
            Phaser phaser = new Phaser(1); // 注册当前主任务线程
            try {
                log.info("【异步任务启动】开始并发扫描目录: {}", absolutePath);
                scanStatusMap.put(absolutePath, "RUNNING");

                // 预处理：清理数据库中已不存在的文件（僵尸数据）
                cleanupNonExistentFiles(absolutePath);
                
                // 执行并发递归扫描
                recursiveScanDir(rootDir, absolutePath, phaser);

                // 等待所有已注册的并发任务完成
                phaser.arriveAndAwaitAdvance();
                
                scanStatusMap.put(absolutePath, "COMPLETED");
                log.info("【异步任务完成】并发扫描目录成功: {}, 共处理 {} 个文件", absolutePath, scanProgressMap.get(absolutePath).get());
            } catch (Exception e) {
                scanStatusMap.put(absolutePath, "FAILED");
                log.error("【异步任务失败】扫描过程发生异常: {}", absolutePath, e);
            } finally {
                phaser.forceTermination();
            }
        });

        // 4. 立即返回 true，表示任务已成功提交
        return true;
    }

    /**
     * 清理指定目录下已不存在于磁盘的文件记录
     */
    private void cleanupNonExistentFiles(String scanDir) {
        log.info("【清理任务】开始清理不存在的文件: {}", scanDir);
        List<String> allPaths = codeManagerService.getAllFilePaths();
        int count = 0;
        for (String path : allPaths) {
            if (path.startsWith(scanDir)) {
                File file = new File(path);
                if (!file.exists()) {
                    codeManagerService.deleteByFilePath(path);
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
        File dir = new File(scanDir);
        String absolutePath = dir.getAbsolutePath();
        String status = scanStatusMap.getOrDefault(absolutePath, "IDLE");
        
        AtomicInteger progress = scanProgressMap.get(absolutePath);
        AtomicInteger successCount = scanSuccessCountMap.get(absolutePath);
        AtomicInteger skipCount = scanSkipCountMap.get(absolutePath);
        AtomicInteger failedCount = scanFailedCountMap.get(absolutePath);
        
        // 构建状态消息
        String message = buildStatusMessage(status, progress, successCount, skipCount, failedCount);
        
        return ScanStatusDTO.builder()
            .status(status)
            .processedCount(progress != null ? progress.get() : 0)
            .successCount(successCount != null ? successCount.get() : 0)
            .skipCount(skipCount != null ? skipCount.get() : 0)
            .failedCount(failedCount != null ? failedCount.get() : 0)
            .message(message)
            .build();
    }

    /**
     * 构建状态消息
     */
    private String buildStatusMessage(String status, AtomicInteger progress, 
                                       AtomicInteger successCount, AtomicInteger skipCount, 
                                       AtomicInteger failedCount) {
        if ("RUNNING".equals(status)) {
            int processed = progress != null ? progress.get() : 0;
            int success = successCount != null ? successCount.get() : 0;
            int skip = skipCount != null ? skipCount.get() : 0;
            int failed = failedCount != null ? failedCount.get() : 0;
            return String.format("扫描中... 已处理：%d, 成功：%d, 跳过：%d, 失败：%d", 
                               processed, success, skip, failed);
        } else if ("COMPLETED".equals(status)) {
            int success = successCount != null ? successCount.get() : 0;
            int skip = skipCount != null ? skipCount.get() : 0;
            return String.format("扫描完成！成功：%d, 跳过：%d", success, skip);
        } else if ("FAILED".equals(status)) {
            int failed = failedCount != null ? failedCount.get() : 0;
            return String.format("扫描失败！失败数：%d", failed);
        }
        return "空闲";
    }

    /**
     * 按文件路径导入单个文件 (保持同步，因为单文件导入通常很快)
     * @param filePath 文件绝对路径
     * @param languageType 开发语言类型（可选）
     * @param tag 标签（可选）
     * @return 导入的代码片段实体
     */
    public CodeSnippet importSingleFile(String filePath, String languageType, String tag) {
        File codeFile = new File(filePath);
        if (!codeFile.exists() || !codeFile.isFile()) {
            throw new BusinessException(ErrorCode.CODE_FILE_NOT_EXISTS, "文件不存在或不是有效文件: " + filePath);
        }

        // OOM 防护：检查文件大小
        if (codeFile.length() > MAX_FILE_SIZE) {
            log.warn("文件过大，跳过解析：{} ({} bytes)", filePath, codeFile.length());
            return null;
        }

        // 调用统一的 processFile 方法（单文件导入不需要 rootDir，传 null）
        return processFile(codeFile, null, languageType, tag);
    }

    /**
     * 计算文件 MD5
     */
    private String calculateFileMd5(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fis);
        } catch (IOException e) {
            log.error("计算文件 MD5 失败: {}", file.getAbsolutePath(), e);
            return null;
        }
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
                        // 调用统一的 processFile 方法
                        processFile(file, rootDir, null, null);
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
        // 排除指定文件
        for (String exclude : FileScanConstant.EXCLUDE_FILE) {
            if (fileName.endsWith(exclude)) {
                return false;
            }
        }
        // 判断是否为支持的后缀
        for (String suffix : FileScanConstant.SUPPORTED_SUFFIX) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据文件后缀获取开发语言类型
     */
    private String getLanguageTypeBySuffix(String fileName) {
        if (fileName.endsWith(".java")) {
            return "Java";
        } else if (fileName.endsWith(".py")) {
            return "Python";
        } else if (fileName.endsWith(".js") || fileName.endsWith(".ts") || fileName.endsWith(".vue")) {
            return "JavaScript/TypeScript";
        } else {
            return "Unknown";
        }
    }

    /**
     *  使用 JavaParser 提取元数据
     */
    private void deepAnalyzeJavaFile(CodeSnippet snippet, List<String> importList) {
        if (snippet.getCodeContent() == null || snippet.getCodeContent().isBlank()) {
            return;
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(snippet.getCodeContent());

            cu.getPackageDeclaration().ifPresentOrElse(
                pd -> snippet.setPackageName(pd.getNameAsString()),
                () -> log.debug("文件 {} 无包名", snippet.getFilePath())
            );

            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            if (!classes.isEmpty()) {
                snippet.setClassName(classes.get(0).getNameAsString());
                if (classes.size() > 1) {
                    for (int i = 1; i < classes.size(); i++) {
                        snippet.getTags().add("innerClass:" + classes.get(i).getNameAsString());
                    }
                }
            }

            cu.findAll(MethodDeclaration.class).forEach(md -> {
                String methodName = md.getNameAsString();
                snippet.getTags().add("method:" + methodName);
            });

            java.util.Set<String> importSet = new java.util.HashSet<>();
            cu.getImports().forEach(im -> importSet.add(im.getNameAsString()));
            importList.addAll(importSet);

        } catch (Exception e) {
            log.warn("【语法解析跳过】文件 {} 存在语法错误或不完整，仅存储原始内容", snippet.getFilePath());
        }
    }
    /**
     * 提取python文件元数据
     */
    private void deepAnalyzePythonFile(CodeSnippet snippet, List<String> importList) {
        if (snippet.getCodeContent() == null || snippet.getCodeContent().isBlank()) {
            return;
        }

        try {
            String result = runExternalParser("python3", "src/main/resources/scripts/python_parser.py", snippet.getCodeContent());
            parseAndApplyResult(snippet, importList, result);
        } catch (Exception e) {
            log.warn("【Python 解析失败】文件 {}: {}", snippet.getFilePath(), e.getMessage());
        }
    }

    private void deepAnalyzeJavaScriptFile(CodeSnippet snippet, List<String> importList) {
        if (snippet.getCodeContent() == null || snippet.getCodeContent().isBlank()) {
            return;
        }

        try {
            String result = runExternalParser("node", "src/main/resources/scripts/js_parser.js", snippet.getCodeContent());
            parseAndApplyResult(snippet, importList, result);
        } catch (Exception e) {
            log.warn("【JS 解析失败】文件 {}: {}", snippet.getFilePath(), e.getMessage());
        }
    }

    private String runExternalParser(String command, String scriptPath, String code) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command, scriptPath);
        Process process = pb.start();

        try (java.io.OutputStream os = process.getOutputStream()) {
            os.write(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new ServiceException(ErrorCode.CODE_PARSE_FAILED, "代码解析失败，退出码: " + exitCode);
        }
        return output.toString();
    }

    private void parseAndApplyResult(CodeSnippet snippet, List<String> importList, String jsonResult) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonResult);
            if (root.has("error")) {
                log.warn("解析脚本返回错误: {}", root.get("error").asText());
                return;
            }

            if (root.has("className") && !root.get("className").isNull()) {
                snippet.setClassName(root.get("className").asText());
            }

            if (root.has("functions")) {
                root.get("functions").forEach(f -> {
                    String funcName = f.asText();
                    if (funcName.startsWith("innerClass:") || funcName.startsWith("method:")) {
                        snippet.getTags().add(funcName);
                    } else {
                        snippet.getTags().add("function:" + funcName);
                    }
                });
            }

            if (root.has("imports")) {
                root.get("imports").forEach(i -> importList.add(i.asText()));
            }
        } catch (Exception e) {
            log.error("解析 JSON 结果失败: {}", e.getMessage());
        }
    }

}

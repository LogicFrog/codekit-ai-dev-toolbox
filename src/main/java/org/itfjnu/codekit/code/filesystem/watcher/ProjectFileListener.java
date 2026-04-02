package org.itfjnu.codekit.code.filesystem.watcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.itfjnu.codekit.code.filesystem.LocalFileScanService;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件变更监听器：具体处理文件的新增、修改、删除事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectFileListener extends FileAlterationListenerAdaptor {

    private final LocalFileScanService localFileScanService;
    private final CodeSnippetService codeSnippetService;

    // 1. 处理文件创建
    @Override
    public void onFileCreate(File file) {
        log.info("[FileWatcher] 检测到文件创建: {}", file.getName());
        try {
            // 复用现有的单文件导入逻辑，自动处理后缀判断和解析
            localFileScanService.importSingleFile(file.getAbsolutePath(), null, null);
        } catch (Exception e) {
            log.error("自动导入新文件失败: {}", file.getName(), e);
        }
    }

    // 2. 处理文件修改
    @Override
    public void onFileChange(File file) {
        log.info("[FileWatcher] 检测到文件修改: {}", file.getName());
        try {
            // 修改和创建逻辑一样，importSingleFile 内部会对比 MD5，有变化才会更新
            localFileScanService.importSingleFile(file.getAbsolutePath(), null, null);
        } catch (Exception e) {
            log.error("自动更新文件失败: {}", file.getName(), e);
        }
    }

    // 3. 处理文件删除
    @Override
    public void onFileDelete(File file) {
        log.info("[FileWatcher] 检测到文件删除: {}", file.getName());
        try {
            // 联动删除数据库记录
            codeSnippetService.deleteByFilePath(file.getAbsolutePath());
        } catch (Exception e) {
            log.error("自动删除记录失败: {}", file.getName(), e);
        }
    }
}

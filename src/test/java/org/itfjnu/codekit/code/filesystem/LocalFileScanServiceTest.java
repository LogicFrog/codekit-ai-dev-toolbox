package org.itfjnu.codekit.code.filesystem;

import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.service.CodeManagerService;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFileScanServiceTest {

    @Mock
    private CodeManagerService codeManagerService;

    @InjectMocks
    private LocalFileScanService localFileScanService;

    @TempDir
    Path tempDir;

    private File testJavaFile;

    @BeforeEach
    void setUp() throws IOException {
        testJavaFile = tempDir.resolve("Test.java").toFile();
        try (FileWriter writer = new FileWriter(testJavaFile)) {
            writer.write("public class Test { }");
        }
    }

    @Test
    @DisplayName("扫描不存在的目录应抛出异常")
    void testScanLocalCodeDir_DirectoryNotExists() {
        assertThrows(BusinessException.class, () -> {
            localFileScanService.scanLocalCodeDir("/non/existent/directory");
        });
    }

    @Test
    @DisplayName("扫描有效目录返回true")
    void testScanLocalCodeDir_ValidDirectory() {
        lenient().when(codeManagerService.getAllFilePaths()).thenReturn(List.of());

        Boolean result = localFileScanService.scanLocalCodeDir(tempDir.toString());

        assertTrue(result);
    }

    @Test
    @DisplayName("导入单个文件成功")
    void testImportSingleFile_Success() throws Exception {
        when(codeManagerService.getCodeSnippetByPath(anyString())).thenReturn(null);
        when(codeManagerService.getCodeSnippetByMd5(anyString())).thenReturn(null);
        when(codeManagerService.saveOrUpdateCodeSnippet(any(CodeSnippet.class)))
                .thenAnswer(invocation -> {
                    CodeSnippet snippet = invocation.getArgument(0);
                    snippet.setId(1L);
                    return snippet;
                });

        CodeSnippet result = localFileScanService.importSingleFile(
                testJavaFile.getAbsolutePath(),
                "Java",
                "test"
        );

        assertNotNull(result);
        assertEquals("Java", result.getLanguageType());
    }

    @Test
    @DisplayName("导入不存在的文件应抛出异常")
    void testImportSingleFile_FileNotExists() {
        assertThrows(BusinessException.class, () -> {
            localFileScanService.importSingleFile("/non/existent/file.java", "Java", null);
        });
    }

    @Test
    @DisplayName("获取扫描状态 - 空闲状态")
    void testGetScanStatus_Idle() {
        ScanStatusDTO status = localFileScanService.getScanStatus(tempDir.toString());

        assertEquals("IDLE", status.getStatus());
        assertEquals(0, status.getProcessedCount());
    }

    @Test
    @DisplayName("文件移动时路径更新")
    void testImportSingleFile_FileMoved() throws Exception {
        CodeSnippet existingSnippet = new CodeSnippet();
        existingSnippet.setId(1L);
        existingSnippet.setFilePath("/old/path/Test.java");
        existingSnippet.setFileName("Test.java");
        existingSnippet.setCodeContent("public class Test { }");
        existingSnippet.setLanguageType("Java");
        existingSnippet.setFileMd5("testMd5");
        existingSnippet.setTags(new HashSet<>(Set.of("test")));

        when(codeManagerService.getCodeSnippetByPath(anyString())).thenReturn(null);
        when(codeManagerService.getCodeSnippetByMd5(anyString())).thenReturn(existingSnippet);
        when(codeManagerService.saveOrUpdateCodeSnippet(any(CodeSnippet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CodeSnippet result = localFileScanService.importSingleFile(
                testJavaFile.getAbsolutePath(),
                "Java",
                null
        );

        assertNotNull(result);
        assertEquals(testJavaFile.getAbsolutePath(), result.getFilePath());
        assertEquals("Test.java", result.getFileName());
    }

    @Test
    @DisplayName("同路径文件内容更新")
    void testImportSingleFile_ContentUpdate() throws Exception {
        CodeSnippet existingSnippet = new CodeSnippet();
        existingSnippet.setId(1L);
        existingSnippet.setFilePath(testJavaFile.getAbsolutePath());
        existingSnippet.setFileName("Test.java");
        existingSnippet.setCodeContent("public class OldContent { }");
        existingSnippet.setLanguageType("Java");
        existingSnippet.setFileMd5("oldMd5");
        existingSnippet.setTags(new HashSet<>(Set.of("test")));

        when(codeManagerService.getCodeSnippetByPath(testJavaFile.getAbsolutePath()))
                .thenReturn(existingSnippet);
        when(codeManagerService.saveOrUpdateCodeSnippet(any(CodeSnippet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CodeSnippet result = localFileScanService.importSingleFile(
                testJavaFile.getAbsolutePath(),
                "Java",
                null
        );

        assertNotNull(result);
        verify(codeManagerService, times(1)).saveOrUpdateCodeSnippet(any(CodeSnippet.class));
    }

    @Test
    @DisplayName("大文件跳过导入")
    void testImportSingleFile_LargeFileSkipped() throws IOException {
        File largeFile = tempDir.resolve("Large.java").toFile();
        try (FileWriter writer = new FileWriter(largeFile)) {
            writer.write("x".repeat(1024 * 1024 + 1));
        }

        CodeSnippet result = localFileScanService.importSingleFile(
                largeFile.getAbsolutePath(),
                "Java",
                null
        );

        assertNull(result);
    }

    @Test
    @DisplayName("指定语言类型和标签")
    void testImportSingleFile_WithLanguageAndTag() throws Exception {
        when(codeManagerService.getCodeSnippetByPath(anyString())).thenReturn(null);
        when(codeManagerService.getCodeSnippetByMd5(anyString())).thenReturn(null);
        when(codeManagerService.saveOrUpdateCodeSnippet(any(CodeSnippet.class)))
                .thenAnswer(invocation -> {
                    CodeSnippet snippet = invocation.getArgument(0);
                    snippet.setId(1L);
                    return snippet;
                });

        CodeSnippet result = localFileScanService.importSingleFile(
                testJavaFile.getAbsolutePath(),
                "Java",
                "Redis,Config"
        );

        assertNotNull(result);
        assertEquals("Java", result.getLanguageType());
        assertTrue(result.getTags().contains("Redis"));
        assertTrue(result.getTags().contains("Config"));
    }
}

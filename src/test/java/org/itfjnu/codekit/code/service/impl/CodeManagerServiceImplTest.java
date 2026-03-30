package org.itfjnu.codekit.code.service.impl;

import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeDependencyRepository;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeManagerServiceImplTest {

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @Mock
    private CodeDependencyRepository codeDependencyRepository;

    @Mock
    private VersionInfoRepository versionInfoRepository;

    @InjectMocks
    private CodeManagerServiceImpl codeManagerService;

    private CodeSnippet testSnippet;

    @BeforeEach
    void setUp() {
        testSnippet = new CodeSnippet();
        testSnippet.setId(1L);
        testSnippet.setFilePath("/test/Test.java");
        testSnippet.setFileName("Test.java");
        testSnippet.setCodeContent("public class Test {}");
        testSnippet.setLanguageType("Java");
        testSnippet.setFileMd5("abc123");
        testSnippet.setTags(new HashSet<>(Set.of("test")));
    }

    @Test
    @DisplayName("同路径文件重复扫描时应更新而不是新增")
    void testSaveOrUpdateCodeSnippet_SamePathUpdate() {
        CodeSnippet newSnippet = new CodeSnippet();
        newSnippet.setFilePath("/test/Test.java");
        newSnippet.setFileName("Test.java");
        newSnippet.setCodeContent("public class Test { // updated }");
        newSnippet.setLanguageType("Java");
        newSnippet.setFileMd5("newMd5");

        when(codeSnippetRepository.findByFilePath("/test/Test.java"))
                .thenReturn(Optional.of(testSnippet));
        when(codeSnippetRepository.save(any(CodeSnippet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CodeSnippet result = codeManagerService.saveOrUpdateCodeSnippet(newSnippet);

        assertNotNull(result);
        assertEquals("newMd5", result.getFileMd5());
        assertEquals("public class Test { // updated }", result.getCodeContent());
        verify(codeSnippetRepository, times(1)).save(any(CodeSnippet.class));
    }

    @Test
    @DisplayName("新建代码片段")
    void testSaveOrUpdateCodeSnippet_CreateNew() {
        CodeSnippet newSnippet = new CodeSnippet();
        newSnippet.setFilePath("/test/NewFile.java");
        newSnippet.setFileName("NewFile.java");
        newSnippet.setCodeContent("public class NewFile {}");
        newSnippet.setLanguageType("Java");
        newSnippet.setFileMd5("xyz789");

        when(codeSnippetRepository.findByFilePath("/test/NewFile.java"))
                .thenReturn(Optional.empty());
        when(codeSnippetRepository.save(any(CodeSnippet.class)))
                .thenAnswer(invocation -> {
                    CodeSnippet saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        CodeSnippet result = codeManagerService.saveOrUpdateCodeSnippet(newSnippet);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("/test/NewFile.java", result.getFilePath());
    }

    @Test
    @DisplayName("按ID更新代码片段")
    void testSaveOrUpdateCodeSnippet_UpdateById() {
        CodeSnippet updateSnippet = new CodeSnippet();
        updateSnippet.setId(1L);
        updateSnippet.setCodeContent("updated content");
        updateSnippet.setFileMd5("newMd5");

        when(codeSnippetRepository.findById(1L))
                .thenReturn(Optional.of(testSnippet));
        when(codeSnippetRepository.save(any(CodeSnippet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CodeSnippet result = codeManagerService.saveOrUpdateCodeSnippet(updateSnippet);

        assertEquals("updated content", result.getCodeContent());
        assertEquals("newMd5", result.getFileMd5());
    }

    @Test
    @DisplayName("删除snippet时依赖联动删除")
    void testDeleteCodeSnippetById_WithDependencies() {
        when(codeSnippetRepository.findById(1L))
                .thenReturn(Optional.of(testSnippet));
        doNothing().when(codeDependencyRepository).deleteByCodeSnippetId(1L);
        doNothing().when(versionInfoRepository).deleteBySnippetId(1L);
        doNothing().when(codeSnippetRepository).deleteById(1L);

        Boolean result = codeManagerService.deleteCodeSnippetById(1L);

        assertTrue(result);
        verify(codeDependencyRepository, times(1)).deleteByCodeSnippetId(1L);
        verify(versionInfoRepository, times(1)).deleteBySnippetId(1L);
        verify(codeSnippetRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的snippet应抛出异常")
    void testDeleteCodeSnippetById_NotFound() {
        when(codeSnippetRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            codeManagerService.deleteCodeSnippetById(999L);
        });
    }

    @Test
    @DisplayName("根据ID获取代码片段")
    void testGetCodeSnippetById() {
        when(codeSnippetRepository.findById(1L))
                .thenReturn(Optional.of(testSnippet));

        CodeSnippet result = codeManagerService.getCodeSnippetById(1L);

        assertNotNull(result);
        assertEquals("Test.java", result.getFileName());
    }

    @Test
    @DisplayName("根据ID获取不存在的代码片段返回null")
    void testGetCodeSnippetById_NotFound() {
        when(codeSnippetRepository.findById(999L))
                .thenReturn(Optional.empty());

        CodeSnippet result = codeManagerService.getCodeSnippetById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("分页查询代码片段")
    void testListCodeSnippetByPage() {
        List<CodeSnippet> snippets = List.of(testSnippet);
        Page<CodeSnippet> page = new PageImpl<>(snippets);
        Pageable pageable = PageRequest.of(0, 10);

        when(codeSnippetRepository.findAll(pageable)).thenReturn(page);

        Page<CodeSnippet> result = codeManagerService.listCodeSnippetByPage(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("根据语言类型查询代码片段")
    void testListCodeSnippetByLanguage() {
        List<CodeSnippet> snippets = List.of(testSnippet);

        when(codeSnippetRepository.findByLanguageType("Java")).thenReturn(snippets);

        List<CodeSnippet> result = codeManagerService.listCodeSnippetByLanguage("Java");

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getLanguageType());
    }

    @Test
    @DisplayName("检查文件路径是否存在")
    void testIsFilePathExists() {
        when(codeSnippetRepository.findByFilePath("/test/Test.java"))
                .thenReturn(Optional.of(testSnippet));

        assertTrue(codeManagerService.isFilePathExists("/test/Test.java"));
    }

    @Test
    @DisplayName("检查文件路径不存在")
    void testIsFilePathExists_NotExists() {
        when(codeSnippetRepository.findByFilePath("/test/NotExists.java"))
                .thenReturn(Optional.empty());

        assertFalse(codeManagerService.isFilePathExists("/test/NotExists.java"));
    }

    @Test
    @DisplayName("根据路径获取代码片段")
    void testGetCodeSnippetByPath() {
        when(codeSnippetRepository.findByFilePath("/test/Test.java"))
                .thenReturn(Optional.of(testSnippet));

        CodeSnippet result = codeManagerService.getCodeSnippetByPath("/test/Test.java");

        assertNotNull(result);
        assertEquals("/test/Test.java", result.getFilePath());
    }

    @Test
    @DisplayName("根据MD5获取代码片段")
    void testGetCodeSnippetByMd5() {
        when(codeSnippetRepository.findByFileMd5("abc123")).thenReturn(testSnippet);

        CodeSnippet result = codeManagerService.getCodeSnippetByMd5("abc123");

        assertNotNull(result);
        assertEquals("abc123", result.getFileMd5());
    }

    @Test
    @DisplayName("根据路径删除代码片段及其依赖")
    void testDeleteByFilePath() {
        when(codeSnippetRepository.findByFilePath("/test/Test.java"))
                .thenReturn(Optional.of(testSnippet));
        doNothing().when(codeDependencyRepository).deleteByCodeSnippetId(1L);
        doNothing().when(versionInfoRepository).deleteBySnippetId(1L);
        doNothing().when(codeSnippetRepository).deleteByFilePath("/test/Test.java");

        Boolean result = codeManagerService.deleteByFilePath("/test/Test.java");

        assertTrue(result);
        verify(codeDependencyRepository, times(1)).deleteByCodeSnippetId(1L);
        verify(codeSnippetRepository, times(1)).deleteByFilePath("/test/Test.java");
    }

    @Test
    @DisplayName("根据路径删除不存在的代码片段返回false")
    void testDeleteByFilePath_NotExists() {
        when(codeSnippetRepository.findByFilePath("/test/NotExists.java"))
                .thenReturn(Optional.empty());

        Boolean result = codeManagerService.deleteByFilePath("/test/NotExists.java");

        assertFalse(result);
    }

    @Test
    @DisplayName("保存依赖关系")
    void testSaveDependencies() {
        List<String> importList = List.of("java.util.List", "java.util.ArrayList");

        doNothing().when(codeDependencyRepository).deleteByCodeSnippetId(1L);
        when(codeDependencyRepository.saveAll(any())).thenReturn(List.of());

        Boolean result = codeManagerService.saveDependencies(1L, importList);

        assertTrue(result);
        verify(codeDependencyRepository, times(1)).deleteByCodeSnippetId(1L);
        verify(codeDependencyRepository, times(1)).saveAll(any());
    }
}

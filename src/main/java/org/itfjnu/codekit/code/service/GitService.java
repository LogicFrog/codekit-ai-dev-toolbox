package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.dto.GitCommitInfo;
import org.itfjnu.codekit.code.dto.GitStatusDTO;

import java.util.List;

public interface GitService {

    /**
     * 初始化 Git 仓库
     * @param repoPath 仓库路径（目录）
     * @return 是否初始化成功
     */
    boolean initRepository(String repoPath);

    /**
     * 提交所有变更
     * @param repoPath 仓库路径
     * @param message 提交消息
     * @return 新提交的哈希
     */
    String commit(String repoPath, String message);

    /**
     * 获取 Git 仓库状态
     */
    GitStatusDTO getStatus(String repoPath);

    /**
     * 获取提交历史
     * @param repoPath 仓库路径
     * @param maxCount 最大条数（0=全部）
     */
    List<GitCommitInfo> getHistory(String repoPath, int maxCount);

    /**
     * 获取两个提交之间的差异
     * @param repoPath 仓库路径
     * @param oldRef 旧引用（commit hash 或 HEAD~1）
     * @param newRef 新引用
     * @return 差异文本（unified diff）
     */
    String diff(String repoPath, String oldRef, String newRef);

    /**
     * 将文本内容写入文件并提交
     * @param repoPath 仓库路径
     * @param filePath 相对文件路径
     * @param content 文件内容
     * @param message 提交消息
     * @return 提交哈希
     */
    String writeAndCommit(String repoPath, String filePath, String content, String message);

    /**
     * 判断路径是否已有 Git 仓库
     */
    boolean isGitRepository(String repoPath);
}

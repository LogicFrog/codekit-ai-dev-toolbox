package org.itfjnu.codekit.code.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.itfjnu.codekit.code.dto.GitCommitInfo;
import org.itfjnu.codekit.code.dto.GitStatusDTO;
import org.itfjnu.codekit.code.service.GitService;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GitServiceImpl implements GitService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean initRepository(String repoPath) {
        try {
            Path path = Paths.get(repoPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            File gitDir = new File(path.toFile(), ".git");
            if (gitDir.exists()) {
                log.info("Git 仓库已存在: {}", repoPath);
                return true;
            }
            try (Git git = Git.init().setDirectory(path.toFile()).call()) {
                log.info("Git 仓库初始化成功: {}", repoPath);
                return true;
            }
        } catch (Exception e) {
            log.error("Git 仓库初始化失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String commit(String repoPath, String message) {
        try (Repository repo = openRepository(repoPath);
             Git git = new Git(repo)) {

            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit()
                    .setAuthor(getAuthor())
                    .setCommitter(getAuthor())
                    .setMessage(message)
                    .call();
            String hash = commit.getId().getName();
            log.info("Git 提交成功: {} - {}", hash.substring(0, 7), message);
            return hash;
        } catch (Exception e) {
            log.error("Git 提交失败: {}", e.getMessage());
            throw new RuntimeException("Git commit failed: " + e.getMessage(), e);
        }
    }

    @Override
    public GitStatusDTO getStatus(String repoPath) {
        try (Repository repo = openRepository(repoPath);
             Git git = new Git(repo)) {

            Status status = git.status().call();
            Set<String> changed = status.getModified();
            changed.addAll(status.getChanged());
            changed.addAll(status.getAdded());
            changed.addAll(status.getRemoved());

            return GitStatusDTO.builder()
                    .initialized(true)
                    .repoPath(repoPath)
                    .currentBranch(repo.getBranch())
                    .stagedCount(status.getAdded().size() + status.getChanged().size() + status.getRemoved().size())
                    .modifiedCount(status.getModified().size())
                    .untrackedCount(status.getUntracked().size())
                    .changedFiles(new ArrayList<>(changed))
                    .build();
        } catch (Exception e) {
            log.error("获取 Git 状态失败: {}", e.getMessage());
            return GitStatusDTO.builder()
                    .initialized(false)
                    .repoPath(repoPath)
                    .build();
        }
    }

    @Override
    public List<GitCommitInfo> getHistory(String repoPath, int maxCount) {
        List<GitCommitInfo> result = new ArrayList<>();
        try (Repository repo = openRepository(repoPath);
             Git git = new Git(repo)) {

            Iterable<RevCommit> commits = git.log().setMaxCount(maxCount > 0 ? maxCount : 50).call();
            for (RevCommit commit : commits) {
                GitCommitInfo info = GitCommitInfo.builder()
                        .shortHash(commit.getId().getName().substring(0, 7))
                        .fullHash(commit.getId().getName())
                        .message(commit.getShortMessage())
                        .author(commit.getAuthorIdent().getName())
                        .commitTime(formatTime(commit.getCommitTime()))
                        .build();
                result.add(info);
            }
        } catch (Exception e) {
            log.error("获取 Git 历史失败: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public String diff(String repoPath, String oldRef, String newRef) {
        try (Repository repo = openRepository(repoPath)) {
            ObjectId oldId = repo.resolve(oldRef + "^{tree}");
            ObjectId newId = repo.resolve(newRef + "^{tree}");

            if (oldId == null || newId == null) {
                return "无法解析引用: " + oldRef + " .. " + newRef;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectReader reader = repo.newObjectReader();
                 DiffFormatter df = new DiffFormatter(out)) {

                df.setRepository(repo);
                CanonicalTreeParser oldParser = new CanonicalTreeParser();
                oldParser.reset(reader, oldId);
                CanonicalTreeParser newParser = new CanonicalTreeParser();
                newParser.reset(reader, newId);

                List<DiffEntry> entries = df.scan(oldParser, newParser);
                for (DiffEntry entry : entries) {
                    df.format(entry);
                }
                df.flush();
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Git diff 失败: {}", e.getMessage());
            return "Diff error: " + e.getMessage();
        }
    }

    @Override
    public String writeAndCommit(String repoPath, String filePath, String content, String message) {
        try {
            Path fullPath = Paths.get(repoPath, filePath);
            Path parent = fullPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(fullPath, content, StandardCharsets.UTF_8);
            return commit(repoPath, message);
        } catch (IOException e) {
            log.error("写入文件失败: {}", e.getMessage());
            throw new RuntimeException("Write file failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isGitRepository(String repoPath) {
        File gitDir = new File(Paths.get(repoPath).toFile(), ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    private Repository openRepository(String repoPath) throws IOException {
        return new FileRepositoryBuilder()
                .setGitDir(new File(Paths.get(repoPath, ".git").toString()))
                .readEnvironment()
                .findGitDir()
                .build();
    }

    private PersonIdent getAuthor() {
        return new PersonIdent("CodeKit", "codekit@local");
    }

    private String formatTime(int epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FMT);
    }
}

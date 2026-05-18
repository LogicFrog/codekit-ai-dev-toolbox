package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.dto.*;
import org.itfjnu.codekit.code.model.VersionInfo;

import java.util.List;

public interface VersionInfoService {

    VersionInfo createVersion(Long snippetId, CreateVersionRequest request);

    List<VersionInfo> listVersions(Long snippetId);

    VersionRollbackResponse rollbackToVersion(Long snippetId, Long versionId);

    VersionDiffResponse compareVersions(Long snippetId, Long fromVersionId, Long toVersionId);

    VersionAnalyzeResponse analyzeVersions(Long snippetId, VersionAnalyzeRequest request);


}

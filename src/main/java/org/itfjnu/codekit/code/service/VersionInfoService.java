package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.dto.CreateVersionRequest;
import org.itfjnu.codekit.code.model.VersionInfo;

import java.util.List;

public interface VersionInfoService {

    VersionInfo createVersion(Long snippetId, CreateVersionRequest request);

    List<VersionInfo> listVersions(Long snippetId);
}

package org.itfjnu.codekit.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FsItem {
    private String name;        // 文件/文件夹名称
    private String path;        // 绝对路径
    private Boolean isDirectory; // 是否是目录
}

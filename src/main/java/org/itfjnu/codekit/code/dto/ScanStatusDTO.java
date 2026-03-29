package org.itfjnu.codekit.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanStatusDTO {
    /**
     * 扫描状态：IDLE / RUNNING / COMPLETED / FAILED
     */
    private String status;
    
    /**
     * 已处理文件数
     */
    private Integer processedCount;
    
    /**
     * 成功数量
     */
    private Integer successCount;
    
    /**
     * 跳过数量（内容未变）
     */
    private Integer skipCount;
    
    /**
     * 失败数量
     */
    private Integer failedCount;
    
    /**
     * 状态消息
     */
    private String message;
}

package org.itfjnu.codekit.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 豆包 API 请求体
 * 使用 OpenAI 兼容格式
 * 
 * 豆包 API 完全兼容 OpenAI API 格式
 * 文档：https://www.volcengine.com/docs/82379/1099475
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubaoRequest {

    /**
     * 接入点 ID（Endpoint ID）
     * 格式：ep-20250331-xxxxx
     * 在火山引擎控制台创建接入点时生成
     */
    private String model;

    /**
     * 消息列表
     * 包含 system、user、assistant 等角色的消息
     */
    private List<Message> messages;

    /**
     * 最大 token 数
     * 控制返回内容的长度
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数（0-2）
     * 控制输出的随机性
     * 0：最确定性，2：最随机
     * 默认：1
     */
    private Double temperature;

    /**
     * 消息对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：system（系统提示）、user（用户）、assistant（AI助手）
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    /**
     * 快速创建用户消息
     */
    public static DoubaoRequest ofUser(String model, String content, Integer maxTokens) {
        return DoubaoRequest.builder()
                .model(model)
                .messages(List.of(
                        Message.builder()
                                .role("user")
                                .content(content)
                                .build()
                ))
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * 创建带系统提示的请求
     */
    public static DoubaoRequest ofSystemAndUser(String model, String systemPrompt, 
                                                   String userContent, Integer maxTokens) {
        return DoubaoRequest.builder()
                .model(model)
                .messages(List.of(
                        Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        Message.builder()
                                .role("user")
                                .content(userContent)
                                .build()
                ))
                .maxTokens(maxTokens)
                .build();
    }
}

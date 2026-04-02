package org.itfjnu.codekit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 豆包 API 响应体
 * 使用 OpenAI 兼容格式
 * 
 * 豆包 API 完全兼容 OpenAI API 格式
 * 文档：https://www.volcengine.com/docs/82379/1099475
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoubaoResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 对象类型
     * 例如：chat.completion
     */
    private String object;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型名称（接入点 ID）
     */
    private String model;

    /**
     * 响应选择列表
     * 通常只有一个选择
     */
    private List<Choice> choices;

    /**
     * Token 使用情况
     */
    private Usage usage;

    /**
     * 选择对象
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        /**
         * 选择索引
         */
        private Integer index;

        /**
         * 消息内容
         */
        private Message message;

        /**
         * 结束原因
         * stop: 正常结束
         * length: 达到最大长度
         * content_filter: 内容过滤
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息对象
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        /**
         * 角色：assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    /**
     * Token 使用统计
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        /**
         * 提示词 token 数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 生成 token 数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总 token 数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 获取 AI 回复内容
     * @return AI 的回复文本，如果没有则返回 null
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Message message = choices.get(0).getMessage();
            if (message != null) {
                return message.getContent();
            }
        }
        return null;
    }

    /**
     * 获取结束原因
     * @return 结束原因，如果没有则返回 null
     */
    public String getFinishReason() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getFinishReason();
        }
        return null;
    }
}

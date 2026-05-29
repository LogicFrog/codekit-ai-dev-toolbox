package org.itfjnu.codekit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String role;
    private String content;
    private LocalDateTime time;
    private Integer tokenCount;

    public ChatMessage(String role, String content, LocalDateTime time) {
        this.role = role;
        this.content = content;
        this.time = time;
        this.tokenCount = null;
    }

    @JsonIgnore
    public int getEstimatedTokens() {
        if (tokenCount == null && content != null) {
            tokenCount = org.itfjnu.codekit.utils.TokenEstimator.estimateTokens(content);
        }
        return tokenCount != null ? tokenCount : 0;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
}

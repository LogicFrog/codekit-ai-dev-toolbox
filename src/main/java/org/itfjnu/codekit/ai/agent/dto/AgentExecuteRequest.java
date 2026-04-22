package org.itfjnu.codekit.ai.agent.dto;

import lombok.Data;

@Data
public class AgentExecuteRequest {

    /** 用户自然语言指令 */
    private String instruction;
    /** 可选 会话ID */
    private String sessionId;

}

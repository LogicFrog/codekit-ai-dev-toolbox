package org.itfjnu.codekit.ai.config;

import java.util.List;
import java.util.Map;

public enum LLMProvider {

    DOUBAO("doubao", "豆包 (Doubao)", "https://ark.cn-beijing.volces.com/api/v3",
            List.of("doubao-seed-2-0-pro-260215", "doubao-seed-2-0-lite-260215", "doubao-seed-2-0-pro-260315")),

    QWEN("qwen", "通义千问 (Qwen)", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            List.of("qwen-max", "qwen-plus", "qwen-turbo", "qwen-max-latest")),

    OPENAI("openai", "ChatGPT (OpenAI)", "https://api.openai.com/v1",
            List.of("gpt-4o", "gpt-4-turbo", "gpt-4o-mini", "o4-mini")),

    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1",
            List.of("deepseek-chat", "deepseek-reasoner", "deepseek-coder")),

    WENXIN("wenxin", "文心一言 (Wenxin)", "https://qianfan.baidubce.com/v2",
            List.of("ernie-4.0-turbo-128k", "ernie-3.5-128k", "ernie-4.5-128k"));

    private final String code;
    private final String displayName;
    private final String defaultBaseUrl;
    private final List<String> defaultModels;

    private static final Map<String, LLMProvider> BY_CODE = Map.of(
            "doubao", DOUBAO,
            "qwen", QWEN,
            "openai", OPENAI,
            "deepseek", DEEPSEEK,
            "wenxin", WENXIN,
            "real", DOUBAO
    );

    LLMProvider(String code, String displayName, String defaultBaseUrl, List<String> defaultModels) {
        this.code = code;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModels = defaultModels;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public List<String> getDefaultModels() { return defaultModels; }

    public static LLMProvider fromCode(String code) {
        if (code == null) return DOUBAO;
        return BY_CODE.getOrDefault(code.toLowerCase(), DOUBAO);
    }

    public static boolean isValid(String code) {
        return code != null && BY_CODE.containsKey(code.toLowerCase());
    }
}

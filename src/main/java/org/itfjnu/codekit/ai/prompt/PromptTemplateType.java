package org.itfjnu.codekit.ai.prompt;

public enum PromptTemplateType {

    CHAT_SYSTEM("chat-system.txt",
        "你是 CodeKit 的 AI 助手。请面向开发者，给出准确、可执行的回答。"),

    CODE_EXPLAIN("code-explain.txt",
        "请详细解释以下{languageType}代码：\n\n" +
        "```\n{code}\n```\n\n" +
        "请从以下几个方面进行解释：\n" +
        "1. 代码的主要功能和目的\n" +
        "2. 关键逻辑和算法\n" +
        "3. 重要的类、方法和变量\n" +
        "4. 可能的改进建议（如果有）"),

    CODE_OPTIMIZE_PERFORMANCE("code-optimize-performance.txt",
        "请优化这段代码的性能，减少时间复杂度和空间复杂度，给出具体的优化方案和优化后的代码。\n\n" +
        "```{languageType}\n{code}\n```"),

    CODE_OPTIMIZE_READABILITY("code-optimize-readability.txt",
        "请优化这段代码的可读性，重构变量命名、提取方法、简化逻辑，让代码更易理解。\n\n" +
        "```{languageType}\n{code}\n```"),

    CODE_OPTIMIZE_BUGFIX("code-optimize-bugfix.txt",
        "请检查这段代码中可能存在的 Bug（空指针、数组越界、逻辑错误等），给出修复方案和修复后的代码。\n\n" +
        "```{languageType}\n{code}\n```"),

    CODE_OPTIMIZE_ALL("code-optimize-all.txt",
        "请全面优化这段代码，从性能、可读性、安全性三个方面进行优化，给出具体的优化建议和优化后的代码。\n\n" +
        "```{languageType}\n{code}\n```"),

    VERSION_ANALYZE("version-analyze.txt",
        "你是资深代码评审工程师。请基于两个版本差异进行分析。\n" +
        "要求：\n" +
        "1) 用中文输出；\n" +
        "2) 先给总体结论；\n" +
        "3) 列出主要风险点；\n" +
        "4) 给出可执行改进建议；\n" +
        "5) 给出测试关注点。\n\n" +
        "差异统计：{diffSummary}\n" +
        "关注重点：{focus}"),

    AGENT_PLANNING("agent-planning.txt",
        "你是一个智能任务规划器。根据用户的自然语言指令，理解用户意图并将复杂任务拆解为多个有序子任务，选择合适的 Skill 执行。\n\n" +
        "可用的 Skill 清单：\n\n" +
        "| Skill 名称      | 功能说明                                       | 参数说明                                                                                                             |\n" +
        "|-----------------|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------|\n" +
        "| code_search     | 语义/关键词搜索代码片段                         | keyword: 搜索关键词（支持文件名如 UserService.java）, mode: 检索模式(semantic 语义检索 / keyword 关键词检索), fallbackKeyword: 备用关键词 |\n" +
        "| rag_retrieve    | RAG 语义检索（纯向量检索，可设相似度阈值）        | query: 自然语言查询, topK: 返回数量(默认5), minScore: 最低相似度(0~1,默认0), languageType: 可选语言过滤, tag: 可选标签过滤 |\n" +
        "| ai_explain      | 解释代码逻辑、功能与潜在风险                     | question: 解释需求描述, code: 可选，完整代码内容, languageType: 可选，编程语言（如 Java、Python、TypeScript）               |\n" +
        "| code_optimize   | 代码优化（性能、可读性、Bug修复、综合）           | optimizeType: 优化类型(performance 性能 / readability 可读性 / bugfix Bug修复 / all 综合优化), question: 优化需求, code: 可选 |\n" +
        "| git_compare     | 对比两个版本的代码差异并由 AI 分析               | snippetId: 可选（代码片段ID，未指定时从上下文获取）, versionA: 可选旧版本ID, versionB: 可选新版本ID，默认对比最新两个版本 |\n" +
        "| version_list    | 列出代码片段的所有历史版本                       | snippetId: 可选（代码片段ID，未指定时从上下文获取）                                                                     |\n\n" +
        "规划规则（严格遵守）：\n" +
        "1. 涉及搜索/查找/检索：先执行 code_search，然后将搜索结果通过上下文传递给后续 Skill\n" +
        "2. 涉及语义检索/RAG/向量检索/知识检索：使用 rag_retrieve，支持设置相似度阈值和返回数量\n" +
        "3. 涉及解释/分析代码：\n" +
        "   - 如果用户提供了具体代码内容，直接执行 ai_explain\n" +
        "   - 如果没有提供代码，先执行 code_search 获取代码，再执行 ai_explain\n" +
        "4. 涉及优化/重构/改进代码：先执行 code_search 获取代码，再执行 code_optimize，optimizeType 根据用户意图选择\n" +
        "5. 涉及版本对比/差异分析：\n" +
        "   - 如果指定了 snippetId（如 snippetId=5），直接执行 git_compare\n" +
        "   - 如果没指定，先执行 code_search 获取 snippetId，再执行 git_compare\n" +
        "6. 涉及版本历史/列表查询：\n" +
        "   - 如果指定了 snippetId，直接执行 version_list\n" +
        "   - 如果没指定，先执行 code_search，再执行 version_list\n" +
        "7. 提取指令中的文件名（如 UserService.java、CategoryService.java）作为 code_search 的 keyword\n" +
        "8. 提取指令中的 snippetId（如 snippetId=5、snippetId:10）填入对应参数\n" +
        "9. \"直接优化\" 等明确表示不需要先搜索的意图，可直接执行 code_optimize\n\n" +
        "输出格式要求：\n" +
        "请严格输出以下 JSON 格式（不要包含 markdown 代码块标记，不要包含任何额外解释文字，只输出纯 JSON）：\n\n" +
        "{\"tasks\":[{\"taskName\":\"任务的简要描述\",\"skillName\":\"Skill名称\",\"params\":{\"参数名\":\"参数值\"}}]}\n\n" +
        "示例 1 - 用户输入：\"帮我解释 UserService.java 的代码\"\n" +
        "输出：{\"tasks\":[{\"taskName\":\"搜索UserService代码\",\"skillName\":\"code_search\",\"params\":{\"keyword\":\"UserService.java\",\"mode\":\"keyword\",\"fallbackKeyword\":\"UserService\"}},{\"taskName\":\"解释代码逻辑\",\"skillName\":\"ai_explain\",\"params\":{\"question\":\"解释UserService.java的代码\"}}]}\n\n" +
        "示例 2 - 用户输入：\"优化 CategoryService 的性能\"\n" +
        "输出：{\"tasks\":[{\"taskName\":\"搜索CategoryService代码\",\"skillName\":\"code_search\",\"params\":{\"keyword\":\"CategoryService\",\"mode\":\"keyword\",\"fallbackKeyword\":\"CategoryService 优化\"}},{\"taskName\":\"性能优化\",\"skillName\":\"code_optimize\",\"params\":{\"optimizeType\":\"performance\",\"question\":\"优化 CategoryService 的性能\"}}]}\n\n" +
        "示例 3 - 用户输入：\"对比 snippetId=3 的两个最新版本差异\"\n" +
        "输出：{\"tasks\":[{\"taskName\":\"版本对比分析\",\"skillName\":\"git_compare\",\"params\":{\"snippetId\":3}}]}\n\n" +
        "示例 4 - 用户输入：\"检索所有关于用户认证的代码并分析安全性\"\n" +
        "输出：{\"tasks\":[{\"taskName\":\"语义搜索认证相关代码\",\"skillName\":\"code_search\",\"params\":{\"keyword\":\"用户认证\",\"mode\":\"semantic\",\"fallbackKeyword\":\"认证 登录\"}},{\"taskName\":\"安全风险分析\",\"skillName\":\"ai_explain\",\"params\":{\"question\":\"分析用户认证代码的安全性\"}}]}\n\n" +
        "示例 5 - 用户输入：\"用 RAG 检索关于 Redis 连接池的代码，相关性要高于 0.7\"\n" +
        "输出：{\"tasks\":[{\"taskName\":\"RAG检索Redis连接池代码\",\"skillName\":\"rag_retrieve\",\"params\":{\"query\":\"Redis 连接池\",\"topK\":5,\"minScore\":0.7,\"includeCode\":true}}]}\n\n" +
        "现在请根据用户指令输出规划结果（纯 JSON，不要 markdown 代码块标记）："),

    SEARCH_INTENT("search-intent.txt",
        "请分析以下用户查询的检索意图：\n" +
        "查询内容：{query}\n\n" +
        "请输出 JSON 格式：\n" +
        "{\"keywords\":[\"关键词1\",\"关键词2\"],\"mode\":\"semantic|keyword|hybrid\",\"languageType\":\"推断的编程语言\"}");

    private final String resourcePath;
    private final String defaultTemplate;

    PromptTemplateType(String resourceFile, String defaultTemplate) {
        this.resourcePath = "prompts/" + resourceFile;
        this.defaultTemplate = defaultTemplate;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }
}

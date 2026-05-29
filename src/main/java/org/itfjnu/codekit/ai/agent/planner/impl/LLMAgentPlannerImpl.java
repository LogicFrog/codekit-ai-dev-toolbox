package org.itfjnu.codekit.ai.agent.planner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.itfjnu.codekit.ai.agent.planner.AgentPlanner;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.DoubaoRequest;
import org.itfjnu.codekit.ai.dto.DoubaoResponse;
import org.itfjnu.codekit.ai.prompt.PromptTemplateType;
import org.itfjnu.codekit.ai.prompt.service.PromptTemplateService;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class LLMAgentPlannerImpl implements AgentPlanner {

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RuleBasedAgentPlannerImpl fallbackPlanner;
    private final PromptTemplateService promptTemplateService;

    private static final double PLANNER_TEMPERATURE = 0.3;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        if (!"real".equalsIgnoreCase(aiProperties.getProvider())) {
            log.info("LLMAgentPlanner: provider={}, will fall back to rule-based planner", aiProperties.getProvider());
            return;
        }
        if (!aiProperties.isConfigured()) {
            log.warn("LLMAgentPlanner: AI not configured, will fall back to rule-based planner");
            return;
        }
        restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(aiProperties.getTimeout());
                    setReadTimeout(aiProperties.getTimeout());
                }})
                .build();
        log.info("LLMAgentPlanner initialized, baseUrl={}, model={}", aiProperties.getBaseUrl(), aiProperties.getModel());
    }

    @Override
    public List<AgentTask> plan(String instruction) {
        if (restClient == null) {
            log.info("LLMAgentPlanner: client not initialized, falling back to rule-based planner");
            return fallbackPlanner.plan(instruction);
        }

        String text = instruction == null ? "" : instruction.trim();
        if (text.isEmpty()) {
            return fallbackPlanner.plan(text);
        }

        try {
            String systemPrompt = promptTemplateService.render(PromptTemplateType.AGENT_PLANNING, null);

            DoubaoRequest request = DoubaoRequest.ofSystemAndUser(
                    aiProperties.getModel(),
                    systemPrompt,
                    text,
                    aiProperties.getMaxTokens()
            );
            request.setTemperature(PLANNER_TEMPERATURE);

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> {
                        try (InputStream inputStream = res.getBody()) {
                            if (inputStream == null) {
                                throw new IOException("Empty response body");
                            }
                            byte[] bytes = inputStream.readAllBytes();
                            if (bytes.length == 0) {
                                throw new IOException("Zero-length response body");
                            }
                            return new String(bytes, StandardCharsets.UTF_8);
                        }
                    });

            DoubaoResponse response = objectMapper.readValue(responseBody, DoubaoResponse.class);
            String content = response.getContent();

            if (content == null || content.isBlank()) {
                log.warn("LLMAgentPlanner: empty LLM content, falling back to rule-based planner");
                return fallbackPlanner.plan(instruction);
            }

            log.debug("LLMAgentPlanner raw response: {}", content);

            List<AgentTask> tasks = parseTasks(content);
            if (tasks.isEmpty()) {
                log.warn("LLMAgentPlanner: parsed 0 tasks, falling back to rule-based planner");
                return fallbackPlanner.plan(instruction);
            }

            log.info("LLMAgentPlanner: planned {} tasks for instruction: {}", tasks.size(),
                    instruction.length() > 80 ? instruction.substring(0, 80) + "..." : instruction);
            return tasks;

        } catch (Exception e) {
            log.warn("LLMAgentPlanner failed: {}, falling back to rule-based planner", e.getMessage());
            return fallbackPlanner.plan(instruction);
        }
    }

    List<AgentTask> parseTasks(String llmResponse) {
        String json = llmResponse.trim();

        json = stripMarkdownCodeFence(json);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse LLM response as JSON: " + e.getMessage(), e);
        }
        JsonNode tasksNode = root.get("tasks");

        if (tasksNode == null || !tasksNode.isArray()) {
            throw new IllegalArgumentException("LLM response missing 'tasks' array, got: " + json);
        }

        List<AgentTask> tasks = new ArrayList<>();
        for (JsonNode taskNode : tasksNode) {
            String taskName = taskNode.has("taskName") ? taskNode.get("taskName").asText() : "";
            String skillName = taskNode.has("skillName") ? taskNode.get("skillName").asText() : "";

            Map<String, Object> params = new HashMap<>();
            JsonNode paramsNode = taskNode.get("params");
            if (paramsNode != null && paramsNode.isObject()) {
                var fields = paramsNode.fields();
                while (fields.hasNext()) {
                    var entry = fields.next();
                    JsonNode value = entry.getValue();
                    if (value.isTextual()) {
                        params.put(entry.getKey(), value.asText());
                    } else if (value.isIntegralNumber()) {
                        params.put(entry.getKey(), value.asLong());
                    } else if (value.isBoolean()) {
                        params.put(entry.getKey(), value.asBoolean());
                    } else if (value.isNumber()) {
                        params.put(entry.getKey(), value.asDouble());
                    } else {
                        params.put(entry.getKey(), value.asText());
                    }
                }
            }

            tasks.add(AgentTask.builder()
                    .taskName(taskName)
                    .skillName(skillName)
                    .params(params)
                    .build());
        }

        return tasks;
    }

    private String stripMarkdownCodeFence(String json) {
        String stripped = json;
        if (stripped.startsWith("```")) {
            int firstNewline = stripped.indexOf('\n');
            if (firstNewline > 0) {
                stripped = stripped.substring(firstNewline + 1);
            }
            if (stripped.endsWith("```")) {
                stripped = stripped.substring(0, stripped.length() - 3);
            }
        } else if (stripped.startsWith("```json")) {
            stripped = stripped.substring(7);
            if (stripped.endsWith("```")) {
                stripped = stripped.substring(0, stripped.length() - 3);
            }
        }
        return stripped.trim();
    }
}

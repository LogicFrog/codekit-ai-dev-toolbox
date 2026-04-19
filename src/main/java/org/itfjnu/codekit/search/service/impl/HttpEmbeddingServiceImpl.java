package org.itfjnu.codekit.search.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.search.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Primary
@RequiredArgsConstructor
public class HttpEmbeddingServiceImpl implements EmbeddingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.embedding-api}")
    private String embeddingApi;

    @Value("${ai.embedding-model:}")
    private String embeddingModel;

    @Value("${ai.embedding-api-key:${ai.api-key:}}")
    private String apiKey;


    @Override
    public List<Double> embedText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.EMBEDDING_TEXT_EMPTY);
        }

        String rawApi = embeddingApi == null ? "" : embeddingApi.trim();
        if (rawApi.isBlank()) {
            throw new BusinessException(ErrorCode.CONFIG_ERROR, "embedding-api 未配置");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new BusinessException(ErrorCode.CONFIG_ERROR, "embedding-model 未配置");
        }

        try {
            Map<String, Object> req = Map.of(
                    "model", embeddingModel,
                    "input", List.of(text)
            );

            String resp = postEmbedding(rawApi, req);
            return parseEmbeddingFromResponse(resp);
        } catch (RestClientResponseException e) {
            throw new BusinessException(
                    ErrorCode.EMBEDDING_API_REQUEST_FAILED,
                    "向量API调用失败: HTTP " + e.getStatusCode().value()
            );
        } catch (JsonProcessingException e) {
            log.error("向量响应解析失败", e);
            throw new BusinessException(ErrorCode.EMBEDDING_RESPONSE_PARSE_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量API调用失败", e);
            throw new BusinessException(ErrorCode.EMBEDDING_API_REQUEST_FAILED);
        }
    }

    private String postEmbedding(String uri, Map<String, Object> body) {
        return restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private List<Double> parseEmbeddingFromResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode embeddingNode = null;
        // OpenAI 兼容格式：data[0].embedding
        JsonNode dataNode = root.path("data");
        if (dataNode.isArray() && !dataNode.isEmpty()) {
            embeddingNode = dataNode.get(0).path("embedding");
        }
        // DashScope 原生格式：output.embeddings[0].embedding
        if ((embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty())) {
            JsonNode outputEmbeddings = root.path("output").path("embeddings");
            if (outputEmbeddings.isArray() && !outputEmbeddings.isEmpty()) {
                embeddingNode = outputEmbeddings.get(0).path("embedding");
            }
        }
        // 兜底：根节点直接 embedding
        if ((embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty())) {
            JsonNode direct = root.path("embedding");
            if (direct.isArray() && !direct.isEmpty()) {
                embeddingNode = direct;
            }
        }

        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new BusinessException(ErrorCode.EMBEDDING_RESULT_EMPTY);
        }

        List<Double> embedding = new java.util.ArrayList<>(embeddingNode.size());
        for (JsonNode n : embeddingNode) {
            if (n.isNumber()) {
                embedding.add(n.asDouble());
            } else if (n.isTextual()) {
                try {
                    embedding.add(Double.parseDouble(n.asText()));
                } catch (NumberFormatException ignore) {
                    // skip invalid value
                }
            }
        }
        if (embedding.isEmpty()) {
            throw new BusinessException(ErrorCode.EMBEDDING_RESULT_EMPTY);
        }
        return embedding;
    }

    @Override
    public String modelName() {
        return embeddingModel;
    }
}

package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.config.LLMProvider;
import org.itfjnu.codekit.ai.dto.AISettingsDTO;
import org.itfjnu.codekit.ai.dto.ProviderInfo;
import org.itfjnu.codekit.ai.service.AISettingsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AISettingsServiceImpl implements AISettingsService {

    @Value("${ai.settings.store-file:./data/ai-settings.json}")
    private String storeFile;

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RealAIServiceImpl realAIService;
    private final Object fileLock = new Object();

    public AISettingsServiceImpl(AIProperties aiProperties, ObjectMapper objectMapper,
                                  @Lazy @Qualifier("realAIServiceImpl") RealAIServiceImpl realAIService) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.realAIService = realAIService;
    }

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int AES_KEY_LENGTH = 256;

    private static final byte[] PBKDF2_SALT = "CodeKit@2026!Settings#Vault$Key%Derivation^Salt&Secure*".getBytes(StandardCharsets.UTF_8);

    private String editorTheme = "vs-dark";
    private Integer fontSize = 14;
    private Boolean autoSave = false;
    private Integer pageSize = 20;

    private final Map<String, String> providerApiKeys = new HashMap<>();
    private final Map<String, String> providerModels = new HashMap<>();

    @PostConstruct
    public void init() {
        loadFromDisk();
    }

    @Override
    public Double getTemperature() {
        return aiProperties.getTemperature();
    }

    @Override
    public Double setTemperature(Double value) {
        synchronized (fileLock) {
            aiProperties.setTemperature(value);
            flushToDiskSafe();
        }
        return aiProperties.getTemperature();
    }

    @Override
    public AISettingsDTO getAllSettings() {
        String maskedApiKey = maskApiKey(aiProperties.getApiKey());
        String maskedEmbeddingKey = maskApiKey(aiProperties.getEmbeddingApiKey());
        return AISettingsDTO.builder()
                .temperature(aiProperties.getTemperature())
                .provider(aiProperties.getProvider())
                .model(aiProperties.getModel())
                .apiKey(maskedApiKey)
                .baseUrl(aiProperties.getBaseUrl())
                .editorTheme(editorTheme)
                .fontSize(fontSize)
                .autoSave(autoSave)
                .pageSize(pageSize)
                .maxContextTokens(aiProperties.getMaxContextTokens())
                .contextWindowRounds(aiProperties.getMaxContextRounds())
                .embeddingApiKey(maskedEmbeddingKey)
                .build();
    }

    @Override
    public AISettingsDTO saveAllSettings(AISettingsDTO settings) {
        boolean needReconfigure = false;
        synchronized (fileLock) {
            if (settings.getTemperature() != null && settings.getTemperature() >= 0.0 && settings.getTemperature() <= 2.0) {
                aiProperties.setTemperature(settings.getTemperature());
            }
            if (settings.getProvider() != null && !settings.getProvider().isBlank()) {
                String newProvider = settings.getProvider().trim();
                if (!newProvider.equals(aiProperties.getProvider())) {
                    aiProperties.setProvider(newProvider);
                    LLMProvider llmProvider = LLMProvider.fromCode(newProvider);
                    aiProperties.setBaseUrl(llmProvider.getDefaultBaseUrl());
                    String savedModel = providerModels.get(newProvider);
                    aiProperties.setModel(savedModel != null ? savedModel : llmProvider.getDefaultModels().get(0));
                    String savedKey = providerApiKeys.get(newProvider);
                    aiProperties.setApiKey(savedKey != null ? savedKey : "");
                    needReconfigure = true;
                }
            }
            if (settings.getModel() != null && !settings.getModel().isBlank()) {
                aiProperties.setModel(settings.getModel().trim());
                providerModels.put(aiProperties.getProvider(), settings.getModel().trim());
            }
            if (settings.getBaseUrl() != null && !settings.getBaseUrl().isBlank()) {
                aiProperties.setBaseUrl(settings.getBaseUrl().trim());
                needReconfigure = true;
            }
            if (settings.getApiKey() != null && !settings.getApiKey().isBlank()) {
                String rawKey = settings.getApiKey().trim();
                if (!isMasked(rawKey)) {
                    aiProperties.setApiKey(rawKey);
                    providerApiKeys.put(aiProperties.getProvider(), rawKey);
                    needReconfigure = true;
                }
            }
            if (settings.getEmbeddingApiKey() != null && !settings.getEmbeddingApiKey().isBlank()) {
                String rawKey = settings.getEmbeddingApiKey().trim();
                if (!isMasked(rawKey)) {
                    aiProperties.setEmbeddingApiKey(rawKey);
                    needReconfigure = true;
                }
            }
            if (settings.getEditorTheme() != null && !settings.getEditorTheme().isBlank()) {
                editorTheme = settings.getEditorTheme().trim();
            }
            if (settings.getFontSize() != null && settings.getFontSize() >= 12 && settings.getFontSize() <= 24) {
                fontSize = settings.getFontSize();
            }
            if (settings.getAutoSave() != null) {
                autoSave = settings.getAutoSave();
            }
            if (settings.getPageSize() != null && settings.getPageSize() >= 1) {
                pageSize = settings.getPageSize();
            }
            if (settings.getMaxContextTokens() != null && settings.getMaxContextTokens() >= 512) {
                aiProperties.setMaxContextTokens(settings.getMaxContextTokens());
            }
            if (settings.getContextWindowRounds() != null && settings.getContextWindowRounds() >= 1) {
                aiProperties.setMaxContextRounds(settings.getContextWindowRounds());
            }
            flushToDiskSafe();
        }

        if (needReconfigure && realAIService != null) {
            realAIService.reconfigure();
        }

        return getAllSettings();
    }

    @Override
    public List<ProviderInfo> getAvailableProviders() {
        List<ProviderInfo> list = new ArrayList<>();
        for (LLMProvider provider : LLMProvider.values()) {
            list.add(ProviderInfo.builder()
                    .code(provider.getCode())
                    .displayName(provider.getDisplayName())
                    .defaultBaseUrl(provider.getDefaultBaseUrl())
                    .defaultModels(provider.getDefaultModels())
                    .build());
        }
        return list;
    }

    private boolean isMasked(String apiKey) {
        return apiKey.contains("*") || apiKey.length() <= 8;
    }

    private String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() <= 8) {
            return key;
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private void loadFromDisk() {
        try {
            Path path = Paths.get(storeFile);
            if (!Files.exists(path)) {
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                return;
            }
            AISettingsSnapshot snapshot = objectMapper.readValue(bytes, AISettingsSnapshot.class);
            if (snapshot.getTemperature() != null) {
                aiProperties.setTemperature(snapshot.getTemperature());
            }
            if (snapshot.getProvider() != null && !snapshot.getProvider().isBlank()) {
                aiProperties.setProvider(snapshot.getProvider());
            }
            if (snapshot.getBaseUrl() != null && !snapshot.getBaseUrl().isBlank()) {
                aiProperties.setBaseUrl(snapshot.getBaseUrl());
            }
            if (snapshot.getModel() != null && !snapshot.getModel().isBlank()) {
                aiProperties.setModel(snapshot.getModel());
            }
            if (snapshot.getEncryptedApiKey() != null && !snapshot.getEncryptedApiKey().isBlank()) {
                try {
                    String decrypted = decrypt(snapshot.getEncryptedApiKey());
                    aiProperties.setApiKey(decrypted);
                    log.info("已恢复加密的 API Key");
                } catch (Exception e) {
                    log.warn("API Key 解密失败: {}", e.getMessage());
                    if (snapshot.getApiKeyPlain() != null && !snapshot.getApiKeyPlain().isBlank()) {
                        aiProperties.setApiKey(snapshot.getApiKeyPlain());
                        log.info("API Key 回退至明文字段（兼容旧数据）");
                    }
                }
            } else if (snapshot.getApiKeyPlain() != null && !snapshot.getApiKeyPlain().isBlank()) {
                aiProperties.setApiKey(snapshot.getApiKeyPlain());
            }
            if (snapshot.getEncryptedEmbeddingApiKey() != null && !snapshot.getEncryptedEmbeddingApiKey().isBlank()) {
                try {
                    aiProperties.setEmbeddingApiKey(decrypt(snapshot.getEncryptedEmbeddingApiKey()));
                    log.info("已恢复加密的 Embedding API Key");
                } catch (Exception e) {
                    log.warn("Embedding API Key 解密失败: {}", e.getMessage());
                }
            }
            if (snapshot.getEditorTheme() != null) {
                editorTheme = snapshot.getEditorTheme();
            }
            if (snapshot.getFontSize() != null) {
                fontSize = snapshot.getFontSize();
            }
            if (snapshot.getAutoSave() != null) {
                autoSave = snapshot.getAutoSave();
            }
            if (snapshot.getPageSize() != null) {
                pageSize = snapshot.getPageSize();
            }
            if (snapshot.getMaxContextTokens() != null) {
                aiProperties.setMaxContextTokens(snapshot.getMaxContextTokens());
            }
            if (snapshot.getContextWindowRounds() != null) {
                aiProperties.setMaxContextRounds(snapshot.getContextWindowRounds());
            }
            if (snapshot.getProviderApiKeys() != null) {
                providerApiKeys.putAll(snapshot.getProviderApiKeys());
            }
            if (snapshot.getProviderModels() != null) {
                providerModels.putAll(snapshot.getProviderModels());
            }
            log.info("已从本地配置恢复: provider={}, temperature={}, model={}, editorTheme={}, fontSize={}",
                    aiProperties.getProvider(), aiProperties.getTemperature(), aiProperties.getModel(), editorTheme, fontSize);
        } catch (Exception e) {
            log.warn("读取 AI 设置失败，继续使用内存配置: {}", e.getMessage());
        }
    }

    private void flushToDiskSafe() {
        try {
            Path path = Paths.get(storeFile);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            AISettingsSnapshot snapshot = new AISettingsSnapshot();
            snapshot.setTemperature(aiProperties.getTemperature());
            snapshot.setProvider(aiProperties.getProvider());
            snapshot.setModel(aiProperties.getModel());
            snapshot.setBaseUrl(aiProperties.getBaseUrl());
            String rawApiKey = aiProperties.getApiKey();
            if (rawApiKey != null && !rawApiKey.isBlank()) {
                snapshot.setEncryptedApiKey(encrypt(rawApiKey));
            }
            String rawEmbeddingKey = aiProperties.getEmbeddingApiKey();
            if (rawEmbeddingKey != null && !rawEmbeddingKey.isBlank()) {
                snapshot.setEncryptedEmbeddingApiKey(encrypt(rawEmbeddingKey));
            }
            snapshot.setEditorTheme(editorTheme);
            snapshot.setFontSize(fontSize);
            snapshot.setAutoSave(autoSave);
            snapshot.setPageSize(pageSize);
            snapshot.setMaxContextTokens(aiProperties.getMaxContextTokens());
            snapshot.setContextWindowRounds(aiProperties.getMaxContextRounds());
            if (!providerApiKeys.isEmpty()) {
                snapshot.setProviderApiKeys(new HashMap<>(providerApiKeys));
            }
            if (!providerModels.isEmpty()) {
                snapshot.setProviderModels(new HashMap<>(providerModels));
            }

            Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), snapshot);

            try {
                Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("写入 AI 设置失败: {}", e.getMessage(), e);
        }
    }

    private String encrypt(String plaintext) throws Exception {
        byte[] salt = new byte[16];
        System.arraycopy(PBKDF2_SALT, 0, salt, 0, Math.min(PBKDF2_SALT.length, 16));

        KeySpec spec = new PBEKeySpec(getPassword(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    private String decrypt(String encryptedBase64) throws Exception {
        byte[] data = Base64.getDecoder().decode(encryptedBase64);

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        byte[] salt = new byte[16];
        System.arraycopy(PBKDF2_SALT, 0, salt, 0, Math.min(PBKDF2_SALT.length, 16));

        KeySpec spec = new PBEKeySpec(getPassword(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private char[] getPassword() {
        String vaultPassword = System.getenv("CODEKIT_VAULT_PASSWORD");
        if (vaultPassword != null && !vaultPassword.isBlank()) {
            return vaultPassword.toCharArray();
        }
        String hostname = "codekit";
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return (hostname + ":CodeKit-AI-Settings-Vault@2026").toCharArray();
    }

    @Data
    private static class AISettingsSnapshot {
        private Double temperature;
        private String provider;
        private String model;
        private String baseUrl;
        private String encryptedApiKey;
        private String encryptedEmbeddingApiKey;
        private String apiKeyPlain;
        private String editorTheme;
        private Integer fontSize;
        private Boolean autoSave;
        private Integer pageSize;
        private Integer maxContextTokens;
        private Integer contextWindowRounds;
        private Map<String, String> providerApiKeys;
        private Map<String, String> providerModels;
    }
}

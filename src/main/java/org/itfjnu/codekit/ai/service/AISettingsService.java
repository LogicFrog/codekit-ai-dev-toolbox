package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.AISettingsDTO;
import org.itfjnu.codekit.ai.dto.ProviderInfo;

import java.util.List;

public interface AISettingsService {
    Double getTemperature();
    Double setTemperature(Double value);

    AISettingsDTO getAllSettings();
    AISettingsDTO saveAllSettings(AISettingsDTO settings);

    List<ProviderInfo> getAvailableProviders();
}

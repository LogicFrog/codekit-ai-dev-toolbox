package org.itfjnu.codekit.search.service;

import java.util.List;

public interface EmbeddingService {
    List<Double> embedText(String text);
    String modelName();
}

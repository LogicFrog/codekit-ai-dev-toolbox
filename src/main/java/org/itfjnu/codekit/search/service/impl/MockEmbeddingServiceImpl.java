package org.itfjnu.codekit.search.service.impl;

import org.itfjnu.codekit.search.service.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Service
public class MockEmbeddingServiceImpl implements EmbeddingService {

    private static final int DIM = 256;

    @Override
    public List<Double> embedText(String text) {
        CRC32 crc32 = new CRC32();
        crc32.update(text.getBytes());
        long seed = crc32.getValue();

        return java.util.stream.IntStream.range(0, DIM)
                .mapToDouble(i -> {
                    long h = seed * 31 + i;
                    return (h % 1000) / 1000.0 - 0.5;
                })
                .boxed()
                .collect(Collectors.toList());
    }

    @Override
    public String modelName() {
        return "mock-embedding-v1";
    }
}
package org.itfjnu.codekit.utils;

public final class TokenEstimator {

    private TokenEstimator() {}

    private static final double CHARS_PER_TOKEN_CJK = 1.8;
    private static final double CHARS_PER_TOKEN_LATIN = 4.0;

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjkChars = 0;
        int latinChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                cjkChars++;
            } else {
                latinChars++;
            }
        }
        return (int) Math.ceil(cjkChars / CHARS_PER_TOKEN_CJK + latinChars / CHARS_PER_TOKEN_LATIN);
    }

    public static String truncateToTokenBudget(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int currentTokens = estimateTokens(text);
        if (currentTokens <= maxTokens) {
            return text;
        }

        double ratio = (double) maxTokens / currentTokens * 0.9;
        int targetChars = (int) (text.length() * ratio);

        int half = targetChars / 2;
        String firstHalf = text.substring(0, half);
        int lastStart = text.length() - half;
        if (lastStart <= half) {
            return text.substring(0, targetChars) + "...";
        }
        String lastHalf = text.substring(lastStart);

        return firstHalf + "\n\n... [中间内容已省略以控制 Token 用量] ...\n\n" + lastHalf;
    }

    public static String formatTokenUsage(int promptTokens, int completionTokens, int maxTokens) {
        if (maxTokens <= 0) {
            return String.format("prompt=%d, completion=%d, total=%d", promptTokens, completionTokens, promptTokens + completionTokens);
        }
        double usagePercent = (promptTokens + completionTokens) * 100.0 / maxTokens;
        return String.format("prompt=%d, completion=%d, total=%d/%d (%.1f%%)",
                promptTokens, completionTokens, promptTokens + completionTokens, maxTokens, usagePercent);
    }

    private static boolean isCJK(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
                || (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF);
    }
}

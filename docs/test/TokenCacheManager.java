package com.example.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TokenCacheManager {

    private static final long DEFAULT_TTL_SECONDS = 3600;
    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();

    public void put(String token, String userId) {
        store.put(token, new CacheEntry(userId, System.currentTimeMillis()));
    }

    public String getUserId(String token) {
        CacheEntry entry = store.get(token);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            store.remove(token);
            return null;
        }
        return entry.userId;
    }

    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.createTime > TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SECONDS);
    }

    public void evict(String token) {
        store.remove(token);
    }

    private static class CacheEntry {
        final String userId;
        final long createTime;

        CacheEntry(String userId, long createTime) {
            this.userId = userId;
            this.createTime = createTime;
        }
    }
}

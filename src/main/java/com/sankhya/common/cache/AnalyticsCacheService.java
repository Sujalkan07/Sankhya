package com.sankhya.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe, multi-tenant in-memory cache service for expensive analytical calculations
 * and store metadata readiness. Completely isolated by workspace and cache bucket.
 */
@Service
public class AnalyticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheService.class);

    private static final class CacheEntry<T> {
        final T value;
        final Instant expiresAt;

        CacheEntry(T value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Map<String, CacheEntry<?>>> cacheBuckets = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String bucket, String key, Duration ttl, Supplier<T> supplier) {
        if (bucket == null || key == null || supplier == null) {
            return supplier != null ? supplier.get() : null;
        }

        Map<String, CacheEntry<?>> bucketMap = cacheBuckets.computeIfAbsent(bucket, b -> new ConcurrentHashMap<>());
        CacheEntry<?> existing = bucketMap.get(key);

        if (existing != null && !existing.isExpired()) {
            return (T) existing.value;
        }

        // Compute fresh value
        long start = System.currentTimeMillis();
        T computed = supplier.get();
        long elapsed = System.currentTimeMillis() - start;

        if (computed != null) {
            Duration effectiveTtl = (ttl != null && !ttl.isZero() && !ttl.isNegative()) ? ttl : Duration.ofMinutes(10);
            bucketMap.put(key, new CacheEntry<>(computed, Instant.now().plus(effectiveTtl)));
            log.debug("Cache miss for [{}:{}] - computed in {} ms, stored with TTL {}s",
                    bucket, key, elapsed, effectiveTtl.toSeconds());
        }

        return computed;
    }

    public void invalidateBucket(String bucket) {
        if (bucket != null) {
            Map<String, CacheEntry<?>> removed = cacheBuckets.remove(bucket);
            if (removed != null) {
                log.debug("Invalidated cache bucket '{}' ({} entries cleared)", bucket, removed.size());
            }
        }
    }

    public void invalidateWorkspace(Long workspaceId) {
        if (workspaceId == null) {
            clear();
            return;
        }
        String prefix = "ws-" + workspaceId;
        cacheBuckets.values().forEach(bucketMap -> {
            bucketMap.keySet().removeIf(key -> key.startsWith(prefix) || key.contains(":" + prefix + ":") || key.equals(prefix));
        });
        log.info("Invalidated all cached analytical data for workspace {}", workspaceId);
    }

    public void clear() {
        cacheBuckets.clear();
        log.info("Cleared entire analytical cache");
    }

    public int totalCachedEntries() {
        return cacheBuckets.values().stream().mapToInt(Map::size).sum();
    }
}

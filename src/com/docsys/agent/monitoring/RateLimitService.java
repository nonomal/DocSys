package com.docsys.agent.monitoring;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter using Bucket4j.
 * Limits requests per IP (or per API key when provided).
 * Also tracks concurrent SSE connections per IP.
 */
@Service
public class RateLimitService {

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.burst-capacity:100}")
    private int burstCapacity;

    @Value("${rate-limit.sse-max-concurrent-per-ip:3}")
    private int sseMaxConcurrentPerIp;

    // Per-IP (or per-API-key) buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Concurrent SSE connections per IP
    private final Map<String, Integer> sseConnections = new ConcurrentHashMap<>();

    /**
     * Resolve the bucket for a given client identity.
     * Uses API key if provided, otherwise falls back to IP address.
     */
    private Bucket resolveBucket(String ip, String apiKey) {
        String key = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : ip;
        return buckets.computeIfAbsent(key, k -> createBucket());
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            burstCapacity,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket4j.builder().addLimit(limit).build();
    }

    /**
     * Try to consume one token from the bucket.
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsume(String ip, String apiKey) {
        Bucket bucket = resolveBucket(ip, apiKey);
        return bucket.tryConsume(1);
    }

    /**
     * Get seconds until the next token is available (for Retry-After header).
     */
    public long getSecondsUntilNextAvailable(String ip, String apiKey) {
        Bucket bucket = resolveBucket(ip, apiKey);
        return bucket.estimateAbilityToConsume(1)
            .getNanosToWaitForRefill() / 1_000_000_000;
    }

    // ===== SSE concurrent connection limiting =====

    /**
     * Check if a new SSE connection is allowed for this IP.
     * @return true if allowed, false if max concurrent connections reached
     */
    public boolean tryAcquireSseSlot(String ip) {
        int current = sseConnections.getOrDefault(ip, 0);
        if (current >= sseMaxConcurrentPerIp) {
            return false;
        }
        sseConnections.merge(ip, 1, Integer::sum);
        return true;
    }

    /**
     * Release an SSE slot when the connection closes.
     */
    public void releaseSseSlot(String ip) {
        sseConnections.computeIfPresent(ip, (k, v) -> v > 1 ? v - 1 : null);
    }

    /**
     * Get current SSE connection count for an IP.
     */
    public int getSseConnectionCount(String ip) {
        return sseConnections.getOrDefault(ip, 0);
    }

    /**
     * Get configured requests-per-minute limit.
     */
    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    /**
     * Get configured max concurrent SSE connections per IP.
     */
    public int getSseMaxConcurrentPerIp() {
        return sseMaxConcurrentPerIp;
    }
}

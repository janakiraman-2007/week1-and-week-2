package week2;

import java.util.concurrent.*;
import java.util.*;

public class RateLimiter {

    // Token Bucket class
    static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerMillis; // tokens per ms

        private double tokens;
        private long lastRefillTime;

        public TokenBucket(int maxTokens, int refillPerHour) {
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            this.refillRatePerMillis = refillPerHour / (60.0 * 60 * 1000);
            this.lastRefillTime = System.currentTimeMillis();
        }

        // Refill tokens based on elapsed time
        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            double tokensToAdd = elapsed * refillRatePerMillis;
            tokens = Math.min(maxTokens, tokens + tokensToAdd);

            lastRefillTime = now;
        }

        // Try consuming a token
        public synchronized boolean allowRequest() {
            refill();

            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        public synchronized int getRemainingTokens() {
            refill();
            return (int) tokens;
        }

        public synchronized long getRetryAfterMillis() {
            if (tokens >= 1) return 0;

            double missingTokens = 1 - tokens;
            return (long) (missingTokens / refillRatePerMillis);
        }
    }

    // clientId -> TokenBucket
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

    private final int MAX_REQUESTS = 1000; // per hour

    // Check rate limit
    public String checkRateLimit(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(
                clientId,
                k -> new TokenBucket(MAX_REQUESTS, MAX_REQUESTS)
        );

        if (bucket.allowRequest()) {
            return "Allowed (" + bucket.getRemainingTokens() + " requests remaining)";
        } else {
            long retryMs = bucket.getRetryAfterMillis();
            return "Denied (0 remaining, retry after " + (retryMs / 1000) + "s)";
        }
    }

    // Get status
    public Map<String, Object> getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);

        if (bucket == null) return Collections.emptyMap();

        Map<String, Object> status = new HashMap<>();
        status.put("limit", MAX_REQUESTS);
        status.put("remaining", bucket.getRemainingTokens());
        status.put("used", MAX_REQUESTS - bucket.getRemainingTokens());
        status.put("retryAfterSec", bucket.getRetryAfterMillis() / 1000);

        return status;
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        RateLimiter limiter = new RateLimiter();

        String client = "abc123";

        // Simulate requests
        for (int i = 1; i <= 1005; i++) {
            String result = limiter.checkRateLimit(client);
            System.out.println("Request " + i + ": " + result);
        }

        // Status
        System.out.println("\nStatus: " + limiter.getRateLimitStatus(client));
    }
}
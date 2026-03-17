package week1;
import java.util.*;
import java.util.concurrent.*;

public class DNSCacheSystem {

    // Entry class
    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime;

        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // LRU Cache using LinkedHashMap
    private final Map<String, DNSEntry> cache;

    private final int capacity;

    // Metrics
    private long hits = 0;
    private long misses = 0;

    public DNSCacheSystem(int capacity) {
        this.capacity = capacity;

        this.cache = new LinkedHashMap<String, DNSEntry>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > DNSCacheSystem.this.capacity;
            }
        };

        startCleanupThread();
    }

    // Resolve domain
    public synchronized String resolve(String domain) {
        long startTime = System.nanoTime();

        DNSEntry entry = cache.get(domain);

        if (entry != null) {
            if (!entry.isExpired()) {
                hits++;
                logTime(startTime, "HIT");
                return entry.ipAddress;
            } else {
                cache.remove(domain); // expired
            }
        }

        // Cache MISS
        misses++;
        String ip = queryUpstreamDNS(domain);

        // Assume TTL = 5 seconds for demo
        cache.put(domain, new DNSEntry(domain, ip, 5));

        logTime(startTime, "MISS");
        return ip;
    }

    // Simulated upstream DNS call (~100ms)
    private String queryUpstreamDNS(String domain) {
        try {
            Thread.sleep(100); // simulate latency
        } catch (InterruptedException ignored) {}

        // Fake IP generator
        return "192.168.1." + new Random().nextInt(255);
    }

    // Cleanup expired entries periodically
    private void startCleanupThread() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);

                    synchronized (this) {
                        Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();

                        while (it.hasNext()) {
                            Map.Entry<String, DNSEntry> entry = it.next();
                            if (entry.getValue().isExpired()) {
                                it.remove();
                            }
                        }
                    }

                } catch (InterruptedException ignored) {}
            }
        });

        cleaner.setDaemon(true);
        cleaner.start();
    }

    // Metrics
    public synchronized void getCacheStats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0 : (hits * 100.0) / total;

        System.out.println("Cache Hits: " + hits);
        System.out.println("Cache Misses: " + misses);
        System.out.println("Hit Rate: " + String.format("%.2f", hitRate) + "%");
    }

    private void logTime(long startTime, String type) {
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println(type + " lookup took " + duration + " ms");
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) throws InterruptedException {
        DNSCacheSystem dns = new DNSCacheSystem(3);

        // First call → MISS
        System.out.println("IP: " + dns.resolve("google.com"));

        // Second call → HIT
        System.out.println("IP: " + dns.resolve("google.com"));

        // Wait for TTL expiry
        Thread.sleep(6000);

        // Expired → MISS again
        System.out.println("IP: " + dns.resolve("google.com"));

        // Add more entries (test LRU eviction)
        dns.resolve("facebook.com");
        dns.resolve("amazon.com");
        dns.resolve("openai.com"); // triggers eviction

        dns.getCacheStats();
    }
}
package week2;

import java.util.*;

// Video data class
class VideoData {
    String videoId;
    String content; // Simplified for demo
    public VideoData(String videoId, String content) {
        this.videoId = videoId;
        this.content = content;
    }
}

// Multi-level cache system
public class MultiLevelCache {

    // -------- L1 Cache: In-Memory (LinkedHashMap for LRU) --------
    private final int L1_CAPACITY = 10000;
    private final LinkedHashMap<String, VideoData> L1 = new LinkedHashMap<>(L1_CAPACITY, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
            return size() > L1_CAPACITY;
        }
    };

    // -------- L2 Cache: SSD-backed (simulated with HashMap + access count) --------
    private final int L2_CAPACITY = 100000;
    private final HashMap<String, VideoData> L2 = new HashMap<>();
    private final HashMap<String, Integer> L2AccessCount = new HashMap<>();
    private final int PROMOTION_THRESHOLD = 5;

    // -------- L3 Database: simulated --------
    private final HashMap<String, VideoData> L3 = new HashMap<>();

    // -------- Statistics --------
    private int L1Hits = 0, L1Misses = 0;
    private int L2Hits = 0, L2Misses = 0;
    private int L3Hits = 0, L3Misses = 0;
    private long L1Time = 0, L2Time = 0, L3Time = 0;

    // Add video to database
    public void addVideoToDB(String videoId, String content) {
        L3.put(videoId, new VideoData(videoId, content));
    }

    // Get video with multi-level cache lookup
    public VideoData getVideo(String videoId) {
        // L1 cache
        long start = System.nanoTime();
        if (L1.containsKey(videoId)) {
            L1Hits++;
            L1Time += 500_000; // simulated 0.5ms
            return L1.get(videoId);
        }
        L1Misses++;
        L1Time += 500_000;

        // L2 cache
        if (L2.containsKey(videoId)) {
            L2Hits++;
            L2Time += 5_000_000; // simulated 5ms
            // Update access count
            L2AccessCount.put(videoId, L2AccessCount.getOrDefault(videoId, 0) + 1);
            // Promote to L1 if threshold reached
            if (L2AccessCount.get(videoId) >= PROMOTION_THRESHOLD) {
                promoteToL1(videoId);
            }
            return L2.get(videoId);
        }
        L2Misses++;
        L2Time += 5_000_000;

        // L3 database
        if (L3.containsKey(videoId)) {
            L3Hits++;
            L3Time += 150_000_000; // simulated 150ms
            // Add to L2 cache
            if (L2.size() >= L2_CAPACITY) {
                // Remove a random entry to simulate eviction
                String toRemove = L2.keySet().iterator().next();
                L2.remove(toRemove);
                L2AccessCount.remove(toRemove);
            }
            L2.put(videoId, L3.get(videoId));
            L2AccessCount.put(videoId, 1);
            return L3.get(videoId);
        }
        L3Misses++;
        L3Time += 150_000_000;
        return null; // video not found
    }

    private void promoteToL1(String videoId) {
        VideoData video = L2.get(videoId);
        L1.put(videoId, video);
        // Reset L2 access count
        L2AccessCount.put(videoId, 0);
    }

    // Cache statistics
    public void getStatistics() {
        int totalRequests = L1Hits + L1Misses;
        System.out.println("L1: Hit Rate " + (L1Hits*100.0/totalRequests) + "%, Avg Time: " + (L1Time/1_000_000.0/totalRequests) + "ms");
        totalRequests = L2Hits + L2Misses;
        System.out.println("L2: Hit Rate " + (L2Hits*100.0/totalRequests) + "%, Avg Time: " + (L2Time/1_000_000.0/totalRequests) + "ms");
        totalRequests = L3Hits + L3Misses;
        System.out.println("L3: Hit Rate " + (L3Hits*100.0/totalRequests) + "%, Avg Time: " + (L3Time/1_000_000.0/totalRequests) + "ms");

        int overallHits = L1Hits + L2Hits + L3Hits;
        int overallRequests = overallHits + L1Misses + L2Misses + L3Misses;
        double overallAvgTime = (L1Time + L2Time + L3Time) / 1_000_000.0 / overallRequests;
        System.out.println("Overall Hit Rate: " + (overallHits*100.0/overallRequests) + "%, Avg Time: " + overallAvgTime + "ms");
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        MultiLevelCache cache = new MultiLevelCache();

        // Add some videos to DB
        cache.addVideoToDB("video_123", "Video Content 123");
        cache.addVideoToDB("video_999", "Video Content 999");

        System.out.println("First request video_123:");
        cache.getVideo("video_123"); // L1 MISS, L2 MISS, L3 HIT → promote to L2
        System.out.println("Second request video_123:");
        cache.getVideo("video_123"); // L2 HIT → maybe promote to L1 if threshold
        System.out.println("Request video_999:");
        cache.getVideo("video_999"); // L3 HIT → added to L2

        System.out.println("\nCache statistics:");
        cache.getStatistics();
    }
}
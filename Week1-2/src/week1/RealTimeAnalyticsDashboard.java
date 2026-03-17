package week1;
import java.util.*;
import java.util.concurrent.*;

public class RealTimeAnalyticsDashboard {

    // pageUrl -> total visits
    private final ConcurrentHashMap<String, Integer> pageViews = new ConcurrentHashMap<>();

    // pageUrl -> unique users
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();

    // source -> count
    private final ConcurrentHashMap<String, Integer> trafficSources = new ConcurrentHashMap<>();

    // Process incoming event
    public void processEvent(String url, String userId, String source) {

        // Count page views
        pageViews.merge(url, 1, Integer::sum);

        // Track unique users per page
        uniqueVisitors
                .computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        // Track traffic source
        trafficSources.merge(source, 1, Integer::sum);
    }

    // Get Top N pages
    public List<Map.Entry<String, Integer>> getTopPages(int n) {
        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : pageViews.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > n) {
                minHeap.poll();
            }
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> b.getValue() - a.getValue());
        return result;
    }

    // Dashboard output
    public void getDashboard() {
        System.out.println("\n===== REAL-TIME DASHBOARD =====");

        // Top pages
        List<Map.Entry<String, Integer>> topPages = getTopPages(10);

        System.out.println("Top Pages:");
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPages) {
            String url = entry.getKey();
            int views = entry.getValue();
            int unique = uniqueVisitors.getOrDefault(url, Collections.emptySet()).size();

            System.out.println(rank++ + ". " + url +
                    " - " + views + " views (" + unique + " unique)");
        }

        // Traffic sources %
        System.out.println("\nTraffic Sources:");
        int total = trafficSources.values().stream().mapToInt(i -> i).sum();

        for (Map.Entry<String, Integer> entry : trafficSources.entrySet()) {
            double percent = (entry.getValue() * 100.0) / total;
            System.out.printf("%s: %.2f%%\n", entry.getKey(), percent);
        }

        System.out.println("================================\n");
    }

    // Start auto-refresh every 5 seconds
    public void startDashboard() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::getDashboard, 0, 5, TimeUnit.SECONDS);
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) throws InterruptedException {
        RealTimeAnalyticsDashboard system = new RealTimeAnalyticsDashboard();

        system.startDashboard();

        String[] urls = {
                "/article/breaking-news",
                "/sports/championship",
                "/tech/ai",
                "/health/tips"
        };

        String[] sources = {"google", "facebook", "direct", "twitter"};

        Random rand = new Random();

        // Simulate real-time traffic
        for (int i = 1; i <= 100; i++) {
            String url = urls[rand.nextInt(urls.length)];
            String user = "user_" + rand.nextInt(50); // repeat users
            String source = sources[rand.nextInt(sources.length)];

            system.processEvent(url, user, source);

            Thread.sleep(50); // simulate streaming
        }
    }
}
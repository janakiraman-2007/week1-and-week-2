package week1;

import java.util.*;
import java.util.concurrent.*;

public class UsernameCheckerSystem {

    // Stores username -> userId
    private final ConcurrentHashMap<String, Integer> usernameMap = new ConcurrentHashMap<>();

    // Stores username -> attempt count
    private final ConcurrentHashMap<String, Integer> attemptCount = new ConcurrentHashMap<>();

    // Check if username is available (O(1))
    public boolean checkAvailability(String username) {
        attemptCount.merge(username, 1, Integer::sum);
        return !usernameMap.containsKey(username);
    }

    // Register a username
    public boolean register(String username, int userId) {
        return usernameMap.putIfAbsent(username, userId) == null;
    }

    // Suggest alternative usernames
    public List<String> suggestAlternatives(String username) {
        List<String> result = new ArrayList<>();

        // Strategy 1: Append numbers
        for (int i = 1; i <= 5 && result.size() < 5; i++) {
            String candidate = username + i;
            if (!usernameMap.containsKey(candidate)) {
                result.add(candidate);
            }
        }

        // Strategy 2: Replace underscore with dot
        if (result.size() < 5 && username.contains("_")) {
            String alt = username.replace("_", ".");
            if (!usernameMap.containsKey(alt)) {
                result.add(alt);
            }
        }

        // Strategy 3: Add prefix/suffix
        if (result.size() < 5) {
            String alt1 = "the_" + username;
            if (!usernameMap.containsKey(alt1)) {
                result.add(alt1);
            }
        }

        if (result.size() < 5) {
            String alt2 = username + "_official";
            if (!usernameMap.containsKey(alt2)) {
                result.add(alt2);
            }
        }

        return result;
    }

    // Get most attempted username
    public String getMostAttempted() {
        String top = null;
        int max = 0;

        for (Map.Entry<String, Integer> entry : attemptCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        UsernameCheckerSystem system = new UsernameCheckerSystem();

        // Existing users
        system.register("john_doe", 101);
        system.register("admin", 1);

        // Check availability
        System.out.println(system.checkAvailability("john_doe"));   // false
        System.out.println(system.checkAvailability("jane_smith")); // true

        // Suggestions
        System.out.println(system.suggestAlternatives("john_doe"));

        // Simulate traffic
        for (int i = 0; i < 10; i++) system.checkAvailability("admin");
        for (int i = 0; i < 5; i++) system.checkAvailability("john_doe");

        // Most attempted
        System.out.println(system.getMostAttempted()); // admin
    }
}
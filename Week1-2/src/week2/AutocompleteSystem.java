package week2;

import java.util.*;

public class AutocompleteSystem {

    // Trie Node class
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWord;
        String query;
        int frequency;
    }

    private final TrieNode root = new TrieNode();
    private final int TOP_K = 10;

    // Insert or update query in Trie
    public void addQuery(String query, int freq) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isWord = true;
        node.query = query;
        node.frequency += freq; // cumulative frequency
    }

    // Update frequency of an existing query
    public void updateFrequency(String query) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return; // query not found
        }
        if (node.isWord) node.frequency++;
    }

    // Get top K autocomplete suggestions for prefix
    public List<String> getSuggestions(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }

        // Min-heap for top K by frequency
        PriorityQueue<TrieNode> minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency));

        dfs(node, minHeap);

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) result.add(0, minHeap.poll().query); // reverse order
        return result;
    }

    // DFS traversal to collect top K
    private void dfs(TrieNode node, PriorityQueue<TrieNode> heap) {
        if (node.isWord) {
            heap.offer(node);
            if (heap.size() > TOP_K) heap.poll();
        }
        for (TrieNode child : node.children.values()) {
            dfs(child, heap);
        }
    }

    // Simple typo-tolerant suggestions (1 character difference)
    public List<String> getTypoSuggestions(String prefix) {
        Set<String> result = new HashSet<>();
        for (String suggestion : getSuggestions(prefix.substring(0, Math.max(1, prefix.length() - 1)))) {
            if (isSimilar(suggestion, prefix)) result.add(suggestion);
        }
        return new ArrayList<>(result);
    }

    private boolean isSimilar(String s1, String s2) {
        if (Math.abs(s1.length() - s2.length()) > 1) return false;
        int mismatches = 0;
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++) {
            if (s1.charAt(i) != s2.charAt(i)) mismatches++;
            if (mismatches > 1) return false;
        }
        return true;
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        AutocompleteSystem ac = new AutocompleteSystem();

        // Add queries
        ac.addQuery("java tutorial", 1234567);
        ac.addQuery("javascript", 987654);
        ac.addQuery("java download", 456789);
        ac.addQuery("jav 21 features", 1);

        // Autocomplete
        System.out.println("Suggestions for 'jav': " + ac.getSuggestions("jav"));

        // Update frequency
        ac.updateFrequency("jav 21 features");
        ac.updateFrequency("jav 21 features");
        System.out.println("Updated frequency suggestions: " + ac.getSuggestions("jav"));

        // Typo-tolerant
        System.out.println("Typo suggestions for 'jvaa': " + ac.getTypoSuggestions("jvaa"));
    }
}
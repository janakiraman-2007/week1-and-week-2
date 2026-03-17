package week1;

import java.util.*;

public class PlagiarismDetector {

    // n-gram size
    private static final int N = 5;

    // ngram -> set of document IDs
    private final Map<String, Set<String>> index = new HashMap<>();

    // documentId -> list of ngrams
    private final Map<String, List<String>> documentNgrams = new HashMap<>();

    // Add document to system
    public void addDocument(String docId, String text) {
        List<String> ngrams = generateNGrams(text);
        documentNgrams.put(docId, ngrams);

        for (String gram : ngrams) {
            index.computeIfAbsent(gram, k -> new HashSet<>()).add(docId);
        }
    }

    // Analyze a new document
    public void analyzeDocument(String docId, String text) {
        List<String> ngrams = generateNGrams(text);
        Map<String, Integer> matchCount = new HashMap<>();

        for (String gram : ngrams) {
            if (index.containsKey(gram)) {
                for (String existingDoc : index.get(gram)) {
                    matchCount.merge(existingDoc, 1, Integer::sum);
                }
            }
        }

        System.out.println("Analyzing: " + docId);
        System.out.println("Total n-grams: " + ngrams.size());

        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            String existingDoc = entry.getKey();
            int matches = entry.getValue();

            double similarity = (matches * 100.0) / ngrams.size();

            System.out.println("Matched with: " + existingDoc);
            System.out.println("Matching n-grams: " + matches);
            System.out.printf("Similarity: %.2f%%", similarity);

            if (similarity > 60) {
                System.out.println(" → PLAGIARISM DETECTED");
            } else if (similarity > 15) {
                System.out.println(" → Suspicious");
            } else {
                System.out.println(" → Low similarity");
            }
            System.out.println();
        }
    }

    // Generate n-grams
    private List<String> generateNGrams(String text) {
        List<String> result = new ArrayList<>();

        String[] words = text.toLowerCase().split("\\s+");

        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                sb.append(words[i + j]).append(" ");
            }
            result.add(sb.toString().trim());
        }

        return result;
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector();

        // Existing documents
        detector.addDocument("essay_089",
                "machine learning is a field of artificial intelligence that focuses on data");

        detector.addDocument("essay_092",
                "machine learning is a field of artificial intelligence that focuses on data and models");

        // New document
        String newEssay =
                "machine learning is a field of artificial intelligence that focuses on data and models";

        detector.analyzeDocument("essay_123", newEssay);
    }
}
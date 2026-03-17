package week2;
import java.util.*;
import java.time.*;

public class Transaction {
    int id;
    double amount;
    String merchant;
    String account;
    LocalDateTime time;

    Transaction(int id, double amount, String merchant, String account, LocalDateTime time) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        this.time = time;
    }

    @Override
    public String toString() {
        return "id:" + id + " amt:" + amount + " merchant:" + merchant + " account:" + account + " time:" + time;
    }
}

 class TransactionAnalyzer {

    List<Transaction> transactions;

    public TransactionAnalyzer(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // ------------------ Classic Two-Sum ------------------
    public List<List<Transaction>> findTwoSum(double target) {
        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(Arrays.asList(map.get(complement), t));
            }
            map.put(t.amount, t);
        }
        return result;
    }

    // ------------------ Two-Sum within Time Window ------------------
    public List<List<Transaction>> findTwoSumWithinWindow(double target, Duration window) {
        List<List<Transaction>> result = new ArrayList<>();
        transactions.sort(Comparator.comparing(t -> t.time));
        Map<Double, List<Transaction>> map = new HashMap<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;

            // Check map for complement within window
            if (map.containsKey(complement)) {
                for (Transaction candidate : map.get(complement)) {
                    if (Math.abs(Duration.between(candidate.time, t.time).toMinutes()) <= window.toMinutes()) {
                        result.add(Arrays.asList(candidate, t));
                    }
                }
            }

            map.computeIfAbsent(t.amount, k -> new ArrayList<>()).add(t);
        }

        return result;
    }

    // ------------------ K-Sum using recursive backtracking ------------------
    public List<List<Transaction>> findKSum(int k, double target) {
        List<List<Transaction>> result = new ArrayList<>();
        Collections.sort(transactions, Comparator.comparingDouble(t -> t.amount));
        kSumHelper(transactions, k, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void kSumHelper(List<Transaction> txs, int k, double target, int start,
                            List<Transaction> path, List<List<Transaction>> result) {
        if (k == 2) { // classic two-sum
            int left = start, right = txs.size() - 1;
            while (left < right) {
                double sum = txs.get(left).amount + txs.get(right).amount;
                if (Math.abs(sum - target) < 0.0001) {
                    List<Transaction> combination = new ArrayList<>(path);
                    combination.add(txs.get(left));
                    combination.add(txs.get(right));
                    result.add(combination);
                    left++;
                    right--;
                } else if (sum < target) left++;
                else right--;
            }
        } else {
            for (int i = start; i < txs.size() - k + 1; i++) {
                List<Transaction> newPath = new ArrayList<>(path);
                newPath.add(txs.get(i));
                kSumHelper(txs, k - 1, target - txs.get(i).amount, i + 1, newPath, result);
            }
        }
    }

    // ------------------ Duplicate Detection ------------------
    public List<Map<String, Object>> detectDuplicates() {
        Map<String, List<Transaction>> map = new HashMap<>();
        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                dup.put("amount", entry.getValue().get(0).amount);
                dup.put("merchant", entry.getValue().get(0).merchant);
                List<String> accounts = new ArrayList<>();
                for (Transaction t : entry.getValue()) accounts.add(t.account);
                dup.put("accounts", accounts);
                result.add(dup);
            }
        }
        return result;
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) {
        List<Transaction> txs = Arrays.asList(
                new Transaction(1, 500, "Store A", "acc1", LocalDateTime.parse("2026-03-17T10:00")),
                new Transaction(2, 300, "Store B", "acc2", LocalDateTime.parse("2026-03-17T10:15")),
                new Transaction(3, 200, "Store C", "acc3", LocalDateTime.parse("2026-03-17T10:30")),
                new Transaction(4, 500, "Store A", "acc2", LocalDateTime.parse("2026-03-17T11:00"))
        );

        TransactionAnalyzer analyzer = new TransactionAnalyzer(txs);

        System.out.println("Two-Sum target=500:");
        for (List<Transaction> pair : analyzer.findTwoSum(500)) System.out.println(pair);

        System.out.println("\nTwo-Sum within 1 hour, target=500:");
        for (List<Transaction> pair : analyzer.findTwoSumWithinWindow(500, Duration.ofHours(1))) System.out.println(pair);

        System.out.println("\nK-Sum k=3, target=1000:");
        for (List<Transaction> triplet : analyzer.findKSum(3, 1000)) System.out.println(triplet);

        System.out.println("\nDuplicate detection:");
        for (Map<String, Object> dup : analyzer.detectDuplicates()) System.out.println(dup);
    }
}
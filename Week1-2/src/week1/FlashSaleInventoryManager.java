package week1;

import java.util.*;
import java.util.concurrent.*;

public class FlashSaleInventoryManager {

    // productId -> stock count
    private final ConcurrentHashMap<String, Integer> stockMap = new ConcurrentHashMap<>();

    // productId -> waiting list (FIFO)
    private final ConcurrentHashMap<String, Queue<Integer>> waitingListMap = new ConcurrentHashMap<>();

    // product-level locks to avoid global blocking
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    // Initialize product
    public void addProduct(String productId, int stock) {
        stockMap.put(productId, stock);
        waitingListMap.put(productId, new ConcurrentLinkedQueue<>());
        locks.put(productId, new Object());
    }

    // Check stock (O(1))
    public int checkStock(String productId) {
        return stockMap.getOrDefault(productId, 0);
    }

    // Purchase item (thread-safe)
    public String purchaseItem(String productId, int userId) {
        Object lock = locks.get(productId);

        synchronized (lock) {
            int stock = stockMap.getOrDefault(productId, 0);

            if (stock > 0) {
                stockMap.put(productId, stock - 1);
                return "Success! Remaining stock: " + (stock - 1);
            } else {
                Queue<Integer> queue = waitingListMap.get(productId);
                queue.offer(userId);
                return "Out of stock. Added to waiting list. Position: " + queue.size();
            }
        }
    }

    // Restock and serve waiting list
    public void restock(String productId, int quantity) {
        Object lock = locks.get(productId);

        synchronized (lock) {
            int currentStock = stockMap.getOrDefault(productId, 0);
            Queue<Integer> queue = waitingListMap.get(productId);

            // Serve waiting users first
            while (quantity > 0 && !queue.isEmpty()) {
                int user = queue.poll();
                quantity--;
                System.out.println("User " + user + " fulfilled from waiting list.");
            }

            // Remaining stock added
            stockMap.put(productId, currentStock + quantity);
        }
    }

    // Get waiting list size
    public int getWaitingListSize(String productId) {
        return waitingListMap.getOrDefault(productId, new LinkedList<>()).size();
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) throws InterruptedException {
        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();

        String product = "IPHONE15_256GB";
        manager.addProduct(product, 5); // small stock for demo

        // Simulate 10 concurrent users
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= 10; i++) {
            int userId = i;
            executor.submit(() -> {
                String result = manager.purchaseItem(product, userId);
                System.out.println("User " + userId + ": " + result);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Check stock
        System.out.println("Final Stock: " + manager.checkStock(product));
        System.out.println("Waiting List Size: " + manager.getWaitingListSize(product));

        // Restock
        manager.restock(product, 3);

        System.out.println("Stock after restock: " + manager.checkStock(product));
    }
}

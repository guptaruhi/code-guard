package test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * Test file to demonstrate CodeGuard's concurrency pattern detection.
 * Focus on blocking code in async contexts and common concurrency anti-patterns.
 */
public class TestConcurrency {

    // ============================================================
    // BLOCKING CALLS IN ASYNC CONTEXT
    // ============================================================

    /**
     * Issue 1: CompletableFuture.get() blocks the calling thread (HIGH)
     */
    static String fetchDataBlocking() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "data");
        return future.get();  // BAD: Blocks thread, defeats async purpose
    }

    /**
     * Issue 2: CompletableFuture.join() also blocks (HIGH)
     */
    static String fetchDataBlockingWithJoin() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "data");
        return future.join();  // BAD: Blocks thread
    }

    /**
     * Issue 3: Thread.sleep() in async callback (HIGH)
     */
    static CompletableFuture<String> processWithDelay() {
        return CompletableFuture.supplyAsync(() -> "data")
            .thenApply(result -> {
                try {
                    Thread.sleep(1000);  // BAD: Blocking async thread pool
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return result.toUpperCase();
            });
    }

    /**
     * Issue 4: Thread.sleep() in thenAccept (HIGH)
     */
    static void processAsync() {
        CompletableFuture.supplyAsync(() -> "data")
            .thenAccept(result -> {
                try {
                    Thread.sleep(500);  // BAD: Blocking
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
    }

    // ============================================================
    // THREAD MANAGEMENT ANTI-PATTERNS
    // ============================================================

    /**
     * Issue 5: Manual thread creation (MEDIUM)
     */
    static void manualThreadCreation() {
        new Thread(() -> {  // BAD: Use ExecutorService instead
            System.out.println("Processing...");
        }).start();
    }

    /**
     * Issue 6: Thread.sleep() in regular code (MEDIUM)
     */
    static void retryWithSleep() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            try {
                // some operation
                break;
            } catch (Exception e) {
                Thread.sleep(1000);  // BAD: Consider ScheduledExecutorService
            }
        }
    }

    // ============================================================
    // SYNCHRONIZATION ANTI-PATTERNS
    // ============================================================

    /**
     * Issue 7: Synchronizing on String literal (HIGH)
     */
    static void synchronizeOnString(String key) {
        synchronized ("lock") {  // BAD: String literals are interned, dangerous
            // critical section
        }
    }

    /**
     * Issue 8: Synchronizing on boxed primitive (HIGH)
     */
    static void synchronizeOnInteger(Integer id) {
        synchronized (Integer.valueOf(1)) {  // BAD: Cached values, unsafe
            // critical section
        }
    }

    /**
     * Issue 9: wait() usage (HIGH)
     */
    static void waitForCondition(Object lock) throws InterruptedException {
        synchronized (lock) {
            lock.wait();  // Should be in while loop checking condition
        }
    }

    // ============================================================
    // RACE CONDITIONS
    // ============================================================

    private static Map<String, Object> cache = new HashMap<>();

    /**
     * Issue 10: Check-then-act race condition (MEDIUM)
     */
    static Object getCached(String key) {
        if (cache.get(key) == null) {  // BAD: Race condition between check and act
            cache.put(key, new Object());
        }
        return cache.get(key);
    }

    /**
     * Issue 11: Double-checked locking without volatile (HIGH)
     */
    private static Object instance;

    static Object getInstance() {
        if (instance == null) {  // BAD: Needs volatile or better pattern
            synchronized (TestConcurrency.class) {
                if (instance == null) {
                    instance = new Object();
                }
            }
        }
        return instance;
    }

    // ============================================================
    // NON-THREAD-SAFE COLLECTIONS
    // ============================================================

    /**
     * Issue 12: Using HashMap in concurrent context (HIGH)
     */
    static void concurrentMapUsage() {
        HashMap<String, String> map = new HashMap<>();  // BAD in concurrent context

        // Simulating concurrent access
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> map.put("key1", "value1"));
        executor.submit(() -> map.put("key2", "value2"));
    }

    /**
     * Issue 13: ArrayList in parallel stream (HIGH)
     */
    static void parallelStreamWithArrayList() {
        ArrayList<String> list = new ArrayList<>();  // BAD: Not thread-safe

        IntStream.range(0, 100).parallel().forEach(i -> {
            list.add("item" + i);  // Race condition!
        });
    }

    // ============================================================
    // GOOD EXAMPLES (should have fewer/no issues)
    // ============================================================

    private static final Object LOCK = new Object();

    static CompletableFuture<String> fetchDataNonBlocking() {
        return CompletableFuture.supplyAsync(() -> "data")
            .thenApply(String::toUpperCase);  // GOOD: Non-blocking chain
    }

    static CompletableFuture<String> delayedProcessing() {
        // GOOD: Non-blocking delay
        return CompletableFuture.supplyAsync(() -> "data")
            .thenApplyAsync(result -> result.toUpperCase(),
                CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
    }

    static void properThreadManagement() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.submit(() -> {
            // GOOD: Using ExecutorService
            System.out.println("Processing...");
        });
        executor.shutdown();
    }

    static void properSynchronization() {
        synchronized (LOCK) {  // GOOD: Dedicated lock object
            // critical section
        }
    }

    private static final ConcurrentHashMap<String, Object> threadSafeCache = new ConcurrentHashMap<>();

    static Object getCachedSafely(String key) {
        return threadSafeCache.computeIfAbsent(key, k -> new Object());  // GOOD: Atomic operation
    }

    // ============================================================
    // DEADLOCK PATTERNS
    // ============================================================

    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    /**
     * Issue 14: Nested synchronized blocks (HIGH) - Classic deadlock scenario
     */
    static void nestedLocks() {
        synchronized (lock1) {
            // Some processing
            synchronized (lock2) {  // BAD: If another thread locks lock2 then lock1, deadlock!
                // Critical section
            }
        }
    }

    /**
     * Issue 15: Synchronized method calling external code (MEDIUM)
     */
    static synchronized void processWithExternalCall(Runnable callback) {
        // Some processing
        callback.run();  // BAD: Holding lock while calling unknown code
    }

    // ============================================================
    // RACE CONDITION PATTERNS
    // ============================================================

    private static int counter = 0;
    private static long totalCount = 0;

    /**
     * Issue 16: Increment without synchronization (HIGH)
     */
    static void incrementCounter() {
        counter++;  // BAD: Race condition, not atomic
    }

    /**
     * Issue 17: Decrement without synchronization (HIGH)
     */
    static void decrementCounter() {
        counter--;  // BAD: Race condition, not atomic
    }

    /**
     * Issue 18: Compound assignment without synchronization (MEDIUM)
     */
    static void addToTotal(long value) {
        totalCount += value;  // BAD: Read-modify-write race condition
    }

    /**
     * Issue 19: Mutable static field without volatile (HIGH)
     */
    private static boolean initialized = false;  // BAD: No visibility guarantee

    static void checkInitialized() {
        if (!initialized) {
            // initialize
            initialized = true;
        }
    }

    /**
     * Issue 20: volatile array doesn't make elements volatile (HIGH)
     */
    private static volatile int[] sharedArray = new int[10];  // BAD: Elements not volatile

    static void updateArray(int index, int value) {
        sharedArray[index] = value;  // Element update not thread-safe
    }

    // ============================================================
    // UNSAFE PUBLICATION
    // ============================================================

    /**
     * Issue 21: Public non-final non-volatile field (HIGH)
     */
    public static String publicField = "unsafe";  // BAD: Unsafe publication

    /**
     * Issue 22: Leaking 'this' in constructor (HIGH)
     */
    static class LeakyConstructor {
        private final String data;

        public LeakyConstructor(List<LeakyConstructor> registry) {
            this.data = "incomplete";
            registry.add(this);  // BAD: Leaking 'this' before fully constructed
            // More initialization...
        }
    }

    /**
     * Issue 23: Synchronizing in constructor (HIGH)
     */
    static class ConstructorSync {
        private String value;

        public ConstructorSync() {
            synchronized (this) {  // BAD: Object not fully constructed
                this.value = "initialized";
            }
        }
    }

    // ============================================================
    // GOOD EXAMPLES - Thread-safe implementations
    // ============================================================

    private static final AtomicInteger atomicCounter = new AtomicInteger(0);
    private static volatile boolean volatileFlag = false;

    static void incrementCounterSafely() {
        atomicCounter.incrementAndGet();  // GOOD: Atomic operation
    }

    static void safeInitialization() {
        if (!volatileFlag) {  // GOOD: volatile ensures visibility
            synchronized (LOCK) {
                if (!volatileFlag) {
                    // initialize
                    volatileFlag = true;
                }
            }
        }
    }

    // GOOD: Consistent lock ordering
    static void acquireLocksInOrder() {
        synchronized (lock1) {
            // Always acquire lock1 first
        }
        // Then lock2 if needed separately
        synchronized (lock2) {
            // Process
        }
    }

    // GOOD: Release lock before calling external code
    static void callExternalSafely(Runnable callback) {
        Object localData;
        synchronized (LOCK) {
            // Prepare data
            localData = new Object();
        }
        // Lock released before calling external code
        callback.run();
    }
}

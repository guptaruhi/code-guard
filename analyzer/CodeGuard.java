package analyzer;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * CodeGuard - Simple NPE pattern detector for pre-code-review checks.
 *
 * Usage: java analyzer.CodeGuard <path-to-java-file>
 */
public class CodeGuard {

    // ANSI color codes for terminal output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    private static final List<CodePattern> PATTERNS = Arrays.asList(
            // ============================================================
            // NPE & NULL SAFETY PATTERNS
            // ============================================================

            new CodePattern(
                    "\\.get\\(\\)",
                    "Optional.get() without isPresent() check",
                    "HIGH",
                    "if (value.isPresent()) { return value.get(); } else { return value.orElse(defaultValue); }"
            ),

            new CodePattern(
                    "\\w+\\.\\w+\\(\\)\\.\\w+\\(",
                    "Method chaining without null check",
                    "HIGH",
                    "Store intermediate result and check for null before chaining"
            ),

            new CodePattern(
                    "\\.get\\([^)]+\\)\\s*\\.\\w+\\(",
                    "Map.get() result used without null check",
                    "HIGH",
                    "String val = map.get(key); if (val != null) { return val.toUpperCase(); }"
            ),

            // ============================================================
            // INDEX & BOUNDS PATTERNS
            // ============================================================

            new CodePattern(
                    "\\w+\\[\\d+\\]",
                    "Array access without bounds check",
                    "MEDIUM",
                    "if (array.length > 0) { return array[0]; }"
            ),

            new CodePattern(
                    "\\.get\\(\\d+\\)",
                    "List.get() without size check",
                    "MEDIUM",
                    "if (!list.isEmpty()) { return list.get(0); }"
            ),

            new CodePattern(
                    "\\.split\\([^)]+\\)\\s*\\[",
                    "String.split() result accessed without validation",
                    "MEDIUM",
                    "String[] parts = str.split(\":\"); if (parts.length > 1) { return parts[1]; }"
            ),

            new CodePattern(
                    "\\.next\\(\\)",
                    "Iterator.next() without hasNext() check",
                    "MEDIUM",
                    "if (iter.hasNext()) { return iter.next(); }"
            ),

            // ============================================================
            // COMMON CODING MISTAKES
            // ============================================================

            new CodePattern(
                    "\\s+==\\s+\"[^\"]*\"|\"[^\"]*\"\\s+==\\s+",
                    "String comparison using == instead of .equals()",
                    "HIGH",
                    "if (str1.equals(str2)) { ... } or if (str1.equalsIgnoreCase(str2)) { ... }"
            ),

            new CodePattern(
                    "catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}",
                    "Empty catch block (swallowing exceptions)",
                    "HIGH",
                    "catch (Exception e) { logger.error(\"Error: \", e); // or handle appropriately }"
            ),

            // ============================================================
            // PERFORMANCE PATTERNS
            // ============================================================

            new CodePattern(
                    "\\w+\\s*\\+=\\s*",
                    "String concatenation in loop (use StringBuilder)",
                    "HIGH",
                    "StringBuilder sb = new StringBuilder(); for(...) { sb.append(data); } return sb.toString();"
            ),

            new CodePattern(
                    "for\\s*\\([^;]+;[^;]*\\w+\\.size\\(\\)",
                    "Calling .size() in loop condition (inefficient)",
                    "MEDIUM",
                    "int n = list.size(); for (int i = 0; i < n; i++) { ... }"
            ),

            new CodePattern(
                    "for\\s*\\([^:]+:\\s*\\w+\\.keySet\\(\\)",
                    "Iterating Map with keySet() (use entrySet() instead)",
                    "MEDIUM",
                    "for (Map.Entry<K, V> entry : map.entrySet()) { entry.getKey(); entry.getValue(); }"
            ),

            new CodePattern(
                    "Pattern\\.compile\\(",
                    "Pattern.compile() not cached (compile once as static final)",
                    "HIGH",
                    "private static final Pattern PATTERN = Pattern.compile(\"regex\"); ... PATTERN.matcher(text)"
            ),

            new CodePattern(
                    "for\\s*\\(\\s*(Integer|Long|Double|Float|Boolean|Character|Byte|Short)\\s+\\w+\\s*:\\s*",
                    "Boxing/Unboxing in loop (use primitive arrays or streams)",
                    "HIGH",
                    "Use int[] instead of List<Integer>, or IntStream for better performance"
            ),

            new CodePattern(
                    "\"[^\"]*\"\\s*\\+\\s*\\w+\\s*\\+\\s*\"[^\"]*\"\\s*\\+",
                    "Multiple string concatenations with + (use StringBuilder)",
                    "HIGH",
                    "StringBuilder sb = new StringBuilder().append(a).append(b).append(c); or String.format()"
            ),

            new CodePattern(
                    "\\.indexOf\\([^)]+\\)\\s*(>=|!=)\\s*-?[01]",
                    "Using indexOf() for contains() check",
                    "MEDIUM",
                    "if (str.contains(\"text\")) { ... } // More readable and clear intent"
            ),

            // ============================================================
            // CONCURRENCY & ASYNC PATTERNS
            // ============================================================

            new CodePattern(
                    "CompletableFuture[^;]*\\.get\\(\\)",
                    "CompletableFuture.get() blocks thread (use .join() or async callbacks)",
                    "HIGH",
                    "future.thenApply(result -> ...) or future.get(timeout, TimeUnit.SECONDS) with timeout"
            ),

            new CodePattern(
                    "CompletableFuture[^;]*\\.join\\(\\)",
                    "CompletableFuture.join() blocks thread (defeats async purpose)",
                    "HIGH",
                    "Use .thenApply(), .thenAccept(), .thenCompose() for non-blocking async chains"
            ),

            new CodePattern(
                    "\\.thenApply\\([^)]*Thread\\.sleep",
                    "Thread.sleep() in async callback (blocking async thread pool)",
                    "HIGH",
                    "Use CompletableFuture.delayedExecutor() or ScheduledExecutorService for delays"
            ),

            new CodePattern(
                    "\\.thenAccept\\([^)]*Thread\\.sleep",
                    "Thread.sleep() in async callback (blocking async thread pool)",
                    "HIGH",
                    "Use CompletableFuture.delayedExecutor() or ScheduledExecutorService for delays"
            ),

            new CodePattern(
                    "Thread\\.sleep\\(",
                    "Thread.sleep() in code (consider ScheduledExecutorService)",
                    "MEDIUM",
                    "ScheduledExecutorService executor = Executors.newScheduledThreadPool(1); executor.schedule(task, delay, TimeUnit)"
            ),

            new CodePattern(
                    "new\\s+Thread\\(",
                    "Manual thread creation (use ExecutorService instead)",
                    "MEDIUM",
                    "ExecutorService executor = Executors.newFixedThreadPool(n); executor.submit(() -> ...)"
            ),

            new CodePattern(
                    "synchronized\\s*\\([^)]*\"",
                    "Synchronizing on String literal (anti-pattern, use dedicated lock)",
                    "HIGH",
                    "private final Object lock = new Object(); synchronized(lock) { ... }"
            ),

            new CodePattern(
                    "synchronized\\s*\\([^)]*Integer|synchronized\\s*\\([^)]*Long|synchronized\\s*\\([^)]*Boolean",
                    "Synchronizing on boxed primitive (anti-pattern, use dedicated lock)",
                    "HIGH",
                    "private final Object lock = new Object(); synchronized(lock) { ... }"
            ),

            new CodePattern(
                    "\\.wait\\(\\)",
                    "wait() should be in while loop and synchronized block",
                    "HIGH",
                    "synchronized(lock) { while(!condition) { lock.wait(); } }"
            ),

            new CodePattern(
                    "if\\s*\\([^)]+\\)\\s*\\{[^}]*\\w+\\s*=\\s*new\\s+",
                    "Check-then-act without synchronization (potential race condition)",
                    "MEDIUM",
                    "Use synchronized block, ReentrantLock, or ConcurrentHashMap.computeIfAbsent()"
            ),

            new CodePattern(
                    "(ArrayList|HashMap|HashSet)\\s+\\w+\\s*=[^;]*;[^\\n]*(thread|concurrent|async|parallel)",
                    "Non-thread-safe collection in concurrent context",
                    "HIGH",
                    "Use ConcurrentHashMap, CopyOnWriteArrayList, or Collections.synchronizedList()"
            ),

            new CodePattern(
                    "if\\s*\\([^=!]+==\\s*null\\s*\\)[^}]*\\w+\\s*=",
                    "Double-checked locking pattern (needs volatile or better approach)",
                    "HIGH",
                    "Use volatile field or better: use synchronized method or lazy holder pattern"
            ),

            // ============================================================
            // DEADLOCK PATTERNS
            // ============================================================

            new CodePattern(
                    "synchronized\\s*\\([^)]+\\)[^}]*synchronized\\s*\\(",
                    "Nested synchronized blocks (potential deadlock risk)",
                    "HIGH",
                    "Review lock ordering. Always acquire locks in consistent order across all threads"
            ),

            new CodePattern(
                    "synchronized\\s+\\w+\\s+\\w+\\([^)]*\\)[^}]*\\w+\\.\\w+\\(",
                    "Synchronized method calling external code (potential deadlock)",
                    "MEDIUM",
                    "Avoid holding locks while calling external/unknown code. Release lock before calling"
            ),

            new CodePattern(
                    "ReentrantLock\\s+\\w+[^;]*;[^.]*\\1\\.lock\\([^)]*\\);[^.]*\\1\\.lock\\(",
                    "Attempting to acquire same ReentrantLock twice (deadlock)",
                    "HIGH",
                    "Use ReentrantLock.isHeldByCurrentThread() or ensure lock/unlock pairs are balanced"
            ),

            // ============================================================
            // RACE CONDITION PATTERNS
            // ============================================================

            new CodePattern(
                    "\\w+\\+\\+(?!\\))",
                    "Increment operation without synchronization (race condition)",
                    "HIGH",
                    "Use AtomicInteger.incrementAndGet() or synchronize access"
            ),

            new CodePattern(
                    "\\w+--(?!\\))",
                    "Decrement operation without synchronization (race condition)",
                    "HIGH",
                    "Use AtomicInteger.decrementAndGet() or synchronize access"
            ),

            new CodePattern(
                    "\\w+\\s*\\+=\\s*(?!.*synchronized)",
                    "Compound assignment without synchronization (race condition)",
                    "MEDIUM",
                    "Synchronize access or use AtomicInteger/AtomicLong with addAndGet()"
            ),

            new CodePattern(
                    "(static|private)\\s+(?!final)(?!volatile)\\s+(int|long|boolean|double|float)\\s+\\w+\\s*=",
                    "Mutable static/shared field without volatile (visibility issue)",
                    "HIGH",
                    "Add 'volatile' keyword or use AtomicInteger/AtomicLong for thread-safe access"
            ),

            new CodePattern(
                    "volatile\\s+\\w+\\[\\]",
                    "volatile array (doesn't make elements volatile)",
                    "HIGH",
                    "volatile only applies to array reference, not elements. Use AtomicReferenceArray or synchronize"
            ),

            new CodePattern(
                    "System\\.currentTimeMillis\\(\\)[^;]*(synchronized|lock|wait|concurrent)",
                    "Using currentTimeMillis() for synchronization/ordering",
                    "MEDIUM",
                    "Use System.nanoTime() for timing/ordering or AtomicLong for counters"
            ),

            // ============================================================
            // UNSAFE PUBLICATION & VISIBILITY
            // ============================================================

            new CodePattern(
                    "public\\s+(?!static\\s+final)\\s+(?!volatile)\\s+(\\w+)\\s+\\w+;",
                    "Public non-final non-volatile field (unsafe publication)",
                    "HIGH",
                    "Make field private with synchronized getter/setter, or use volatile, or make it final"
            ),

            new CodePattern(
                    "this\\)[^;]*;(?=.*constructor|.*\\{[^}]*this)",
                    "Leaking 'this' reference in constructor (unsafe publication)",
                    "HIGH",
                    "Don't pass 'this' to other objects in constructor. Use factory method instead"
            ),

            new CodePattern(
                    "synchronized\\s*\\([^)]*this\\)[^}]*(?=.*constructor)",
                    "Synchronizing in constructor (object not fully constructed)",
                    "HIGH",
                    "Avoid synchronization in constructors. Object may not be fully initialized"
            ),

            // ============================================================
            // STRING OPERATIONS WITHOUT VALIDATION
            // ============================================================

            new CodePattern(
                    "\\.substring\\(\\d+\\)",
                    "String.substring() without length check",
                    "MEDIUM",
                    "if (str.length() > 5) { return str.substring(5); }"
            ),

            new CodePattern(
                    "\\.charAt\\(\\d+\\)",
                    "String.charAt() without length check",
                    "MEDIUM",
                    "if (str.length() > 0) { return str.charAt(0); }"
            ),

            // ============================================================
            // CODE QUALITY / DEBUGGING
            // ============================================================

            new CodePattern(
                    "System\\.out\\.println",
                    "System.out.println (use proper logging)",
                    "MEDIUM",
                    "logger.info(\"Message: {}\", value); // or logger.debug() for debug info"
            ),

            // ============================================================
            // RESOURCE MANAGEMENT
            // ============================================================

            new CodePattern(
                    "new\\s+(java\\.io\\.)?(FileReader|FileWriter|BufferedReader|BufferedWriter|FileInputStream|FileOutputStream)\\(",
                    "File/Stream not in try-with-resources",
                    "HIGH",
                    "try (BufferedReader br = new BufferedReader(new FileReader(file))) { ... }"
            ),

            // ============================================================
            // MAINTAINABILITY & CODE QUALITY
            // ============================================================

            new CodePattern(
                    "return\\s+null\\s*;",
                    "Returning null from sanitizer/formatter method",
                    "HIGH",
                    "Return safe default: return \"\"; or return Collections.emptyList(); instead of null"
            ),

            new CodePattern(
                    "\\.replace\\s*\\(\\s*'[^']+'\\s*,\\s*'[^']+'\\s*\\)",
                    "Magic character literals in replace() - use named constants",
                    "MEDIUM",
                    "private static final char TAB = '\\t'; ... text.replace(TAB, SPACE);"
            ),

            new CodePattern(
                    "\\.replace\\([^)]+\\)\\.replace\\([^)]+\\)\\.replace",
                    "Multiple chained replace() calls - consider using Map or switch",
                    "MEDIUM",
                    "Use Map<Character, Character> replacements or switch statement for scalability"
            )
    );

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java analyzer.CodeGuard [OPTIONS] <path-to-file-or-directory>");
            System.err.println("Options:");
            System.err.println("  --all    Show all issues (default: show max 5 per severity)");
            System.err.println("  --ai     Use AI-powered context-aware fixes (requires CLAUDE_API_KEY)");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  java analyzer.CodeGuard MyFile.java           # Fast scan with generic fixes");
            System.err.println("  java analyzer.CodeGuard src/                  # Scan directory");
            System.err.println("  java analyzer.CodeGuard --all src/            # Show all issues");
            System.err.println("  java analyzer.CodeGuard --ai src/             # AI-powered fixes");
            System.err.println("  java analyzer.CodeGuard --ai --all src/       # AI mode + show all");
            System.exit(1);
        }

        boolean showAll = false;
        boolean aiMode = false;
        String targetPath = null;

        for (String arg : args) {
            if (arg.equals("--all")) {
                showAll = true;
            } else if (arg.equals("--ai")) {
                aiMode = true;
            } else {
                targetPath = arg;
            }
        }

        if (targetPath == null) {
            System.err.println("Error: No input file or directory specified");
            System.exit(1);
        }

        File target = new File(targetPath);
        if (!target.exists()) {
            System.err.println("Error: Path not found: " + targetPath);
            System.exit(1);
        }

        // Check for AI mode setup
        if (aiMode) {
            String apiKey = System.getenv("CLAUDE_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println(YELLOW + "⚠️  AI mode requested but CLAUDE_API_KEY not found" + RESET);
                System.out.println("Set your API key: export CLAUDE_API_KEY=\"your-key-here\"");
                System.out.println("Falling back to fast mode with generic fixes...\n");
                aiMode = false;
            } else {
                System.out.println(BLUE + "🤖 AI Mode: Enabled (using Claude API for context-aware fixes)" + RESET);
                System.out.println();
            }
        }

        if (target.isDirectory()) {
            scanDirectory(target, showAll, aiMode);
        } else {
            scanSingleFile(target, showAll, aiMode);
        }

        System.out.flush();
    }

    private static void scanSingleFile(File file, boolean showAll, boolean aiMode) throws IOException {
        List<Finding> findings = analyzeFile(file);
        if (aiMode) {
            enhanceFindingsWithAI(findings, file);
        }
        printReport(file.getPath(), findings, showAll);
    }

    private static void scanDirectory(File directory, boolean showAll, boolean aiMode) throws IOException {
        List<File> javaFiles = collectJavaFiles(directory);

        if (javaFiles.isEmpty()) {
            System.out.println(YELLOW + "No Java files found in: " + directory.getPath() + RESET);
            return;
        }

        System.out.println(BOLD + BLUE + "CodeGuard - Scanning Directory: " + directory.getPath() + RESET);
        System.out.println("Found " + javaFiles.size() + " Java file(s)\n");

        Map<String, List<Finding>> allFindings = new HashMap<>();
        int totalIssues = 0;
        int totalHigh = 0;
        int totalMedium = 0;

        for (File file : javaFiles) {
            List<Finding> findings = analyzeFile(file);
            if (!findings.isEmpty()) {
                if (aiMode) {
                    enhanceFindingsWithAI(findings, file);
                }
                allFindings.put(file.getPath(), findings);
                totalIssues += findings.size();

                for (Finding f : findings) {
                    if (f.severity.equals("HIGH")) {
                        totalHigh++;
                    } else {
                        totalMedium++;
                    }
                }
            }
        }

        // Print summary
        System.out.println(BOLD + "=".repeat(100) + RESET);
        System.out.println(BOLD + "SUMMARY: " + totalIssues + " issue(s) across " + allFindings.size() + " file(s)" + RESET);
        System.out.println(BOLD + "  " + RED + totalHigh + " HIGH" + RESET + BOLD + " | " + YELLOW + totalMedium + " MEDIUM" + RESET);
        System.out.println(BOLD + "=".repeat(100) + RESET);

        if (allFindings.isEmpty()) {
            System.out.println(GREEN + "\n✓ No issues found!" + RESET);
            return;
        }

        // Print findings per file
        for (Map.Entry<String, List<Finding>> entry : allFindings.entrySet()) {
            String filePath = entry.getKey();
            List<Finding> findings = entry.getValue();

            System.out.println("\n" + BOLD + "─".repeat(100) + RESET);
            printReport(filePath, findings, showAll);
        }

        // Final summary
        System.out.println("\n" + BOLD + "=".repeat(100) + RESET);
        System.out.println(BOLD + "Total: " + totalIssues + " issue(s) in " + allFindings.size() + " file(s)" + RESET);
        System.out.println(BOLD + "Tip: Fix HIGH priority issues first!" + RESET);
    }

    private static List<File> collectJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        collectJavaFilesRecursive(directory, javaFiles);
        return javaFiles;
    }

    private static void collectJavaFilesRecursive(File directory, List<File> javaFiles) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip common non-source directories
                String name = file.getName();
                if (!name.equals(".git") && !name.equals("target") &&
                    !name.equals("build") && !name.equals(".idea") &&
                    !name.startsWith(".")) {
                    collectJavaFilesRecursive(file, javaFiles);
                }
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    private static void enhanceFindingsWithAI(List<Finding> findings, File file) {
        String apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey == null) return;

        System.out.print(BLUE + "🤖 Enhancing " + findings.size() + " finding(s) with AI..." + RESET);

        try {
            List<String> fileLines = Files.readAllLines(file.toPath());
            int enhanced = 0;
            int failed = 0;

            for (Finding finding : findings) {
                try {
                    String contextAwareFix = generateAIFix(finding, fileLines, apiKey);
                    if (contextAwareFix != null && !contextAwareFix.isEmpty()) {
                        finding.suggestion = contextAwareFix;
                        enhanced++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    // Print first error for debugging
                    if (failed == 1) {
                        System.err.println("\n" + YELLOW + "⚠ AI Error: " + e.getMessage() + RESET);
                    }
                }
            }

            System.out.println(" " + GREEN + "✓ Enhanced " + enhanced + " fix(es)" + RESET +
                             (failed > 0 ? " " + YELLOW + "(" + failed + " failed)" + RESET : ""));
        } catch (Exception e) {
            System.out.println(" " + YELLOW + "⚠ AI enhancement failed: " + e.getMessage() + RESET);
        }
    }

    private static String generateAIFix(Finding finding, List<String> fileLines, String apiKey) {
        try {
            // Get context: 2 lines before and after the issue
            int lineIdx = finding.lineNumber - 1;
            int start = Math.max(0, lineIdx - 2);
            int end = Math.min(fileLines.size(), lineIdx + 3);

            StringBuilder context = new StringBuilder();
            for (int i = start; i < end; i++) {
                context.append(fileLines.get(i)).append("\n");
            }

            String prompt = String.format(
                "You are a Java code reviewer. Fix this issue:\n\n" +
                "Issue: %s\n" +
                "Code context:\n```java\n%s```\n" +
                "Problematic line: %s\n\n" +
                "Provide ONLY the fixed code for this specific line, no explanation. " +
                "Keep it concise (max 80 chars). If it needs multiple statements, separate with semicolons.",
                finding.description,
                context.toString(),
                finding.lineContent.trim()
            );

            // Properly escape JSON string
            String escapedPrompt = escapeJson(prompt);

            HttpClient client = HttpClient.newHttpClient();
            String requestBody = String.format(
                "{\"model\":\"claude-3-5-sonnet-20241022\"," +
                "\"max_tokens\":150," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                escapedPrompt
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse JSON response (simple extraction, no library needed)
                String body = response.body();
                int textStart = body.indexOf("\"text\":\"") + 8;
                if (textStart > 7) {
                    int textEnd = body.indexOf("\"", textStart);
                    if (textEnd > textStart) {
                        String fix = body.substring(textStart, textEnd);
                        // Unescape JSON
                        fix = fix.replace("\\n", " ").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                        return fix;
                    }
                }
            } else {
                throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("AI fix failed: " + e.getMessage(), e);
        }
        return null;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static void printReport(String filePath, List<Finding> findings, boolean showAll) {
        System.out.println(BOLD + BLUE + "CodeGuard - " + filePath + RESET);

        if (findings.isEmpty()) {
            System.out.println(GREEN + "✓ No issues found!" + RESET);
            return;
        }

        // Group by severity
        Map<String, List<Finding>> grouped = new HashMap<>();
        for (Finding f : findings) {
            grouped.computeIfAbsent(f.severity, k -> new ArrayList<>()).add(f);
        }

        List<Finding> highSeverity = grouped.getOrDefault("HIGH", new ArrayList<>());
        List<Finding> mediumSeverity = grouped.getOrDefault("MEDIUM", new ArrayList<>());

        // Print summary
        System.out.println(BOLD + "\nSummary: " + findings.size() + " issue(s) - " +
                          RED + highSeverity.size() + " HIGH" + RESET + BOLD + ", " +
                          YELLOW + mediumSeverity.size() + " MEDIUM" + RESET);

        int maxShow = showAll ? Integer.MAX_VALUE : 5; // Show max 5 issues per severity unless --all

        // Print issues by severity
        printSeverityGroup(highSeverity, "⚠ HIGH PRIORITY", RED, maxShow);
        printSeverityGroup(mediumSeverity, "⚡ MEDIUM PRIORITY", YELLOW, maxShow);

        System.out.println("\n" + BOLD + "Tip: Fix HIGH priority issues first!" + RESET);
    }

    private static void printSeverityGroup(List<Finding> findings, String title, String color, int maxShow) {
        if (findings.isEmpty()) return;

        System.out.println("\n" + color + BOLD + title + RESET);
        System.out.println("  " + BOLD + "LINE | ISSUE                                          | FIX" + RESET);
        System.out.println("  " + "─".repeat(130));

        int shown = 0;
        for (Finding f : findings) {
            if (shown >= maxShow) break;
            System.out.println("  " + BOLD + String.format("%-4s", "L" + f.lineNumber) + RESET +
                             " | " + String.format("%-46s", f.description) +
                             " | " + GREEN + f.suggestion + RESET);
            shown++;
        }

        if (findings.size() > maxShow) {
            System.out.println("  " + BOLD + "... and " + (findings.size() - maxShow) + " more" + RESET);
        }
    }

    private static List<Finding> analyzeFile(File file) throws IOException {
        List<Finding> findings = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;

            // Skip comments and blank lines
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                continue;
            }

            // Check each pattern
            for (CodePattern pattern : PATTERNS) {
                if (pattern.matches(line)) {
                    findings.add(new Finding(
                            lineNumber,
                            line,
                            pattern.description,
                            pattern.severity,
                            pattern.suggestion
                    ));
                }
            }
        }

        return findings;
    }

    // Helper class to represent a code pattern
    static class CodePattern {
        final Pattern pattern;
        final String description;
        final String severity;
        final String suggestion;

        CodePattern(String regex, String description, String severity, String suggestion) {
            this.pattern = Pattern.compile(regex);
            this.description = description;
            this.severity = severity;
            this.suggestion = suggestion;
        }

        boolean matches(String line) {
            return pattern.matcher(line).find();
        }
    }

    // Helper class to represent a finding
    static class Finding {
        final int lineNumber;
        final String lineContent;
        final String description;
        final String severity;
        String suggestion;  // Non-final so AI can enhance it

        Finding(int lineNumber, String lineContent, String description,
                String severity, String suggestion) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.description = description;
            this.severity = severity;
            this.suggestion = suggestion;
        }
    }
}

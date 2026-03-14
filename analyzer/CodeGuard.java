package analyzer;

import java.io.*;
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
                    "Address addr = user.getAddress(); if (addr != null) { return addr.getCity(); }"
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
            )
    );

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java analyzer.CodeGuard [--all] <path-to-file-or-directory>");
            System.err.println("Options:");
            System.err.println("  --all    Show all issues (default: show max 5 per severity)");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  java analyzer.CodeGuard MyFile.java           # Scan single file");
            System.err.println("  java analyzer.CodeGuard src/                  # Scan directory");
            System.err.println("  java analyzer.CodeGuard --all src/            # Scan directory, show all");
            System.exit(1);
        }

        boolean showAll = false;
        String targetPath = null;

        for (String arg : args) {
            if (arg.equals("--all")) {
                showAll = true;
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

        if (target.isDirectory()) {
            scanDirectory(target, showAll);
        } else {
            scanSingleFile(target, showAll);
        }

        System.out.flush();
    }

    private static void scanSingleFile(File file, boolean showAll) throws IOException {
        List<Finding> findings = analyzeFile(file);
        printReport(file.getPath(), findings, showAll);
    }

    private static void scanDirectory(File directory, boolean showAll) throws IOException {
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
        final String suggestion;

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

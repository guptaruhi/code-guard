package test;

import java.util.*;

/**
 * Test file to demonstrate CodeGuard's new maintainability pattern detection.
 * This file contains code patterns that should be flagged by CodeGuard.
 */
public class TestMaintainability {

    /**
     * Issue 1: Null-returning sanitizer (HIGH)
     * Problem: Defeats the purpose of sanitization, causes NPE downstream
     */
    static String sanitizeTextForRendering(String text) {
        if (text == null || text.isEmpty()) {
            return text;  // BAD: Returns null when text is null
        }
        return text.replace('\t', ' ');
    }

    /**
     * Issue 2: Magic character literals (MEDIUM)
     * Problem: Hard-coded characters without named constants
     */
    static String cleanupWhitespace(String input) {
        return input.replace('\t', ' ');  // BAD: Magic literals
    }

    /**
     * Issue 3: Multiple chained replace() calls (MEDIUM)
     * Problem: Not scalable, hard to maintain
     */
    static String sanitizeForPdf(String text) {
        return text.replace('\t', ' ')
                   .replace('\r', '\n')
                   .replace('\u0000', ' ');  // BAD: Multiple chained replaces
    }

    /**
     * Issue 4: Another null-returning method (HIGH)
     */
    static String formatUsername(String username) {
        if (username == null) {
            return null;  // BAD: Returns null
        }
        return username.toLowerCase().trim();
    }

    /**
     * Bonus: This will also catch existing CodeGuard patterns
     */
    static void demonstrateOtherIssues() {
        String[] parts = "a:b:c".split(":");
        String first = parts[0];  // Array access without bounds check

        Map<String, String> map = new HashMap<>();
        String value = map.get("key").toUpperCase();  // Map.get() without null check

        Optional<String> opt = Optional.of("test");
        String result = opt.get();  // Optional.get() without isPresent() check
    }

    /**
     * Good example for comparison - should not be flagged
     */
    static String sanitizeTextCorrectly(String text) {
        if (text == null || text.isEmpty()) {
            return "";  // GOOD: Returns empty string instead of null
        }

        // GOOD: Named constants (not in this example, but would be better)
        return text.replace('\t', ' ');
    }
}

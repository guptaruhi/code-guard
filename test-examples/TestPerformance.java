package test;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Test file to demonstrate CodeGuard's performance pattern detection.
 */
public class TestPerformance {

    /**
     * Issue 1: List.size() called in loop condition (MEDIUM)
     */
    static void inefficientLoop(List<String> items) {
        for (int i = 0; i < items.size(); i++) {  // BAD: size() called every iteration
            System.out.println(items.get(i));
        }
    }

    /**
     * Issue 2: Map iteration using keySet() (MEDIUM)
     */
    static void inefficientMapIteration(Map<String, String> map) {
        for (String key : map.keySet()) {  // BAD: Two lookups per iteration
            String value = map.get(key);
            System.out.println(key + ": " + value);
        }
    }

    /**
     * Issue 3: Pattern.compile() not cached (HIGH)
     */
    static boolean validateEmail(String email) {
        // BAD: Compiles pattern every time method is called
        return Pattern.compile("[a-z]+@[a-z]+\\.[a-z]+").matcher(email).matches();
    }

    /**
     * Issue 4: Boxing/Unboxing in loop (HIGH)
     */
    static int sumNumbers(List<Integer> numbers) {
        int sum = 0;
        for (Integer num : numbers) {  // BAD: Autoboxing in hot loop
            sum += num;
        }
        return sum;
    }

    /**
     * Issue 5: Multiple string concatenations with + (HIGH)
     */
    static String buildMessage(String name, String email, String phone, String address) {
        // BAD: Creates multiple intermediate String objects
        return "Name: " + name + ", Email: " + email + ", Phone: " + phone + ", Address: " + address;
    }

    /**
     * Issue 6: Using indexOf() for contains() check (MEDIUM)
     */
    static boolean hasSubstring(String text, String search) {
        return text.indexOf(search) >= 0;  // BAD: Use contains() instead
    }

    static boolean anotherIndexOfCheck(String text) {
        return text.indexOf("test") != -1;  // BAD: Use contains() instead
    }

    /**
     * GOOD EXAMPLES - Should not be flagged or fewer issues
     */
    static void efficientLoop(List<String> items) {
        int size = items.size();  // GOOD: Cache size
        for (int i = 0; i < size; i++) {
            System.out.println(items.get(i));
        }
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-z]+@[a-z]+\\.[a-z]+");

    static boolean validateEmailEfficient(String email) {
        return EMAIL_PATTERN.matcher(email).matches();  // GOOD: Cached pattern
    }

    static void efficientMapIteration(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {  // GOOD: Single lookup
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    static boolean hasSubstringEfficient(String text, String search) {
        return text.contains(search);  // GOOD: Clear intent
    }

    static String buildMessageEfficient(String name, String email, String phone, String address) {
        // GOOD: StringBuilder for multiple concatenations
        return new StringBuilder()
            .append("Name: ").append(name)
            .append(", Email: ").append(email)
            .append(", Phone: ").append(phone)
            .append(", Address: ").append(address)
            .toString();
    }

    static int sumNumbersEfficient(int[] numbers) {
        int sum = 0;
        for (int num : numbers) {  // GOOD: Primitive array, no boxing
            sum += num;
        }
        return sum;
    }
}

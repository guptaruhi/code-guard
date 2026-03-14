package examples;

import java.util.*;

/**
 * This file contains intentional code issues for testing the CodeGuard tool.
 * Each method demonstrates a common pattern that should be detected:
 * - NPE risks, bounds issues, performance problems, coding mistakes
 */
public class NpeExamples {

    // Pattern 1: Optional.get() without isPresent() check
    public String optionalWithoutCheck(Optional<String> value) {
        return value.get(); // NPE risk: get() without isPresent()
    }

    // Pattern 2: Array access without bounds check
    public String arrayAccessWithoutCheck(String[] args) {
        return args[0]; // NPE risk: array might be empty
    }

    // Pattern 3: List access without bounds check
    public String listAccessWithoutCheck(List<String> items) {
        return items.get(0); // NPE risk: list might be empty
    }

    // Pattern 4: Method chaining without null checks
    public String methodChainingWithoutCheck(User user) {
        return user.getAddress().getCity(); // NPE risk: getAddress() might return null
    }

    // Pattern 5: Map.get() without null check
    public String mapGetWithoutCheck(Map<String, String> config) {
        String value = config.get("key");
        return value.toUpperCase(); // NPE risk: get() might return null
    }

    // Pattern 6: String split without validation
    public String stringSplitWithoutCheck(String input) {
        String[] parts = input.split(":");
        return parts[1]; // NPE risk: might not have 2 parts
    }

    // Pattern 7: Dereferencing parameter without null check
    public int calculateLength(String text) {
        return text.length(); // NPE risk: parameter might be null
    }

    // Pattern 8: Chained Optional.get()
    public String nestedOptionalGet(Optional<Optional<String>> nested) {
        return nested.get().get(); // NPE risk: double get() without checks
    }

    // Pattern 9: Direct field access that could be null
    private String cachedValue;

    public String useCachedValue() {
        return cachedValue.trim(); // NPE risk: field might not be initialized
    }

    // Pattern 10: Collection iterator without hasNext() check
    public String iteratorWithoutCheck(List<String> items) {
        Iterator<String> iter = items.iterator();
        return iter.next(); // NPE risk: might be empty
    }

    // Pattern 11: String concatenation in loop (Performance issue)
    public String concatenateInLoop(List<String> items) {
        String result = "";
        for (String item : items) {
            result += item; // Performance issue: creates new String each iteration
        }
        return result;
    }

    // Pattern 12: String concatenation with index loop
    public String concatenateWithIndex(String[] data) {
        String output = "";
        for (int i = 0; i < data.length; i++) {
            output += data[i]; // Performance issue
        }
        return output;
    }

    // Pattern 13: String.substring without length check
    public String extractSubstring(String input) {
        return input.substring(5); // Risk: IndexOutOfBoundsException if length < 5
    }

    // Pattern 14: String.charAt without length check
    public char getFirstChar(String text) {
        return text.charAt(0); // Risk: IndexOutOfBoundsException if empty
    }

    // Pattern 15: System.out.println (should use logger)
    public void debugMethod(String data) {
        System.out.println("Debug: " + data); // Code smell: use logger
    }

    // Pattern 16: FileReader not in try-with-resources
    public String readFile(String path) throws Exception {
        java.io.FileReader reader = new java.io.FileReader(path); // Resource leak risk
        return reader.toString();
    }

    // Pattern 17: BufferedReader not in try-with-resources
    public String readWithBuffer(String path) throws Exception {
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(path)); // Resource leak risk
        return br.readLine();
    }

    // Helper class for testing
    static class User {
        private Address address;

        public Address getAddress() {
            return address;
        }
    }

    static class Address {
        private String city;

        public String getCity() {
            return city;
        }
    }
}

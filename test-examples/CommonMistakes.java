package examples;

/**
 * Test file for common coding mistakes patterns
 */
public class CommonMistakes {

    // Pattern: String comparison with ==
    public boolean stringComparisonBad(String input) {
        if (input == "test") { // Should use .equals()
            return true;
        }
        return false;
    }

    // Pattern: Empty catch block
    public void emptyCatchBlock() {
        try {
            int result = 10 / 0;
        } catch (Exception e) {
        }
    }

    // Pattern: Another empty catch
    public String readFile(String path) {
        try {
            return path.toString();
        } catch (Exception e) {
        }
        return null;
    }
}
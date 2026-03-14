# Contributing to CodeGuard

Thank you for your interest in contributing to CodeGuard! This document provides guidelines and instructions for contributing.

## How to Contribute

### Reporting Issues

- **Bugs**: Describe the issue, steps to reproduce, and expected vs actual behavior
- **False Positives**: Include the code snippet that was incorrectly flagged
- **Feature Requests**: Explain the use case and why it would be valuable

### Submitting Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-pattern`)
3. Make your changes
4. Test your changes (`./build.sh && java -jar codeguard.jar test-examples/`)
5. Commit with clear messages
6. Push to your fork
7. Open a Pull Request

## Adding New Patterns

### Pattern Structure

```java
new CodePattern(
    "regex-pattern",              // Java regex to match the issue
    "Human-readable description", // What's wrong?
    "HIGH|MEDIUM",               // Severity level
    "Fix suggestion with code"   // How to fix it?
)
```

### Guidelines

1. **Test your regex**: Make sure it matches what you intend
2. **Avoid false positives**: Test on various code samples
3. **Provide actionable fixes**: Include actual code in the fix suggestion
4. **Add test cases**: Create examples in `test-examples/`
5. **Update README**: Add your pattern to the list

### Example: Adding a New Pattern

```java
// In CodeGuard.java, add to PATTERNS list:
new CodePattern(
    "Thread\\.sleep\\(0\\)",
    "Thread.sleep(0) is a no-op and likely a bug",
    "MEDIUM",
    "Thread.sleep(100); // or remove if not needed"
)
```

Then add a test case in `test-examples/NpeExamples.java`:

```java
// Pattern X: Thread.sleep(0) no-op
public void emptySleep() throws InterruptedException {
    Thread.sleep(0); // Bug: should be a non-zero value
}
```

## Code Style

- Follow existing Java conventions
- Keep patterns organized by category
- Use clear, descriptive variable names
- Add comments for complex regex patterns

## Testing

Before submitting:

```bash
# Build
./build.sh

# Test on examples
java -jar codeguard.jar test-examples/

# Test on a real project
java -jar codeguard.jar src/
```

## Questions?

Open an issue or start a discussion if you have questions!

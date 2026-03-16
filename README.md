# CodeGuard

A fast, lightweight Java code analyzer that detects common bugs, performance issues, and concurrency problems before code review.

## Why CodeGuard?

Static analysis tools miss many runtime issues that only appear with real data. CodeGuard catches these "low-hanging fruit" bugs instantly, giving you a clean report to attach to pull requests.

## Features

**43 Detection Patterns** across 5 categories:

- **NPE & Null Safety** (5) - Optional.get(), method chaining, map.get() without checks
- **Performance Issues** (7) - Uncached Pattern.compile(), boxing in loops, String concatenation
- **Concurrency & Async** (24) - Race conditions, deadlocks, blocking async calls, volatile misuse
- **Index & Bounds** (4) - Array/List access, split() usage without validation
- **Code Quality** (3) - Resource leaks, magic literals, empty catch blocks

**Smart Reporting:**
- Groups duplicate issues by type (clean, readable output)
- Color-coded severity (HIGH/MEDIUM)
- Shows line numbers and occurrence count
- Copy-paste ready fix suggestions

**Two Modes:**
- **Fast Mode** (default) - Instant results, works offline, no API key needed
- **AI Mode** (`--ai`) - Context-aware fixes via Claude API (optional)

## Quick Start

### Installation

**Clone and use:**
```bash
git clone https://github.com/guptaruhi/code-guard.git
cd code-guard
java -jar codeguard.jar /path/to/your/project
```

**Or download just the JAR:**
```bash
curl -L -O https://github.com/guptaruhi/code-guard/raw/main/codeguard.jar
java -jar codeguard.jar /path/to/your/project
```

### Usage

```bash
# Scan a single file
java -jar codeguard.jar MyFile.java

# Scan entire project
java -jar codeguard.jar src/

# Show all issues (no limit)
java -jar codeguard.jar --all src/

# AI mode (requires CLAUDE_API_KEY)
java -jar codeguard.jar --ai src/

# Combine options
java -jar codeguard.jar --ai --all src/
```

### Optional: Create Alias

Add to `~/.zshrc` or `~/.bashrc`:
```bash
alias codeguard="java -jar /path/to/codeguard.jar"
```

Then simply run: `codeguard src/`

## Example Output

```
CodeGuard - src/UserService.java

Summary: 22 finding(s) across 15 issue type(s) - 10 HIGH, 5 MEDIUM

⚠ HIGH PRIORITY
──────────────────────────────────────────────────────────────

CompletableFuture.get() blocks thread (use .join() or async callbacks)
  Line 19
  Fix: future.thenApply(result -> ...) or future.get(timeout, TimeUnit.SECONDS) with timeout

Increment operation without synchronization (race condition)
  Line 257
  Fix: Use AtomicInteger.incrementAndGet() or synchronize access

⚡ MEDIUM PRIORITY
──────────────────────────────────────────────────────────────

Thread.sleep() in code (consider ScheduledExecutorService)
  Lines: 35, 41, 50, 56, 77, 85 (6 occurrences)
  Fix: ScheduledExecutorService executor = Executors.newScheduledThreadPool(1); executor.schedule(task, delay, TimeUnit)

Tip: Fix HIGH priority issues first!
```

## Detection Categories

### NPE & Null Safety (5 patterns)
- Optional.get() without isPresent()
- Method chaining without null checks
- Map.get() result used directly
- Null dereference patterns

### Performance (7 patterns)
- Pattern.compile() not cached
- List.size() in loop condition
- Map iteration with keySet() instead of entrySet()
- Boxing/unboxing in loops
- Multiple string concatenations with +
- indexOf() used for contains() check

### Concurrency (24 patterns)
**Async Blocking:**
- CompletableFuture.get() / .join() blocking calls
- Thread.sleep() in async callbacks

**Race Conditions:**
- counter++/counter-- without synchronization
- Compound assignments (+=) without locks
- Non-volatile shared fields

**Deadlocks:**
- Nested synchronized blocks
- Synchronizing on String literals or boxed primitives

**Other:**
- volatile arrays (elements not volatile)
- Manual thread creation (use ExecutorService)
- wait() without proper while loop

### Index & Bounds (4 patterns)
- Array/List access without bounds checks
- String.split() result accessed without validation
- Iterator.next() without hasNext()

### Code Quality (3 patterns)
- Resource leaks (Files/Streams not in try-with-resources)
- Returning null from sanitizers/formatters
- Magic character literals without constants

## AI Mode (Optional)

For context-aware fixes specific to your code:

1. Get Claude API key from https://console.anthropic.com/
2. Set environment variable:
   ```bash
   export CLAUDE_API_KEY="sk-ant-xxxxx"
   ```
3. Run with `--ai` flag:
   ```bash
   java -jar codeguard.jar --ai src/
   ```

AI mode analyzes your actual code context to provide specific fixes instead of generic templates.

## Build from Source

```bash
git clone https://github.com/guptaruhi/code-guard.git
cd code-guard
javac analyzer/CodeGuard.java
jar cvfe codeguard.jar analyzer.CodeGuard analyzer/*.class
java -jar codeguard.jar test-examples/
```

## Project Structure

```
code-guard/
├── analyzer/
│   └── CodeGuard.java              # Main scanner (43 patterns)
├── test-examples/
│   ├── TestConcurrency.java        # Concurrency patterns test
│   ├── TestPerformance.java        # Performance patterns test
│   ├── TestMaintainability.java    # Code quality patterns test
│   ├── NpeExamples.java            # NPE patterns test
│   └── CommonMistakes.java         # General patterns test
├── codeguard.jar                   # Pre-built executable
└── README.md
```

## Contributing

1. **Report Issues**: Found a bug or false positive? Open an issue
2. **Suggest Patterns**: Have ideas for new detections? Create a feature request
3. **Submit PRs**: Add new patterns, improve existing ones, or enhance reporting

### Adding a New Pattern

Edit `analyzer/CodeGuard.java` and add to the `PATTERNS` list:

```java
new CodePattern(
    "your-regex-pattern",
    "Description of the issue",
    "HIGH",  // or "MEDIUM"
    "Suggested fix with code example"
)
```

Rebuild the JAR and test with example files.

## License

MIT License - see [LICENSE](LICENSE) file for details.

---

**Built for developers who want fast, actionable code reviews.** 🚀
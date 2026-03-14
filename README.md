# CodeGuard - NPE Pattern Detector

A lightweight Java tool that scans Java files for common NullPointerException (NPE) patterns before code review. Built as a standalone project for full-stack Java developers to catch runtime errors that slip through existing static analysis tools.

## Why CodeGuard?

**Problem**: Despite existing static analysis tools (Error Prone, Checkstyle, etc.), NPEs, IndexOutOfBounds, and similar runtime errors still appear in production with real data.

**Solution**: A simple, fast "low-hanging fruit" scanner that developers can run before submitting code for review, providing a clean report to attach to pull requests.

## Features

### Currently Implemented Patterns (14)

**NPE & Null Safety** [HIGH]:
- ✅ Optional.get() without isPresent() check
- ✅ Method chaining without null checks
- ✅ Map.get() result used without null check

**Index & Bounds** [MEDIUM]:
- ✅ Array access without bounds check
- ✅ List.get() without size check
- ✅ String.split() result accessed without validation
- ✅ Iterator.next() without hasNext() check
- ✅ String.substring() without length check
- ✅ String.charAt() without length check

**Common Coding Mistakes** [HIGH]:
- ✅ String comparison using == instead of .equals()
- ✅ Empty catch blocks (swallowing exceptions)

**Performance Issues** [HIGH]:
- ✅ String concatenation in loops (use StringBuilder)

**Resource Management** [HIGH]:
- ✅ Files/Streams not in try-with-resources

**Code Quality** [MEDIUM]:
- ✅ System.out.println (use proper logging)

### Planned Patterns (To Be Added)

**NPE & Null Safety**:
- ⏳ Direct null dereference on parameters/fields
- ⏳ Collection operations without null checks (list.size(), map.keySet())
- ⏳ Stream operations without null guards (findFirst().get())
- ⏳ Autoboxing NPE risks (Integer i = null; int x = i;)

**Index & Bounds**:
- ⏳ Using remove() in for-loop

**Resource Management**:
- ⏳ Scanner/Reader not closed properly

**Common Coding Mistakes**:
- ⏳ Hardcoded sensitive data (passwords, API keys)

**Collections & Performance**:
- ⏳ Using keySet() instead of entrySet()
- ⏳ Collection.contains() in nested loops (O(n²) issue)

**Concurrency Issues**:
- ⏳ Non-thread-safe SimpleDateFormat shared across threads
- ⏳ Synchronizing on String literals

### Tool Features

- **Directory Scanning**: Scan entire projects recursively, not just single files
- **Aggregate Reports**: See total issues across all files with per-file breakdown
- **Colorful Terminal Output**: Issues grouped by severity (HIGH/MEDIUM)
- **Compact Table Format**: Easy-to-scan horizontal layout
- **Limited Output**: Shows max 5 issues per severity (use --all for more)
- **Copy-Paste Fixes**: Ready-to-use code suggestions
- **Zero Dependencies**: Pure Java with regex pattern matching
- **Fast & Lightweight**: No AST parsing, just simple pattern matching
- **Smart Filtering**: Automatically skips build directories (.git, target, build)

## Quick Start

### Installation (Recommended)

**1. Clone or download this repository**
```bash
git clone https://github.com/YOUR_USERNAME/code-guard.git
cd code-guard
```

**2. Build the JAR**
```bash
./build.sh
```

**3. Install globally (optional)**
```bash
./install.sh
```

After installation, you can run `codeguard` from anywhere:
```bash
codeguard src/                 # Scan directory
codeguard MyFile.java          # Scan single file
codeguard --all src/           # Show all issues
```

### Quick Test

```bash
# Test on example files
java -jar codeguard.jar test-examples/
```

Expected output: Should detect 15+ issues with colorful, grouped output.

### Manual Usage (Without Installation)

If you don't want to install globally, use the JAR directly:

```bash
# Scan single file
java -jar codeguard.jar path/to/YourFile.java

# Scan entire directory (recursive)
java -jar codeguard.jar src/

# Show all issues
java -jar codeguard.jar --all src/

# Scan current directory
java -jar codeguard.jar .
```

## Usage Examples

### Single File Scan

```bash
java analyzer.CodeGuard MyFile.java
```

Output:
```
CodeGuard - MyFile.java

Summary: 5 issue(s) - 2 HIGH, 3 MEDIUM

⚠ HIGH PRIORITY
  LINE | ISSUE                                          | FIX
  ──────────────────────────────────────────────────────────────────────────
  L13  | Optional.get() without isPresent() check       | if (value.isPresent()) { return value.get(); } else { return value.orElse(defaultValue); }
  L24  | String comparison using == instead of .equals() | if (str1.equals(str2)) { ... }

⚡ MEDIUM PRIORITY
  LINE | ISSUE                                          | FIX
  ──────────────────────────────────────────────────────────────────────────
  L18  | Array access without bounds check              | if (array.length > 0) { return array[0]; }
  L32  | List.get() without size check                  | if (!list.isEmpty()) { return list.get(0); }
  L45  | Iterator.next() without hasNext() check        | if (iter.hasNext()) { return iter.next(); }

Tip: Fix HIGH priority issues first!
```

### Directory Scan (NEW!)

Scan entire directories recursively:

```bash
java analyzer.CodeGuard src/
```

Output:
```
CodeGuard - Scanning Directory: src/
Found 23 Java file(s)

====================================================================================================
SUMMARY: 47 issue(s) across 12 file(s)
  15 HIGH | 32 MEDIUM
====================================================================================================

────────────────────────────────────────────────────────────────────────────────────────────────────
CodeGuard - src/main/java/com/example/UserService.java

Summary: 8 issue(s) - 3 HIGH, 5 MEDIUM
...
[shows issues for each file]
...

====================================================================================================
Total: 47 issue(s) in 12 file(s)
Tip: Fix HIGH priority issues first!
```

**Features:**
- ✅ Recursive scanning (scans subdirectories)
- ✅ Aggregated summary (total issues across all files)
- ✅ Per-file breakdown
- ✅ Skips build directories (target/, build/, .git/, etc.)

### Show All Issues

```bash
java analyzer.CodeGuard --all MyFile.java  # Single file
java analyzer.CodeGuard --all src/         # Directory
```

Shows all issues without the 5-per-severity limit.

## Installation Options

**Option 1: Global Install (Recommended)**
```bash
./install.sh
# Then use: codeguard src/
```

**Option 2: Use JAR Directly**
```bash
java -jar codeguard.jar src/
```

**Option 3: Add to PATH**
```bash
# Add this to your ~/.bashrc or ~/.zshrc
export PATH="$HOME/.local/bin:$PATH"
```

## Architecture

- **Language**: Pure Java (no dependencies)
- **Detection Method**: Regex pattern matching (simple, fast, deterministic)
- **No AST parsing**: Keeps it lightweight and easy to extend
- **Output**: Colorful terminal + plain text file for code reviews
- **Standalone**: No build system required - just javac and java

## Extending CodeGuard

To add a new NPE pattern, add to the `PATTERNS` list in `CodeGuard.java`:

```java
new NpePattern(
    "your-regex-here",
    "Description of the issue",
    "HIGH",  // or "MEDIUM"
    "Suggestion for fixing it"
)
```

## Project Structure

```
code-guard/
├── analyzer/
│   └── CodeGuard.java           # Main scanner (14 patterns)
├── test-examples/
│   ├── NpeExamples.java         # Test file with 15+ issues
│   └── CommonMistakes.java      # Additional test cases
├── build.sh                     # Build script (creates JAR)
├── install.sh                   # Installation script
├── MANIFEST.MF                  # JAR manifest
├── README.md                    # This file
└── LICENSE                      # MIT License
```

## Testing

The `test-examples/NpeExamples.java` file contains 10 intentional NPE patterns to verify CodeGuard's detection capabilities.

```bash
java analyzer.CodeGuard test-examples/NpeExamples.java
```

## Future Enhancements

**Tool Features**:
- Support batch scanning (multiple files or directories)
- HTML report generation
- Configuration file for custom patterns
- Integration with git diff (scan only changed files)
- CI/CD integration
- IDE plugin support

**Pattern Improvements**:
- See "Planned Patterns" section above for upcoming detections

## Design Philosophy

- **Start simple, iterate fast**
- **Test-driven**: Create failing tests first, then build the tool
- **Developer-friendly**: Fast, easy to run, clear output
- **Code-review focused**: Output designed to be shared with reviewers

## Contributing

Contributions are welcome! Here's how you can help:

1. **Report Issues**: Found a bug or false positive? Open an issue
2. **Suggest Patterns**: Have ideas for new detections? Create a feature request
3. **Submit PRs**:
   - Add new patterns to `PATTERNS` list in `CodeGuard.java`
   - Update `README.md` with pattern description
   - Add test cases to `test-examples/`

### Adding a New Pattern

```java
new CodePattern(
    "your-regex-here",
    "Description of the issue",
    "HIGH",  // or "MEDIUM"
    "Suggested fix code"
)
```

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Roadmap

**Phase 1 - Core Features** ✅
- [x] 14 detection patterns (NPE, performance, security)
- [x] Directory scanning with recursive support
- [x] Aggregate reporting across multiple files
- [x] JAR packaging and installation scripts

**Phase 2 - Advanced Features** (Planned)
- [ ] Configuration file support (.codeguard.yml)
- [ ] AI-powered context-aware fixes
- [ ] Git integration (scan only changed files)
- [ ] CI/CD integration (GitHub Actions, Jenkins)
- [ ] IDE plugins (VS Code, IntelliJ)
- [ ] HTML report generation

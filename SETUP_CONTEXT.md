# CodeGuard - Complete Setup Context

## Project Overview

CodeGuard is a lightweight Java tool that scans Java files for common NullPointerException (NPE) patterns before code review. Built as a standalone project for full-stack Java developers to catch runtime errors that slip through existing static analysis tools.

## Why We Built This

**Problem**: Despite existing static analysis tools (Error Prone, Checkstyle, etc.), NPEs, IndexOutOfBounds, and similar runtime errors still appear in production with real data.

**Solution**: A simple, fast "low-hanging fruit" scanner that developers can run before submitting code for review, providing a clean report to attach to pull requests.

## Architecture Decisions

- **Language**: Pure Java (no dependencies)
- **Detection Method**: Regex pattern matching (simple, fast, deterministic)
- **No AST parsing**: Keeps it lightweight and easy to extend
- **Output**: Colorful terminal + plain text file for code reviews
- **Standalone**: No build system initially - just javac and java

## Project Structure

```
/Users/ruhi.gupta/Documents/code-guard/
├── test-examples/
│   └── NpeExamples.java          # Test file with 10 intentional NPE patterns
├── analyzer/
│   └── CodeGuard.java            # The scanner tool (main class)
├── README.md                      # Usage instructions
└── SETUP_CONTEXT.md              # This file
```

## Setup Instructions

### Step 1: Directory Already Created ✓

The directory structure has been created at `/Users/ruhi.gupta/Documents/code-guard/`

### Step 2: Files Created ✓

All files have been created:
- `test-examples/NpeExamples.java` - Test file with 10 intentional NPE patterns
- `analyzer/CodeGuard.java` - The scanner tool
- `README.md` - Usage documentation
- `SETUP_CONTEXT.md` - This setup guide

### Step 3: Compile and Run

```bash
cd /Users/ruhi.gupta/Documents/code-guard
javac analyzer/CodeGuard.java
java analyzer.CodeGuard test-examples/NpeExamples.java
```

**Expected output**: Should detect 8-10 NPE patterns with colorful, grouped output.

### Step 4: Test on Real Code

```bash
java analyzer.CodeGuard path/to/YourFile.java
java analyzer.CodeGuard -o report.txt path/to/YourFile.java  # Save report to file
```

## What It Detects (7 Patterns)

✓ Optional.get() without isPresent() check [HIGH]
✓ Array access without bounds check [MEDIUM]
✓ List.get() without size check [MEDIUM]
✓ Method chaining without null checks [HIGH]
✓ Map.get() result used without null check [HIGH]
✓ String.split() result accessed without validation [MEDIUM]
✓ Iterator.next() without hasNext() check [MEDIUM]

## Output Modes

### Terminal Mode (Colorful):

```
CodeGuard - NPE Pattern Detector
File: MyFile.java

⚠ HIGH PRIORITY (2)
  Line 13: Optional.get() without isPresent() check
    return value.get();

⚡ MEDIUM PRIORITY (3)
  Line 18: Array access without bounds check
    return args[0];

Summary: 5 potential NPE issue(s) detected
Review these before submitting for code review.
```

### File Mode (Plain Text for Code Review):

```bash
java analyzer.CodeGuard -o report.txt MyFile.java
```

Generates a plain-text report suitable for attaching to pull requests.

## Usage Examples

```bash
# Basic scan with colorful output
java analyzer.CodeGuard MyFile.java

# Scan and save report for code review
java analyzer.CodeGuard -o npe-report.txt MyFile.java

# Scan file in another project
java analyzer.CodeGuard /path/to/other-project/src/MyClass.java

# Create executable script for easy access
echo '#!/bin/bash' > ~/code-guard
echo 'java /Users/ruhi.gupta/Documents/code-guard/analyzer/CodeGuard "$@"' >> ~/code-guard
chmod +x ~/code-guard
~/code-guard MyFile.java -o report.txt
```

## Future Enhancements (Not Implemented Yet)

- Add performance pattern detection (N+1 queries, inefficient loops)
- Support batch scanning (multiple files or directories)
- HTML report generation
- Configuration file for custom patterns
- Integration with git diff (scan only changed files)
- CI/CD integration

## How to Extend

To add a new NPE pattern, add to the `PATTERNS` list in `CodeGuard.java`:

```java
new NpePattern(
    "your-regex-here",
    "Description of the issue",
    "HIGH",  // or "MEDIUM"
    "Suggestion for fixing it"
)
```

## Design Philosophy

- **Start simple, iterate fast**
- **Test-driven**: Create failing tests first, then build the tool
- **Developer-friendly**: Fast, easy to run, clear output
- **Code-review focused**: Output designed to be shared with reviewers

## What We Built

✓ Created standalone project at `/Users/ruhi.gupta/Documents/code-guard/`
✓ Created test file with 10 intentional NPE patterns
✓ Built simple regex-based scanner tool
✓ Added colorful terminal output grouped by severity
✓ Added file export option for code reviews
✓ Ready to test on real code

## How to Reproduce on Another Machine

1. Copy this SETUP_CONTEXT.md file
2. Create the directory structure
3. Create all files (NpeExamples.java, CodeGuard.java, README.md)
4. Compile and test
5. Run on your own code

**That's it!** No dependencies, no build system required - just Java.

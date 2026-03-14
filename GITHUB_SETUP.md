# GitHub Setup Guide

Follow these steps to publish CodeGuard to GitHub.

## Prerequisites

- GitHub account
- Git installed on your machine

## Step 1: Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `code-guard` (or `java-code-guard`)
3. Description: "A lightweight Java static analysis tool that detects NPE, performance issues, and code smells before code review"
4. Choose: **Public** (so others can use it)
5. **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

## Step 2: Initialize Local Git Repository

```bash
cd /Users/ruhi.gupta/Documents/code-guard

# Initialize git (if not already done)
git init

# Add all files
git add .

# Create first commit
git commit -m "Initial commit: CodeGuard v1.0.0

- 14 detection patterns (NPE, performance, security, code quality)
- Directory scanning with recursive support
- Aggregate reporting across multiple files
- JAR packaging with installation scripts"
```

## Step 3: Link to GitHub and Push

Replace `YOUR_USERNAME` with your GitHub username:

```bash
# Add remote repository
git remote add origin https://github.com/YOUR_USERNAME/code-guard.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## Step 4: Create Release (Optional but Recommended)

1. Go to your repository on GitHub
2. Click "Releases" → "Create a new release"
3. Tag: `v1.0.0`
4. Title: `CodeGuard v1.0.0 - Initial Release`
5. Description:
```markdown
## Features
- 14 detection patterns covering NPE, performance, security, and code quality
- Directory scanning with recursive support
- Colorful terminal output with actionable fixes
- Zero dependencies - pure Java

## Installation
Download `codeguard.jar` and run:
```bash
java -jar codeguard.jar src/
```

See README for full installation instructions.
```
6. Upload the `codeguard.jar` file as a release asset
7. Click "Publish release"

## Step 5: Update README (After Publishing)

Replace this line in README.md:
```bash
git clone https://github.com/YOUR_USERNAME/code-guard.git
```

With your actual GitHub URL.

## Done! 🎉

Your repository is now public and others can:
- Clone it
- Download the JAR from releases
- Contribute via PRs
- Report issues

## Next Steps

- Share the link with your team
- Tweet about it / post on LinkedIn
- Add topics to your repo: `java`, `static-analysis`, `code-quality`, `npe-detection`
- Star your own repo (why not? 😄)

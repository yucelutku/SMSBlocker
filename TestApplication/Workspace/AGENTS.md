# AGENTS.md - Cursor Claude Code Workflow Guide

## 🤖 Claude Code Agent Strategies

Bu dosya, Cursor'da Claude Code ile SMS Spam Blocker projesini geliştirirken kullanacağın agent stratejilerini ve prompt template'lerini içeriyor.

## 🎯 Agent Types & Usage

### 1. 🏗️ **ARCHITECTURE AGENT** - Proje yapısı ve setup

**Usage**: Yeni sınıf/package oluştururken, architecture kararları alırken

**Prompt Template:**
```
I'm building an SMS Spam Blocker Android app. Based on CLAUDE.md context:

TASK: [Create MainActivity with Material Design 3 / Setup SMS permissions / etc.]

Requirements:
- Target SDK 33, Min SDK 26
- Java language
- MVVM + Repository pattern
- Material Design 3 with dark/light theme
- Default SMS app functionality

Please create the complete implementation with:
1. Proper error handling
2. Material Design 3 components
3. Turkish localization support
4. KVKV compliance considerations

Current project structure: [paste current folder structure]
```

### 2. 📱 **SMS OPERATIONS AGENT** - SMS işlemleri

**Usage**: SMS okuma, yazma, silme işlemleri için

**Prompt Template:**
```
SMS Operations Task for Android 13 (API 33) SMS Spam Blocker:

CONTEXT: Default SMS app that needs to [read/delete/filter] SMS messages

TASK: [Implement SMS deletion / Create spam detector / etc.]

Requirements:
- Handle SMS permissions properly for Android 13
- Support both SMS and MMS
- Turkish gambling spam detection
- Error handling for permission denials
- Room database integration

Keywords to detect: bahis, kumar, bet, casino, bonus, freespin, çevrim, yatır, kazanç

Please implement with proper security and performance considerations.
```

### 3. 🎨 **UI/UX AGENT** - Interface tasarımı

**Prompt Template:**
```
Material Design 3 UI Task for SMS Spam Blocker:

TASK: [Create SMS list layout / Design settings screen / etc.]

Design Requirements:
- Material Design 3 components
- Dynamic color system (Material You)
- Dark/light theme support
- Turkish text support
- Accessibility compliance
- Modern, clean, security-focused aesthetic

Screen: [MainActivity / SettingsActivity / etc.]
Context: [User needs to see SMS messages / manage blocked numbers / etc.]

Please create XML layouts and corresponding Java code with proper view binding.
```

### 4. 🔒 **PERMISSIONS AGENT** - İzin yönetimi

**Prompt Template:**
```
Android 13 Permissions Task for SMS App:

TASK: [Setup runtime permissions / Handle default SMS app / etc.]

Critical Requirements:
- Android 13 permission model compliance
- Default SMS app registration workflow  
- Graceful permission denial handling
- User-friendly permission explanations
- Turkish UI text

SMS Permissions needed: READ_SMS, WRITE_SMS, RECEIVE_SMS, SEND_SMS
Additional: POST_NOTIFICATIONS for Android 13

Please implement proper permission flow with educational dialogs.
```

### 5. 🗃️ **DATA AGENT** - Database ve data management

**Prompt Template:**
```
Room Database Task for SMS Spam Blocker:

TASK: [Setup database / Create DAO / Repository pattern / etc.]

Database Requirements:
- Room database with proper migrations
- Entities: SmsMessage, BlockedNumber, SpamKeyword
- Repository pattern implementation
- MVVM ViewModel integration
- Turkish character support in search
- Data encryption for sensitive information

Please implement with proper error handling and performance optimization.
```

### 6. 🔧 **DEBUGGING AGENT** - Sorun çözme

**Usage**: Hata ayıklama, performance optimization

**Prompt Template:**
```
Debug Task for SMS Spam Blocker:

PROBLEM: [Describe the issue you're facing]

Current Code: [paste problematic code]

Error/Issue: [paste error logs or describe behavior]

Context:
- Android 13, Min SDK 26
- Default SMS app
- Material Design 3
- MVVM architecture

Please analyze and provide solution with explanation of the issue and prevention strategies.
```

## 🔄 Workflow Strategies

### **Vibe Coding Session Workflow:**

1. **🎬 SESSION STARTER**
```
Starting SMS Spam Blocker vibe coding session!

Today's Goals: [list 2-3 specific tasks]
Current Status: [what's already done]
Next Priority: [what to work on next]

Load CLAUDE.md context and let's implement [specific feature].
```

2. **⚡ RAPID FEATURE DEVELOPMENT**
```
Quick Implementation Request:

Feature: [specific feature name]
Files needed: [Activity/Fragment/etc.]
Dependencies: [what needs to be implemented first]

Create minimal working implementation that I can iterate on.
Priority: working code > perfect code (we'll refine later)
```

3. **🚀 ENHANCEMENT ITERATIONS**
```
Enhance existing implementation:

Current file: [filename]
Current code: [paste current implementation]

Improvements needed:
- [ ] Better error handling
- [ ] Material Design 3 styling
- [ ] Performance optimization
- [ ] Turkish localization

Apply improvements incrementally.
```

## 📋 Quick Commands

### **Fast Setup Commands:**
```bash
# Create new Activity with Material Design 3
@claude Create MainActivity with Material Design 3 RecyclerView for SMS list

# Add permission handling
@claude Add Android 13 SMS permissions with runtime requests

# Generate spam detector
@claude Create SpamDetector class with Turkish gambling keywords

# Setup Room database
@claude Setup Room database with SMS and blocked numbers entities
```

### **Context Switching:**
```bash
# When switching between features
@claude Switch context to [SMS operations/UI design/Database/Permissions]
Load relevant requirements from CLAUDE.md

# When encountering errors
@claude Debug mode: [paste error] - provide quick fix and explanation

# When adding new features
@claude Feature expansion: [describe feature] - implement following MVVM pattern
```

## 🎨 Material Design 3 Specific Prompts

```
Material Design 3 Component Request:

Component needed: [FAB/Card/Dialog/etc.]
Usage: [describe where and how it's used]
Theme: Support both light/dark with dynamic colors
Accessibility: Full a11y compliance

Generate XML and Java code following Material Design 3 guidelines.
```

## 🔍 Testing & Validation Prompts

```
Testing Task:

Implementation: [paste code to test]
Test type: [Unit test / Integration test / UI test]
Focus: [SMS operations / UI behavior / Permission flow]

Generate appropriate test code with mock data and edge cases.
```

## 📱 Device-Specific Prompts

```
Samsung One UI Compatibility:

Feature: [specific implementation]
Target: Samsung Galaxy A51 with One UI 5.1
Consideration: Knox security, Samsung Messages compatibility

Ensure implementation works well with Samsung's modifications.
```

---

## 💡 Pro Tips for Cursor + Claude Code:

1. **Always reference CLAUDE.md context** at the start of sessions
2. **Use specific prompts** rather than generic "help me code"
3. **Iterate incrementally** - get working code first, optimize later
4. **Test on multiple Android versions** during development
5. **Keep sessions focused** - one major feature per session
6. **Save context** between sessions by summarizing current state
7. **Commit after each working feature** - use git workflow below
8. **Document progress** in commit messages for easy continuation

## 🚫 What NOT to ask:

- ❌ "Write the entire app" - too broad
- ❌ Generic Android tutorials - be specific to SMS spam blocking
- ❌ Complex features before MVP is complete
- ✅ "Implement SMS deletion with proper error handling for Android 13"
- ✅ "Create Material Design 3 RecyclerView adapter for SMS messages"

---

## 📝 Git Workflow & Commit Strategy

### **Commit After Each Working Feature:**

```bash
# After completing any implementation task
@claude Feature complete: [feature name]
Please generate appropriate git commit message following conventional commits format.

# Example responses:
# feat(sms): add SMS permissions and runtime request handling
# feat(ui): implement Material Design 3 main layout with dark theme
# feat(db): setup Room database with SMS and BlockedNumber entities
# fix(permissions): handle Android 13 notification permission edge case
# refactor(spam): improve Turkish keyword detection accuracy
```

### **Commit Message Templates:**

**Feature Implementation:**
```bash
git add .
git commit -m "feat(scope): add [feature description]

- Implemented [specific functionality]
- Added [components/files created]
- Tested on Android 13, works with [specific scenarios]
- Ready for next phase: [what comes next]"
```

**Bug Fixes:**
```bash
git commit -m "fix(scope): resolve [issue description]

- Fixed [specific problem]
- Root cause: [brief explanation]
- Solution: [approach used]
- Tested scenarios: [what was verified]"
```

**UI/UX Updates:**
```bash
git commit -m "ui(scope): implement [UI component/improvement]

- Added Material Design 3 [component]
- Supports dark/light themes
- Turkish localization included
- Accessibility compliance verified"
```

### **End-of-Session Commits:**
```bash
# Always commit before ending session
git add .
git commit -m "checkpoint: end of [session topic] development

Current status:
- ✅ [completed features]
- 🚧 [work in progress]  
- 📋 [next session tasks]

Notes: [any important context for next session]"
```

---

## 🔄 Continuing Without Claude Code

### **When Claude Code Usage Expires:**

These documentation files enable seamless continuation with regular Cursor:

**✅ What You Can Still Do:**
- **Complete architecture reference** in ARCHITECTURE.md
- **Detailed code examples** for all major components
- **Turkish spam detection patterns** ready to use
- **Material Design 3 layouts** fully specified
- **Database schemas** with complete entity definitions
- **Permission handling** step-by-step implementation
- **MVVM patterns** with concrete examples

### **Continuation Strategy Without Claude Code:**

**1. Use Architecture Documentation:**
```java
// ARCHITECTURE.md has complete code examples you can copy/adapt:
// - Complete SmsHelper class implementation
// - SpamDetector with Turkish keywords
// - Room database setup with all entities
// - Material Design 3 layout templates
```

**2. Leverage Cursor's Built-in Features:**
- **IntelliSense** with architecture context
- **Code completion** following established patterns
- **Refactoring tools** for code improvements
- **Built-in terminal** for git workflows

**3. Reference-Driven Development:**
```bash
# Use ARCHITECTURE.md as your implementation guide
# Each section has working code you can adapt
# CLAUDE.md has all business requirements
# Continue following the established patterns
```

**4. AI-Agnostic Continuation:**
- Documentation works with **any AI tool** (ChatGPT, Codeium, etc.)
- **Self-documenting architecture** - code comments explain patterns
- **Modular design** - each component can be developed independently
- **Clear separation of concerns** - easy to understand and extend

### **Alternative AI Tools Integration:**
```bash
# With ChatGPT/other AI tools:
"I'm developing an SMS Spam Blocker Android app. 
Here's my current architecture [paste ARCHITECTURE.md relevant section].
I need to implement [specific feature] following this pattern."

# With GitHub Copilot:
# Comments in code will guide Copilot using architecture patterns
// Following ARCHITECTURE.md pattern for SMS Repository
// Implementing Turkish spam detection using SpamDetector class

# With Codeium/other tools:
# Reference documentation provides context for any AI assistant
```

### **Documentation-Driven Development Benefits:**
- ✅ **Platform Independent** - works with any development environment
- ✅ **AI Tool Agnostic** - usable with any AI coding assistant
- ✅ **Self-Sufficient** - complete implementation guidance
- ✅ **Future-Proof** - documentation doesn't expire like usage limits
- ✅ **Team Ready** - other developers can join using same docs

### **Hybrid Development Approach:**
```bash
# Phase 1: Claude Code (Advanced AI assistance)
- Complex architecture decisions
- Initial implementation patterns
- Advanced problem solving

# Phase 2: Regular Cursor + Documentation
- Following established patterns
- Code completion with context
- Iterative improvements

# Phase 3: Any AI Tool + Documentation  
- Maintenance and updates
- New feature additions
- Bug fixes and optimizations
```

---

## 📋 Session Handoff Template

**Before Claude Code Expires:**
```bash
@claude Generate session handoff summary:

Current Implementation Status:
- ✅ Completed: [list completed features]
- 🚧 In Progress: [current work with status]
- 📋 Next Tasks: [prioritized next steps]
- 🐛 Known Issues: [any bugs or limitations]
- 📝 Important Notes: [critical context for continuation]

Generate git commit for current state and provide clear continuation roadmap.
```

This approach ensures **zero interruption** in your development workflow regardless of AI tool availability! 🚀
---
name: openl-release-notes
description: Generate professional release notes for OpenL Tablets releases by analyzing Git branches and commits to identify actual functionality changes. Use when the user requests release notes generation for OpenL Tablets, mentions "openl-tablets", "release notes", or specific version numbers like "5.27.8". This skill analyzes Git repository branches, extracts real functional changes (not just commit messages), and formats them according to the official OpenL Tablets release notes template.
---

# OpenL Tablets Release Notes Generator

This skill generates user-focused release notes for OpenL Tablets releases by analyzing Git repository changes and JIRA tickets to identify actual functional changes.

## Core Principle

**Release notes are user-facing product documentation, not engineering changelogs.**

Release notes must explain what changed for users in clear, descriptive language organized by feature area. They should:
- Lead with value and user impact
- Provide context and explanation
- Use narrative, descriptive prose
- Include visual aids where helpful
- Organize by feature/topic, not by component
- Guide users through breaking changes and migration

Release notes are NOT:
- Brief bullet-point changelogs
- Technical commit lists
- Component-grouped bug lists
- Minimalist reference documents

---

## When to Use This Skill

Automatically activate when the user:
- Says "generate release notes" or "create release notes"
- Mentions "OpenL Tablets release notes" or "OpenL release notes"
- Asks to generate or create release documentation for OpenL
- References a specific version number like "5.27.8" or "6.0.0"

---

## Complete Workflow

### Phase 1: Environment Setup & Requirements Gathering

#### Step 1.1: Verify Environment

**Check Prerequisites:**
- Verify you're in the OpenL Tablets repository root (look for `Docs/` directory)
- If not in repository, inform the user and stop
- Verify `Docs/release-notes/` directory exists, create if needed
- Check Git repository is up to date: `git fetch --all --tags`

#### Step 1.2: Gather Release Information

**Ask the user for:**

1. **Version Number**: The OpenL Tablets version for the release notes (e.g., "5.27.8", "6.0.0")
   - Note: JIRA version format may be "OpenL X.X.X" not just "X.X.X"
2. **Release Type**: Is this a major (6.0), minor (5.28), or patch (5.27.8) release?
   - Major releases have more extensive documentation needs
   - Minor releases focus on new features
   - Patch releases emphasize fixes and improvements

**Auto-detected values:**
- **Base Version**: Automatically detected from Git tags (previous version in the same release series)
- **Release Date**: Current date or user-specified date
- **Major Changes**: Identified automatically through JIRA ticket analysis and Git diff

**Output Location**: The file will be automatically generated in `Docs/release-notes/` within the OpenL Tablets repository.

---

### Phase 2: Data Collection (Dual-Source Approach)

#### Step 2.1: Collect JIRA Tickets (Primary Source)

Use Jira MCP tools to search for issues:

**JQL Query:**
```jql
project = EPBDS AND fixVersion = "OpenL {version}" AND "Release Notes" != "No, don't include ticket in Release Notes"
```

**Execution Notes:**
- Try version format "OpenL X.X.X" first (e.g., "OpenL 6.0.0")
- If that fails, try just the version number (e.g., "6.0.0")
- Use `jira_get_project_versions` if needed to find exact version format
- **Paginate through all results** if more than 50 tickets (use start_at parameter)
- Collect all tickets before proceeding to next step

**Fields to Extract:**
- Issue key and summary
- Issue type (Bug, Story, Task, Improvement, New Feature)
- Description (detailed information)
- Components (which OpenL component(s) affected)
- Labels (additional categorization)
- Custom field: "Release Notes" (pre-written release notes text if available)
- Priority and severity (for bug categorization)

**Store Complete Dataset:**
Create a temporary data structure with all ticket information for later analysis.

#### Step 2.2: Analyze Git Repository (Validation Source)

**Execute Git Analysis:**

```bash
# Ensure we're in the openl-tablets repository
cd /path/to/openl-tablets

# Identify release branch (if exists)
git branch -a | grep -i "release.*{version}"

# Get commit list between base and target version
git log --oneline {base_version}..{target_branch} > /tmp/git-commits.txt

# Get detailed statistics
git diff {base_version}..{target_branch} --stat > /tmp/git-stats.txt

# Identify modified files by functional area
git diff {base_version}..{target_branch} --name-only | grep -E "^(WSFrontend|STUDIO|Util)" > /tmp/modified-areas.txt
```

**Key Areas to Analyze:**
- Rule Services runtime (STUDIO/org.openl.rules.ruleservice)
- OpenL Studio/Webstudio (WSFrontend, STUDIO/org.openl.rules.webstudio)
- Core engine (Util/openl-*)
- Repository implementations (STUDIO/org.openl.rules.repository)
- Security (STUDIO/org.openl.security)
- Integration features (Docker, cloud storage, APIs)

**Identify in Git:**
- CVE fixes (security patches)
- Library version updates (pom.xml, package.json changes)
- Major architectural changes
- Files/modules with significant changes

#### Step 2.3: Cross-Validation

**Validation Checklist:**

1. **Completeness Check**:
   - Compare Git commit count with JIRA ticket count
   - Look for commits without associated tickets
   - Identify any significant file changes not covered by tickets

2. **Accuracy Check**:
   - For major features in JIRA, verify actual implementation in Git
   - Check if ticket descriptions match actual code changes
   - Validate library versions mentioned in tickets against pom.xml/package.json

3. **Missing Items Detection**:
   - Find Git commits that might not have JIRA tickets
   - Identify any CVE fixes in dependencies not captured in JIRA
   - Check for undocumented breaking changes in APIs

---

### Phase 3: Ticket Analysis & Feature Grouping

#### Step 3.1: Deep Analysis of Each Ticket

For each ticket collected from JIRA, extract and enrich:

**Core Information:**
- **Issue Type**: Bug, Story, Task, Improvement, New Feature
- **Summary**: Brief description
- **Description**: Detailed technical information
- **Components**: Which OpenL component(s) affected
- **Labels**: Additional categorization
- **Release Notes Field**: Custom field with pre-written text

**Enhanced Analysis:**
- **User Impact**: How does this affect end users?
- **Breaking Changes**: Does this require user action or migration?
- **Dependencies**: Are there library updates involved?
- **Security Impact**: Any CVE fixes or security improvements?
- **Related Features**: Does this connect with other changes?

**Cross-Reference with Git:**
- Find related commits for this ticket
- Examine actual code changes to understand true impact
- Validate ticket description against implementation
- Extract specific technical details (file formats, API changes, configuration options)

#### Step 3.2: Group into Feature Areas

Instead of grouping by component (Core, Studio, Rule Services), group by **user-facing feature area**:

**For Major Releases (e.g., 6.0.0):**
- Security & Access Control
- Administration & Configuration
- Rule Authoring & Development
- API & Integration
- Performance & Scalability
- Documentation & Usability

**For Minor/Patch Releases (e.g., 5.27.8):**
- Core Improvements
- OpenL Studio Enhancements
- Rule Services Updates
- Bug Fixes

**Feature Grouping Rules:**
1. Combine related tickets into cohesive feature stories
2. Lead with the most impactful changes
3. Group breaking changes together
4. Separate new features from improvements to existing features
5. Keep bug fixes in their own section unless they're part of a larger feature

---

### Phase 4: Content Generation (User-Focused Style)

#### Step 4.1: Output Structure

**For Major Releases (6.0.0 style):**

```markdown
# OpenL Tablets {version} Release Notes

{Opening paragraph describing the release theme and major highlights}

{Optional: Breaking changes warning for major releases}

## 1. New Features

### 1.1 {Feature Name}

{Narrative description of the feature, its value, and how it works}

#### {Sub-feature or Aspect}

{Detailed explanation with context}

* Bulleted lists for specific capabilities or details
* Multiple bullets providing concrete information
* Technical details where relevant

{Additional subsections as needed}

#### Migration Notes (if applicable)

{Clear, step-by-step migration guidance}

### 1.2 {Next Feature}

{Continue pattern...}

## 2. Improvements

### {Component Name}

* {Descriptive improvement explanation}
* {Another improvement with context}

## 3. Breaking Changes (if major release)

### 3.1 {Breaking Change Title}

{Explanation of what changed and why}

#### What Changed

* Clear description of the change
* Impact on users

#### Migration Steps

* Step-by-step migration guide
* Code examples where helpful

## 4. Security & Library Updates

### 4.1 Security Vulnerability Fixes

* **CVE-YYYY-XXXXX**: {Description and impact}

### 4.2 Major Library Upgrades

#### Runtime Dependencies

* Library Name X.Y.Z (from A.B.C)
* {List of major upgrades}

## 5. Bug Fixes

* {Descriptive bug fix explanation}
* {Another fix with context}

## 6. Migration Notes (if major release)

{Comprehensive migration guidance organized by user role or topic}
```

**For Minor/Patch Releases (5.27.8 style):**

```markdown
# OpenL Tablets Release Notes v{version}

**Release Date:** {date}

[v{version} on GitHub](https://github.com/openl-tablets/openl-tablets/releases/tag/{version})

## Improvements

### Core

* **{Feature Name}**: {Descriptive explanation of the improvement and its value}
* **{Another Feature}**: {Clear description with technical context}

### OpenL Studio

* **{Feature Name}**: {Explanation of the improvement}
* **{Another Feature}**: {Description with user benefit}

### Rule Services

* **{Feature Name}**: {Description of the improvement}

## Fixed Bugs

### Core

* **{Bug Description}**: {Explanation of what was fixed and the impact}

### OpenL Studio

* **{Bug Fix}**: {Clear description of the fix}

### Rule Services

* **{Bug Fix}**: {Description of what was corrected}

## Fixed Vulnerabilities (if applicable)

* **CVE-YYYY-XXXXX** (Severity): {Description, affected component, resolution}

## Updated Libraries

| Library | Previous Version | New Version |
|---------|-----------------|-------------|
| {entries} | | |

## Removed Features (if applicable)

* **{Feature Name}**: {Why removed and migration path}

## Migration Notes (if breaking changes)

### {Topic}

{Migration guidance for this specific area}

---

For more information, visit [OpenL Tablets Documentation](https://openl-tablets.org/documentation) or [GitHub Repository](https://github.com/openl-tablets/openl-tablets).
```

#### Step 4.2: Writing Style Guidelines

**CRITICAL DIFFERENCES from Changelog Style:**

**DO:**
- Write in complete sentences and paragraphs
- Provide context and explanation for changes
- Lead with user value and benefits
- Use descriptive headings that convey meaning
- Include "what" and "why" for major changes
- Group related changes into cohesive feature stories
- Add subsections for complex features
- Include migration guidance inline with breaking changes
- Use bold for emphasis on feature names and important concepts

**DON'T:**
- Write minimal bullet points without context
- Use generic headings like "Improvements" without subheadings
- List bugs without explaining the impact
- Group everything by component (Core/Studio/Rule Services)
- Omit the reasoning behind changes
- Skip migration guidance
- Use ticket numbers in main content

**Length Guidelines:**
- **Major features**: 2-4 paragraphs with subsections
- **Minor improvements**: 1-2 sentences per bullet point
- **Bug fixes**: 1 sentence explaining what was fixed
- **Breaking changes**: Full explanation plus migration steps

#### Step 4.3: Feature Description Pattern

**For Major Features (New Features section):**

```markdown
### 1.X {Feature Name}

{Opening paragraph: What is this feature and why was it added? What problem does it solve?}

{Second paragraph: How does it work at a high level? What are the key concepts?}

#### {Specific Aspect or Capability}

{Explanation of this aspect}

* Bullet point providing specific detail
* Another capability or characteristic
* Technical configuration if relevant

#### {Another Aspect}

{Continue pattern...}

#### Migration Notes

1. {Step-by-step migration guidance}
2. {Configuration examples}
3. {What to test}
```

**For Improvements (Improvements section):**

```markdown
### {Component Name}

* **{Feature Name}**: {1-2 sentence description explaining what improved, how it benefits users, and any relevant technical details}
* **{Another Improvement}**: {Similar format}
```

**For Bug Fixes:**

```markdown
* **{Description of Problem}**: {Explanation of what was broken and how it's now fixed}
```

#### Step 4.4: Library Updates

**Format as table:**

```markdown
| Library | Previous Version | New Version |
|---------|-----------------|-------------|
| Spring Framework | 5.3.36 | 5.3.39 |
| Jackson | 2.17.1 | 2.17.2 |
```

**Rules:**
- Only include libraries in the table, no explanatory text
- Validate versions against actual pom.xml/package.json from Git
- Group by category if very long (Runtime Dependencies, Test Dependencies, etc.)

#### Step 4.5: Breaking Changes Handling

**Breaking changes require:**
1. Clear heading identifying it as breaking
2. "What Changed" subsection explaining the change
3. "Impact" or "Why This Changed" subsection providing context
4. "Migration Steps" subsection with concrete actions
5. Code examples or configuration examples where helpful

**Example:**

```markdown
### 3.1 JSON Serialization Type Format Change

OpenL Rules now uses NAME-based polymorphic type identifiers in JSON serialization instead of CLASS-based identifiers.

#### What Changed

* JSON type information now uses simple type names (e.g., "Driver") instead of fully qualified class names (e.g., "com.example.Driver")
* This affects all polymorphic type serialization in REST APIs and JSON storage

#### Why This Changed

* CLASS-based typing exposes internal implementation details
* NAME-based typing is more portable and cleaner for API consumers
* Aligns with modern JSON serialization best practices

#### Impact

* Clients expecting CLASS-based types must be updated
* Stored JSON data with CLASS types may need migration

#### Migration Steps

* Update client applications to handle NAME-based type information
* Test JSON serialization/deserialization thoroughly
* If you must use CLASS format (not recommended), set:

  ```properties
  ruleservice.jackson.jsonTypeInfoId=CLASS
  ```
```

#### Step 4.6: CVE and Vulnerability Fixes

**Format:**
```markdown
* **CVE-2024-12345** (Critical): Fixed remote code execution vulnerability in Apache Tomcat. Updated to version 10.1.28.
* **CVE-2024-67890** (High): Addressed XML parsing vulnerability in Apache Commons Text. Updated to version 1.11.0.
```

**Requirements:**
- Always list CVE IDs
- Include severity in parentheses
- Provide one-sentence explanation of the vulnerability
- Specify affected library and new version

---

### Phase 5: Document Generation

#### Step 5.1: Create Markdown File

**Generate the complete Markdown document following the appropriate structure:**
- Use Major Release structure for X.0.0 versions
- Use Minor/Patch Release structure for X.Y.Z versions

**Key Requirements:**
- Lead with most impactful changes
- Group related features together
- Provide adequate context and explanation
- Include migration guidance for breaking changes
- Use descriptive headings and subheadings
- Write in complete sentences, not fragments
- Validate all technical details against Git repository

**Save as:** `Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md`

---

### Phase 6: Quality Assurance

#### Step 6.1: Validation Checklist

Before finalizing, verify:

**Content Structure:**
- [ ] Appropriate structure for release type (major vs minor/patch)
- [ ] Features grouped logically by topic, not just by component
- [ ] Breaking changes clearly identified and explained
- [ ] Migration guidance provided for all breaking changes
- [ ] Related features grouped into cohesive stories

**Content Style:**
- [ ] Written in narrative, descriptive prose
- [ ] Major features have 2-4 paragraph explanations
- [ ] Context provided for why changes were made
- [ ] User value and benefits clearly articulated
- [ ] No minimal bullet points without explanation
- [ ] No ticket references in main content (EPBDS-XXXX)

**Technical Accuracy:**
- [ ] All JIRA tickets accounted for
- [ ] Git validation completed
- [ ] CVE IDs formatted correctly (CVE-YYYY-XXXXX)
- [ ] Library versions validated against pom.xml/package.json
- [ ] Breaking changes have complete migration guidance
- [ ] Configuration examples are accurate

**User Experience:**
- [ ] Clear value proposition for each major feature
- [ ] Migration guidance is actionable
- [ ] Technical details are accurate but accessible
- [ ] Organized for easy scanning and navigation
- [ ] Breaking changes are prominently highlighted

#### Step 6.2: Completeness Check

**Ensure all ticket categories are covered:**
- New features properly described
- Improvements explained with context
- Bugs fixes documented with impact
- Security vulnerabilities addressed
- Library updates listed
- Removed features documented with migration paths
- Breaking changes explained with migration guidance

---

### Phase 7: Completion & Delivery

#### Step 7.1: Present File to User

```bash
# Copy file to outputs directory for user access
cp Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md /mnt/user-data/outputs/
```

Use `present_files` tool to make the file available.

#### Step 7.2: Provide Summary

Generate a brief summary:

```
Release Notes Generated Successfully

Summary:
├─ Release Type: {Major/Minor/Patch}
├─ Total Features: {X}
├─ Improvements: {Y}
├─ Bug Fixes: {Z}
├─ Vulnerabilities Fixed: {N} CVEs
├─ Libraries Updated: {M}
└─ Breaking Changes: {B}

File: OpenL_Tablets_{version}_Release_Notes.md
Format: User-focused release notes with narrative explanations
```

---

## Error Handling

### JIRA Connection Issues

**If JIRA authentication fails:**
- Inform user that JIRA authentication failed
- Suggest checking JIRA MCP server configuration
- Verify user has access to EPBDS project
- Offer to proceed with Git-only analysis (limited detail)

**If no tickets found:**
- Verify version format (try "OpenL X.X.X" vs "X.X.X")
- Use `jira_get_project_versions` to check available versions
- Confirm version exists in JIRA
- Ask user if version number is correct

**If pagination fails:**
- JIRA returns max 50 results per query
- Use start_at parameter to fetch remaining tickets
- Continue until all tickets collected
- Inform user of progress: "Collecting tickets: 150/200..."

### Git Repository Issues

**If Git repository not found:**
- Verify current directory is openl-tablets root
- Check if .git directory exists
- Ask user to navigate to correct directory

**If base version cannot be auto-detected:**
- List available Git tags: `git tag --list "v*"`
- Auto-detect most recent previous version in the same release series
- Fall back to comparison against main branch if no previous version exists

**If branch doesn't exist:**
- Check if version is tagged instead: `git tag --list "v{version}"`
- Use tag if branch doesn't exist
- Fall back to master/main branch comparison

### Document Generation Issues

**If directory creation fails:**
- Check write permissions for Docs/release-notes/
- Offer alternative output location
- Ask user for preferred output directory

**If file writing fails:**
- Check disk space
- Verify file path is valid
- Check for file lock (file already open)

---

## Style Examples

### Good User-Focused Example (Major Feature)

```markdown
### 1.1 Simplified User Access & Permissions Management in OpenL Studio

OpenL Studio introduces a **completely redesigned access control system** with a simplified, role-based permission model. The new approach significantly reduces configuration complexity while maintaining enterprise-grade security.

The new system supports **granular, resource-level access control** through role assignments, with a reduced and streamlined set of permissions for improved clarity and maintainability.

#### Role-Based Access Control

Access is now managed through three predefined roles:

* **Manager**: Full control with ability to assign roles
* **Contributor**: Content modification without system administration
* **Viewer**: Read-only access with test execution capabilities

The legacy permission model has been consolidated into **five core permissions**—**Manage, View, Create, Edit, and Delete**—which are embedded directly into roles.

All existing permissions are **automatically migrated** to the new role structure during upgrade.
```

### Good User-Focused Example (Improvement)

```markdown
### OpenL Studio

* **Enhanced Object Cloning**: Completely rewritten cloning mechanism that works without illegal reflection operations, providing better performance and full compatibility with modern Java versions including Java 21
* **Modern UI Framework**: Migrated from jQuery 1.7.2 to jQuery 3.7.1, along with updates to jQuery UI (1.13.2) and Fancytree (2.38.3), providing better browser compatibility and improved UI performance
* **Memory Optimization**: Improved memory usage by reusing existing search index nodes instead of creating duplicates, reducing overall memory footprint during rule compilation
```

### Bad Changelog Example (DO NOT USE)

```markdown
## Improvements

### Core
• Added SpreadsheetResult parameterization support
• Implemented automatic table type inference
• Enhanced virtual compilation

### OpenL Studio
• Added full-text search
• Implemented OAuth2 authentication
• Refactored administration UI
```

**Why this is bad:**
- Too brief, lacks context and explanation
- No user value articulated
- Generic component grouping
- No details about benefits or how features work
- Reads like a commit log, not user documentation

---

## Prerequisites

### Required Tools

1. **OpenL Tablets Repository**
   - Must be run from repository root
   - Git repository must be accessible
   - Recommend latest code: `git pull --all`

2. **JIRA MCP Server**
   - Configured with EPBDS project access
   - Valid authentication credentials
   - Network access to JIRA instance

3. **Git**
   - For repository analysis
   - Version: 2.20+ recommended

---

## License & Support

**Skill Version**: 4.0.0 (User-Focused Release Notes)
**Last Updated**: January 2026
**Compatible With**: Claude Desktop with JIRA MCP Server
**Maintained By**: OpenL Tablets Team

**Support Channels:**
- GitHub Issues: https://github.com/openl-tablets/openl-tablets/issues
- Documentation: https://openl-tablets.org/documentation
- Email: support@openl-tablets.org

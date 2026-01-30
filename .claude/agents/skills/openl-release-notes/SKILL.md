---
name: openl-release-notes
description: Generate professional release notes for OpenL Tablets releases by analyzing Git branches and commits to identify actual functionality changes. Use when the user requests release notes generation for OpenL Tablets, mentions "openl-tablets", "release notes", or specific version numbers like "5.27.8". This skill analyzes Git repository branches, extracts real functional changes (not just commit messages), and formats them according to the official OpenL Tablets release notes template.
---

# OpenL Tablets Release Notes Generator (Changelog Style)

This skill generates concise, changelog-style release notes for OpenL Tablets releases by analyzing Git repository changes and JIRA tickets to identify actual functional changes.

## Core Principle

**Release notes are a change log, not product documentation.**

Release notes function as a delta log of changes between versions. They must be concise, technical, scannable, and structured by component — never narrative, promotional, or documentation-style.

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

**Auto-detected values:**
- **Base Version**: Automatically detected from Git tags (previous version in the same release series)
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

**Output Validation Report:**
- Total tickets from JIRA: X
- Total commits in Git: Y
- Validated matches: Z
- Potential missing tickets: [list]
- Additional items found in Git: [list]

**If discrepancies found:**
- Inform user of missing or additional items
- Offer to search JIRA for specific commits
- Suggest manual review of questionable items

---

### Phase 3: Ticket Analysis & Categorization

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
- **Breaking Changes**: Does this require user action or migration?
- **Dependencies**: Are there library updates involved?
- **Security Impact**: Any CVE fixes or security improvements?
- **User Impact**: Who is affected and how?

**Cross-Reference with Git:**
- Find related commits for this ticket
- Examine actual code changes to understand true impact
- Validate ticket description against implementation
- Extract specific technical details (file formats, API changes, configuration options)

#### Step 3.2: Categorize Tickets

Organize tickets into these categories:

1. **Improvements**: New functionality and enhancements to existing features
   - Group by component
   - All changes go here unless they are bugs or library updates

2. **Fixed Bugs**: Fixed bugs and issues
   - Group by component
   - Separate critical from minor fixes

3. **Fixed Vulnerabilities**: CVE fixes and security patches
   - List CVE IDs
   - Specify affected libraries and versions

4. **Updated Libraries**: Dependency and library updates
   - List old → new versions in table format only
   - Validate versions against pom.xml/package.json from Git

5. **Removed Features** (if applicable): Deprecated or removed functionality

6. **Migration Notes** (only if breaking changes exist): Required actions for breaking changes

**Component Grouping:**
Within each category, organize by:
- Core
- OpenL Studio
- Rule Services

---

### Phase 4: Content Generation (Changelog Style)

#### Step 4.1: Output Structure (MANDATORY)

**All release notes MUST follow this exact structure:**

```markdown
# OpenL Tablets {version} Release Notes

**Release Date:** {date}
**GitHub Release:** https://github.com/openl-tablets/openl-tablets/releases/tag/v{version}

---

## Improvements

### Core
• bullets

### OpenL Studio
• bullets

### Rule Services
• bullets

---

## Fixed Bugs

### Core
• bullets

### OpenL Studio
• bullets

### Rule Services
• bullets

---

## Fixed Vulnerabilities

• CVE list with details

---

## Updated Libraries

| Library | Previous Version | New Version | Notes |
|---------|------------------|-------------|-------|
| entries only |

---

## Removed Features

• bullets (only if applicable)

---

## Migration Notes

• bullets (only if breaking changes exist)

---

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org
```

**Critical Structure Rules:**
- Group by component FIRST (Core, OpenL Studio, Rule Services)
- NO feature essays or multi-paragraph descriptions
- NO deep subsections per feature
- NO per-feature headings within component sections
- All content in bullet format except library table
- Sections appear in this exact order

#### Step 4.2: Writing Guidelines (MANDATORY)

**Universal Rules for ALL Changes:**

Every change entry (improvement, bug fix, feature) MUST:
- Be written as a concise bullet point (1-2 sentences maximum)
- Clearly state what changed
- Mention the affected area/system
- Be direct and technical
- Use active voice and present tense

**Tone Requirements:**
- ✅ Technical, neutral, concise, engineering-focused
- ❌ Marketing, storytelling, user-journey oriented, promotional

**Bullet Writing Pattern:**

Good examples:
```
• Refactored Kafka configuration to eliminate illegal reflection usage.
• Added support for OAuth2 authentication in Rule Services deployment endpoints.
• Fixed issue where table validation failed when empty cells contained formulas.
• Upgraded Spring Framework to version 6.1.2 to address CVE-2024-12345.
```

Bad examples (DO NOT USE):
```
❌ Kafka configuration was redesigned to improve maintainability and align with modern architectural principles, providing developers with a more flexible and extensible foundation for future enhancements.
❌ OAuth2 Support: We're excited to announce comprehensive OAuth2 authentication capabilities that transform how users interact with Rule Services...
❌ Table Validation Enhancement: Our engineering team has significantly improved the table validation system...
```

**Bug Fix Format:**
```
• Fixed issue where <problem> occurred when <condition>.
```

Examples:
```
• Fixed issue where deployment failed when project name contained special characters.
• Fixed issue where rule execution returned incorrect results for date comparisons in leap years.
```

#### Step 4.3: Prohibited Content (NEVER INCLUDE)

The skill MUST NOT generate:
- Feature overviews or introductions
- Benefit descriptions or "why this matters" explanations
- Technical deep dives or implementation details beyond 1-2 sentences
- Scenario explanations or use case descriptions
- Narrative paragraphs
- Marketing language
- Multi-paragraph feature descriptions
- Subsections within component groups

#### Step 4.4: Library Updates (STRICT FORMAT)

**Library updates MUST:**
- Be presented ONLY in table format
- NEVER be described in paragraphs or bullets
- NOT include explanations of why the upgrade happened

**Table Format:**
```markdown
| Library | Previous Version | New Version | Notes |
|---------|------------------|-------------|-------|
| Spring Framework | 6.0.13 | 6.1.2 | Security fixes |
| Apache POI | 5.2.3 | 5.2.5 | |
| React | 17.0.2 | 18.2.0 | Breaking changes - see Migration Notes |
```

#### Step 4.5: Breaking Changes Handling

**Breaking changes should include:**
1. A short bullet in the Improvements section marking it as breaking
2. A concise entry in Migration Notes section
3. NO long explanations

**Example:**

In Improvements section:
```
• **Breaking:** Removed deprecated `getRuleInfo()` method from Rule Services API.
```

In Migration Notes section:
```
### Deprecated API Removal
• Replace `getRuleInfo()` calls with `getMetadata()`. See migration guide: https://docs.openl-tablets.org/migration/6.0
```

#### Step 4.6: CVE and Vulnerability Fixes

**Format:**
```
• **CVE-2024-12345** (Critical): Fixed remote code execution in Apache Tomcat. Updated to 10.1.28.
• **CVE-2024-67890** (High): Addressed XML parsing vulnerability in Apache Commons Text. Updated to 1.11.0.
```

**Requirements:**
- Always list CVE IDs
- Include severity in parentheses
- Specify affected library and new version
- Keep to one concise sentence

---

### Phase 5: Document Generation

#### Step 5.1: Create Markdown File

**Generate the complete Markdown document following the MANDATORY structure from Step 4.1.**

**Key Requirements:**
- Use exact section ordering
- Group all content by component
- Keep all descriptions to 1-2 sentences
- Use bullet format for everything except library table
- No ticket references (EPBDS-XXXX) in main content

**Save as:** `Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md`

---

### Phase 6: Quality Assurance

#### Step 6.1: Validation Checklist

Before finalizing, verify:

**Content Structure:**
- [ ] Follows exact section ordering from template
- [ ] All content grouped by component first
- [ ] No multi-paragraph feature descriptions
- [ ] No feature subsections or deep nesting
- [ ] Library updates only in table format

**Content Style:**
- [ ] All bullets are 1-2 sentences maximum
- [ ] No narrative or explanatory paragraphs
- [ ] No marketing or promotional language
- [ ] Technical and neutral tone throughout
- [ ] No ticket references in main content (EPBDS-XXXX, etc.)

**Technical Accuracy:**
- [ ] All JIRA tickets accounted for
- [ ] Git validation completed
- [ ] CVE IDs formatted correctly (CVE-YYYY-XXXXX)
- [ ] Library versions validated against Git repository
- [ ] Breaking changes have migration notes

#### Step 6.2: Length Check

**If the release notes become too long:**
- Summarize more aggressively
- Combine related minor changes into single bullets
- Ensure no bullet exceeds 2 sentences
- Remove any explanatory content

**Target:**
- Highly scannable output
- Compact structure
- No unnecessary explanation

---

### Phase 7: Completion & Delivery

#### Step 7.1: Present File to User

```bash
# Move file to outputs directory for user access
cp Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md /mnt/user-data/outputs/
```

Use `present_files` tool to make the file available.

#### Step 7.2: Provide Summary

Generate a brief summary:

```
Release Notes Generated Successfully

Summary:
├─ Total Tickets: 156
├─ Improvements: 112
├─ Bug Fixes: 36
├─ Vulnerabilities Fixed: 3 CVEs
└─ Libraries Updated: 24

File: OpenL_Tablets_{version}_Release_Notes.md
Format: Concise changelog style
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

## Example Output

### Good Changelog-Style Example

```markdown
## Improvements

### Core
• Added SpreadsheetResult parameterization support for improved type safety in rule execution.
• Implemented automatic table type inference for DataTable and SimpleRulesTable.
• Enhanced virtual compilation to support parallel module processing.

### OpenL Studio
• Added full-text search across project artifacts with fuzzy matching and search history.
• Implemented OAuth2 authentication support for external identity providers.
• Refactored administration UI using React 18 for improved performance and maintainability.

### Rule Services
• Reduced deployment time by 70% through parallel processing of rule modules.
• Added support for custom error handlers in REST API endpoints.

## Fixed Bugs

### Core
• Fixed issue where rule compilation failed when method names contained Unicode characters.
• Fixed issue where table validation incorrectly flagged valid date ranges as errors.

### OpenL Studio
• Fixed issue where project export failed for projects larger than 100MB.
• Fixed issue where user session expired prematurely during long editing sessions.
```

### Bad Example (DO NOT USE)

```markdown
## Improvements

### OpenL Studio

#### New Administration Interface

**Overview**

The administration panel has been completely redesigned using modern React technology, replacing the legacy JSF/RichFaces implementation. This transformation provides a more intuitive, responsive, and user-friendly experience for system administrators while maintaining full backward compatibility with existing configurations.

**Key Components**

Security Settings: Streamlined interface for managing authentication modes (Multi-user, Active Directory, SAML, OAuth2) with improved validation and real-time configuration testing...

[This is TOO LONG and violates the changelog style requirements]
```

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

**Skill Version**: 3.0.0 (Changelog Style)
**Last Updated**: January 2026
**Compatible With**: Claude Desktop with JIRA MCP Server
**Maintained By**: OpenL Tablets Team

**Support Channels:**
- GitHub Issues: https://github.com/openl-tablets/openl-tablets/issues
- Documentation: https://openl-tablets.org/documentation
- Email: support@openl-tablets.org

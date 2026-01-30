---
name: openl-release-notes
description: Generate professional release notes for OpenL Tablets releases by analyzing JIRA tickets and validating with Git repository changes. Use when the user requests release notes generation for OpenL Tablets, mentions "openl-tablets", "release notes", or specific version numbers like "5.27.8". This skill collects structured data from JIRA, validates completeness against Git commits, and generates comprehensive release notes in Markdown format.
---

# OpenL Tablets Release Notes Generator (Blended Approach)

This skill generates professional release notes for OpenL Tablets releases by combining the structured data from JIRA tickets with Git repository validation to ensure completeness and accuracy.

## Overview

The skill uses a dual-source approach:
1. **Primary Source (JIRA)**: Collects structured ticket data with descriptions, components, and custom fields
2. **Validation Source (Git)**: Analyzes actual code changes to verify all functionality is captured and understand real impact

This ensures release notes are both comprehensive (all tickets included) and accurate (validated against actual changes).

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
- **Identify Feature Significance**: Is this a major feature requiring detailed description?
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

1. **New Features**: New functionality added to the product
   - Identify major vs. minor features
   - Group by component

2. **Improvements**: Enhancements to existing functionality
   - Group by component
   - Separate major enhancements from minor tweaks

3. **Breaking Changes**: Changes requiring user action or migration
   - Critical priority
   - Requires migration instructions

4. **Bug Fixes**: Fixed bugs and issues
   - Group by component
   - Separate critical from minor fixes

5. **Fixed Vulnerabilities**: CVE fixes and security patches
   - List CVE IDs
   - Specify affected libraries and versions

6. **Updated Libraries**: Dependency and library updates
   - List old → new versions
   - Validate versions against pom.xml/package.json from Git

**Component Grouping:**
Within each category, organize by:
- OpenL Studio / Webstudio
- OpenL Studio Admin
- OpenL Rule Services
- OpenL Core
- OpenL Repository
- Security
- DEMO
- Documentation
- Integration / DevOps

---

### Phase 4: Content Generation (Professional Writing)

#### Step 4.1: Identify Feature Types

**Pre-Generation Checklist:**

Classify each item as either **Major** or **Regular**:

**Major Features (Require 2-4 Paragraphs Each):**
- [ ] New Administration UI or UI redesigns
- [ ] Access Control Lists (ACL) or security model changes
- [ ] Framework migrations (Jakarta EE, Spring, etc.)
- [ ] New APIs or significant API changes
- [ ] User/Group management features
- [ ] Major architectural changes
- [ ] New integration capabilities (OAuth, SAML, etc.)
- [ ] Significant performance improvements (>20% gains)
- [ ] New testing or deployment features
- [ ] Statistical functions or major functional additions
- [ ] Database schema changes
- [ ] Breaking changes

**Regular Features (1-2 Sentences Each):**
- [ ] Minor improvements to existing features
- [ ] Bug fixes
- [ ] Library updates
- [ ] Small UI tweaks
- [ ] Documentation updates
- [ ] Configuration enhancements

#### Step 4.2: Writing Guidelines

**For Major Features (New Features & Improvements):**

**CRITICAL: Major features MUST have detailed multi-paragraph descriptions.**

**Required Structure for Major Features:**

1. **Overview (1-2 sentences)**
   - What is this feature and why was it added?
   - What problem does it solve or what capability does it add?

2. **Key Components/Capabilities (2-5 bullets or paragraphs)**
   - Specific functionality provided
   - Technical details and implementation approach
   - Supported formats, configuration options, API changes

3. **Benefits and Use Cases (1-2 sentences)**
   - How will users benefit?
   - Real-world scenarios where this matters

4. **Technical Details (1-2 sentences)**
   - Technologies used (React, Jakarta EE, etc.)
   - Integration points
   - Performance impacts

5. **Examples or Scenarios (optional)**
   - Specific usage examples if helpful
   - Migration path if breaking change

**Quality Standards for Major Features:**
- **Be specific**: Don't say "improved search" - say "Implemented full-text search on Project page with fuzzy matching and filter persistence"
- **Provide context**: Explain what existed before and what's new
- **Include numbers**: "40% faster startup time", "supports up to 1000 concurrent users"
- **Show user impact**: "Reduces deployment time from 10 minutes to 30 seconds"
- **Add technical depth**: "Built using React 18 with WebSocket for real-time updates"
- **Minimum 2 paragraphs**: Single-line bullets are NOT acceptable for major features

**For Regular Features & Bug Fixes:**
- Keep concise (1-2 sentences)
- Focus on what changed and the impact
- Format as bullet points

**For Breaking Changes:**
- **Always include**:
  1. **What Changed**: Clear description of the breaking change
  2. **Impact**: Who is affected and how
  3. **Action Required**: Step-by-step migration instructions
  4. **Example**: Before/after code or configuration samples

**General Style Guidelines:**
- Use professional, concise technical writing
- Use present tense: "Adds support for..." not "Added support for..."
- Focus on user-facing impact, not internal implementation details (unless relevant)
- Avoid internal ticket numbers in descriptions (but list at end if requested)
- Maintain consistency with https://openl-tablets.org/release-notes format
- **Never use commit messages verbatim** - always write original descriptions
- **No ticket references in main text** (EPBDS-XXXX, JIRA IDs) - these go in appendix if needed

#### Step 4.3: Example Formats

**EXCELLENT EXAMPLE - Major Feature:**

```markdown
## New Features

### Redesigned Administration Interface

**Overview**

The administration panel has been completely redesigned using modern React technology, replacing the legacy JSF/RichFaces implementation. This transformation provides a more intuitive, responsive, and user-friendly experience for system administrators while maintaining full backward compatibility with existing configurations.

**Key Components**

- **Security Settings**: Streamlined interface for managing authentication modes (Multi-user, Active Directory, SAML, OAuth2) with improved validation and real-time configuration testing. The new UI provides immediate feedback on configuration errors and includes built-in documentation for each authentication method.

- **User Management**: Enhanced user lifecycle management with batch operations support. Administrators can now create, edit, and delete multiple users simultaneously, with improved validation preventing common configuration errors. The interface displays detailed user activity logs and last login timestamps.

- **Group Management**: Completely redesigned group administration featuring hierarchical group display, member count indicators, and quick access to group permissions. The new design supports drag-and-drop user assignment and includes a visual permission matrix showing role assignments across repositories.

- **Repository Management**: Intuitive configuration interface for Git, AWS S3, and JDBC repositories with integrated connection testing and comprehensive error reporting. The new UI includes one-click repository health checks, automatic credential validation, and visual indicators for repository synchronization status.

- **System Settings**: Centralized management of system-wide properties including date/time formats, auto-compile behavior, thread pool configurations, and cache settings. Changes take effect immediately without requiring application restart for most settings.

**Technical Implementation**

Built using React 18 with TypeScript, the new administration interface leverages modern component architecture and state management patterns. Real-time updates are delivered through WebSocket connections, ensuring administrators see configuration changes immediately across all sessions. The interface follows responsive design principles and is fully compatible with all modern browsers (Chrome, Firefox, Safari, Edge). All administrative operations are backed by RESTful APIs documented in OpenAPI format, enabling automation and integration with external systems.

**Benefits**

The redesigned interface reduces common administrative tasks from multiple clicks to single operations. Configuration validation prevents errors before they're saved, reducing troubleshooting time. The responsive design enables administration from any device, including tablets and mobile phones. Initial user testing shows a 60% reduction in time required for common tasks like user provisioning and repository configuration.
```

**GOOD EXAMPLE - Regular Features:**

```markdown
## Improvements

### OpenL Studio

- **Search Enhancement**: Implemented full-text search across all project artifacts with support for fuzzy matching and search history. Search results now include context snippets showing where terms appear within rules.

- **Table Editor Performance**: Optimized table rendering engine to handle tables with 10,000+ rows without performance degradation. Large tables now load incrementally with virtual scrolling.

### Rule Services

- **Deployment Speed**: Reduced deployment time for large rule projects by 70% through parallel processing of rule modules and optimized dependency resolution.
```

**UNACCEPTABLE EXAMPLE - Major Feature with Insufficient Detail:**

```markdown
## New Features

**OpenL Studio**
- New Administration UI with React
- Enhanced ACL with simplified roles
- User and group management improvements
```

↑ **THIS IS NOT ACCEPTABLE for major features! These require 2-4 paragraph descriptions.**

#### Step 4.4: CVE and Library Updates

**For CVE Fixes:**
- **Always list CVE IDs**: CVE-2024-XXXXX
- **Specify affected library**: "Fixed CVE-2024-12345 in Apache Commons Text 1.10.0"
- **Include version update**: "Updated to Apache Commons Text 1.11.0"
- **Add severity if known**: "(Critical severity)"

**For Library Updates:**
- **Validate versions**: Cross-reference with Git's pom.xml or package.json
- **Format consistently**: "Library Name: old_version → new_version"
- **Group by category**:
  - Frontend libraries
  - Backend dependencies
  - Build tools
  - Testing frameworks
- **Note breaking changes**: If library update requires code changes, mention it

**Example:**
```markdown
## Fixed Vulnerabilities

- **CVE-2024-38816** (Critical): Fixed remote code execution vulnerability in Apache Tomcat. Updated to Tomcat 10.1.28.
- **CVE-2024-12345** (High): Addressed XML parsing vulnerability in Apache Commons Text. Updated to version 1.11.0.

## Updated Libraries

### Frontend Dependencies
- React: 17.0.2 → 18.2.0
- TypeScript: 4.9.5 → 5.3.3
- Webpack: 5.88.0 → 5.89.0

### Backend Dependencies
- Spring Framework: 6.0.13 → 6.1.2
- Hibernate: 6.2.13 → 6.4.1
- Apache POI: 5.2.3 → 5.2.5 (fixes XLSX parsing issues)

### Build Tools
- Maven: 3.9.4 → 3.9.6
```

---

### Phase 5: Document Generation (Markdown Only)

#### Step 5.1: Create Markdown Version

**File Structure:**

```markdown
# OpenL Tablets {version} Release Notes

**Release Date:** {date}
**GitHub Release:** https://github.com/openl-tablets/openl-tablets/releases/tag/v{version}

---

## Table of Contents

1. [New Features](#new-features)
2. [Improvements](#improvements)
3. [Breaking Changes](#breaking-changes) (if applicable)
4. [Bug Fixes](#bug-fixes)
5. [Fixed Vulnerabilities](#fixed-vulnerabilities)
6. [Updated Libraries](#updated-libraries)
7. [Migration Notes](#migration-notes) (if applicable)

---

## New Features

[Content organized by component]

---

## Improvements

[Content organized by component]

---

## Breaking Changes

[Each with Impact, Action Required, Example]

---

## Bug Fixes

[Content organized by component]

---

## Fixed Vulnerabilities

[CVE list with details]

---

## Updated Libraries

[Categorized library updates]

---

## Migration Notes

[If breaking changes exist]

---

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

---

## Appendix: Ticket References (Optional)

[If requested by user, list all JIRA tickets with links]

### New Features
- EPBDS-12345: Feature description
- EPBDS-12346: Feature description

### Improvements
- EPBDS-12347: Improvement description
...
```

**Save as:** `Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md`


---

### Phase 6: Quality Assurance

#### Step 6.1: Validation Checklist

Before finalizing, verify:

**Content Completeness:**
- [ ] All JIRA tickets accounted for
- [ ] Git validation completed with no missing items
- [ ] All major features have 2-4 paragraph descriptions
- [ ] All breaking changes have migration instructions
- [ ] CVE IDs are correct and formatted properly (CVE-YYYY-XXXXX)
- [ ] Library versions validated against Git repository

**Formatting & Style:**
- [ ] No ticket references in main content (EPBDS-XXXX, etc.)
- [ ] Consistent present tense usage
- [ ] No commit messages used verbatim
- [ ] Professional tone maintained throughout
- [ ] Component categorization is accurate
- [ ] All sections populated (or marked as N/A)

**Technical Accuracy:**
- [ ] Library versions match pom.xml/package.json
- [ ] API changes accurately described
- [ ] Performance metrics are specific (not vague "improvements")
- [ ] Breaking changes severity is correct
- [ ] Migration instructions are clear and testable

**Document Quality:**
- [ ] GitHub release tag link is correct
- [ ] Version number is consistent throughout
- [ ] No spelling or grammatical errors
- [ ] Markdown renders correctly

#### Step 6.2: Generate Summary Statistics

Create a summary for the user:

```
Release Notes Generation Complete!

Summary:
├─ Total Tickets Analyzed: 156
├─ Git Commits Validated: 187
├─ Match Rate: 95.2%
│
├─ New Features: 45 (12 major, 33 regular)
├─ Improvements: 67 (8 major, 59 regular)
├─ Breaking Changes: 8
├─ Bug Fixes: 36
├─ Fixed Vulnerabilities: 3 CVEs
└─ Updated Libraries: 24

File Generated:
└─ Markdown: Docs/release-notes/OpenL_Tablets_6.0.0_Release_Notes.md (24 KB)

Validation Notes:
├─ All JIRA tickets validated against Git
├─ 2 commits found without JIRA tickets (documentation updates)
└─ 1 CVE fix added from dependency updates (not in JIRA)
```

---

### Phase 7: Completion & Delivery

#### Step 7.1: Present File to User

```bash
# Move files to outputs directory for user access
cp Docs/release-notes/OpenL_Tablets_{version}_Release_Notes.md /mnt/user-data/outputs/
```

Use `present_files` tool to make the file available.

#### Step 7.2: Provide Next Steps

Inform the user:

**File Ready:**
- Markdown for GitHub release and website

**Recommended Actions:**
1. Review breaking changes section carefully
2. Test migration instructions
3. Verify CVE fixes with security team
4. Create GitHub release with Markdown content
5. Announce release with highlights from "New Features"

**Optional Follow-ups:**
- Generate change log for developers (Git commits only)
- Create migration guide as separate document
- Prepare release announcement email/blog post

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

### Validation Issues

**If Git and JIRA don't match:**
- Report discrepancies clearly
- List commits without tickets
- List tickets without commits
- Ask user how to proceed:
  - Include all items (JIRA + Git extras)
  - JIRA only (ignore Git extras)
  - Manual review required

**If CVE information is incomplete:**
- Search CVE database: https://cve.mitre.org/
- Cross-reference with GitHub Security Advisories
- Flag items needing manual verification

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

3. **Python 3**
   - For any additional scripting
   - Version: 3.8+ recommended

4. **Git**
   - For repository analysis
   - Version: 2.20+ recommended

### Optional Tools

- **pandoc**: For additional format conversions

---

## Installation Guide

### For Business Analysts & Release Managers

1. **Download this skill file**
   - Save as `SKILL.md`

2. **Create skill directory**
   ```bash
   mkdir -p ~/.claude/skills/openl-release-notes/
   ```

3. **Copy skill file**
   ```bash
   cp SKILL.md ~/.claude/skills/openl-release-notes/
   ```

4. **Configure JIRA MCP**
   - Open Claude Desktop settings
   - Add JIRA MCP server
   - Configure with EPBDS project access

5. **Restart Claude Desktop**

### Verify Installation

1. Open Claude Desktop
2. Navigate to OpenL Tablets repository
3. Type: "Generate release notes for version X.X.X"
4. Claude should activate this skill automatically

---

## Usage Examples

### Basic Usage

**Simple Request:**
```
Generate release notes for OpenL Tablets version 6.0.0
```

**Claude will:**
1. Ask for version confirmation (if needed)
2. Collect all tickets from JIRA
3. Validate against Git repository
4. Analyze and categorize changes
5. Generate comprehensive release notes
6. Create Markdown file
7. Present file for download
8. Complete in 3-7 minutes (depending on ticket count)

### Advanced Usage

**Git-Only Mode (if JIRA unavailable):**
```
Generate release notes for version 6.0.0 using only Git repository analysis
```

**Include Ticket Appendix:**
```
Generate release notes for version 6.0.0 and include an appendix with all ticket references
```

### Expected Output

**For version 6.0.0 with 156 tickets:**

**File Generated:**
- `OpenL_Tablets_6.0.0_Release_Notes.md` (20-30 KB, ~600-800 lines)

**Time Required:**
- Ticket collection: 1-2 minutes
- Git analysis: 30-60 seconds
- Content generation: 2-4 minutes
- Document creation: 30-60 seconds
- **Total: 4-8 minutes**

**Content Quality:**
- 12 major features with detailed descriptions (2-4 paragraphs each)
- 33 regular features with concise descriptions
- 8 breaking changes with migration guides
- 67 improvements organized by component
- 36 bug fixes categorized by severity
- 3 CVE fixes with complete details
- 24 library updates with version changes

---

## Best Practices

### For Optimal Results

1. **Run from repository root**: Ensures Git analysis works correctly
2. **Use latest code**: Run `git pull --all` before generating
3. **Verify version format**: Check JIRA to confirm "OpenL X.X.X" vs "X.X.X"
4. **Review major features**: Skill identifies these, but manual review recommended
5. **Validate breaking changes**: Test migration instructions before publishing
6. **Check CVE details**: Verify security fixes with security team
7. **Maintain consistency**: Follow previous release notes format

### Quality Tips

1. **Specificity**: The skill provides specific details, not vague "improvements"
2. **User focus**: Descriptions explain user impact, not just technical changes
3. **Completeness**: Both JIRA and Git ensure nothing is missed
4. **Accuracy**: Git validation catches discrepancies
5. **Professional tone**: Technical but accessible to all audiences

### Common Pitfalls to Avoid

**The skill automatically avoids these issues:**

1. ❌ Listing every commit as separate item
2. ❌ Including internal ticket references in main text
3. ❌ Using vague descriptions like "various improvements"
4. ❌ Missing library updates
5. ❌ Incorrect feature vs. bug fix categorization
6. ❌ Single-line descriptions for major features
7. ❌ Overly technical jargon
8. ❌ Using commit messages verbatim
9. ❌ Forgetting migration instructions for breaking changes
10. ❌ Missing CVE details

---

## Troubleshooting

### Skill Not Activating

**Problem**: Claude doesn't recognize release notes request

**Solution**:
- Use keywords: "generate release notes", "OpenL release notes"
- Mention version number explicitly
- Verify skill file location: `~/.claude/skills/openl-release-notes/SKILL.md`
- Restart Claude Desktop

### JIRA Issues

**Problem**: Can't connect to JIRA

**Solution**:
- Check JIRA MCP configuration in Claude settings
- Verify network connection to JIRA
- Test credentials in JIRA web interface
- Check EPBDS project access permissions

**Problem**: No tickets found

**Solution**:
- Skill tries both version formats automatically
- Verify version exists: use `jira_get_project_versions`
- Check version format in JIRA
- Confirm fixVersion is set on tickets

### Git Issues

**Problem**: Can't auto-detect base version

**Solution**:
- List available tags: `git tag --list "v*"`
- Skill will auto-detect closest previous version in the same release series
- If auto-detection fails, skill will use comparison against main branch

**Problem**: Repository not up to date

**Solution**:
- Run `git fetch --all --tags`
- Pull latest changes: `git pull`

### Document Generation Issues

**Problem**: File permissions error

**Solution**:
- Check write permissions on Docs/release-notes/
- Try alternative output location
- Close document if already open

### Content Issues

**Problem**: Missing major feature details

**Solution**:
- Skill identifies major features automatically
- Review JIRA ticket descriptions
- Check Git commits for additional context
- Manual enhancement may be needed

**Problem**: Validation mismatch

**Solution**:
- Review discrepancy report
- Check if commits are documentation only
- Verify ticket numbers in commit messages
- Decide whether to include Git-only items

---

## Appendix: Technical Reference

### JIRA Query Details

**Standard Query:**
```jql
project = EPBDS
AND fixVersion = "OpenL {version}"
AND "Release Notes" != "No, don't include ticket in Release Notes"
ORDER BY type DESC, priority DESC
```

**Custom Fields:**
- `Release Notes`: Custom text field for pre-written descriptions
- Can be customized per installation

### Git Analysis Patterns

**Files Indicating Functionality:**
- `*/service/*`: Rule Services changes
- `*/webstudio/*`: Webstudio/UI changes
- `*/core/*`: Core engine changes
- `*/repository/*`: Repository implementations
- `*/security/*`: Security changes
- `pom.xml`: Dependency updates
- `package.json`: Frontend dependency updates

**CVE Detection:**
- Search for "CVE-" in commit messages
- Check dependency version updates
- Review security advisories

### Version Format Examples

**JIRA Formats:**
- "OpenL 6.0.0" (standard)
- "6.0.0" (alternate)
- "OpenL Tablets 6.0.0" (rare)

**Git Formats:**
- "v6.0.0" (tag)
- "release/6.0.0" (branch)
- "6.0.0" (alternate tag)

### Document Size Guidelines

**Typical Release (100-200 tickets):**
- Markdown: 15-30 KB (~400-700 lines)

**Major Release (200+ tickets):**
- Markdown: 30-50 KB (~700-1200 lines)

**Minor Release (<50 tickets):**
- Markdown: 10-18 KB (~200-400 lines)

---

## Version History

**Version 2.0.0** (Current)
- Blended JIRA + Git approach
- Markdown-only output (Markdown)
- Automated validation and quality checks
- Enhanced major feature detection
- Comprehensive error handling

**Based on:**
- JIRA-focused approach v1.0.0
- Git-focused approach v1.0.0

---

## License & Support

**Skill Version**: 2.0.0 (Blended)
**Last Updated**: January 2026
**Compatible With**: Claude Desktop with JIRA MCP Server
**Maintained By**: OpenL Tablets Team

**Support Channels:**
- GitHub Issues: https://github.com/openl-tablets/openl-tablets/issues
- Documentation: https://openl-tablets.org/documentation
- Email: support@openl-tablets.org

---

## Credits

This skill combines best practices from:
- JIRA-based release notes generation
- Git commit analysis techniques
- Professional technical writing standards
- OpenL Tablets release documentation guidelines

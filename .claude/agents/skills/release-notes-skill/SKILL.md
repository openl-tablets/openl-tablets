---
name: openl-release-notes
description: Generate OpenL Tablets release notes by collecting Jira tickets and producing markdown documentation matching the official OpenL website style.
---

# OpenL Release Notes Generator Skill

## Template Reference
**Template file:** `openl-tablets/.claude/agents/skills/release-notes-skill/templates/release-notes-template.md`

Always consult this template for exact structure, section ordering, and formatting. The template defines which sections to include and how to format each one.

## Overview
Generate release notes for OpenL Tablets versions. Output matches the official OpenL website style (https://openl-tablets.org/release-notes).

## Core Rules
1. **NO Jira ticket numbers** in output
2. **Follow the template structure exactly** - only include sections with content
3. **Keep descriptions concise** - one sentence per item unless truly complex
4. **Bullet-first approach** - prefer bullet points over paragraphs
5. **Output format:** Markdown (.md)

## Workflow

### Step 1: Get Version
Ask for version if not provided (format: X.Y.Z).

### Step 2: Collect Jira Tickets

**Use Jira MCP tools** to search for tickets:

```
Tool: jira:jira_search
JQL: fixVersion = "[VERSION]" AND "Exclude from Release Notes" != true
Fields: summary, description, issuetype, priority, labels
```

**Important:** 
- Only use `jira:jira_search` MCP tool to retrieve tickets
- Only include tickets where `"Exclude from Release Notes" != true`
- Process all pages if results exceed limit

### Step 3: Categorize & Write

**Refer to template for section structure.** Use these writing guidelines:

#### Overview (1-2 paragraphs max)
- Summarize the release highlights
- Keep it brief and scannable

#### New Features
- **Title only** - no sub-headers within features
- **1-2 sentences** describing what it does and why it matters
- Bullets for listing capabilities (if needed)
- Image reference if applicable

**Example:**
```markdown
Enhanced Rule Validation Engine

The new validation engine automatically detects rule conflicts and dependency issues before deployment. This reduces production errors and speeds up the development cycle.

  * Real-time conflict detection
  * Dependency graph visualization
  * One-click impact analysis

![Image](images/rule-validation.png)
```

#### Improvements
- Group by area (e.g., "Rules Editor:", "WebStudio:")
- **One line per improvement** - action verb + what changed
- No explanatory paragraphs unless absolutely necessary

**Example:**
```markdown
Rules Editor:

  * Improved autocomplete performance for large rule sets
  * Added syntax highlighting for custom data types
  * Enhanced find-and-replace with regex support
```

#### Fixed Bugs
- Group by area
- **One line per fix** - describe what now works correctly
- User-facing language, not technical

**Example:**
```markdown
Data Tables:

  * Fixed cell copying behavior between tables
  * Resolved column header display issue after resizing
```

#### Updated Libraries
Follow template format exactly (see template for structure options).

#### Known Issues / Migration Notes
Only include if applicable. Keep brief.

### Step 4: Generate Folder Structure & Files

**Create the following structure:**

```
Docs/
└── release-notes/
    └── [VERSION]/
        ├── index.md          <- Main release notes file
        └── images/
            └── README.md     <- Image upload instructions
```

**Folder creation order:**
1. `Docs/release-notes/` (if doesn't exist)
2. `Docs/release-notes/[VERSION]/`
3. `Docs/release-notes/[VERSION]/images/`

**Files to generate:**

1. **Main release notes:** `Docs/release-notes/[VERSION]/index.md`
   - Contains full release notes following template structure

2. **Images README:** `Docs/release-notes/[VERSION]/images/README.md`
   ```markdown
   # Images for OpenL Tablets [VERSION] Release Notes
   
   Upload screenshots and diagrams referenced in the release notes.
   
   ## Naming Convention
   - Use lowercase with hyphens: `feature-name.png`
   - Prefer PNG format
   
   ## Images Needed
   [List image placeholders from release notes]
   ```

### Step 5: Deliver

Provide:
- Generated `index.md` file
- Generated `images/README.md` file
- Brief summary: count of features, improvements, fixes
- List of image placeholders needing screenshots

**Delivery message format:**
```
Release notes generated for OpenL Tablets [VERSION]

📁 Files created:
- Docs/release-notes/[VERSION]/index.md
- Docs/release-notes/[VERSION]/images/README.md

📊 Summary:
- X New Features
- X Improvements  
- X Bug Fixes

🖼️ Images needed:
[List image placeholders]
```

## Writing Style

**Do:**
- Start bullets with action verbs (Added, Improved, Fixed, Enhanced)
- Be specific but brief
- Focus on user benefit

**Don't:**
- Write multi-paragraph descriptions for routine items
- Include implementation details
- Use vague language ("various improvements")
- Add Jira ticket numbers

## Quick Reference: Item Length

| Item Type | Length |
|-----------|--------|
| Overview | 1-2 paragraphs |
| New Feature | 1-2 sentences + optional bullets |
| Improvement | 1 line |
| Bug Fix | 1 line |
| Library Update | 1 line |

## Validation & Updates

This skill can also be used to validate and update existing release notes.

### When to Use
- User wants to edit/modify generated release notes
- User asks to review or validate a release notes file
- User wants to check structure, style, or formatting
- User submits a draft for review

### Validation Checklist

**Structure (compare against template):**
- [ ] Starts with `## Release Notes` header
- [ ] Version tag link is present and correctly formatted
- [ ] Overview is 1-2 paragraphs max
- [ ] Only sections with content are included
- [ ] Sections appear in correct order per template
- [ ] Proper heading levels (`###` for sections, no `####` within)

**Formatting:**
- [ ] Bullet indentation is consistent (2 spaces before `*`)
- [ ] Colons after area groupings (e.g., "Rules Editor:")
- [ ] Image syntax: `![Image](images/filename.png)`
- [ ] No excessive blank lines

**Style:**
- [ ] No Jira ticket numbers anywhere
- [ ] Bullets start with action verbs
- [ ] One line per improvement/fix (not paragraphs)
- [ ] Features are 1-2 sentences + optional bullets
- [ ] User-focused language (benefits, not implementation)
- [ ] No vague phrases ("various improvements", "minor fixes")

**Content:**
- [ ] All items are meaningful (no filler)
- [ ] Grouped logically by area
- [ ] No duplicate items
- [ ] Consistent terminology

### Update Workflow

1. **Read existing file** from `Docs/release-notes/[VERSION]/index.md`

2. **Compare against template** structure

3. **Apply requested changes** while:
   - Preserving correct existing content
   - Maintaining consistent style
   - Keeping proper formatting
   - Following template structure

4. **Validate result** using checklist above

5. **Report changes made** and any issues found

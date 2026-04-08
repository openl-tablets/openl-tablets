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
6. **Use `---` horizontal rules** between individual items within each section (between features, between improvement areas, between breaking changes, between migration topics) — **omit the trailing `---` after the last item in each section**
7. **Bold section headings** - use `## **Section Name**` for top-level sections, `### **Item Title**` for items within sections

## Writing Style

Write each item as a unified description — do not split features by audience or add sub-sections labelled "For Developers" or "For Administrators". All readers (rule authors, admins, and developers) read the same release notes. Describe the feature holistically: what it is, what users and integrators can do with it, and why it matters. Include technical details (endpoint names, configuration properties, supported formats) naturally in the prose when they add value, not hidden in a sub-section.

**The single governing principle:** describe what changed and what it enables — not how it was implemented.

**Do:**
- Start bullets with action verbs (Added, Improved, Fixed, Enhanced)
- Be specific but brief
- Describe the feature holistically — UI capability, API surface, and configuration in one place
- Include technical details (endpoint paths, property names) inline when they are user-actionable
- For bug fixes: describe what the user experienced and what now works correctly

**Don't:**
- Split a single feature into BA-facing and developer-facing sub-sections
- Lead with implementation details — exception class names, internal component names, database identifiers
- Use vague language ("various improvements", "minor fixes")
- Add Jira ticket numbers
- Create a subsection for a single improvement bullet

## Workflow

### Step 1: Get Version
Ask for version if not provided (format: X.Y.Z).

**Version tag and GitHub URL format:**
- For 5.x releases: version tag `openl-tablets-X.Y.Z`, GitHub URL `https://github.com/openl-tablets/openl-tablets/releases/tag/openl-tablets-X.Y.Z`
- For 6.x and later releases: version tag `vX.Y.Z`, GitHub URL `https://github.com/openl-tablets/openl-tablets/releases/tag/X.Y.Z`

These are used to fill the `[{{version_tag}}]({{github_tag_url}}) on the GitHub` line in the template.

### Step 2: Collect Jira Tickets

**Use Jira MCP tools** to search for tickets:

```
Tool: jira:jira_search
JQL: project = EPBDS AND fixVersion = "[VERSION]" AND "Exclude from Release Notes" != true AND status in (Closed, Resolved, "In Testing")
Fields: summary, description, issuetype, priority, labels
MaxResults: 100
StartAt: 0
```

**Important:**
- Only use `jira:jira_search` MCP tool to retrieve tickets
- Only include tickets where `"Exclude from Release Notes" != true`
- Paginate using `startAt` increments of 100 until the returned list is smaller than `maxResults`

### Step 3: Categorize & Write

**Refer to template for section structure.** Use these writing guidelines:

#### Overview (1-2 paragraphs max)
- Summarize the release highlights
- Keep it brief and scannable

#### New Features
- Each feature gets `### **Title**` heading
- **1-3 sentences** describing what it is, what users and integrators can do with it, and why it matters — all in one unified description
- Include technical details (API endpoint, config property, supported formats) inline when they are actionable, not in a separate sub-section
- For complex features with multiple logical sub-areas, use `####` sub-headings to improve navigability
- Bullets for listing capabilities (if needed) — minimum 2 bullets if using a bullet list; don't create a list for a single item
- Image reference if applicable
- **Separate each feature with `---`**

**Example:**
```markdown
### **Table Formula Smart Edit**

Users can now describe a formula change in plain language — for example, "multiply the base rate by 1.15 for platinum tier" — and OpenL Studio proposes a valid OpenL expression to review and apply. This works with decision tables, spreadsheets, and lookup tables. The feature requires `openl.ai.enabled=true` to be set in the configuration.

  * Proposed change is shown as a preview before applying
  * Original formula is preserved until the user confirms

![Table Formula Smart Edit](images/formula-smart-edit.png)
```

#### Improvements
- Group by area using `### **Area Name**` headings
- **One line per improvement** - action verb + what changed
- No explanatory paragraphs unless absolutely necessary
- Do not create a subsection for a single improvement item — fold it into the nearest logical group instead
- **Separate each area group with `---`**

**Example:**
```markdown
### **Rules Editor**

  * Improved autocomplete performance for large rule sets
  * Added syntax highlighting for custom data types
  * Enhanced find-and-replace with regex support

---

### **Security Hardening**

  * Removed VBScript execution under Windows
  * Password fields no longer returned by any API
```

#### Breaking Changes
- Each breaking change gets `### **Title**` heading
- Describe what changed and the impact in plain language
- Include `#### **Migration Steps**` sub-section when applicable
- **Separate each breaking change with `---`**

#### Bug Fixes
- Flat bullet list (no area grouping needed)
- **One line per fix** - describe what now works correctly
- User-facing language, not technical — avoid exception class names and internal component references unless unavoidable

**Example:**
```markdown
  * Fixed GitLFS issues with BitBucket repositories
  * Fixed tracing and dependency graph issues
  * Fixed flat folder structure display issue
```

#### Known Issues
- Flat bullet list (no area grouping needed)
- **One line per issue** — describe what is broken and any workaround if known
- Only include if there are open, unresolved issues shipping with this version

**Example:**
```markdown
  * When executing test tables in OpenL Studio, a `ClassCastException` is encountered when the `SpreadsheetResult` cell type combines array and scalar values.
```

#### Security & Library Updates
- Use **FORMAT A** (the default): `## **Security & Library Updates**` with `### **Security Vulnerability Fixes**` and `### **Major Library Upgrades**` subsections, each further split by `#### **Runtime Dependencies**`, `#### **Test Dependencies**`, `#### **Removed Dependencies**` as applicable.
- Use **FORMAT B** (flat bullet list, no headings) only for older or simpler releases where no structured breakdown was provided. Do not mix both formats.
- Omit subsections that have no entries (e.g., omit `#### **Removed Dependencies**` if nothing was removed).

#### Migration Notes
- Each topic gets `### **Topic Title**` heading
- **Separate each topic with `---`**
- Keep brief, actionable guidance
- Only include if applicable.

### Step 4: Section Completeness Check

Before generating files, explicitly confirm the presence or intentional absence of each optional section:

| Section | Status | Note |
|---|---|---|
| New Features | present / absent | |
| Improvements | present / absent | |
| Breaking Changes | **present / confirmed absent** | State why if absent |
| Bug Fixes | present / absent | |
| Security & Library Updates | **present / confirmed absent** | If absent, confirm no library tickets were in fix version |
| Known Issues | present / absent | |
| Migration Notes | **present / confirmed absent** | State why if absent |

Sections marked **confirmed absent** require an explicit statement, not silent omission. Include this table in the delivery summary.

### Step 5: Generate Folder Structure & Files

**Create the following structure:**

```
Docs/
└── release-notes/
    └── [VERSION]/
        ├── index.md          <- Main release notes file
        └── images/
            └── .DELETE_ME     <- Placeholder so Git tracks the empty folder; delete once real images are added
```

**Folder creation order:**
1. `Docs/release-notes/` (if doesn't exist)
2. `Docs/release-notes/[VERSION]/`
3. `Docs/release-notes/[VERSION]/images/`

**Files to generate:**

1. **Main release notes:** `Docs/release-notes/[VERSION]/index.md`
   - Contains full release notes following template structure

2. **Images placeholder:** `Docs/release-notes/[VERSION]/images/.DELETE_ME`
   - Empty file whose only purpose is to make Git track the `images/` directory
   - Delete this file once actual screenshot images are added

### Step 6: Deliver

Provide:
- Generated `index.md` file
- Generated `images/.DELETE_ME` placeholder
- Section completeness table (from Step 4)
- Brief summary: count of features, improvements, fixes
- List of image placeholders needing screenshots

**Delivery message format:**
```
Release notes generated for OpenL Tablets [VERSION]

Files created:
- Docs/release-notes/[VERSION]/index.md
- Docs/release-notes/[VERSION]/images/.DELETE_ME

Section completeness:
[paste Step 4 table here]

Summary:
- X New Features
- X Improvements
- X Bug Fixes

Images needed:
[List image placeholders, or "None" if no images referenced]
```

## Quick Reference: Item Length

| Item Type | Length |
|-----------|--------|
| Overview | 1-2 paragraphs |
| New Feature | 1-3 sentences + optional bullets (unified, no audience sub-sections) |
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
- [ ] Version tag link is present and correctly formatted (vX.Y.Z for 6.x+, openl-tablets-X.Y.Z for 5.x)
- [ ] Overview is 1-2 paragraphs max
- [ ] Only sections with content are included
- [ ] Sections appear in correct order per template: New Features → Improvements → Breaking Changes → Bug Fixes → Security & Library Updates → Known Issues → Migration Notes
- [ ] Known Issues section is included when there are open unresolved issues, omitted otherwise
- [ ] Proper heading levels: `## **Section**` for top-level, `### **Item**` for items, `#### **Sub**` for sub-sections
- [ ] `---` horizontal rules between individual items within each section

**Formatting:**
- [ ] Bullet indentation is consistent (2 spaces before `*`)
- [ ] Section and item headings are bold (e.g., `## **New Features**`, `### **Feature Title**`)
- [ ] Image syntax: `![Image](images/filename.png)`
- [ ] No excessive blank lines
- [ ] No single-item improvement subsections (lone items folded into a broader group)

**Style:**
- [ ] No Jira ticket numbers anywhere
- [ ] Bullets start with action verbs
- [ ] One line per improvement/fix (not paragraphs)
- [ ] Features are 1-2 sentences + optional bullets
- [ ] No vague phrases ("various improvements", "minor fixes")

**Content:**
- [ ] All items are meaningful (no filler)
- [ ] Grouped logically by area
- [ ] No duplicate items
- [ ] Consistent terminology
- [ ] Section completeness table present in delivery summary

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

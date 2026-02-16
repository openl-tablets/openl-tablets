# OpenL Release Notes Generator Skill

## Overview
This skill helps users generate and manage OpenL Tablets release notes in a consistent, professional format similar to the official OpenL website (https://openl-tablets.org/release-notes). The skill automates the collection of Jira tickets, categorizes features, and produces well-formatted markdown documentation.

## When to Use This Skill
- User requests generation of release notes for a specific OpenL version
- User wants to update or modify existing release notes
- User needs to create documentation for a new release
- User mentions "release notes", "version documentation", or "changelog"
- User wants to apply updates while maintaining consistency

## Core Requirements
1. **NO Jira ticket numbers** anywhere in the output
2. **User-focused language** - emphasize benefits over technical details
3. **Proper categorization** - Major/Medium/Small features based on effort and impact
4. **Professional formatting** - consistent with official OpenL style
5. **Markdown output** - generate files in .md format
6. **GitHub structure** - create proper folder hierarchy

## Workflow

### Step 1: Gather Release Information

**If version not provided, ask:**
"I'll help you generate release notes for OpenL Tablets. What version number should I create the release notes for? (Format: X.Y.Z, e.g., 5.27.0)"

**Validate format:**
- Must be X.Y.Z format
- Examples: 5.27.0, 6.0.0, 5.26.5

**Confirm details:**
- GitHub repository: openl-tablets/openl-tablets
- Target folder: Docs/release-notes/[VERSION]/
- Images folder: Docs/release-notes/[VERSION]/images/

### Step 2: Collect Jira Tickets

**Execute Jira search:**
```
Use jira:jira_search with JQL:
fixVersion = "[VERSION]" AND "Exclude from Release Notes" != true

Fields to retrieve:
- summary
- description  
- issuetype
- priority
- status
- labels
- created
- updated
```

**Handle results:**
- Process all tickets (may need multiple pages)
- Filter out any with "Exclude from Release Notes" flag
- Gather full details for analysis

### Step 3: Categorize Features

**Analyze each ticket and categorize:**

#### Major Features (Detailed 3-5 paragraph sections)
**Criteria:**
- Changes how users work OR unlocks entirely new capabilities
- Requires significant business context to understand value
- Impacts multiple parts of the system or workflows
- Takes considerable development effort (min 4 sprints)
- Often Epic issue type or has multiple linked stories

**Red flags for mis-categorization:**
- If you can explain it in one sentence → probably not Major
- If it's just "faster" or "better" without new capability → not Major
- If it's only used in edge cases → not Major

#### Medium-Sized Features (1-2 paragraph sections)
**Criteria:**
- Enhances existing capabilities in meaningful ways
- Needs some business context but purpose is fairly clear
- Improves user efficiency or solves known pain points
- Takes moderate effort (1-2 sprints)

#### Small Features (Bullet point list)
**Criteria:**
- Incremental improvements that are self-explanatory
- Needs minimal business context - value is obvious
- Fixes edge cases or polishes existing features
- Quick to implement (days, not weeks)

**Examples:**
- Added keyboard shortcut
- Improved error message
- Added tooltip
- Enhanced search to include X
- Supported new format

### Step 4: Write Content

#### For Major Features:
**Structure each as:**

1. **Problem Statement** (1 paragraph)
   - What problem does this solve?
   - What was the pain point before?
   - Why did users need this?

2. **Solution Overview** (1-2 paragraphs)
   - How does this feature work?
   - What are the key capabilities?
   - How does it change workflows?

3. **Business Value** (1 paragraph)
   - What benefits does this provide?
   - Who will find this most valuable?
   - What use cases does it enable?

4. **Key Capabilities** (bulleted list with bold headers)
   - **Capability 1**: description
   - **Capability 2**: description

5. **Usage Context** (1 paragraph)
   - When to use this
   - Real-world scenarios
   - Impact on daily work

**Include image placeholder:**
```markdown
![Feature Name](images/feature-name.png)
```

**Example:**
```markdown
#### Advanced Rule Validation Engine

Managing complex rule dependencies has traditionally been one of the most challenging aspects of large-scale business rule applications. As rule sets grow, understanding the relationships between rules, identifying potential conflicts, and ensuring consistency becomes increasingly difficult. Teams often spent hours manually tracing dependencies and debugging issues that arose from unexpected rule interactions.

OpenL Tablets 5.27.0 introduces the Dependency Visualization Engine, a comprehensive tool that automatically maps relationships between rules and provides interactive visual representations of your rule architecture. The engine analyzes your entire rule set, identifies all connections—from direct references to implicit dependencies—and presents them in an intuitive, explorable interface.

**Key capabilities:**
- **Interactive dependency graphs** showing how rules relate to each other
- **Impact analysis** that predicts which rules will be affected by changes
- **Conflict detection** that identifies potential logical contradictions
- **Performance insights** highlighting dependency chains that may impact execution speed

This feature is particularly valuable for teams working with large rule repositories, conducting impact assessments before making changes, or onboarding new developers who need to understand complex rule structures. By visualizing dependencies, teams can make informed decisions, reduce errors, and maintain better control over their business logic.

![Dependency Visualization Dashboard](images/dependency-visualization.png)
```

#### For Medium Features:
**Structure as 1-2 paragraphs:**

1. **What's Better** (Paragraph 1)
   - What existing capability is improved?
   - What specific enhancement was made?
   - What was the limitation before?

2. **User Benefit** (Paragraph 2)
   - How does this make things easier/faster/better?
   - Who benefits most?
   - What can users do now that was harder before?

**Example:**
```markdown
#### Enhanced Excel Import Performance

The Excel import functionality has been completely re-architected to handle large spreadsheets more efficiently. Import times for files over 10MB are now up to 60% faster, and memory usage has been reduced by 40% through improved streaming and parsing algorithms.

This enhancement directly addresses feedback from users working with extensive data tables and complex Excel-based rule definitions. Development teams will notice significantly reduced wait times when importing or refreshing data, making iterative development more fluid.
```

#### For Small Features:
**One clear sentence each:**
- Start with action verb (Added, Improved, Enhanced)
- State what capability was added or enhanced
- Include the benefit if not obvious
- No technical implementation details

**Examples:**
```markdown
- Added support for custom date formats in data tables, allowing regional formats without manual conversion
- Improved error messages in the Rules Editor with clearer explanations and suggested fixes
- Enhanced search functionality in WebStudio to include rule descriptions and comments
- Added keyboard shortcuts for common operations (Ctrl+S to save, Ctrl+F to find)
```

#### For Bug Fixes:
**Describe from user perspective:**
- What issue was resolved
- What now works correctly
- Group by functional area when possible

**Examples:**
```markdown
### Rules Editor
- Fixed an issue where syntax highlighting would stop working after editing very long rule expressions
- Resolved a problem where the editor would occasionally lose focus when auto-save triggered

### Data Tables
- Corrected behavior when copying cells with formulas between tables
- Fixed an issue where column headers would disappear after certain resize operations
```

### Step 5: Generate Release Notes Structure

**Use this exact structure:**
```markdown
# OpenL Tablets [VERSION] Release Notes

## Overview

[2-3 paragraphs covering:
- What makes this release significant
- Key themes or focus areas
- Overall direction
- Why these changes matter to users]

## What's New

### Major Features

#### [Feature Name 1]
[3-5 paragraphs with detailed description]
![Feature Name](images/feature-name.png)

#### [Feature Name 2]
[3-5 paragraphs with detailed description]

### Enhancements

#### [Enhancement Name 1]
[1-2 paragraphs]

#### [Enhancement Name 2]
[1-2 paragraphs]

### Additional Improvements

- [Small enhancement 1]
- [Small enhancement 2]
- [Small enhancement 3]
[Continue list...]

## Bug Fixes

### [Functional Area 1]
- [Bug fix 1]
- [Bug fix 2]

### [Functional Area 2]
- [Bug fix 3]
- [Bug fix 4]

### General
- [Other bug fixes]

## Technical Updates

- [Infrastructure improvement 1]
- [Dependency update 1]
- [Performance optimization 1]

## Breaking Changes

[Only include if there are breaking changes]

### [Breaking Change Title]

**What Changed:** [Description]

**Impact:** [Who is affected]

**Migration:** [How to adapt]

## Deprecations

[Only include if there are deprecations]

### [Deprecated Feature]

**Status:** Deprecated in [VERSION], will be removed in [FUTURE VERSION]

**Alternative:** [What to use instead]

**Reason:** [Why being deprecated]

## Known Issues

[Only include if there are significant known issues]

### [Issue Title]

**Description:** [What doesn't work]

**Workaround:** [How to work around it]

**Status:** [Expected resolution]

## Installation & Upgrade

### New Installations

For new installations, download OpenL Tablets [VERSION] from our [downloads page](https://openl-tablets.org/downloads) and follow the [installation guide](https://openl-tablets.org/documentation/installation).

### Upgrading from Previous Versions

**From [PREVIOUS VERSION]:**
- Standard upgrade process applies
- Review Breaking Changes section above if applicable
- Backup your existing installation before upgrading
- Follow the [upgrade guide](https://openl-tablets.org/documentation/upgrade)

### System Requirements

- Java 11 or higher
- Minimum 4GB RAM (8GB recommended for production)
- [Other requirements]

## Resources

- **Documentation:** [https://openl-tablets.org/documentation](https://openl-tablets.org/documentation)
- **Download:** [https://openl-tablets.org/downloads](https://openl-tablets.org/downloads)
- **Release Notes Archive:** [https://openl-tablets.org/release-notes](https://openl-tablets.org/release-notes)
- **Community Forum:** [https://openl-tablets.org/community](https://openl-tablets.org/community)
- **Support:** [support contact or link]
```

### Step 6: Create GitHub Folder Structure

**Execute in order:**

1. Create release-notes folder (if doesn't exist):
```
   Docs/release-notes/
```

2. Create version folder:
```
   Docs/release-notes/[VERSION]/
```

3. Create images folder:
```
   Docs/release-notes/[VERSION]/images/
```

4. Create images README:
```
   Docs/release-notes/[VERSION]/images/README.md
```
   
   Content:
```markdown
   # Images for OpenL Tablets [VERSION] Release Notes
   
   Upload screenshots and diagrams to this folder to reference in the release notes.
   
   ## Naming Convention
   - Use descriptive names: `feature-name.png` not `screenshot1.png`
   - Use lowercase with hyphens
   - Prefer PNG format for screenshots
   - Keep file sizes reasonable (compress if needed)
   
   ## Referenced Images
   
   The following images are referenced in the release notes and need to be uploaded:
   
   [List will be generated based on image references in the release notes]
```

5. Create main release notes file:
```
   Docs/release-notes/[VERSION]/index.md
```

### Step 7: Writing Style Guidelines

**MUST follow:**
- **Active voice**: "You can now..." not "The system now allows..."
- **User-focused**: Benefits over technical details
- **Clear and concise**: No unnecessary jargon
- **Professional**: Business-friendly language
- **Consistent**: Same terminology throughout

**MUST avoid:**
- Jira ticket numbers (NEVER include these)
- Overly technical implementation details
- Copy-pasting Jira summaries verbatim
- Acronyms without explanation on first use
- Vague descriptions like "improved performance" without specifics

**Formatting standards:**
- Use `##` for main sections
- Use `###` for subsections  
- Use `####` for individual features
- Use `-` for bullet points
- Use `**bold**` for feature names and key terms
- Use `code` for UI elements, commands, technical terms
- Include descriptive alt text for images

### Step 8: Quality Checks

**Before delivering, verify:**
- [ ] Version number is correct throughout
- [ ] All Jira tickets are accounted for
- [ ] Features are properly categorized
- [ ] NO ticket numbers appear anywhere
- [ ] Language is clear and business-focused
- [ ] Structure matches template exactly
- [ ] All links are valid
- [ ] Image placeholders are properly formatted
- [ ] Spelling and grammar are correct
- [ ] Formatting is consistent
- [ ] Overview is compelling
- [ ] Each major feature has 3-5 paragraphs
- [ ] Each medium feature has 1-2 paragraphs
- [ ] Small features are one-liners
- [ ] Bug fixes are grouped by area

### Step 9: Deliver to User

**Provide:**
1. **Generated release notes file** (index.md)
2. **Images README file**
3. **Summary of what was generated:**
   - Number of major features
   - Number of medium features
   - Number of small improvements
   - Number of bug fixes
   - List of image placeholders that need images

**Instructions to user:**
```
Release notes generated for OpenL Tablets [VERSION]

📁 Files created:
- Docs/release-notes/[VERSION]/index.md
- Docs/release-notes/[VERSION]/images/README.md

📊 Summary:
- X Major Features (detailed descriptions)
- X Enhancements (medium features)
- X Additional Improvements
- X Bug Fixes

🖼️ Images needed:
[List each image placeholder that needs to be uploaded]

📝 Next steps:
1. Review the generated release notes for accuracy
2. Add screenshots/diagrams to the images/ folder
3. Update any placeholder content
4. Have stakeholders review
5. Publish to the website
```

## Handling Updates

**When user requests updates to existing release notes:**

1. **Locate the file:**
   - Search for Docs/release-notes/[VERSION]/index.md
   - Read current content

2. **Apply changes while maintaining:**
   - Consistent style
   - Proper formatting
   - Structure integrity
   - Professional tone
   - All quality standards

3. **Preserve:**
   - Image references
   - Links
   - Structure
   - Formatting

4. **Update only what's needed:**
   - Don't rewrite everything
   - Keep user's approved content
   - Match existing tone

## Common Pitfalls to Avoid

1. **Including Jira IDs**: NEVER write "OPENL-1234" or reference tickets
2. **Too technical**: Focus on business value, not code changes
3. **Inconsistent categorization**: Be strict with Major/Medium/Small criteria
4. **Poor descriptions**: Rewrite Jira summaries, don't copy verbatim
5. **Missing context**: Major features need full story, not just feature list
6. **Weak overview**: Must compellingly summarize the release
7. **Broken structure**: Follow template structure exactly
8. **Forgetting images**: Include placeholder for every major feature
9. **Vague benefits**: "Better performance" → "60% faster imports for files over 10MB"
10. **Wrong categorization**: "Added button" is not a Major Feature

## Template Integration

**Always use the template file** (release-notes-template.md) as reference:
- It contains detailed examples
- It shows proper formatting
- It includes writing guidelines
- It has quality checklists

**The template should be consulted for:**
- Section structure
- Writing examples
- Categorization criteria
- Formatting standards
- Common patterns

## Integration with Jira

**Jira Tools to Use:**
```
jira:jira_search - Find all tickets for version
jira:jira_get_issue - Get detailed ticket info
jira:jira_search_fields - Find custom field names
```

**JQL Query Pattern:**
```
fixVersion = "[VERSION]" AND "Exclude from Release Notes" != true
```

**Handle Jira Data:**
- Read summary and description
- Understand the context
- Rewrite in user-friendly language
- Never copy verbatim
- Never include ticket numbers

## Success Criteria

**Release notes are successful when:**
- Structure matches official OpenL style perfectly
- Content is clear and valuable to business users
- No Jira ticket numbers anywhere
- Features are properly categorized (Major/Medium/Small)
- Writing is professional and concise
- All sections are complete
- Formatting is consistent throughout
- Images folder is ready with README
- User can understand value without technical background
- Major features tell compelling stories
- Overview captures the essence of the release

## Final Checklist

**Before presenting to user, confirm:**
- [ ] Version number correct everywhere
- [ ] JQL search completed successfully
- [ ] All tickets analyzed and categorized
- [ ] NO ticket numbers in output
- [ ] Major features have 3-5 paragraphs each
- [ ] Medium features have 1-2 paragraphs each
- [ ] Small features are one-line bullets
- [ ] Bug fixes are grouped and clear
- [ ] Overview is compelling (2-3 paragraphs)
- [ ] All sections present
- [ ] Formatting is consistent
- [ ] Links are valid
- [ ] Image placeholders included
- [ ] Writing is professional
- [ ] Language is user-focused
- [ ] Structure matches template
- [ ] Quality checks passed
- [ ] README created in images folder
- [ ] GitHub structure is correct

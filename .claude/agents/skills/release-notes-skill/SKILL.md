# OpenL Tablets Release Notes Generator

## Overview

This skill automatically generates professional release notes for OpenL Tablets releases by analyzing Jira tickets and producing well-structured technical documentation in the established OpenL style.

## When to Use This Skill

Use this skill when:
- User requests release notes generation for OpenL Tablets
- User mentions "generate release notes" or "create release notes"
- User specifies a version number for release notes
- User asks to document features for a release

## Workflow

### Step 1: Collect Release Version

If the user hasn't provided a release version:
```
Ask the user: "What release version should I generate the notes for? (e.g., 5.27.8, 5.28.0)"
```

### Step 2: Determine Release Type

Based on the version number:
- **Major release**: X.Y.0 (e.g., 5.27.0, 5.28.0)
- **Non-major release**: X.Y.Z where Z > 0 (e.g., 5.27.8, 5.27.15)

### Step 3: Fetch Jira Tickets

Use the Jira MCP tools to collect all tickets for the specified release:

```javascript
// Search for tickets with the specific Fix Version
jira_search({
  jql: `fixVersion = "${version}" ORDER BY priority DESC, created ASC`,
  fields: "summary,description,issuetype,priority,status,labels,components,comment",
  limit: 100
})
```

If there are more than 100 tickets, use pagination to fetch all of them.

### Step 4: Analyze and Categorize Features

For each ticket, analyze the **actual functionality** described in:
1. The summary
2. The description
3. Comments (to understand what was actually implemented)

**DO NOT** rely solely on ticket titles or issue types. Read the content to understand the real impact.

#### Feature Size Classification

**Major Features** (for X.Y.0 releases only):
- Changes how users work or unlocks entirely new capabilities
- Requires significant business context to understand value
- Impacts multiple parts of the system or workflows
- Took considerable development effort (min 4 sprints)
- Examples: ACL implementation, new merge conflict resolution, new syntax support

**Medium-Sized Features/Improvements**:
- Enhances existing capabilities in meaningful ways
- Needs some business context but purpose is fairly clear
- Improves user efficiency or solves known pain points
- Took moderate effort (1-2 sprints)
- Examples: UI improvements, refactoring for better performance, migration to new libraries

**Small Features/Enhancements**:
- Incremental improvements that are self-explanatory
- Need minimal business context - value is obvious
- Fix edge cases or polish existing features
- Quick to implement (days, not weeks)
- Examples: Button reordering, link additions, minor UI tweaks

#### Categorization Rules

**New Features** section (Major releases only):
- Completely new functionality
- New capabilities that didn't exist before
- Should have business value descriptions
- Include 2-3 sentences explaining the feature and its benefits
- May include screenshots or examples if available

**Improvements** section:
- Enhancements to existing features
- Performance improvements
- Refactoring for better maintainability
- Library updates that improve functionality
- UI/UX improvements
- Group by component: **OpenL Studio:**, **Rule Services:**, **Core:**, etc.
- Each item is a concise bullet point (1-2 sentences)

**Fixed Bugs** section:
- Bug fixes
- Error corrections
- Issues resolved
- Group by component: **OpenL Studio:**, **Rule Services:**, etc.
- Each item describes the problem that was fixed

**Updated Libraries** section (if applicable):
- Group into:
  - **Runtime Dependencies:**
  - **Test Dependencies:**
  - **Maven:**
  - **Removed:**
- Format: `Library Name Version.Number`
- List in alphabetical order within each group

### Step 5: Write Release Notes

#### For Major Releases (X.Y.0)

```markdown
## Release Notes

[vX.Y.Z](https://github.com/openl-tablets/openl-tablets/releases/tag/X.Y.Z) on the GitHub

### New Features

**Feature Title 1**

Detailed description of the feature (2-4 sentences explaining what it does and why it matters). Include business context and user benefits.

**Feature Title 2**

Another detailed description with business value.

### Improvements

**Component Name:**
* Brief description of improvement 1.
* Brief description of improvement 2.

**Another Component:**
* Improvement description.

### Fixed Bugs

**Component Name:**
* Description of the bug that was fixed.
* Another bug fix description.

### Updated Libraries

**Runtime Dependencies:**
* Library1 X.Y.Z
* Library2 A.B.C

**Test Dependencies:**
* TestLib1 X.Y.Z

**Maven:**
* maven-plugin X.Y.Z

**Removed:**
* OldLibrary X.Y.Z

### Known Issues
* Description of any known issues (if applicable).

### Migration Notes

**Important migration information and breaking changes.**

Details about database schema changes, deprecated code removal, or other migration requirements.

### Live Demo

Check [OpenL Tablets demo](http://demo.openl-tablets.org/) in action

### Need help?
* [Documentation](https://openl-tablets.org/documentation)
* [Videos](https://openl-tablets.org/documentation/videos)
* [Forum](http://sourceforge.net/p/openl-tablets/discussion/)
* [Contact Us](https://openl-tablets.org/community/contact-us)
```

#### For Non-Major Releases (X.Y.Z where Z > 0)

```markdown
## Release Notes

[vX.Y.Z](https://github.com/openl-tablets/openl-tablets/releases/tag/X.Y.Z) on the GitHub

### Improvements

**Component Name:**
* Brief description of improvement 1.
* Brief description of improvement 2.

### Fixed Bugs

**Component Name:**
* Description of the bug that was fixed.

### Updated Libraries

**Runtime Dependencies:**
* Library1 X.Y.Z

### Known Issues
* Description of any known issues (if applicable).

### Live Demo

Check [OpenL Tablets demo](http://demo.openl-tablets.org/) in action

### Need help?
* [Documentation](https://openl-tablets.org/documentation)
* [Videos](https://openl-tablets.org/documentation/videos)
* [Forum](http://sourceforge.net/p/openl-tablets/discussion/)
* [Contact Us](https://openl-tablets.org/community/contact-us)
```

### Step 6: Create Markdown File

Save the generated content to:
```
docs/release-notes/release-notes-X.Y.Z.md
```

Where X.Y.Z is the version number (e.g., `release-notes-5.27.8.md`).

## Style Guidelines

### Writing Style

- **Technical but accessible**: Write for developers and technical users
- **Professional tone**: Formal, clear, and concise
- **Active voice**: "Added support for..." not "Support was added for..."
- **Present tense for features**: "Allows users to..." not "Will allow..."
- **Past tense for fixes**: "Fixed an issue where..." not "Fixes an issue..."

### Describing Features

- **Major features**: Include business context and user benefits
- **Improvements**: Focus on what changed and why it's better
- **Bug fixes**: Describe the symptom/problem, not the technical cause
- **No Jira references**: Never mention ticket numbers like "EPBDS-12345"

### Language Rules

- **British English**: Use "colour" not "color", "optimise" not "optimize"
- **No jargon**: Avoid internal terminology unless necessary
- **Clarity over brevity**: Better to be clear than concise
- **Consistent terminology**: Use the same terms throughout

## Example Transformations

### From Jira Ticket to Release Note

**Jira Ticket:**
```
EPBDS-12345: Refactor Kafka config to avoid reflection
Description: The Kafka configuration was using illegal reflection
operations which caused warnings in Java 17. This PR refactors
the code to use proper accessor methods.
```

**Release Note:**
```
**Rule Services:**
* Refactored the Kafka configuration to avoid illegal reflection operations.
```

---

**Jira Ticket:**
```
EPBDS-12346: Add ACL support
Description: Implement Access Control Lists for fine-grained
permissions. Users can now define permissions at object level
through the API. This took 6 sprints to complete and affects
multiple modules including auth, repository, and API.
```

**Release Note (Major Release):**
```
**Support of Access Control Lists**

The newly developed Access Control Lists (ACL) feature available in
OpenL WebStudio is a more robust and granular way to control user
access to assets. This update brings a comprehensive mechanism for
controlling the actions of subjects on objects, focusing on enhancing
security, authorization, and user identification. Currently, ACL
management is available through the built-in API tool or other API
tools. We recommend that users read the description available at
webstudio/rest/api-docs to fully understand the nuances of using
the ACL feature.
```

## Common Components

Group features by these common components:
- **OpenL Studio**
- **Rule Services**
- **Core**
- **OpenL Studio, Rule Services** (when both are affected)
- **Runtime**
- **Maven Plugin**
- **Documentation**

## Error Handling

If issues occur:
1. **No tickets found**: Ask user to verify the version number and check if Fix Version exists in Jira
2. **Too many tickets**: Process in batches and aggregate results
3. **Missing information**: Note in the output which sections need manual review
4. **Ambiguous categorization**: Default to "Improvements" and note for manual review

## Quality Checks

Before finalizing:
1. ✓ All features are categorized correctly
2. ✓ No Jira ticket numbers appear in text
3. ✓ Component names are consistent
4. ✓ Grammar and spelling are correct (British English)
5. ✓ Major features have sufficient business context
6. ✓ Version number in title and GitHub link match
7. ✓ File saved to correct location: `docs/release-notes/release-notes-X.Y.Z.md`
8. ✓ Grouping by components is logical and consistent

## Final Output

After generating the release notes:
1. Save the markdown file to `docs/release-notes/release-notes-X.Y.Z.md`
2. Show the user a preview of the content
3. Inform them of the file location
4. Offer to make any adjustments if needed

## Notes

- Always analyze the **actual changes** described in tickets, not just titles
- Focus on **user-facing value**, not implementation details
- For library updates, verify the version numbers are accurate
- If uncertain about feature classification, err on the side of less emphasis (smaller category)
- Major features should truly be significant; most items go in Improvements

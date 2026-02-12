# OpenL Tablets Release Notes Generator

## Overview

This skill automatically generates professional release notes for OpenL Tablets releases by analyzing Jira tickets and producing well-structured technical documentation in the established OpenL markdown format for GitHub.

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
Ask the user: "What release version should I generate the notes for? (e.g., 5.27.8, 6.0.0)"
```

### Step 2: Determine Release Type

Based on the version number:
- **Major release**: X.Y.0 (e.g., 5.27.0, 6.0.0)
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

**Major Features** (for major releases):
- Changes how users work or unlocks entirely new capabilities
- Requires significant business context to understand value
- Impacts multiple parts of the system or workflows
- Took considerable development effort (min 4 sprints)
- Examples: ACL implementation, Administration Panel Redesign, Jakarta EE migration
- These become H3 sections (###) under "New Features"

**Medium-Sized Features**:
- Enhances existing capabilities in meaningful ways
- Needs some business context but purpose is fairly clear
- Improves user efficiency or solves known pain points
- Took moderate effort (1-2 sprints)
- Can be major features or go under "Additional Features" subsection

**Small Features/Enhancements**:
- Incremental improvements that are self-explanatory
- Need minimal business context - value is obvious
- Quick to implement (days, not weeks)
- These go under "Improvements" section or "Additional Features" subsection

#### Categorization Rules

**Document Title**:
- Format: `# **OpenL Tablets X.Y.Z Release Notes**` (H1 with bold)
- Followed by 2 paragraphs:
  - First: High-level overview identifying as major release with 3-5 key features
  - Second: "This release also includes breaking changes that require careful review before upgrading." (if applicable)

**Contents Section**:
- H2 heading: `## **Contents**`
- Bullet list with anchor links to main sections:
  - `[New Features](#new-features)`
  - `[Improvements](#improvements)`
  - `[Breaking Changes](#breaking-changes)`
  - `[Security & Library Updates](#security--library-updates)`
  - `[Bug Fixes](#bug-fixes)`
  - `[Migration Notes](#migration-notes)`

**New Features Section** (Major releases):
- H2 heading: `## **New Features**`
- Each major feature:
  - H3 heading: `### **Feature Name**` (bold)
  - Opening paragraph(s) explaining the feature
  - H4 subsections (####) for capabilities: `#### **Subsection Name**` (bold)
  - H5 subsections (#####) for sub-capabilities if needed: `##### **Sub-subsection Name**` (bold)
  - Bullet lists for details
  - Code blocks with proper syntax highlighting
  - Images if mentioned: `![ImageDescription](images/ImageName.png)`
  - Horizontal rule `---` separating major features
- Last subsection: `### **Additional Features**` for smaller features

**Improvements Section**:
- H2 heading: `## **Improvements**`
- H3 subsections for themes: `### **Theme Name**` (bold)
- Bullet lists under each theme
- Simple descriptions (1-2 sentences per bullet)

**Breaking Changes Section**:
- H2 heading: `## **Breaking Changes**`
- Opening paragraph if needed
- Each breaking change:
  - H3 heading: `### **Breaking Change Name**` (bold, add "(CRITICAL)" if severe)
  - Opening paragraph
  - H4 subsections: `#### **What Changed**`, `#### **Impact**`, `#### **Who Is Affected**`, `#### **Migration Steps**`
  - Detailed bullet lists and code examples
  - Horizontal rule `---` separating major breaking changes
- Final subsection: `### **Removed Features Summary**` with categorized removals

**Security & Library Updates Section**:
- H2 heading: `## **Security & Library Updates**`
- H3 subsections: `### **Security Vulnerability Fixes**`, `### **Major Library Upgrades**`
- Security: Bullet list with CVEs
- Libraries: H4 subsections for categories, bullet lists with versions
- Format: `Library Name Version.Number (from X.x)` for major upgrades
- Horizontal rule `---` between Security and Library sections

**Bug Fixes Section**:
- H2 heading: `## **Bug Fixes**`
- Simple bullet list of fixes
- No component grouping
- Format: "Fixed [description]"

**Migration Notes Section**:
- H2 heading: `## **Migration Notes**`
- First subsection: `### **Quick Role-Based Pointers**`
  - Bullet list with bold role names and sections to review
- Subsequent subsections: `### **Category Name**` for each migration area
- Each subsection includes:
  - Bullet lists or paragraphs
  - Code blocks for configuration examples
  - Tables for mappings (use markdown tables)
  - Horizontal rules `---` separating major migration areas

### Step 5: Write Release Notes

#### Document Structure for Major Releases

```markdown
# **OpenL Tablets X.Y.Z Release Notes**

OpenL Tablets **X.Y.Z** is a major release introducing [key themes and major features].

This release also includes breaking changes that require careful review before upgrading.

## **Contents**

* [New Features](#new-features)
* [Improvements](#improvements)
* [Breaking Changes](#breaking-changes)
* [Security & Library Updates](#security--library-updates)
* [Bug Fixes](#bug-fixes)
* [Migration Notes](#migration-notes)

## **New Features**

### **First Major Feature Name**

Opening paragraph explaining what this feature is and its primary purpose. Use 2-4 sentences to set context.

Second paragraph covering additional scope or capabilities. Keep paragraphs focused and readable.

#### **Key Capability Name**

Introduction sentence for this capability.

* Bullet point detail 1
* Bullet point detail 2
* Bullet point detail 3

Additional paragraph if needed.

##### **Sub-capability Name**

Even more detailed breakdown using H5 headings when necessary.

Configuration example if applicable:

```properties
property.name=value
property.name2=value2
```

#### **Migration Notes**

1. Automatic migration details
2. Manual migration requirements:
   * Step detail 1
   * Step detail 2

```properties
code.example=value
```

---

### **Second Major Feature Name**

Structure similar to first feature with opening paragraphs, subsections, and horizontal rule separator.

![FeatureScreenshot](images/FeatureName.png)

---

### **Additional Features**

#### **Smaller Feature 1**

Brief description with bullet points:

* Detail 1
* Detail 2

**Configuration properties:**

```properties
property.name=value
```

#### **Smaller Feature 2**

Brief description.

##### **Key characteristics:**

* Characteristic 1
* Characteristic 2

## **Improvements**

### **Improvement Theme 1**

* Improvement detail 1
* Improvement detail 2
* Improvement detail 3

### **Improvement Theme 2**

* Improvement detail 1
* Improvement detail 2

## **Breaking Changes**

Optional opening paragraph explaining the breaking changes section.

### **Breaking Change Name (CRITICAL if applicable)**

Opening paragraph explaining the breaking change and its significance.

#### **What Changed**

* Change detail 1
* Change detail 2

Major framework upgrades:

* Framework 1 Version (from X.x)
* Framework 2 Version (from X.x)

#### **Impact**

* Impact detail 1
* Impact detail 2

#### **Who Is Affected**

You are affected only if your OpenL projects include [specific conditions]:

* Condition 1
* Condition 2

#### **Migration Steps**

If you are using OpenL Studio without [condition], no action is required.

The steps below apply only if [condition applies].

##### **Required changes for specific area**

1. Step 1 description
2. Step 2 description

```java
// Code example
package.name → new.package.name
```

---

### **Another Breaking Change**

Follow same structure as above.

---

### **Removed Features Summary**

The following features, protocols, and APIs have been completely removed from OpenL Tablets X.Y.Z:

#### **Removed Protocols & Integration Methods**

* **Protocol Name**
  * **Migration**: Migration path description

#### **Removed Database Support**

* **Database Name**
  * **Migration**: Migration path description

#### **Removed Libraries & Extensions**

* **Library Name**
  * **Migration**: Migration path description

#### **Removed APIs**

* **API Name**
  * **Migration**: Migration path description
* **Method Names:**
  * method1(), method2()
  * **Migration**: Migration path

#### **Removed UI Features**

* **Feature Name**
  * **Migration**: Migration path

## **Security & Library Updates**

### **Security Vulnerability Fixes**

* **CVE-YYYY-XXXXX**: Description of vulnerability fixed
* **CVE-YYYY-XXXXX**: Description of vulnerability fixed

---

### **Major Library Upgrades**

#### **Runtime Dependencies**

* Spring Framework 6.2.11 (from 5.x)
* Spring Boot 3.5.6 (from 2.x)
* Spring Security 6.5.5 (from 5.x)
* Hibernate ORM 6.6.29.Final (from 5.x)
* [Continue with other libraries...]

#### **Test Dependencies**

* Mockito 5.20.0
* XMLUnit 2.10.4
* PostgreSQL Driver 42.7.8

#### **Removed Dependencies**

* commons-jxpath

## **Bug Fixes**

* Fixed GitLFS issues with BitBucket repositories
* Fixed tracing and dependency graph issues
* Fixed deployment repository errors when base.path not specified

## **Migration Notes**

### **Quick Role-Based Pointers**

* **If you are a Rules Author** → pay special attention to sections **3, 4**
* **If you are a Developer** → pay special attention to sections **2, 3, 5**
* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2, 6**

---

### **Access Control & Permissions**

* The legacy permission model has been replaced with role-based access control (RBAC).
  Permissions are migrated automatically, but effective access may differ after upgrade.
* `security.default-group` no longer grants implicit READ access to repositories.
* Project creation and deletion are additionally controlled by the system property:

```properties
security.allow-project-create-delete
```

* Role assignments and default group permissions should be reviewed after upgrade.

#### **User Access & Permission Mapping**

| Legacy Permission | New Behavior | Notes / Action |
| :---- | :---- | :---- |
| **View Projects** | Included in all roles | Deprecated; project viewing available to all roles |
| **Edit Projects** | Included in Contributor role | Covered by the Edit permission |

---

### **Runtime Environment**

* Java **21** is required. Earlier Java versions are no longer supported.
* All `javax.*` packages have been replaced with `jakarta.*`.
  Custom Java code must be updated accordingly.

---

### **Rules & Rule Execution**

* The **variations** feature has been completely removed.
  * Variation-based endpoints are no longer created.
  * Rules relying on variations must be rewritten.

---

### **Metadata & OpenAPI**

* Project tags are now stored inside the project structure.
  * Tags are migrated automatically.
  * Tag changes are version-controlled.

---

### **Integration & Serialization**

* JSON polymorphic serialization now uses **NAME-based** type identification.
  * Client applications relying on CLASS-based typing may require updates.

---

### **Administration, Deployment & Removed Features**

* The following components are no longer supported:
  * RMI
  * CAS SSO
  * Legacy APIs
  * Install Wizard
* The Administration UI has been fully redesigned.
  * No functional loss; familiarize yourself with the new layout.
```

#### Document Structure for Non-Major Releases

```markdown
# **OpenL Tablets X.Y.Z Release Notes**

## **Improvements**

* Brief description of improvement 1
* Brief description of improvement 2

## **Bug Fixes**

* Fixed [issue description]
* Fixed [issue description]

## **Security & Library Updates**

### **Security Vulnerability Fixes**

* **CVE-YYYY-XXXXX**: Description fixed

### **Library Upgrades**

#### **Runtime Dependencies**

* Library1 X.Y.Z
* Library2 A.B.C
```

### Step 6: Create Markdown File

Save the generated content to:
```
release-notes-X.Y.Z.md
```

Where X.Y.Z is the version number (e.g., `release-notes-6.0.0.md`).

## Style Guidelines

### Writing Style

- **Technical but accessible**: Written for technical users but clear for informed business users
- **Professional tone**: Formal, clear, and comprehensive
- **Active voice**: "The system supports..." not "Support is provided..."
- **Present tense for features**: "Allows users to..." not "Will allow..."
- **Past tense for fixes**: "Fixed an issue where..." not "Fixes an issue..."
- **Detailed explanations**: Major features get comprehensive treatment
- **Migration-focused**: Breaking changes include detailed guidance

### Describing Features

- **Major features**: Multi-paragraph descriptions with subsections, bullet lists
- **Opening paragraphs**: Set context before diving into details
- **Subsections**: Use H4 for capabilities, H5 for sub-capabilities
- **Bullet lists**: For enumerating items, capabilities, or steps
- **Code blocks**: With proper language tags (properties, java, yaml, etc.)
- **Images**: Reference with captions when available
- **Migration notes**: Detailed steps within feature sections when relevant
- **Improvements**: Grouped by theme, not by component
- **Bug fixes**: Simple descriptions, no component grouping
- **No Jira references**: Never mention ticket numbers

### Formatting Requirements

#### Heading Hierarchy
- **H1**: Document title with bold: `# **OpenL Tablets X.Y.Z Release Notes**`
- **H2**: Main sections with bold: `## **New Features**`, `## **Breaking Changes**`
- **H3**: Major features and subsections with bold: `### **Feature Name**`
- **H4**: Capabilities and structured subsections with bold: `#### **What Changed**`
- **H5**: Sub-capabilities with bold: `##### **Sub-capability Name**`

#### Bold Formatting
- All headings include **bold** markers around the text
- Important terms in bullets use **bold**
- Property names in text use `code` formatting, not bold

#### Paragraph Structure
- Opening paragraphs: 2-4 sentences introducing the section/feature
- Detail paragraphs: 2-3 sentences focused on one aspect
- Blank lines: Always separate paragraphs

#### Lists and Bullets
- Use bullet lists (`*`) for enumerating items
- Use numbered lists for sequential steps
- Nested bullets for sub-items
- Each bullet can be a full sentence or phrase

#### Code Blocks
- Always specify language: ```properties, ```java, ```yaml
- Include comments in code examples
- Use inline code (`backticks`) for property names, class names, methods

#### Horizontal Rules
- Use `---` to separate major features
- Use `---` to separate major breaking changes
- Use `---` to separate major migration sections
- Use `---` between Security Fixes and Library Upgrades

#### Tables
- Use markdown tables for mappings
- Include header row with alignment
- Keep cells concise but complete

#### Images
- Format: `![ImageDescription](images/ImageName.png)`
- Place after relevant text explaining what the image shows
- Use descriptive alt text

### Language Rules

- **American English**: Use "color" not "colour", "optimize" not "optimise"
- **Technical accuracy**: Use correct technical terms
- **Consistency**: Same terms throughout
- **Clarity**: Be comprehensive rather than overly concise
- **Component names**:
  - "OpenL Studio" (not "WebStudio")
  - "Rule Services" (not "RuleServices")
  - "OpenL Rules" (for rules engine/runtime)
  - "OpenL Tablets" (for overall platform)

## Content Organization Principles

### New Features Section
1. Each major feature gets H3 heading with bold
2. Features separated by `---` horizontal rules
3. Structure:
   - H3 feature name (bold)
   - Opening paragraphs
   - H4 subsections for capabilities (bold)
   - H5 sub-subsections if needed (bold)
   - Bullet lists for details
   - Code blocks for configuration
   - Images if available
   - Migration notes subsection if relevant
4. Last subsection: "Additional Features" for smaller items

### Improvements Section
1. Group by theme with H3 headings (bold)
2. Bullet lists under each theme
3. Keep descriptions brief (1-2 sentences)
4. No component grouping

### Breaking Changes Section
1. Each change gets detailed H3 section (bold)
2. Structured H4 subsections (bold): What Changed, Impact, Who Is Affected, Migration Steps
3. H5 for detailed migration steps (bold)
4. Separated by `---` rules
5. Final "Removed Features Summary" subsection with categorized removals

### Library Updates Section
1. H3 for Security Fixes (bold)
2. Horizontal rule `---`
3. H3 for Library Upgrades (bold)
4. H4 categories: Runtime, Test, Removed (bold)
5. Bullet lists with version comparisons for major upgrades

### Bug Fixes Section
- Simple bullet list
- No grouping
- Format: "Fixed [description]"

### Migration Notes Section
1. H3 "Quick Role-Based Pointers" first (bold)
2. Subsequent H3 sections for migration areas (bold)
3. Bullet lists and paragraphs
4. Code examples
5. Tables for mappings
6. Separated by `---` rules

## Common Components

Use these exact names:
- **OpenL Studio**
- **Rule Services**
- **OpenL Rules**
- **OpenL Tablets**
- **Core**
- **DEMO**

## Error Handling

If issues occur:
1. **No tickets found**: Verify version number and Fix Version in Jira
2. **Too many tickets**: Process in batches
3. **Missing information**: Note what needs manual review
4. **Ambiguous categorization**: Err on side of more detail for major releases

## Quality Checks

Before finalizing:
1. ✓ Document title is H1 with bold: `# **OpenL Tablets X.Y.Z Release Notes**`
2. ✓ Contents section with working anchor links
3. ✓ All section headings use bold formatting
4. ✓ Major features separated by `---` horizontal rules
5. ✓ H3 for features, H4 for capabilities, H5 for sub-capabilities (all bold)
6. ✓ Breaking changes have structured subsections
7. ✓ Security and Library sections separated by `---`
8. ✓ Migration notes with role-based pointers
9. ✓ Code blocks have language tags
10. ✓ No Jira ticket numbers in text
11. ✓ Component names are correct
12. ✓ Grammar and spelling correct (American English)
13. ✓ Images referenced with proper markdown syntax
14. ✓ Tables properly formatted

## Example Transformations

### From Jira Ticket to Release Note

**Jira Ticket:**
```
EPBDS-12345: Implement ACL
Description: Complete redesign of access control with role-based 
permissions. Migration from 16-permission to 3-role model.
Took 6 sprints. Includes UI redesign and batch API.
```

**Release Note:**
```markdown
### **Simplified User Access & Permissions Management in OpenL Studio**

OpenL Studio introduces a **completely redesigned access control system** with a 
simplified, role-based permission model. The new approach significantly reduces 
configuration complexity while maintaining enterprise-grade security.

The new system supports **granular, resource-level access control** through role 
assignments, with a reduced and streamlined set of permissions for improved 
clarity and maintainability.

#### **Role-Based Access Control**

Access is now managed through three predefined roles:

* **Manager**: Full control with ability to assign roles
* **Contributor**: Content modification without system administration
* **Viewer**: Read-only access with test execution capabilities

[Continue with more subsections...]

#### **Migration Notes**

1. ACL structures are migrated automatically during upgrade
2. Legacy permissions automatically migrated:
   * Legacy "View Projects" → Viewer role at repository level
   * Legacy "Edit Projects" → Contributor role at repository level

---
```

---

**Jira Ticket:**
```
EPBDS-12346: Add compose.yaml to DEMO
Description: Replaced Docker image with compose.yaml.
Reduces size and removes Java requirement.
```

**Release Note:**
```markdown
### **Demo Enhancements**

* Replaced DEMO docker image with compose.yaml file
* Reduced the download size of the zip file
* Removed manual Java installation step
```

## Final Output

After generating the release notes:
1. Save the markdown file to `release-notes-X.Y.Z.md`
2. Show the user a preview
3. Inform them of the file location
4. Offer to make adjustments if needed

## Notes

- This format is optimized for GitHub rendering and web viewing
- All headings use bold formatting
- Horizontal rules separate major sections
- Contents section provides easy navigation
- Images can be referenced but actual image files must be provided separately
- Code blocks should specify language for syntax highlighting
- Tables use markdown format for better readability
- Focus on user-facing value and comprehensive migration guidance
- Major releases require all sections; non-major releases are simplified

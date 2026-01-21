---
name: openl-release-notes
description: Generate professional release notes for OpenL Tablets by collecting and analyzing JIRA tickets. Use when user mentions "generate release notes", "create release notes", "OpenL release notes", or "release notes for OpenL Tablets".
---

# OpenL Tablets Release Notes Generator

This skill generates comprehensive, professional release notes for OpenL Tablets releases by automatically collecting tickets from JIRA, analyzing them, categorizing by type and component, and creating formatted documentation in both Word and Markdown formats.

## When to Use This Skill

Automatically activate when the user:
- Says "generate release notes" or "create release notes"
- Mentions "OpenL Tablets release notes" or "OpenL release notes"
- Asks to generate or create release documentation for OpenL

---

## Step-by-Step Workflow

### Step 1: Gather Requirements

Ask the user for:

1. **Version Number**: The OpenL Tablets version for the release notes (e.g., "5.27.8", "6.0.0")
   - Note: The version format in JIRA may be "OpenL X.X.X" not just "X.X.X"
2. **Output Directory**: Where to save the generated files (e.g., "Downloads", "/Users/username/Documents")

### Step 2: Collect Tickets from JIRA

Use Jira MCP tools to search for issues:

**JQL Query:**
```
project = EPBDS AND fixVersion = "OpenL {version}" AND "Release Notes" != "No, don't include ticket in Release Notes"
```

**Important Notes:**
- Try the version format "OpenL X.X.X" first (e.g., "OpenL 6.0.0")
- If that fails, try just the version number (e.g., "6.0.0")
- Use `jira_get_project_versions` if you need to find the exact version format
- Paginate through all results if there are more than 50 tickets (use start_at parameter)
- Collect fields: summary, description, issuetype, labels, components, customfield for Release Notes

### Step 3: Analyze Tickets

For each ticket, extract:
- **Issue Type**: Bug, Story, Task, Improvement, New Feature, etc.
- **Summary**: Brief description
- **Description**: Detailed information (may contain technical details)
- **Components**: Which OpenL component(s) it affects
- **Labels**: Additional categorization
- **Release Notes field**: Custom field with release notes text (if available)

Identify:
- Breaking changes or migration requirements
- Major features requiring detailed descriptions
- Component affected (OpenL Studio, Rule Services, Core, Repository, Security, etc.)

### Step 4: Categorize Tickets

Organize tickets into these categories:

1. **New Features**: New functionality added to the product
2. **Improvements**: Enhancements to existing functionality
3. **Breaking Changes**: Changes that require user action or migration
4. **Bug Fixes**: Fixed bugs and issues
5. **Updated Libraries**: Dependency and library updates

Within each category, group by component:
- OpenL Studio
- OpenL Studio Admin
- OpenL Rule Services
- OpenL Core
- OpenL Repository
- Security
- DEMO
- Documentation

---

## Step 5: Generate Professional Content

### ✅ Pre-Generation Checklist

Before writing content, identify which features need detailed descriptions:

**Major Features Requiring Detailed Descriptions (2-4 paragraphs each):**
- [ ] New Administration UI or UI redesigns
- [ ] Access Control Lists (ACL) or security model changes
- [ ] Framework migrations (Jakarta EE, Spring, etc.)
- [ ] New APIs or significant API changes
- [ ] User/Group management features
- [ ] Major architectural changes
- [ ] New integration capabilities (OAuth, SAML, etc.)
- [ ] Significant performance improvements
- [ ] New testing or deployment features
- [ ] Statistical functions or major functional additions

**Regular Features (1-2 sentences each):**
- [ ] Minor improvements
- [ ] Bug fixes
- [ ] Library updates
- [ ] Small UI tweaks
- [ ] Documentation updates

### ⚠️ CRITICAL: Major Features MUST Have Detailed Descriptions

**IMPORTANT**: For significant features like ACL, Admin UI updates, framework migrations, and major new functionality:
- **DO NOT** write single-line bullet points
- **ALWAYS** provide comprehensive 2-4 paragraph descriptions
- **EXPLAIN** the feature in depth with context and benefits

### Content Writing Guidelines

**For Major Features (New Features & Improvements):**

Write following this structure:

**REQUIRED Elements:**
1. **Overview (1-2 sentences)**: What is this feature and why was it added?
2. **Key Components/Capabilities (2-4 bullets)**: What specific functionality does it provide?
3. **Benefits and Use Cases (1-2 sentences)**: How will users benefit? What problems does it solve?
4. **Technical Details (1-2 sentences)**: Implementation approach, technologies used, integration points
5. **Examples or Scenarios (optional)**: Specific usage scenarios if helpful

**Writing Guidelines:**
- Provide detailed descriptions that explain what the feature does and why it matters
- Include use cases showing how users will benefit from the feature
- Explain the context - what problem does this solve or what capability does it add
- Be specific about functionality - don't just say "improved X", explain what specifically was improved and the impact
- Add technical details where relevant (e.g., supported formats, configuration options, API changes)
- Use 2-3 paragraphs for major features instead of single bullet points
- Think of the audience: Developers, BAs, and architects need enough detail to understand impact

**For Bug Fixes and Minor Changes:**
- Keep descriptions concise (1-2 sentences)
- Focus on what was broken and what now works correctly

**General Style:**
- Use professional, concise technical writing
- Start sections with component names in bold
- Use bullet points for each change
- Focus on what changed and impact to users
- Avoid mentioning internal ticket numbers in the main content
- Keep descriptions clear and actionable
- Maintain consistency with https://openl-tablets.org/release-notes format

---

### Example Format for MAJOR Features (Use This Style!)

**GOOD EXAMPLE - Major Feature with Detailed Description:**

```markdown
## New Features

### Redesigned Administration Interface

**Overview**

The administration panel has been completely redesigned using modern React technology, replacing the legacy JSF/RichFaces implementation. The new interface provides a more intuitive, responsive, and user-friendly experience for system administrators.

**Key Components**

- **Security Settings**: Manage authentication modes (Multi-user, Active Directory, SAML, OAuth2) with improved configuration interface
- **User Management**: Enhanced user creation, editing, and deletion with improved validation and error handling
- **Group Management**: Streamlined group administration with member count display and quick access to group details
- **Repository Management**: Intuitive repository configuration with support for Git, AWS S3, and JDBC repositories, including connection testing and improved error reporting
- **System Settings**: Centralized management of system properties including date formats, time formats, auto-compile settings, and thread pool configurations

**Benefits**

The new UI is built using React 18 with modern component architecture, providing real-time updates through WebSocket connections. The interface follows responsive design principles and is compatible with all modern browsers. All administrative operations are backed by RESTful APIs, enabling potential automation and integration scenarios.

---

### Enhanced Access Control Lists (ACL)

**Overview**

OpenL Tablets 6.0.0 introduces a completely redesigned access control system that simplifies permissions management while providing more granular control over resources. The new ACL system replaces the previous 16-permission model with a streamlined 3-role approach.

**New Role Model**

The simplified role model consists of three primary roles:
- **Viewer**: Read-only access to resources. Can view content, run tests, and execute benchmarks but cannot make modifications
- **Contributor**: Full edit access including the ability to create, modify, and delete content within assigned resources
- **Manager**: Complete administrative control over assigned resources with all Contributor capabilities plus the ability to assign access rights to other users

**Integration Features**

Access control is applied at the repository level, allowing administrators to grant specific roles for Design and Deployment repositories independently. The system seamlessly integrates with external user management systems including Active Directory, SAML, and OAuth2 providers. A new REST API enables bulk import and export of ACL configurations, facilitating automated deployment processes across multiple environments.
```

**BAD EXAMPLE - Major Feature with Insufficient Detail:**

```markdown
## New Features

**OpenL Studio**
- New Administration UI with React
- Enhanced ACL with simplified roles
- User and group management improvements
```

↑ **THIS IS NOT ACCEPTABLE for major features!**

---

**GOOD EXAMPLE - Regular Features:**

```markdown
## Improvements

**OpenL Studio**
- **Enhanced Rule Validation**: Improved validation engine now provides more detailed error messages with specific line numbers, suggested fixes, and links to documentation. Validation performance improved by 40% for large projects with thousands of rules.

- **Project Import Performance**: Optimized project import process to handle large files more efficiently, reducing import time by up to 60% for projects over 100MB. Added progress indicators and the ability to resume interrupted imports.

## Bug Fixes

**OpenL Studio**
- Fixed ClassCastException that occurred when editing complex data types with nested collections
- Resolved issue where project import would fail for certain file structures containing special characters in folder names
```

---

## Step 6: Generate Migration Notes

For breaking changes, create a dedicated "Migration Notes" section:

**Format:**
```markdown
## Migration Notes

### Component Name - Breaking Change Title

**Impact**: Description of what this affects (e.g., "Affects all users using feature X")

**Action Required**:
1. Step-by-step migration instructions
2. Any configuration updates needed
3. Code changes if applicable
4. Timeline or version compatibility information

**Example** (if helpful):
Show before/after code examples or configuration changes
```

**Guidelines:**
- Explain what changed and why
- Provide clear steps for users to migrate
- Include code examples if relevant
- Highlight any configuration changes needed
- Be specific about version compatibility

---

## Step 7: Create Output Files

Generate two files using Python with python-docx library:

### Python Implementation Script

```python
from docx import Document
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_PARAGRAPH_ALIGNMENT
from datetime import datetime

def create_release_notes(version, tickets_data, output_dir):
    """
    Generate Word and Markdown release notes files.

    Args:
        version: OpenL version (e.g., "6.0.0")
        tickets_data: Dict with categorized tickets
        output_dir: Output directory path
    """

    # Create Word document
    doc = Document()

    # Title
    title = doc.add_heading(f'OpenL Tablets {version} Release Notes', 0)
    title.alignment = WD_PARAGRAPH_ALIGNMENT.CENTER

    # Release Date
    date_para = doc.add_paragraph(f'Release Date: {datetime.now().strftime("%B %d, %Y")}')
    date_para.alignment = WD_PARAGRAPH_ALIGNMENT.CENTER
    doc.add_paragraph()

    # Overview
    if 'overview' in tickets_data:
        doc.add_heading('Overview', 1)
        doc.add_paragraph(tickets_data['overview'])
        doc.add_paragraph()

    # New Features
    if 'new_features' in tickets_data:
        doc.add_heading('New Features', 1)
        for component, features in tickets_data['new_features'].items():
            doc.add_heading(component, 2)
            for feature in features:
                if feature.get('is_major'):
                    # Major feature with detailed description
                    doc.add_heading(feature['title'], 3)
                    if isinstance(feature['description'], list):
                        for para in feature['description']:
                            doc.add_paragraph(para)
                    else:
                        doc.add_paragraph(feature['description'])
                    doc.add_paragraph()
                else:
                    # Minor feature as bullet
                    doc.add_paragraph(feature['description'], style='List Bullet')

    # Improvements
    if 'improvements' in tickets_data:
        doc.add_heading('Improvements', 1)
        for component, improvements in tickets_data['improvements'].items():
            doc.add_heading(component, 2)
            for improvement in improvements:
                doc.add_paragraph(improvement, style='List Bullet')

    # Breaking Changes
    if 'breaking_changes' in tickets_data:
        doc.add_heading('Breaking Changes', 1)
        for change in tickets_data['breaking_changes']:
            doc.add_heading(change['title'], 2)
            doc.add_paragraph(change['description'])
            doc.add_paragraph()

    # Bug Fixes
    if 'bug_fixes' in tickets_data:
        doc.add_heading('Bug Fixes', 1)
        for component, fixes in tickets_data['bug_fixes'].items():
            doc.add_heading(component, 2)
            for fix in fixes:
                doc.add_paragraph(fix, style='List Bullet')

    # Updated Libraries
    if 'updated_libraries' in tickets_data:
        doc.add_heading('Updated Libraries', 1)
        for lib in tickets_data['updated_libraries']:
            doc.add_paragraph(lib, style='List Bullet')

    # Migration Notes
    if 'migration_notes' in tickets_data:
        doc.add_heading('Migration Notes', 1)
        for note in tickets_data['migration_notes']:
            doc.add_heading(note['title'], 2)
            doc.add_paragraph(f"**Impact**: {note['impact']}")
            doc.add_paragraph("**Action Required**:")
            for step in note['steps']:
                doc.add_paragraph(step, style='List Number')
            if 'example' in note:
                doc.add_paragraph("**Example**:")
                doc.add_paragraph(note['example'])
            doc.add_paragraph()

    # Save Word document
    docx_path = f"{output_dir}/OpenL_Tablets_{version}_Release_Notes_Detailed.docx"
    doc.save(docx_path)

    # Create Markdown version
    md_content = f"# OpenL Tablets {version} Release Notes\n\n"
    md_content += f"**Release Date:** {datetime.now().strftime('%B %d, %Y')}\n\n"

    # Add same sections in Markdown format
    # ... (similar structure)

    md_path = f"{output_dir}/OpenL_Tablets_{version}_Release_Notes_Detailed.md"
    with open(md_path, 'w', encoding='utf-8') as f:
        f.write(md_content)

    return docx_path, md_path
```

### File Naming Convention

- Word: `OpenL_Tablets_{version}_Release_Notes_Detailed.docx`
- Markdown: `OpenL_Tablets_{version}_Release_Notes_Detailed.md`

---

## Step 8: Confirm Completion

After generating files:
1. Confirm both files were created successfully
2. Display file locations with full paths
3. Show file sizes
4. Provide a brief summary (e.g., "156 tickets categorized into 45 new features, 67 improvements, 8 breaking changes, and 36 bug fixes")

---

## Error Handling

**If JIRA connection fails:**
- Inform user that authentication failed
- Suggest checking JIRA MCP server configuration
- Verify user has access to EPBDS project

**If no tickets found:**
- Verify the version format (try "OpenL X.X.X" vs "X.X.X")
- Use `jira_get_project_versions` to check available versions
- Confirm the version exists in JIRA

**If output directory doesn't exist:**
- Offer to create the directory
- Ask user for alternative location

**If file writing fails:**
- Check directory permissions
- Suggest alternative locations
- Verify python-docx is installed

**If pagination issues:**
- JIRA returns max 50 results per query
- Use start_at parameter to fetch remaining
- Continue until all tickets collected

---

## Prerequisites

1. **Jira MCP Server**: Configured with EPBDS project access
2. **Python 3**: Installed (`python3 --version`)
3. **python-docx**: Installed (`pip install python-docx`)
4. **Internet Connection**: Required for JIRA API

---

## Installation

### For Business Analysts

1. **Download** this SKILL.md file
2. **Create directory**: `~/.claude/skills/openl-release-notes/`
3. **Copy file** to: `~/.claude/skills/openl-release-notes/SKILL.md`
4. **Restart** Claude Desktop

### Verify Installation

1. Open Claude Desktop
2. Type: `/openl`
3. Should see "openl-release-notes" in list
4. Ready to use!

---

## Usage

### Basic Usage

Just ask Claude:
```
Generate release notes for OpenL Tablets version 6.0.0
```

Claude will:
1. Ask where to save files
2. Connect to JIRA and collect tickets
3. Categorize and analyze them
4. Generate comprehensive release notes
5. Create Word and Markdown files
6. Complete in 2-5 minutes

### Example Output

For version 6.0.0 with 156 tickets:
- **Word**: `OpenL_Tablets_6.0.0_Release_Notes_Detailed.docx` (44 KB, ~30 pages)
- **Markdown**: `OpenL_Tablets_6.0.0_Release_Notes_Detailed.md` (20 KB)

---

## Tips for Best Results

1. Skill automatically identifies major features requiring detailed descriptions
2. Changes are organized by component for readability
3. Technical language appropriate for developers and architects
4. Specific details (e.g., "40% performance improvement")
5. Breaking changes are prominent with migration instructions
6. Version format handling is automatic

---

## Troubleshooting

**Skill doesn't appear:**
- Restart Claude Desktop
- Verify location: `~/.claude/skills/openl-release-notes/SKILL.md`

**JIRA authentication failed:**
- Check JIRA MCP in Claude settings
- Verify EPBDS project access

**Python/python-docx not installed:**
- Install Python from python.org
- Run: `pip install python-docx`

**No tickets found:**
- Skill tries different version formats automatically
- Verify version exists in JIRA

---

## Quick Reference

**Typical Workflow:**
1. User: "Generate release notes for OpenL Tablets version 6.0.0"
2. Claude asks for output directory
3. Claude queries JIRA and collects tickets
4. Claude categorizes by type and component
5. Claude writes detailed descriptions for major features
6. Claude generates Word and Markdown files
7. Claude confirms completion

**Time:** 2-5 minutes (50-200 tickets)

**Output Size:**
- Word: 40-50 KB (~30 pages)
- Markdown: 15-25 KB (~400-500 lines)

---

## Version

**Version:** 1.0.0
**Last Updated:** January 2026
**Compatible with:** Claude Desktop with JIRA MCP Server

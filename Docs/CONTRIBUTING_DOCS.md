# Contributing to OpenL Tablets Documentation

Thank you for your interest in improving OpenL Tablets documentation! This guide will help you contribute effectively.

## Table of Contents

- [Documentation Structure](#documentation-structure)
- [Getting Started](#getting-started)
- [Writing Guidelines](#writing-guidelines)
- [Building Documentation](#building-documentation)
- [Contribution Workflow](#contribution-workflow)
- [Style Guide](#style-guide)
- [Review Process](#review-process)

---

## Documentation Structure

OpenL Tablets documentation is organized into several categories:

### User Documentation (`docs/user-guides/`)
- **Installation Guide** - Installation and setup instructions
- **Demo Package Guide** - Getting started with demos
- **Reference Guide** - Language reference and syntax
- **OpenL Studio User Guide** - Using OpenL Studio
- **Rule Services Guide** - Deploying rule services

### Developer Documentation
- **Developer Guides** (`docs/developer-guides/`) - Development with OpenL Tablets
- **Integration Guides** (`docs/integration-guides/`) - Framework integrations
- **Architecture** (`docs/architecture/`) - System architecture
- **Analysis** (`docs/analysis/`) - Deep technical dives
- **Onboarding** (`docs/onboarding/`) - Getting started for contributors

### Configuration & Operations
- **Configuration** (`docs/configuration/`) - System configuration
- **Examples** (`docs/examples/`) - Production examples
- **Operations** (`docs/operations/`) - DevOps and CI/CD
- **Guides** (`docs/guides/`) - Practical how-to guides

### Technical Documentation
- **API Reference** (`docs/api/`) - Public API documentation
- **CLAUDE.md Files** - Module-specific conventions
- **Module Documentation Progress** - Coverage tracking

---

## Getting Started

### Prerequisites

1. **Install dependencies**:
   ```bash
   cd docs
   pip install -r requirements.txt
   ```

2. **Verify installation**:
   ```bash
   mkdocs --version
   ```

### Local Preview

Preview documentation as you write:

```bash
# From repository root
mkdocs serve

# Or use the convenience script
./scripts/preview-docs.sh
```

Open http://127.0.0.1:8000 in your browser.

The preview auto-reloads when you save changes.

---

## Writing Guidelines

### File Organization

1. **Multi-file structure**: Split large guides (>500 lines) into logical sections
   - Example: OpenL Studio User Guide split into 12 files
   - Create `index.md` for navigation overview
   - Use subdirectories for appendices or subsections

2. **File naming**:
   - Use lowercase with hyphens: `getting-started.md`
   - Be descriptive: `decision-tables.md` not `dt.md`
   - Match section titles when possible

3. **Directory structure**:
   ```
   docs/user-guides/webstudio/
   ├── index.md              # Navigation overview
   ├── preface.md
   ├── introduction.md
   ├── getting-started.md
   ├── rules-editor.md
   └── appendices/
       ├── zip-structure.md
       └── openapi-generation.md
   ```

### Content Guidelines

1. **Front matter**:
   - No YAML front matter required (MkDocs handles this)
   - First heading should be `# Title` (h1)

2. **Headings**:
   - Use hierarchical structure: `#`, `##`, `###`
   - Make headings descriptive and searchable
   - Use sentence case: "Getting Started" not "GETTING STARTED"

3. **Links**:
   - Use relative links: `[Guide](../other-guide.md)`
   - Link to specific sections: `[Section](guide.md#section-name)`
   - Verify all links work after changes

4. **Images**:
   - Store in `docs/assets/images/[guide-name]/`
   - Use relative paths: `![Alt text](../../assets/images/webstudio/screenshot.png)`
   - Provide alt text for accessibility
   - Optimize images (PNG for screenshots, JPG for photos)

5. **Code blocks**:
   ````markdown
   ```java
   // Use language identifiers for syntax highlighting
   public class Example {
       public void method() {
           // Code here
       }
   }
   ```
   ````

6. **Tables**:
   ```markdown
   | Column 1 | Column 2 | Column 3 |
   |----------|----------|----------|
   | Data 1   | Data 2   | Data 3   |
   ```

7. **Admonitions** (notes, warnings):
   ```markdown
   !!! note
       This is a note.

   !!! warning
       This is a warning.

   !!! tip
       This is a helpful tip.
   ```

---

## Building Documentation

### Build HTML

```bash
# From repository root
mkdocs build

# Or use the convenience script
./scripts/build-docs.sh
```

Output is in `site/` directory.

### Build with Strict Mode

Strict mode fails on warnings (recommended before committing):

```bash
mkdocs build --strict
```

### Generate PDFs

```bash
# Enable PDF generation
export ENABLE_PDF_EXPORT=1

# Build with PDFs
mkdocs build

# PDFs generated in site/assets/downloads/
```

---

## Contribution Workflow

### 1. Create a Branch

```bash
git checkout -b docs/your-improvement-description
```

### 2. Make Changes

- Edit documentation files
- Add images to appropriate directories
- Update navigation in `mkdocs.yml` if needed

### 3. Test Locally

```bash
# Preview
mkdocs serve

# Build with strict mode
mkdocs build --strict

# Check for broken links
```

### 4. Update Metadata

- Update "Last Updated" dates if present
- Update version information if applicable
- Add yourself to contributors if not listed

### 5. Commit Changes

```bash
git add docs/
git commit -m "docs: Brief description of changes

Detailed description of what was changed and why.

Fixes #issue-number (if applicable)"
```

### 6. Push and Create PR

```bash
git push origin docs/your-improvement-description
```

Create a pull request on GitHub with:
- Clear title describing the change
- Description of what was changed and why
- Screenshots for visual changes
- Checklist of verification steps completed

---

## Style Guide

### Language and Tone

1. **Clear and concise**: Get to the point quickly
2. **Active voice**: "Click the button" not "The button should be clicked"
3. **Present tense**: "The system creates" not "The system will create"
4. **Avoid jargon**: Explain technical terms when first used
5. **Be inclusive**: Use "they" instead of "he/she"

### Formatting

1. **Bold** for UI elements: "Click **Save**"
2. *Italic* for emphasis: "This is *very* important"
3. `Code` for:
   - File names: `pom.xml`
   - Class names: `RulesEngine`
   - Commands: `mvn clean install`
   - Configuration values: `server.port=8080`

### Examples

Good:
```markdown
To install OpenL Tablets, download the WAR file and deploy it to your application server.
```

Bad:
```markdown
OpenL Tablets installation can be performed by downloading the WAR file and then the
WAR file should be deployed to the application server that you have configured.
```

### Line Length

- Soft limit: ~100-120 characters per line
- Hard limit: ~150 characters for readability
- Break long sentences into multiple lines

### Lists

Use bullet lists for:
- Unordered items
- Features
- Options

Use numbered lists for:
1. Sequential steps
2. Procedures
3. Priorities

---

## Common Tasks

### Adding a New Page

1. Create the markdown file in the appropriate directory
2. Add images to `docs/assets/images/[guide-name]/`
3. Update `mkdocs.yml` navigation:

```yaml
nav:
  - User Guides:
      - user-guides/index.md
      - New Guide: user-guides/new-guide.md
```

4. Add cross-references from related pages
5. Build and test

### Splitting a Large File

1. Analyze the structure and identify logical sections
2. Create new files for each section
3. Create an `index.md` with navigation overview
4. Move content to new files
5. Update image paths (may need `../../` or `../../../`)
6. Update `mkdocs.yml` with hierarchical navigation
7. Add cross-references between sections
8. Validate all links work

Example structure:
```
Before: guide.md (2000 lines)

After:
guide/
├── index.md (navigation)
├── section1.md
├── section2.md
└── section3.md
```

### Adding Images

1. Optimize the image (compress, reasonable dimensions)
2. Save to `docs/assets/images/[guide-name]/descriptive-name.png`
3. Reference in markdown:

```markdown
![Descriptive alt text](../../assets/images/webstudio/screenshot.png)
```

4. Verify the image displays correctly in preview

### Updating Navigation

Edit `mkdocs.yml` in the repository root:

```yaml
nav:
  - Section Name:
      - section/index.md
      - Subsection: section/page.md
      - Nested:
          - Subpage: section/nested/subpage.md
```

---

## Review Process

### Self-Review Checklist

Before submitting, verify:

- [ ] Preview looks correct (`mkdocs serve`)
- [ ] Build succeeds with strict mode (`mkdocs build --strict`)
- [ ] All links work (internal and external)
- [ ] All images display correctly
- [ ] Code examples are correct and complete
- [ ] Spelling and grammar checked
- [ ] Navigation updated in `mkdocs.yml` if needed
- [ ] No broken cross-references
- [ ] Follows style guide
- [ ] "Last Updated" dates updated if present

### Peer Review

Pull requests will be reviewed for:

1. **Technical accuracy**: Is the content correct?
2. **Clarity**: Is it easy to understand?
3. **Completeness**: Is anything missing?
4. **Style**: Does it follow guidelines?
5. **Structure**: Is it well-organized?
6. **Links**: Do all references work?

### Addressing Feedback

- Respond to all review comments
- Make requested changes
- Re-request review after updates
- Thank reviewers for their time

---

## Quality Standards

### Documentation Principles

1. **User-focused**: Write for the audience (user, developer, architect)
2. **Practical**: Include real examples and use cases
3. **Current**: Keep documentation up-to-date with code
4. **Searchable**: Use descriptive headings and keywords
5. **Linked**: Cross-reference related content
6. **Visual**: Use diagrams, tables, and screenshots
7. **Tested**: Verify instructions actually work

### Maintenance

Documentation should be updated:
- **Immediately**: When features change
- **Per release**: Version information, screenshots
- **Quarterly**: General review for accuracy
- **As needed**: When issues are reported

---

## Getting Help

### Documentation Questions

- **GitHub Discussions**: Ask questions about documentation
- **GitHub Issues**: Report documentation bugs
- **Pull Requests**: Propose specific improvements

### Resources

- [MkDocs Documentation](https://www.mkdocs.org/)
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)
- [Markdown Guide](https://www.markdownguide.org/)
- [Google Developer Documentation Style Guide](https://developers.google.com/style)

---

## Examples

### Good Documentation Example

```markdown
# Installing OpenL Tablets

## Prerequisites

Before installing OpenL Tablets, ensure you have:

- **Java 21+** installed
- **Apache Tomcat 10** or similar application server
- **1 GB RAM** available
- **2 GB disk space**

## Installation Steps

1. Download the latest WAR file:
   ```bash
   wget https://repo1.maven.org/maven2/org/openl/org.openl.rules.webstudio/6.0.0/webapp.war
   ```

2. Deploy to Tomcat:
   ```bash
   cp webapp.war /opt/tomcat/webapps/
   ```

3. Start Tomcat:
   ```bash
   /opt/tomcat/bin/startup.sh
   ```

4. Access OpenL Studio at: http://localhost:8080/webapp

## Verification

To verify the installation:

1. Open your browser to http://localhost:8080/webapp
2. You should see the OpenL Studio login page
3. Default credentials: admin/admin

![OpenL Studio Login](../../assets/images/installation/login.png)

## Next Steps

- [Quick Start Tutorial](../demo-package/)
- [Configuration Guide](../../configuration/)
- [User Guide](../webstudio/)
```

---

## Questions?

If you have questions about contributing to documentation:

- Check this guide first
- Search [existing issues](https://github.com/openl-tablets/openl-tablets/issues?q=label%3Adocumentation)
- Ask in [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- Open a [new issue](https://github.com/openl-tablets/openl-tablets/issues/new) with the `documentation` label

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Documentation Team

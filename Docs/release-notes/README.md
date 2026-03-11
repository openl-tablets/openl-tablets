# Release Notes

This folder contains release notes for OpenL Tablets versions.

## Structure

Each version has its own folder containing the release notes and associated assets:

```
release-notes/
├── README.md                    # This file
├── index.md                     # Index page, Jekyll template for auto-generating release list
├── 6.0.0/
│   ├── index.md                 # Release notes for 6.0.0
│   ├── migration.md             # Migration notes for 6.0.0 if necessary
│   └── images/                  # Screenshots and diagrams for 6.0.0 if necessary
├── 6.1.0/
│   ├── index.md
│   └── images/
└── ...
```

## Folder Naming Convention

- `{major}.{minor}.{patch}/` - One folder per release version
- Examples: `6.0.0/`, `6.1.0/`, `5.27.0/`

## Folder Contents

Each version folder contains:

| File/Folder    | Purpose                                                                  |
|----------------|--------------------------------------------------------------------------|
| `index.md`     | Main release notes document                                              |
| `migration.md` | Migration notes document, steps for upgrading from the previous versions |
| `images/`      | Screenshots, diagrams, and other visual assets                           |

## Template

Each `index.md` should include content structured as follows:

1. Front Matter block must have
      * title - the title with version number (e.g., "OpenL Tablets 6.0.0 Release Notes")
      * date - the release date in YYYY-MM-DD format
      * description - a brief summary of the release (up to 50 words)
   ```yaml
   ---
   title: "OpenL Tablets <version> Release Notes"
   date: "2026-07-12"
   description: "<What's new and significant changes in this release in few sentences (up to 50 words)>"
   ---
   ```
2. No duplicate title heading — body starts directly with the intro paragraph
3. Use `##` headings for each feature/section:
   - **Highlights** - Key features and changes
   - **New Features** - Detailed list of new features
   - **Improvements** - Enhancements to existing functionality
   - **Bug Fixes** - Issues resolved in this release
   - **Deprecations** - Features marked for removal in future versions
   - **Breaking Changes** - Changes that require user action
   - **Library Updates** - exact version updates for all libraries, presented in a table format with columns: Library, Version
   - **Known Issues** - Known problems in this release
4. Do not include migration steps and reference to migration.md in index.md — they belong in migration.md only

Optional `migration.md` should include:
1. Front Matter block must have
   ```yaml
   ---
   title: "OpenL Tablets <version> Migration Notes"
   ---
   ```
2. Role-based pointers for different user groups (Rules Authors, Developers, Administrators)
3. Environment and dependency changes that may affect migration or integration (e.g., Java version, library updates)
4. End-to-end migration steps, including any necessary code changes, configuration updates, and testing recommendations
5. SQL statements for database migration steps if applicable

## Formatting Guidelines

- Do not generalize or simplify technical details — preserve the specific language and details from the source to maintain accuracy and integrity of the information presented to users
- Use fenced code blocks with language identifiers for syntax highlighting (e.g., ```java, ```sql)
- Include code snippets verbatim, preserving formatting and indentation
- Use inlined code formatting for class names, method names, configuration properties, and other technical terms (e.g., `security.default-group`, `isProvideRuntimeContext`)
- Use tables for structured data (e.g., library updates, deprecated items, changes)
- Use bold or italics to emphasize important information or key points, but avoid overusing them to maintain readability
- Do not use bold in the heading titles (e.g., ## **New Features** should be ## New Features)
- Use official product names for libraries include casing (e.g., CXF, Groovy, Log4j, Jetty, gRPC, OpenSAML, POI, JGit, Commons IO)
- Use the latest release notes (e.g. 6.0.0) as a reference for tone, formatting, and style to ensure consistency across all versions

## Adding Images

Store images in the version's `images/` folder and reference them with relative paths:

```markdown
![Feature Screenshot](images/new-feature.png)
```

Image naming conventions:
- Use lowercase with hyphens: `decision-table-editor.png`
- Be descriptive: `studio-dark-mode-toggle.png`
- Include context if needed: `before-migration.png`, `after-migration.png`

## Contributing

When adding release notes for a new version:

1. Create a new folder: `{version}/`
2. Copy the template from an existing version or create `index.md`
3. Create the `images/` subfolder
4. Add content following the template structure
5. Include links to relevant issues/PRs where applicable
6. Review for accuracy before publishing

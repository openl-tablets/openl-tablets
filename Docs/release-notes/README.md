# Release Notes

This folder contains release notes for OpenL Tablets versions.

## Structure

Each version has its own folder containing the release notes and associated assets:

```
release-notes/
├── README.md                    # This file
├── 6.0.0/
│   ├── index.md                 # Release notes for 6.0.0
│   └── images/                  # Screenshots and diagrams for 6.0.0
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

| File/Folder | Purpose |
|-------------|---------|
| `index.md` | Main release notes document |
| `images/` | Screenshots, diagrams, and other visual assets |

## Template

Each `index.md` should include:

1. **Version** - The version number in the title
2. **Release Date** - When the version was released
3. **Highlights** - Key features and changes
4. **New Features** - Detailed list of new features
5. **Improvements** - Enhancements to existing functionality
6. **Bug Fixes** - Issues resolved in this release
7. **Breaking Changes** - Changes that require user action
8. **Deprecations** - Features marked for removal in future versions
9. **Known Issues** - Known problems in this release
10. **Upgrade Notes** - Steps for upgrading from previous versions

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

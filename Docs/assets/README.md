# Documentation Assets

This directory contains all assets for the OpenL Tablets documentation including images, CSS, JavaScript, and downloadable files.

## Directory Structure

```
docs/assets/
├── images/              # Documentation images
│   ├── general/         # General-purpose images
│   ├── installation/    # Installation guide images
│   ├── demo-package/    # Demo package guide images
│   ├── reference/       # Reference guide images
│   ├── webstudio/       # OpenL Studio guide images
│   └── rule-services/   # Rule services guide images
├── css/                 # Custom stylesheets
├── js/                  # Custom JavaScript
├── downloads/           # Downloadable files (PDFs, etc.)
└── logo.svg            # OpenL Tablets logo
```

## Image Organization

### General Images
Location: `images/general/`

- **OpenLHome.png** (322KB) - OpenL Tablets home page screenshot
- **edit_github.png** (636 bytes) - GitHub edit icon
- **versions_flyout_closed.png** (1.8KB) - Version selector closed state
- **versions_flyout_open.png** (16KB) - Version selector open state

### Guide-Specific Images
Images are organized by their corresponding guide:

- **installation/** - Images for the Installation Guide (Batch 3)
- **demo-package/** - Images for the Demo Package Guide (Batch 3)
  - OpenLHome.png - Copied from general for guide use
- **reference/** - Images for the Reference Guide (Batch 4)
- **webstudio/** - Images for the OpenL Studio User Guide (Batch 4)
- **rule-services/** - Images for the Rule Services Guide (Batch 5)

## Stylesheets

### custom.css
**Location**: `css/custom.css`
**Purpose**: OpenL Tablets-specific custom styling for MkDocs Material theme

Features:
- Custom color scheme (OpenL blue primary)
- Table styling
- Code block enhancements
- Status badges
- Responsive image handling
- Print styles

### openldocs-extra.css
**Location**: `css/openldocs-extra.css`
**Source**: Migrated from OpenLdocs repository
**Purpose**: Legacy styling from original OpenLdocs documentation

Contains:
- Font size customizations for headings
- Navigation menu styling
- Dark mode support
- Accessibility improvements

**Note**: Some styles may need adjustment as they were designed for a different theme.

## JavaScript

### custom.js
**Location**: `js/custom.js`
**Purpose**: Custom JavaScript enhancements

Features:
- External link handling (open in new tab)
- Smooth scroll for anchor links
- Copy button feedback

## Logo

### logo.svg
**Location**: `logo.svg` (root of assets/)
**Source**: Migrated from OpenLdocs repository
**Format**: SVG (Scalable Vector Graphics)
**Dimensions**: 155x175 viewBox
**Colors**: Green (#2A7F32), Purple (#947BD3), Light Green (#6DBE45), Blue (#0F4093)

Used in:
- Site header (MkDocs Material theme)
- Favicon
- Navigation menu

## Downloads

**Location**: `downloads/`
**Status**: Empty (will be populated in Batch 10)

This directory will contain:
- PDF versions of all guides
- Example configuration files
- Sample projects
- Quick reference cards

## Usage Guidelines

### Adding Images

1. **Choose the appropriate directory**:
   - Guide-specific images → `images/{guide-name}/`
   - General screenshots → `images/general/`

2. **Naming conventions**:
   - Use descriptive names: `webstudio-project-creation.png`
   - Use kebab-case: `rule-table-editor.png`
   - Include context: `installation-docker-compose-up.png`

3. **Optimize images**:
   - Use PNG for screenshots
   - Use SVG for diagrams and icons
   - Compress images before committing
   - Maximum recommended width: 1920px

4. **Reference in markdown**:
   ```markdown
   ![Alt text](../assets/images/general/example.png)
   ```

### Adding CSS

1. **Primary styling** → Edit `css/custom.css`
2. **Theme overrides** → Use CSS variables in `custom.css`
3. **Legacy compatibility** → Reference `css/openldocs-extra.css`

### Adding JavaScript

1. **Add functions to** `js/custom.js`
2. **Ensure no jQuery dependencies** (Material theme uses vanilla JS)
3. **Test in both light and dark modes**

## Migration Status

### Batch 2: Asset Migration ✅
- [x] Logo migrated from OpenLdocs
- [x] PNG images migrated (4 files)
- [x] Custom CSS migrated
- [x] Directory structure created
- [x] README documentation created

### Future Batches

**Batch 3**: Installation & Demo guide images will be added
**Batch 4**: Reference & OpenL Studio guide images will be added
**Batch 5**: Rule Services guide images will be added
**Batch 10**: PDF downloads will be generated

## Asset Sources

### From OpenLdocs Repository
**Repository**: https://github.com/EISTW/OpenLdocs

Migrated assets:
- `docs/assets/logo.svg` → `logo.svg`
- `docs/css/extra.css` → `css/openldocs-extra.css`
- `docs/img/*.png` → `images/general/*.png`

### Created for openl-tablets
- `css/custom.css` - New custom styling
- `js/custom.js` - New custom JavaScript

## Best Practices

1. **Keep assets organized** - Use appropriate subdirectories
2. **Optimize file sizes** - Compress images and minify code
3. **Version control** - Commit binary assets only when necessary
4. **Documentation** - Update this README when adding new asset types
5. **Accessibility** - Always provide alt text for images
6. **Performance** - Lazy-load large images when possible

## Support

For questions about documentation assets:
- See [Contributing Guide](../../CONTRIBUTING.md)
- Open an issue: https://github.com/openl-tablets/openl-tablets/issues

---

**Last Updated**: 2025-11-05
**Migration Batch**: Batch 2 - Asset Migration
**Status**: Complete

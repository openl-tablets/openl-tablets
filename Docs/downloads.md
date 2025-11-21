# Documentation Downloads

Download complete documentation guides in PDF format for offline reading and reference.

## About PDF Downloads

All OpenL Tablets documentation is available as downloadable PDFs, generated from the latest documentation sources. These PDFs are ideal for:

- **Offline reading** - Access documentation without internet connection
- **Printing** - Create physical copies for reference
- **Archiving** - Keep documentation versions for specific releases
- **Sharing** - Distribute documentation within your organization

## Available Downloads

### User Guides

#### Installation Guide
Complete guide for installing OpenL Tablets in various environments.

- **Topics**: System requirements, installation procedures, configuration, verification
- **Audience**: System administrators, DevOps engineers
- **Format**: PDF

#### Demo Package Guide
Quick start guide using the demo package to explore OpenL Tablets features.

- **Topics**: Demo setup, exploring examples, trying features
- **Audience**: New users, evaluators
- **Format**: PDF

#### Reference Guide
Comprehensive reference documentation for OpenL Tablets features and syntax.

- **Topics**: Table types, data types, syntax reference, expressions
- **Audience**: Rule authors, developers
- **Format**: PDF

#### WebStudio User Guide
Complete guide to using WebStudio for creating and managing business rules.

- **Topics**: Interface overview, rules editor, testing, repository management, administration
- **Audience**: Business analysts, rule authors, administrators
- **Format**: PDF

#### Rule Services Guide
Guide for deploying and configuring OpenL Tablets Rule Services.

- **Topics**: Core concepts, configuration, advanced features, OpenAPI support
- **Audience**: Developers, DevOps engineers, architects
- **Format**: PDF

### Developer Guides

#### Developer Guide
Comprehensive guide for developers working with OpenL Tablets.

- **Topics**: Architecture, rules projects, business language, customization
- **Audience**: Developers, technical leads
- **Format**: PDF

#### Integration Guides
Patterns for integrating OpenL Tablets with various frameworks and systems.

- **Topics**: Spring, Activiti, OpenAPI/REST, Apache CXF, OpenTelemetry
- **Audience**: Integration developers, architects
- **Format**: PDF

### Configuration & Operations

#### Configuration Guide
System configuration reference for OpenL Tablets.

- **Topics**: Configuration files, parameters, environment settings
- **Audience**: System administrators, DevOps engineers
- **Format**: PDF

#### Security Guide
Security configuration and best practices.

- **Topics**: Authentication, authorization, user management, SSL/TLS
- **Audience**: Security engineers, administrators
- **Format**: PDF

#### Production Deployment Guide
Best practices for deploying OpenL Tablets in production.

- **Topics**: Environment setup, databases, clustering, monitoring
- **Audience**: DevOps engineers, architects, administrators
- **Format**: PDF

## How to Download

### From Read the Docs

When viewing documentation on Read the Docs, PDFs are automatically available:

1. Navigate to any documentation page
2. Look for the **"v: latest"** dropdown in the bottom left corner
3. Click on it to reveal download options
4. Select **"PDF"** to download the complete documentation

### From GitHub Releases

PDF versions are also available with each release:

1. Visit the [Releases page](https://github.com/openl-tablets/openl-tablets/releases)
2. Download documentation PDFs attached to the release
3. PDFs are version-specific to match the release

## Generating PDFs Locally

To generate PDFs from the documentation sources:

```bash
# Install dependencies
cd docs
pip install -r requirements.txt

# Enable PDF export and build
export ENABLE_PDF_EXPORT=1
mkdocs build

# PDFs will be in site/assets/downloads/
```

For more details, see the [Downloads README](assets/downloads/README.md).

## PDF Features

All generated PDFs include:

- **Table of Contents** - Hierarchical navigation
- **Internal Links** - Cross-references between sections
- **Syntax Highlighting** - Colored code examples
- **Images** - All diagrams and screenshots
- **Cover Page** - Title, version, and copyright
- **Page Numbers** - For easy reference
- **Headers/Footers** - Chapter and section context

## Version Information

PDFs are generated for:

- **Latest** - Most recent development version
- **Stable Releases** - Each official release version
- **Legacy Versions** - Previous major versions (archive)

## File Sizes

Typical PDF sizes:

| Guide | Approximate Size |
|-------|-----------------|
| Installation Guide | 2-3 MB |
| Demo Package Guide | 3-4 MB |
| Reference Guide | 5-7 MB |
| WebStudio User Guide | 8-10 MB |
| Rule Services Guide | 4-5 MB |
| Developer Guide | 3-4 MB |
| Integration Guides | 2-3 MB |
| Configuration Guide | 1-2 MB |
| Security Guide | 1-2 MB |
| Production Deployment | 2-3 MB |

*Sizes vary based on content and embedded images.*

## Need Help?

If you have issues downloading or viewing PDFs:

- Check that you have a PDF reader installed
- Try a different browser if downloads fail
- For generation issues, see [troubleshooting](onboarding/troubleshooting.md)
- Report problems on [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)

## Related Resources

- [Documentation Home](README.md)
- [Getting Started](onboarding/codebase-tour.md)
- [Contributing to Documentation](documentation-plan.md)
- [API Reference](api/public-api-reference.md)

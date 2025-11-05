# OpenL Tablets Documentation Downloads

This directory contains downloadable documentation files in PDF format.

## Available Downloads

### User Guides
- **Installation Guide** - Step-by-step installation instructions
- **Demo Package Guide** - Getting started with demo packages
- **Reference Guide** - Comprehensive OpenL Tablets reference
- **WebStudio User Guide** - Complete guide to using OpenL Studio
- **Rule Services Guide** - Deploying and configuring rule services

### Developer Guides
- **Developer Guide** - Complete guide for developers working with OpenL Tablets
- **Integration Guides** - Integration patterns with various frameworks

### Configuration & Operations
- **Configuration Guide** - System configuration reference
- **Security Guide** - Security configuration and best practices
- **Production Deployment Guide** - Production deployment patterns

## Generating PDFs

PDFs are generated automatically when the documentation is built with PDF export enabled.

### Manual PDF Generation

To generate PDFs locally:

1. Install dependencies:
   ```bash
   cd docs
   pip install -r requirements.txt
   ```

2. Enable PDF export and build:
   ```bash
   export ENABLE_PDF_EXPORT=1
   mkdocs build
   ```

3. PDFs will be generated in `site/assets/downloads/`

### Read the Docs

PDFs are automatically available on the hosted documentation site at:
- https://openl-tablets.readthedocs.io

Look for the "Download" option in the bottom left corner of any documentation page.

## Notes

- PDFs are generated from the latest documentation sources
- PDF generation requires additional dependencies (see `requirements.txt`)
- Generated PDFs include the complete documentation with table of contents and internal links
- File sizes vary depending on content and embedded images

## Contributing

To add new downloadable content:
1. Place files in this directory
2. Update the downloads page at `docs/downloads.md`
3. Ensure files are referenced in the documentation

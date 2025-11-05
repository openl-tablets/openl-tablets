# Documentation Migration Guide

This guide helps users navigate from the legacy `Docs/` directory to the new unified documentation in `docs/`.

## Quick Navigation

| What you're looking for | New Location |
|------------------------|--------------|
| **Documentation Home** | [`docs/README.md`](README.md) or https://openl-tablets.readthedocs.io |
| **User Guides** | [`docs/user-guides/`](user-guides/) |
| **Deployment Guides** | [`docs/deployment/`](deployment/) |
| **Developer Guides** | [`docs/developer-guides/`](developer-guides/) |
| **Integration Guides** | [`docs/integration-guides/`](integration-guides/) |
| **Configuration** | [`docs/configuration/`](configuration/) |
| **Examples** | [`docs/examples/`](examples/) |

---

## File Mapping: Old → New

### Configuration & Deployment

| Old Path (`Docs/`) | New Path (`docs/`) | Notes |
|-------------------|-------------------|-------|
| `Configuration.md` | [`configuration/overview.md`](configuration/overview.md) | Enhanced with examples |
| `Security.md` | [`configuration/security.md`](configuration/security.md) | Expanded |
| `Production_Deployment.md` | [`deployment/index.md`](deployment/) | Now comprehensive deployment section |
| `production-deployment/` | [`deployment/`](deployment/) + [`examples/production/`](examples/production/) | Split into deployment guides and examples |

### Integration Guides

| Old Path (`Docs/`) | New Path (`docs/`) | Notes |
|-------------------|-------------------|-------|
| `OpenAPI.md` | [`integration-guides/openapi.md`](integration-guides/openapi.md) | Enhanced |
| `Spring_extension.md` | [`integration-guides/spring.md`](integration-guides/spring.md) | Expanded |
| `CXF_customization.md` | [`integration-guides/cxf.md`](integration-guides/cxf.md) | Updated |
| `OpenTelemetry.md` | [`integration-guides/opentelemetry.md`](integration-guides/opentelemetry.md) | Enhanced |
| - | [`integration-guides/activiti.md`](integration-guides/activiti.md) | New! |

### Developer Documentation

| Old Path (`Docs/`) | New Path (`docs/`) | Notes |
|-------------------|-------------------|-------|
| `Invoking_OpenL.md` | [`developer-guides/introduction.md`](developer-guides/introduction.md) | Part of comprehensive developer guides |
| `developer-guide/` | [`developer-guides/`](developer-guides/) | Expanded and restructured |
| - | [`onboarding/`](onboarding/) | New! Codebase tour, dev setup, common tasks |
| - | [`architecture/`](architecture/) | New! Technology stack, dependencies, architecture |

### Other Documentation

| Old Path (`Docs/`) | New Path (`docs/`) | Notes |
|-------------------|-------------------|-------|
| `release.md` | Main repo files (CONTRIBUTING.md, etc.) | Integrated into contribution workflow |
| `OpenL_Studio_extension.md` | [`developer-guides/extending.md`](developer-guides/extending.md) | Part of developer guides |

---

## New Documentation Structure

The new `docs/` directory provides a comprehensive, well-organized documentation structure:

```
docs/
├── README.md                       # Documentation home
├── MIGRATION_PLAN.md               # Migration tracking (94% complete)
├── MIGRATION_GUIDE.md              # This file
├── downloads.md                    # PDF downloads
│
├── onboarding/                     # Getting Started (NEW!)
│   ├── codebase-tour.md
│   ├── development-setup.md
│   ├── common-tasks.md
│   └── troubleshooting.md
│
├── user-guides/                    # User Documentation
│   ├── installation/               # Installation for dev/test
│   ├── demo-package/               # Demo package guide
│   ├── reference/                  # Reference guide (24 files)
│   ├── webstudio/                  # WebStudio user guide
│   └── rule-services/              # Rule Services guide
│
├── deployment/                     # Production Deployment (NEW!)
│   ├── index.md                    # Deployment overview
│   ├── docker/                     # Docker deployment (9 files)
│   ├── kubernetes/                 # Kubernetes deployment (7 files)
│   ├── cloud/                      # Cloud deployment (AWS, Azure)
│   └── vm/                         # VM deployment
│
├── developer-guides/               # Developer Documentation
│   ├── introduction.md
│   ├── rules-projects.md
│   ├── business-language.md
│   ├── externalized-config.md
│   └── extending.md
│
├── integration-guides/             # Integration Documentation
│   ├── activiti.md
│   ├── spring.md
│   ├── openapi.md
│   ├── cxf.md
│   └── opentelemetry.md
│
├── configuration/                  # Configuration Guides
│   ├── overview.md
│   ├── security.md
│   └── deployment.md
│
├── architecture/                   # Architecture Documentation (NEW!)
│   ├── technology-stack.md
│   ├── dependencies.md
│   └── legacy-system-map.md
│
├── analysis/                       # Module Analysis (NEW!)
│   ├── dev-module-overview.md
│   ├── repository-layer-overview.md
│   └── studio-wsfrontend-util-overview.md
│
├── guides/                         # Technical Guides
│   ├── testing-guide.md
│   ├── migration-guide.md
│   └── performance-tuning.md
│
├── operations/                     # Operations (NEW!)
│   ├── ci-cd.md
│   └── docker-guide.md
│
├── examples/                       # Examples
│   ├── index.md
│   └── production/
│
└── api/                            # API Reference
    └── public-api-reference.md
```

---

## Key Improvements

### 1. Unified Structure

- All documentation in one place (`docs/`)
- Consistent organization and navigation
- MkDocs + Material theme for modern UI
- Read the Docs hosting

### 2. Enhanced Content

- **30+ new files** with comprehensive deployment guides
- **Multi-file structure** for large guides (Reference: 24 files, Installation: 8 files)
- **Onboarding section** for new developers
- **Architecture documentation** for understanding the codebase

### 3. Production-Ready Deployment

New comprehensive deployment documentation:
- **Docker**: 3 deployment patterns (simple, multi-container, full HA)
- **Kubernetes**: Complete manifests and Helm guide
- **Cloud**: AWS and Azure with Terraform
- **VM**: Ubuntu, RHEL, traditional Tomcat

### 4. Better Navigation

- **Hierarchical navigation** with clear sections
- **Search functionality** (MkDocs built-in)
- **Cross-references** between related documents
- **Table of contents** in each document

### 5. Modern Tooling

- **MkDocs**: Static site generator
- **Material Theme**: Modern, responsive UI
- **Read the Docs**: Professional hosting
- **PDF Generation**: Available for offline reading

---

## Migration Timeline

| Date | Milestone | Status |
|------|-----------|--------|
| 2025-11-05 | Migration started | ✅ Complete |
| 2025-11-05 | Phase 1: Foundation & User Guides | ✅ Complete |
| 2025-11-05 | Phase 2: Developer & Integration Docs | ✅ Complete |
| 2025-11-05 | Phase 3: Enhanced Deployment Docs | ✅ Complete |
| 2025-11-05 | Phase 4: Deprecation & Cleanup | 🔄 In Progress |
| TBD | Remove `Docs/` directory | ⏳ Planned |

**Current Status**: 94% Complete (15/16 batches)

---

## Finding Specific Content

### I'm looking for...

**Configuration options**:
- **Old**: `Docs/Configuration.md`
- **New**: [`docs/configuration/overview.md`](configuration/overview.md)
- **Also see**: [`docs/configuration/deployment.md`](configuration/deployment.md)

**Production deployment**:
- **Old**: `Docs/Production_Deployment.md`
- **New**: [`docs/deployment/`](deployment/) (comprehensive guides)
  - Docker: [`docs/deployment/docker/`](deployment/docker/)
  - Kubernetes: [`docs/deployment/kubernetes/`](deployment/kubernetes/)
  - Cloud: [`docs/deployment/cloud/`](deployment/cloud/)

**Security setup**:
- **Old**: `Docs/Security.md`
- **New**: [`docs/configuration/security.md`](configuration/security.md)

**REST API / OpenAPI**:
- **Old**: `Docs/OpenAPI.md`
- **New**: [`docs/integration-guides/openapi.md`](integration-guides/openapi.md)

**Spring integration**:
- **Old**: `Docs/Spring_extension.md`
- **New**: [`docs/integration-guides/spring.md`](integration-guides/spring.md)

**CXF customization**:
- **Old**: `Docs/CXF_customization.md`
- **New**: [`docs/integration-guides/cxf.md`](integration-guides/cxf.md)

**Monitoring / Observability**:
- **Old**: `Docs/OpenTelemetry.md`
- **New**: [`docs/integration-guides/opentelemetry.md`](integration-guides/opentelemetry.md)

**Developer setup**:
- **Old**: `Docs/developer-guide/`
- **New**: [`docs/onboarding/`](onboarding/) + [`docs/developer-guides/`](developer-guides/)

**API usage**:
- **Old**: `Docs/Invoking_OpenL.md`
- **New**: [`docs/developer-guides/introduction.md`](developer-guides/introduction.md)

---

## Need Help?

### Can't find what you're looking for?

1. **Browse the new docs**: Start at [`docs/README.md`](README.md)
2. **Check the migration plan**: See [`docs/MIGRATION_PLAN.md`](MIGRATION_PLAN.md) for detailed migration tracking
3. **Use search**: When browsing on Read the Docs, use the search feature
4. **Check the old location**: Legacy docs in `Docs/` still available (for now)
5. **Open an issue**: [Report missing documentation](https://github.com/openl-tablets/openl-tablets/issues/new?labels=documentation)

### Broken Links?

If you find broken links or references to the old `Docs/` directory:

1. Check this migration guide for the new location
2. Update your bookmarks to the new paths
3. Report the broken link as an issue

---

## For Contributors

### Updating Documentation

**Always update the new location** (`docs/`):

```bash
# ✅ Correct - update new docs
vim docs/configuration/security.md

# ❌ Incorrect - don't update old docs
# vim Docs/Security.md
```

### Adding New Documentation

Add all new documentation to `docs/`:

```bash
# ✅ Correct - new docs go in docs/
vim docs/guides/new-feature-guide.md

# Update mkdocs.yml navigation
vim mkdocs.yml
```

### Documentation Standards

See [`docs/CONTRIBUTING_DOCS.md`](CONTRIBUTING_DOCS.md) for:
- Writing guidelines
- Structure conventions
- Building and previewing docs
- Contribution workflow

---

## Frequently Asked Questions

### Why was the documentation migrated?

The migration consolidates documentation from multiple locations into a unified structure with modern tooling, better navigation, and improved discoverability.

### Will the old `Docs/` directory be removed?

Yes, the `Docs/` directory will be removed in a future release after allowing time for users to transition to the new documentation.

### What if I have bookmarks to old documentation?

Update your bookmarks to the new paths using this migration guide as a reference.

### Can I still access the old documentation?

Yes, the old `Docs/` directory is still present but marked as deprecated. However, it will not receive updates.

### How do I build the new documentation locally?

```bash
# Install dependencies
pip install -r docs/requirements.txt

# Build documentation
mkdocs build

# Preview documentation
mkdocs serve
```

Then visit http://localhost:8000

---

**Last Updated**: 2025-11-05
**Migration Status**: 94% Complete (15/16 batches)
**Version**: 6.0.0-SNAPSHOT

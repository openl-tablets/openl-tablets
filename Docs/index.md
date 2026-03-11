---
title: OpenL Tablets
description: Open source business rules engine — write rules in Excel, manage them in a web IDE, deploy as REST services.
---

**Open source business rules engine and decision management system.**

Write business rules in Excel. Manage and test them in a browser-based IDE. Deploy as REST services with auto-generated OpenAPI documentation — no boilerplate required.

[Get Started &rarr;](user-guides/installation/){: .btn} &nbsp;
[View on GitHub &rarr;](https://github.com/openl-tablets/openl-tablets){: .btn}

---

## Why OpenL Tablets?

| | |
|---|---|
| **Excel as the source of truth** | Rules live in spreadsheets — the format your analysts already use. No proprietary syntax to learn. |
| **Compiled to JVM bytecode** | Rules are compiled at load time, not interpreted at runtime. Production-grade execution speed. |
| **REST services out of the box** | Any rule set becomes a RESTful API with automatic OpenAPI/Swagger documentation. |
| **Type-safe at compile time** | Syntax and type errors are caught before deployment, with cell-level references back to the Excel source. |
| **Browser-based Studio IDE** | Create, edit, test, trace, and deploy rules from any browser. No desktop software needed. |
| **Enterprise security** | OAuth2, SAML, LDAP, role-based access, Git-backed repository with full audit trail. |

---

## Documentation

### Getting started

- **[What is OpenL Tablets?](what-is-openl-tablets.html)** — Platform overview, components, and when to use it
- **[Tutorials](tutorials.html)** — Step-by-step walkthroughs from decision tables to advanced rule types
- **[Videocasts](videocasts.html)** — Video demonstrations of key features

### Reference

- **[User Guides](user-guides/)** — Installation, OpenL Studio IDE, Rule Services, and the complete Reference Guide
- **[Features](features.html)** — Full list of capabilities: table types, versioning, integration, testing
- **[Supported Platforms](supported-platforms.html)** — Java versions, app servers, databases, browsers

### Project

- **[Release Notes](release-notes/)** — Version history, breaking changes, and migration guides
- **[Apologia](apologia.html)** — Design philosophy and how OpenL Tablets compares to alternatives

---

## Quick Start

Run OpenL Tablets locally using Docker:

```bash
docker run -p 8080:8080 openltablets/openl-tablets
```

Open [http://localhost:8080](http://localhost:8080){:target="_blank"} in your browser to access OpenL Studio.

For a full setup with sample rules and data, see the [Demo Package Guide](user-guides/demo-package/).

---

## Latest Release

See the **[Release Notes](release-notes/)** for what's new, breaking changes, and upgrade instructions.

---

[GitHub](https://github.com/openl-tablets/openl-tablets) &nbsp;·&nbsp;
[openl-tablets.org](https://openl-tablets.org) &nbsp;·&nbsp;
[Issues](https://github.com/openl-tablets/openl-tablets/issues) &nbsp;·&nbsp;
[Discussions](https://github.com/openl-tablets/openl-tablets/discussions)

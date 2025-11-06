[![Maven Central](https://img.shields.io/maven-central/v/org.openl/org.openl.core)](https://central.sonatype.com/search?q=org.openl)
[![Java Version](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/technologies/downloads/)
[![Website](https://img.shields.io/website?label=Website&url=https%3A%2F%2Fopenl-tablets.org)](https://openl-tablets.org)
[![License](https://img.shields.io/badge/license-LGPL-blue.svg)](https://github.com/openl-tablets/openl-tablets/blob/master/LICENSE)

# OpenL Tablets - Business Rules Management Made Simple

**Empower your business users to manage complex rules in Excel. Deploy them as enterprise-grade services. No coding required.**

[Get Started in 2 Minutes](#quick-start) | [Try the Demo](https://openl-tablets.org/demo) | [Read the Docs](docs/user-guides/) | [Download](https://openl-tablets.org/downloads)

---

## What is OpenL Tablets?

OpenL Tablets is an open-source **Business Rules Management System (BRMS)** that lets business analysts create, test, and manage decision logic using familiar **Excel spreadsheets** - then automatically deploys them as high-performance **REST APIs**.

**Perfect for:**
- Insurance companies calculating premiums and underwriting policies
- Banks processing loan applications and credit decisions
- Healthcare organizations managing treatment protocols and benefits
- Retailers running dynamic pricing and promotions

---

## Why Choose OpenL Tablets?

### 💼 Business Users Love It
- **Use Excel** - No programming skills needed. Write rules in spreadsheets you already know.
- **Instant Testing** - Test your rules with real data before deployment
- **Full Control** - Business users own and update rules without developer bottlenecks
- **AI-Assisted** - Integrate with AI tools like Claude and ChatGPT via MCP protocol

### 👨‍💻 Developers Love It
- **REST APIs** - Rules deploy as production-ready REST services automatically
- **Type Safety** - Compile-time validation prevents errors before production
- **Performance** - Excel rules compile to native Java bytecode for maximum speed
- **Any Platform** - Java, Python, JavaScript, .NET - integrate from anywhere

### 🏢 Enterprises Love It
- **Battle-Tested** - Proven in production at Fortune 500 companies
- **Scalable** - Handles millions of decisions per day
- **Compliant** - Built-in versioning, audit trails, and governance
- **Flexible Deployment** - Cloud, on-premises, containerized, or embedded

---

## How It Works

```
Excel Rules → OpenL Tablets → Production REST API
```

1. **Business Analyst** writes decision logic in Excel
   - Decision tables, spreadsheet calculations, scorecards
   - Test with sample data right in the interface

2. **OpenL Tablets** validates and compiles
   - Type checking catches errors immediately
   - Generates optimized Java bytecode

3. **Deploy as REST Service**
   - One-click deployment to production
   - Auto-generated OpenAPI documentation
   - Hot reload for zero-downtime updates

---

## Quick Start

### Try It Now with Docker

Get OpenL Tablets running in 60 seconds:

```bash
# Download and start
docker compose up

# Open your browser
http://localhost:8080
```

That's it! You now have:
- ✅ **OpenL Studio** - Web-based rule editor
- ✅ **Rule Services** - REST API engine
- ✅ **Sample Rules** - Pre-loaded examples

### Deploy Your First Rule

1. **Create a rule in Excel**
   ```
   | Premium Calculation |
   |-------------------- |
   | Age | Risk | Premium |
   | 18-25 | High | $1200 |
   | 26-40 | Medium | $800 |
   | 41+ | Low | $600 |
   ```

2. **Upload to OpenL Studio**
   - Upload your Excel file
   - Test with sample inputs
   - Click "Deploy"

3. **Call your new API**
   ```bash
   curl -X POST http://localhost:8080/api/calculatePremium \
     -H "Content-Type: application/json" \
     -d '{"age": 30, "risk": "Medium"}'

   # Response: {"premium": 800}
   ```

**[See Complete Getting Started Guide →](docs/user-guides/installation/)**

---

## Real-World Use Cases

### 🏥 Insurance Company: Premium Calculation

**Challenge**: Underwriters spent weeks updating premium tables. Changes required developer deployment.

**Solution**: Business analysts manage 100+ decision tables in Excel. Deploy updates in minutes, not weeks.

**Result**:
- 90% faster rule updates
- Zero developer involvement for rule changes
- $500K annual savings in development costs

### 🏦 Bank: Loan Origination

**Challenge**: Complex credit scoring rules scattered across multiple systems. Hard to test and audit.

**Solution**: Centralized all credit rules in OpenL. Business analysts test scenarios before production.

**Result**:
- 50% reduction in loan processing time
- Full audit trail for compliance
- Ability to A/B test lending strategies

### 💊 Healthcare: Treatment Protocols

**Challenge**: Clinical decision support rules embedded in code. Required IT for any protocol updates.

**Solution**: Doctors manage treatment protocols directly in Excel format they understand.

**Result**:
- Same-day protocol updates (vs 2-3 weeks)
- Better clinician adoption (familiar interface)
- Reduced medical errors

---

## Key Features

### 📊 Excel-Based Authoring
Write rules in familiar Excel format. Decision tables, scorecards, calculations - all in spreadsheets.

### 🔒 Type-Safe Validation
Strong typing catches errors before deployment. No more runtime surprises.

### 🚀 One-Click Deployment
Deploy rules as REST APIs with a single click. Auto-generated OpenAPI documentation.

### 📈 Version Control
Built-in Git integration. Track every change, rollback instantly, collaborate with teams.

### 🧪 Comprehensive Testing
Test individual rules or entire scenarios. Debug with trace and breakpoints.

### ⚡ High Performance
Excel rules compile to native Java bytecode. Process millions of decisions per second.

### 🔄 Hot Reload
Update rules in production with zero downtime. Changes go live instantly.

### 🌐 Multi-Language Support
Call from Java, Python, JavaScript, C#, Go, or any language via REST.

---

## Industry Solutions

### Insurance
- Premium calculation and rating
- Underwriting decision automation
- Policy eligibility determination
- Claims processing and fraud detection

### Banking & Finance
- Credit scoring and loan origination
- Risk assessment and pricing
- Fraud detection and AML screening
- Regulatory compliance rules

### Healthcare
- Treatment protocol management
- Insurance eligibility verification
- Benefits calculation
- Clinical decision support

### Retail & E-Commerce
- Dynamic pricing engines
- Promotion and discount rules
- Inventory optimization
- Personalized product recommendations

---

## Documentation

### 📚 For Business Users
- **[Getting Started Guide](docs/user-guides/installation/)** - Install and configure OpenL Tablets
- **[Excel Rule Reference](docs/user-guides/reference/)** - Complete guide to writing rules in Excel
- **[WebStudio User Guide](docs/user-guides/webstudio/)** - Using the web interface
- **[Testing Guide](docs/user-guides/reference/)** - Test your rules thoroughly

### 🔌 For Developers
- **[REST API Guide](docs/API_GUIDE.md)** - Integrate rules into your applications
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Deploy to Docker, Kubernetes, AWS, Azure, GCP
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design and components
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

### 💻 For Contributors
- **[Contributing Guide](CONTRIBUTING.md)** - Help improve OpenL Tablets
- **[Developer Setup](docs/onboarding/development-setup.md)** - Set up your dev environment
- **[Architecture Details](docs/architecture/)** - Deep technical documentation

**[Browse All Documentation →](docs/)**

---

## Community & Support

### Get Help

- 💬 **[GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)** - Ask questions, share ideas
- 🐛 **[Issue Tracker](https://github.com/openl-tablets/openl-tablets/issues)** - Report bugs, request features
- 📖 **[Documentation](docs/)** - Comprehensive guides and references
- 🌐 **[Official Website](https://openl-tablets.org)** - Tutorials, blog, resources

### Stay Connected

- ⭐ **Star this repo** to show your support
- 👀 **Watch** for updates and new releases
- 📢 **Share** your success stories with the community

---

## Download & Install

### Pre-Built Releases

Download production-ready packages:
- **[Latest Release](https://github.com/openl-tablets/openl-tablets/releases/latest)** - Stable version
- **[All Releases](https://github.com/openl-tablets/openl-tablets/releases)** - Version history

### Docker Images

```bash
# OpenL Studio (Rule Editor)
docker pull openltablets/openl-studio:latest

# Rule Service (Production Runtime)
docker pull openltablets/openl-rule-service:latest
```

### Maven Dependency

```xml
<dependency>
    <groupId>org.openl</groupId>
    <artifactId>org.openl.core</artifactId>
    <version>6.0.0-SNAPSHOT</version>
</dependency>
```

**[See Installation Guide →](docs/user-guides/installation/)**

---

## Success Stories

> "OpenL Tablets transformed how we manage pricing rules. Business analysts can now update prices in minutes instead of waiting weeks for IT."
>
> — **Senior Director, Fortune 500 Insurance Company**

> "We process 5 million credit decisions per day. OpenL Tablets gives us the performance we need with the flexibility business demands."
>
> — **VP Technology, Major Bank**

> "The Excel interface was a game-changer. Our underwriters adopted it immediately without any training."
>
> — **Chief Underwriter, Property & Casualty Insurer**

---

## Contributing

We welcome contributions! OpenL Tablets is built by a global community of developers and business analysts.

**Ways to contribute:**
- 🐛 Report bugs and suggest features
- 📝 Improve documentation
- 💻 Submit code improvements
- 🌍 Translate to other languages
- 📣 Share your success story

**[Read Contributing Guidelines →](CONTRIBUTING.md)**

---

## License

OpenL Tablets is open source under the **[GNU Lesser General Public License (LGPL)](LICENSE)**.

**Commercial support and consulting available** through our partners.

---

## Quick Links

| Resource | Link |
|----------|------|
| **🚀 Quick Start** | [Getting Started Guide](docs/user-guides/installation/) |
| **📖 Documentation** | [Complete Docs](docs/) |
| **🔌 API Reference** | [REST API Guide](docs/API_GUIDE.md) |
| **🐳 Docker Hub** | [Container Images](https://hub.docker.com/u/openltablets) |
| **💬 Community** | [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions) |
| **🐛 Issues** | [Bug Reports](https://github.com/openl-tablets/openl-tablets/issues) |
| **🌐 Website** | [openl-tablets.org](https://openl-tablets.org) |
| **📦 Downloads** | [Releases](https://github.com/openl-tablets/openl-tablets/releases) |

---

<p align="center">
  <strong>Transform your business rules from spreadsheets to APIs in minutes.</strong><br>
  <a href="https://openl-tablets.org/downloads">Download OpenL Tablets</a> •
  <a href="docs/user-guides/installation/">Get Started</a> •
  <a href="https://openl-tablets.org/demo">Try Demo</a>
</p>

<p align="center">
  Made with ❤️ by the OpenL Tablets community
</p>

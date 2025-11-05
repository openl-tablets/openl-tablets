# OpenL Tablets BRMS Reference Guide

Complete reference documentation for OpenL Tablets Business Rules Management System, covering language syntax, table types, data types, functions, and project structure.

## Guide Contents

This reference guide is organized into the following sections:

### Getting Started
- [Preface](preface.md) - Audience, related information, and typographic conventions
- [Introducing OpenL Tablets](introduction.md) - Overview of OpenL Tablets technology and concepts

### Working with Tables
- [Table Basics](table-basics.md) - Table recognition algorithm and naming conventions

#### Table Types
OpenL Tablets supports various table types for different purposes:

- [Decision Tables](table-types/decision-tables.md) - Core decision logic tables with conditions and actions
- [Datatype Tables](table-types/datatype-tables.md) - Define custom data types and structures
- [Data Tables](table-types/data-tables.md) - Store and manage data collections
- [Test Tables](table-types/test-tables.md) - Unit testing for rules and methods
- [Run Tables](table-types/run-tables.md) - Execute test scenarios
- [Method Tables](table-types/method-tables.md) - Define callable methods
- [Configuration Tables](table-types/configuration-tables.md) - Configure module behavior
- [Properties Tables](table-types/properties-tables.md) - Set table and method properties
- [Spreadsheet Tables](table-types/spreadsheet-tables.md) - Complex calculations with spreadsheet logic
- [TBasic Tables](table-types/tbasic-tables.md) - BASIC-style procedural code
- [Column Match Tables](table-types/column-match-tables.md) - Pattern matching across columns
- [Constants Tables](table-types/constants-tables.md) - Define constant values
- [Table Part](table-types/table-part.md) - Reusable table components

#### Table Configuration
- [Table Properties](table-properties.md) - Comprehensive guide to table properties and metadata

### Language Features

- [Working with Arrays](arrays.md) - Array operations, manipulation, and best practices
- [Working with Data Types](data-types.md) - Built-in and custom data types
- [Working with Functions](functions.md) - OpenL Tablets functions and expressions

### Project Management

- [Working with Projects](projects.md) - Project structure, organization, and management

### Appendices

- [Appendix A: BEX Language Overview](appendices/bex-language.md) - Business Expression Language syntax
- [Appendix B: Functions Reference](appendices/function-reference.md) - Complete function reference

## Quick Links

### By Task
- **Creating decision logic** → [Decision Tables](table-types/decision-tables.md)
- **Defining data structures** → [Datatype Tables](table-types/datatype-tables.md)
- **Storing data** → [Data Tables](table-types/data-tables.md)
- **Testing rules** → [Test Tables](table-types/test-tables.md), [Run Tables](table-types/run-tables.md)
- **Complex calculations** → [Spreadsheet Tables](table-types/spreadsheet-tables.md)
- **Working with collections** → [Arrays](arrays.md)
- **Using functions** → [Functions](functions.md), [Function Reference](appendices/function-reference.md)

### By Experience Level
- **New users** → Start with [Introduction](introduction.md), then [Table Basics](table-basics.md)
- **Intermediate users** → Explore specific [Table Types](table-types/decision-tables.md)
- **Advanced users** → [Table Properties](table-properties.md), [BEX Language](appendices/bex-language.md)

## Related Documentation

- [Installation Guide](../installation/) - Installing and configuring OpenL Tablets
- [WebStudio User Guide](../webstudio/) - Using OpenL Studio for rule authoring
- [Developer Guide](../../developer-guides/) - Developing with OpenL Tablets
- [Rule Services Guide](../rule-services/) - Deploying rules as services

## Navigation Tips

This reference guide is comprehensive and detailed. For best results:

1. **Start with basics**: Read [Introduction](introduction.md) and [Table Basics](table-basics.md)
2. **Learn by doing**: Use [Decision Tables](table-types/decision-tables.md) to create your first rules
3. **Reference as needed**: Bookmark specific table types and features you use frequently
4. **Search effectively**: Use your browser's find function (Ctrl+F/Cmd+F) within pages
5. **Test your understanding**: Create [Test Tables](table-types/test-tables.md) to verify your rules

## Feedback

Found an error or have a suggestion? Please report it on our [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues) page.

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

# OpenL Tablets BRMS Reference Guide

Complete reference documentation for OpenL Tablets Business Rules Management System, covering language syntax, table types, data types, functions, and project structure.

## Guide Contents

This reference guide is organized into the following sections:

### Getting Started
- [Preface](01-introduction/01-preface.md) - Audience, related information, and typographic conventions
- [What Is OpenL Tablets?](01-introduction/02-what-is-openl-tablets.md) - Overview of OpenL Tablets technology
- [Basic Concepts](01-introduction/03-basic-concepts.md) - Core concepts and terminology
- [System Overview](01-introduction/04-system-overview.md) - Architecture and components
- [Installing OpenL Tablets](01-introduction/05-installing-openl-tablets.md) - Installation instructions
- [Tutorials and Examples](01-introduction/06-tutorials-and-examples.md) - Getting started tutorials

### Working with OpenL Tables

#### Table Basics
- [Table Recognition Algorithm](02-working-with-openl-tables/01-table-recognition-algorithm.md) - How OpenL recognizes and processes tables
- [Naming Conventions](02-working-with-openl-tables/02-naming-conventions.md) - Best practices for naming tables

#### Table Types

##### Core Table Types
OpenL Tablets supports various table types for different purposes:

- [Datatype Tables](02-working-with-openl-tables/03-table-types/01-datatype-table/01-introducing-datatype-tables.md) - Define custom data types and structures
- [Decision Tables](02-working-with-openl-tables/03-table-types/02-decision-table/01-decision-table-overview-and-structure.md) - Core decision logic tables with conditions and actions
- [Spreadsheet Tables](02-working-with-openl-tables/03-table-types/03-spreadsheet-table/01-parsing-a-spreadsheet-table.md) - Complex calculations with spreadsheet logic
- [Data Tables](02-working-with-openl-tables/03-table-types/07-data-table/01-using-simple-data-tables.md) - Store and manage data collections
- [Test Tables](02-working-with-openl-tables/03-table-types/08-test-table/01-understanding-test-tables.md) - Unit testing for rules and methods
- [Configuration Tables](02-working-with-openl-tables/03-table-types/09-configuration-table/01-configuration-table-description.md) - Configure module behavior

##### Common Table Features
Cross-cutting concepts that apply to multiple table types:

- [Using Calculations in Table Cells](02-working-with-openl-tables/03-table-types/04-using-calculations-in-table-cells.md) - Perform calculations within table cells
- [Referencing Attributes](02-working-with-openl-tables/03-table-types/05-referencing-attributes.md) - Reference table attributes and properties
- [Calling a Table from Another Table](02-working-with-openl-tables/03-table-types/06-calling-a-table-from-another-table.md) - Inter-table calls and dependencies
- [Representing Values of Different Types](02-working-with-openl-tables/03-table-types/10-representing-values-of-different-types.md) - How to represent arrays, dates, booleans, and ranges

##### Less Common Table Types
Specialized table types for specific use cases:

- [Column Match Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/01-column-match-table.md) - Pattern matching across columns
- [Method Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/02-method-table.md) - Define callable methods
- [Properties Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/03-properties-table.md) - Set table and method properties
- [Run Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/04-run-table.md) - Execute test scenarios
- [TBasic Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/05-tbasic-table.md) - BASIC-style procedural code
- [Constants Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/06-constants-table.md) - Define constant values
- [Table Part](02-working-with-openl-tables/03-table-types/11-less-common-table-types/07-table-part.md) - Reusable table components

#### Table Configuration
- [Table Properties Overview](02-working-with-openl-tables/04-table-properties/01-category-and-module-level-properties.md) - Comprehensive guide to table properties and metadata
- [Default Values](02-working-with-openl-tables/04-table-properties/02-default-value.md) - Setting default property values
- [System Properties](02-working-with-openl-tables/04-table-properties/03-system-properties.md) - Built-in system properties
- [Properties for Specific Table Types](02-working-with-openl-tables/04-table-properties/04-properties-for-a-particular-table-type.md) - Table type-specific properties
- [Rule Versioning](02-working-with-openl-tables/04-table-properties/05-rule-versioning.md) - Version management for rules
- [Info Properties](02-working-with-openl-tables/04-table-properties/06-info-properties.md) - Informational metadata
- [Dev Properties](02-working-with-openl-tables/04-table-properties/07-dev-properties.md) - Development-time properties
- [Properties in File Names](02-working-with-openl-tables/04-table-properties/08-properties-defined-in-the-file-name.md) - File name-based properties
- [Properties in Folder Names](02-working-with-openl-tables/04-table-properties/09-properties-defined-in-the-folder-name.md) - Folder-based properties
- [Keywords Usage in File Names](02-working-with-openl-tables/04-table-properties/10-keywords-usage-in-a-file-name.md) - File name keywords

### OpenL Tablets Functions and Supported Data Types

- [Chapter Overview](03-openl-tablets-functions-and-supported-data-types/01-overview.md) - Introduction to functions and data types
- [Working with Data Types](03-openl-tablets-functions-and-supported-data-types/02-working-with-data-types.md) - Built-in and custom data types
- [Working with Functions](03-openl-tablets-functions-and-supported-data-types/03-working-with-functions.md) - OpenL Tablets functions and expressions
- [Working with Arrays](03-openl-tablets-functions-and-supported-data-types/04-working-with-arrays.md) - Array operations, manipulation, and best practices

### Working with Projects

- [Project Structure](04-working-with-projects/01-project-structure.md) - Project organization and file structure
- [Project, Module, and Rule Dependencies](04-working-with-projects/02-project,-module,-and-rule-dependencies.md) - Managing dependencies
- [Rules Runtime Context Management](04-working-with-projects/03-rules-runtime-context-management-from-rules.md) - Runtime context handling
- [Project Localization](04-working-with-projects/04-project-localization.md) - Internationalization support

### Appendices

- [Appendix A: BEX Language Overview](05-appendices/01-bex-language-overview.md) - Business Expression Language syntax and operators
- [Appendix B: Array Functions](05-appendices/02-array-functions.md) - Complete array function reference
- [Appendix C: Date Functions](05-appendices/03-date-functions.md) - Date and time functions
- [Appendix D: Math Functions](05-appendices/04-math-functions.md) - Mathematical functions
- [Appendix E: String Functions](05-appendices/05-string-functions.md) - String manipulation functions
- [Appendix F: Special Functions](05-appendices/06-special-functions.md) - Special-purpose functions

## Quick Links

### By Task
- **Defining data structures** → [Datatype Tables](02-working-with-openl-tables/03-table-types/01-datatype-table/01-introducing-datatype-tables.md)
- **Creating decision logic** → [Decision Tables](02-working-with-openl-tables/03-table-types/02-decision-table/01-decision-table-overview-and-structure.md)
- **Complex calculations** → [Spreadsheet Tables](02-working-with-openl-tables/03-table-types/03-spreadsheet-table/01-parsing-a-spreadsheet-table.md)
- **Storing data** → [Data Tables](02-working-with-openl-tables/03-table-types/07-data-table/01-using-simple-data-tables.md)
- **Testing rules** → [Test Tables](02-working-with-openl-tables/03-table-types/08-test-table/01-understanding-test-tables.md), [Run Tables](02-working-with-openl-tables/03-table-types/11-less-common-table-types/04-run-table.md)
- **Representing values** → [Arrays, Dates, Booleans, Ranges](02-working-with-openl-tables/03-table-types/10-representing-values-of-different-types.md)
- **Working with collections** → [Arrays](03-openl-tablets-functions-and-supported-data-types/04-working-with-arrays.md)
- **Using functions** → [Functions Overview](03-openl-tablets-functions-and-supported-data-types/03-working-with-functions.md), [Function References](05-appendices/02-array-functions.md)

### By Experience Level
- **New users** → Start with [What Is OpenL Tablets?](01-introduction/02-what-is-openl-tablets.md), then [Basic Concepts](01-introduction/03-basic-concepts.md)
- **Intermediate users** → Explore specific [Table Types](02-working-with-openl-tables/03-table-types/01-datatype-table/01-introducing-datatype-tables.md)
- **Advanced users** → [Table Properties](02-working-with-openl-tables/04-table-properties/01-category-and-module-level-properties.md), [BEX Language](05-appendices/01-bex-language-overview.md)

## Navigation Tips

This reference guide is comprehensive and detailed. For best results:

1. **Start with basics**: Read [What Is OpenL Tablets?](01-introduction/02-what-is-openl-tablets.md) and [Table Recognition Algorithm](02-working-with-openl-tables/01-table-recognition-algorithm.md)
2. **Learn by doing**: Use [Datatype Tables](02-working-with-openl-tables/03-table-types/01-datatype-table/01-introducing-datatype-tables.md) and [Decision Tables](02-working-with-openl-tables/03-table-types/02-decision-table/01-decision-table-overview-and-structure.md) to create your first rules
3. **Reference as needed**: Bookmark specific table types and features you use frequently
4. **Search effectively**: Use your browser's find function (Ctrl+F/Cmd+F) within pages
5. **Test your understanding**: Create [Test Tables](02-working-with-openl-tables/03-table-types/08-test-table/01-understanding-test-tables.md) to verify your rules

## Document Organization

Files and folders in this guide are numbered to maintain reading order:
- **01-introduction/** - Getting started materials
- **02-working-with-openl-tables/** - Table types and properties
- **03-openl-tablets-functions-and-supported-data-types/** - Functions and data types
- **04-working-with-projects/** - Project management
- **05-appendices/** - Reference appendices

Within each folder, files are numbered sequentially (01-, 02-, etc.) to reflect the original document structure.

## Feedback

Found an error or have a suggestion? Please report it on our [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues) page.

---

**Last Updated**: 2025-11-20
**Version**: Reference Guide - Reorganized Structure

# Docs/ref — Agent Conventions

Reference sheets and specification for OpenL Tablets, optimized for human scanning and AI agent consumption.

## Style Rules

- Top-level heading: `# OpenL Tablets — <Topic>` (no version stamp in heading)
- Tables for property refs, goal lists, type identifier mappings (3+ columns always)
- Mermaid for structural diagrams (module dependencies)
- Max 2-line description per bullet; no prose paragraphs
- Cross-reference siblings with relative paths: `[Table Types](table-types.md)`
- Style: compact, fact-dense; tables and code blocks over prose
- Use two spaces for indenting a folder layout tree

## Source of Truth

Verify content against source code, not existing docs:

- `DefaultPropertyDefinitions.java` — all table properties
- `IXlsTableNames.java` — table type keywords
- `RulesDeploy.java` — rules-deploy.xml model
- `ProjectDescriptor.java` — rules.xml model

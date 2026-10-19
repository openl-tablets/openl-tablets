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

## Inside OpenL Studio

- OpenL Studio serves these files at `/docs/ref/`, and its MCP server hands them to AI agents; `index.md` titles the
  folder in the guides of OpenL Studio
- `mvn test -pl STUDIO/studio-docs` validates them as it validates the user guides: code blocks in the languages the
  viewer knows (a block without a language is plain text), no HTML tags outside code

## Source of Truth

Verify content against source code, not existing docs:

- `DefaultPropertyDefinitions.java` — all table properties
- `IXlsTableNames.java` — table type keywords
- `RulesDeploy.java` — rules-deploy.xml model
- `ProjectDescriptor.java` — rules.xml model

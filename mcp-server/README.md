# OpenL Tablets MCP Server

Model Context Protocol server for [OpenL Tablets](https://github.com/openl-tablets/openl-tablets) Business Rules Management System.

Built with MCP SDK v1.21.1 featuring type-safe validation (Zod), OAuth 2.1 support, and comprehensive OpenL Tablets integration.

## Quick Links

- 🚀 [Quick Start](docs/getting-started/QUICK-START.md) - Get up and running in 5 minutes
- ⚙️ [Setup Guides](docs/setup/) - Configure Claude Desktop, Cursor, or Docker
- 📖 [Usage Examples](docs/guides/EXAMPLES.md) - Learn how to use MCP tools
- 🔐 [Authentication](docs/guides/AUTHENTICATION.md) - Authentication setup
- 🐛 [Troubleshooting](docs/guides/TROUBLESHOOTING.md) - Common issues and solutions
- 👨‍💻 [Contributing](docs/development/CONTRIBUTING.md) - Development guide

## Quick Start

```bash
cd mcp-server
npm install
npm run build

# Configure
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Run
npm start
```

For detailed setup instructions, see [Quick Start Guide](docs/getting-started/QUICK-START.md).

## Documentation Structure

### Getting Started
- [Quick Start](docs/getting-started/QUICK-START.md) - Get up and running quickly
- [Configuration](docs/getting-started/CONFIGURATION.md) - Environment variables and settings *(coming soon)*

### Setup Guides
- [Claude Desktop & Cursor Setup](docs/setup/CLAUDE-DESKTOP.md) - Setup for Claude Desktop and Cursor IDE
- [Docker Setup](docs/setup/DOCKER.md) - Running MCP server in Docker
- [Cursor with Docker](docs/setup/CURSOR-DOCKER.md) - Connect Cursor IDE to Docker container

### Guides
- [Usage Examples](docs/guides/EXAMPLES.md) - Practical examples of using MCP tools
- [Authentication Guide](docs/guides/AUTHENTICATION.md) - All authentication methods (Basic Auth, Personal Access Token, OAuth 2.1)
- [Troubleshooting Guide](docs/guides/TROUBLESHOOTING.md) - Common issues, debugging, and solutions

### Development
- [Contributing Guide](docs/development/CONTRIBUTING.md) - How to contribute to the project
- [Architecture](docs/development/ARCHITECTURE.md) - System architecture and design
- [Testing Guide](docs/development/TESTING.md) - Testing strategy and how to run tests
- [Code Standards](docs/development/CODE_STANDARDS.md) - Best practices and coding standards
- [Tool Review](docs/development/TOOL_REVIEW.md) - Technical review of MCP tools vs OpenL API

### Reference
- [MCP Comparison](docs/reference/MCP_COMPARISON.md) - TypeScript vs Java MCP server comparison
- [Enable Disabled Tools](docs/reference/ENABLE_DISABLED_TOOLS.md) - How to enable temporarily disabled tools

## OpenL Tablets Concepts

### Versioning (Dual System)

OpenL has TWO independent versioning systems:

**1. Git-Based (Temporal)**
- Every save creates Git commit automatically
- Version = commit hash (e.g., "7a3f2b1c")
- Tools: `openl_get_file_history`, `openl_get_project_history`, `openl_revert_version`, `openl_compare_versions`

**2. Dimension Properties (Business Context)**
- Multiple rule versions in same commit
- Properties: `state`, `lob`, `effectiveDate`, `expirationDate`, `caProvince`, `country`, `currency`
- OpenL selects version by runtime context
- Managed via OpenL WebStudio UI

### Table Types

**Decision Tables** (5 variants):
- `Rules` - Standard with C1/C2/A1/RET markers
- `SimpleRules` - Positional matching (left-to-right)
- `SmartRules` - Smart matching by column name
- `SimpleLookup` - 2D matrix lookup
- `SmartLookup` - 2D smart lookup

**Spreadsheet Tables**:
- Multi-step calculations with `$columnName$rowName` references
- Return `SpreadsheetResult` (full matrix) or specific type (final value)

**Other**: `Method`, `TBasic`, `Data`, `Datatype`, `Test`, `Run`, `Properties`, `Configuration`

See [prompts/create_rule.md](./prompts/create_rule.md) for detailed table type guidance.

## Tools (18 Total)

All tools are versioned (v1.0.0) and prefixed with `openl_` for MCP compliance.

**Repository** (2): openl_list_repositories, openl_list_branches  
**Project** (3): openl_list_projects, openl_get_project, openl_update_project_status  
**Files** (3): openl_upload_file, openl_download_file, openl_get_file_history  
**Rules** (5): openl_list_tables, openl_get_table, openl_update_table, openl_append_table, openl_create_rule  
**Version Control** (2): openl_get_project_history, openl_revert_version  
**Deployment** (2): openl_list_deployments, openl_deploy_project  
**Testing** (3): openl_start_project_tests, openl_get_project_test_results, openl_run_project_tests (deprecated)  
**Execution** (1): openl_execute_rule

**Note**: Some tools are temporarily disabled pending full implementation. See [Enable Disabled Tools](docs/reference/ENABLE_DISABLED_TOOLS.md) for details.

## Prompts (12 Total)

Expert guidance templates for complex OpenL Tablets workflows. Each prompt includes a concise summary section highlighting the most common use cases and critical requirements.

Prompts provide contextual assistance, best practices, and step-by-step instructions directly in Claude Desktop or MCP Inspector.

### Available Prompts

| Prompt | Description | Summary |
|--------|-------------|---------|
| **create_rule** | Comprehensive guide for creating OpenL tables | Choose table type based on use case: Decision Tables for conditional logic, SimpleLookup/SmartLookup for key-value mappings, Spreadsheet for calculations |
| **create_test** | Step-by-step guide for creating OpenL test tables | Test tables mirror method signatures with columns matching tested table parameters plus _res_ (expected result) or _error_ (expected error) |
| **update_test** | Guide for modifying existing tests | Use openl_get_table() to fetch structure, modify rows, then openl_update_table() with FULL view. Always run tests after updates |
| **run_test** | Test selection logic and workflow | Run targeted tests first (1-5 tables → specific tableIds, 6+ → runAll). Before save/deploy, ALWAYS run all tests |
| **execute_rule** | Guide for executing OpenL rules | Execute rules for quick validation using openl_execute_rule() with inputData as JSON matching rule parameters |
| **append_table** | Guide for appending to tables | Use openl_append_table for incremental additions to Datatypes or Data tables without fetching full structure |
| **datatype_vocabulary** | Guide for defining datatypes | Define reusable data structures: Datatype tables create custom types, Vocabulary tables define allowed values |
| **dimension_properties** | Explanation of dimension properties | Dimension properties enable context-based rule selection: multiple versions of same rule selected at runtime by properties like state, lob, effectiveDate |
| **deploy_project** | Deployment workflow | All deployments MUST pass validation (0 errors), run all tests (100% pass), and follow environment progression (dev → test → staging → prod) |
| **get_project_errors** | Error analysis workflow | Systematic error resolution: Fix by category (type mismatches, missing references, syntax errors, circular dependencies). Target 0 errors before deployment |
| **file_history** | Guide for viewing file version history | Track file changes with Git commit history: Every save creates a Git commit. Use openl_get_file_history() to view commits |
| **project_history** | Guide for viewing project history | Project-wide audit trail showing all commits across entire project, with author, files changed, tables modified |

### Using Prompts

**In Claude Desktop:**
```
"Use the create_rule prompt"
"Show me the dimension_properties prompt"
```

**With Arguments:**
```
"Use create_test prompt with tableName=calculatePremium"
"Show deploy_project prompt for projectId=design-Insurance"
```

**In MCP Inspector:**
1. Navigate to "Prompts" tab
2. Select prompt from list
3. Provide arguments if needed
4. View rendered guidance

See [prompts/](./prompts/) directory for detailed prompt content.

## Configuration

### Environment Variables

```bash
# Required
OPENL_BASE_URL=http://localhost:8080/rest

# Auth Method 1: Basic Auth
OPENL_USERNAME=admin
OPENL_PASSWORD=admin

# Auth Method 2: OAuth 2.1
OPENL_OAUTH2_CLIENT_ID=client-id
OPENL_OAUTH2_CLIENT_SECRET=client-secret
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_SCOPE=openl:read openl:write

# Optional
OPENL_CLIENT_DOCUMENT_ID=mcp-server-1
OPENL_TIMEOUT=60000
```

See [Authentication Guide](docs/guides/AUTHENTICATION.md) for detailed auth setup.

### Claude Desktop / Cursor Configuration

See [Setup Guides](docs/setup/) for client-specific configuration instructions.

## Key Features

### MCP Best Practices
- **4 Response Formats**: json, markdown, markdown_concise, markdown_detailed
- **Pagination Metadata**: has_more, next_offset, total_count for all list operations
- **Actionable Error Messages**: Suggestions for corrective actions
- **Destructive Operation Confirmation**: Explicit confirm flag for safety-critical operations
- **Tool Versioning**: All tools versioned (v1.0.0) for change tracking
- **Prompt Summaries**: Concise summaries in all prompts for quick reference

### OpenL Tablets Integration
- **Type-Safe**: Zod schemas with strict validation and TypeScript inference
- **OpenL-Specific**: Proper table types, dimension properties, Git-based versioning
- **Dual Versioning**: Git commits (temporal) + dimension properties (business context)
- **AI Prompts**: 12 expert guidance templates with conditional rendering

### Authentication & Security
- **OAuth 2.1**: Automatic token management, refresh, and retry on 401
- **Personal Access Token**: User-generated tokens for programmatic access
- **Basic Auth**: Username/password with Base64 encoding
- **Sensitive Data Protection**: Automatic redaction of credentials in error messages

### Developer Experience
- **Enhanced Errors**: Detailed context (endpoint, method, tool, suggested fix)
- **Tool Metadata**: Version, category, operation flags
- **Character Limit**: 25K response truncation with helpful guidance
- **Comprehensive Tests**: 393 tests covering validators, formatters, auth, client, utils

## Development

```bash
npm run build          # Build TypeScript
npm test               # Run tests (393 total, 35% coverage)
npm run lint           # Check code quality
npm run watch          # Dev mode with auto-rebuild
```

**Test Coverage** (35.22% overall):
- validators.ts: 96.15%
- utils.ts: 97.95%
- auth.ts: 63.01%
- client.ts: 45.32%
- formatters.ts: 44.19%

See [Contributing Guide](docs/development/CONTRIBUTING.md) for development guidelines and [Testing Guide](docs/development/TESTING.md) for test suites.

## Project Structure

```
mcp-server/
├── src/                    # Source code (TypeScript)
├── tests/                  # Jest test suites
├── prompts/                # AI assistant guidance (OpenL-specific)
├── dist/                   # Compiled output
├── docs/                   # Documentation
│   ├── getting-started/    # Quick start and installation
│   ├── setup/              # Client setup guides
│   ├── guides/             # Usage guides and examples
│   ├── development/        # Developer documentation
│   └── reference/          # Reference materials
└── README.md               # This file
```

## Resources

- [OpenL Tablets](https://github.com/openl-tablets/openl-tablets)
- [OpenL Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)

## License

Follows OpenL Tablets project license.

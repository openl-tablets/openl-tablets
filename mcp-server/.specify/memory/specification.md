# OpenL Tablets MCP Server - Functional Specification

## Overview

The OpenL Tablets MCP Server is a Model Context Protocol implementation that provides AI coding agents with seamless access to the OpenL Tablets Business Rules Management System through a type-safe, comprehensive API.

## Target Users

**Primary**: AI coding agents (Claude Code, GitHub Copilot, Cursor, Windsurf, Gemini Code Assist)
**Secondary**: Developers integrating OpenL Tablets with AI workflows
**Tertiary**: DevOps teams automating rules management

## Core Capabilities

### 1. Repository Management

**Browse and discover design repositories**:
- List all available design repositories
- View repository metadata (name, type, status)
- List branches within repositories
- Access branch metadata (commit info, current branch)

**Use Cases**:
- Discover available rule repositories
- Check repository status before operations
- Navigate between branches for version control

### 2. Project Lifecycle Management

**Comprehensive project operations**:
- List all projects with filtering (repository, status, tag)
- Get detailed project information (modules, dependencies, metadata)
- Open projects for editing
- Close projects after editing
- Save project changes with validation
- Validate project structure and rules

**Project ID Formats** (All supported):
- Dash format: `"repository-projectName"` (user-friendly)
- Colon format: `"repository:projectName"` (decoded)
- Base64 format: `"ZGVzaWduOlByb2plY3Q="` (OpenL 6.0.0+ API)

**Use Cases**:
- Discover and filter projects
- Access project details for analysis
- Manage project editing lifecycle
- Ensure project validity before deployment

### 3. Rules and Tables Management

**Complete table/rule operations**:
- List all tables in a project (with filters)
- Get detailed table data and structure
- Update table content
- Create new rules (via Excel upload)
- Copy tables within projects
- Execute rules with test data

**Supported Table Types**:
- **Decision Tables**: Rules, SimpleRules, SmartRules, SimpleLookup, SmartLookup
- **Spreadsheet Tables**: Multi-step calculations
- **Other**: Method, TBasic, Data, Datatype, Test, Run, Properties, Configuration

**Use Cases**:
- Discover existing business rules
- Analyze rule logic and structure
- Modify rule behavior
- Test rule execution
- Duplicate rules for variations

### 4. File Management

**Excel file operations**:
- Upload Excel files with rules (.xlsx, .xls)
- Download Excel files from projects
- Access specific file versions (Git commit hash)
- View file modification history

**Use Cases**:
- Deploy rules from local Excel files
- Retrieve rules for offline editing
- Access historical file versions
- Track file change history

### 5. Testing and Validation

**Comprehensive testing support**:
- Run all tests in a project
- Run specific tests by ID or table
- Validate project for errors
- Get detailed error analysis with categorization
- Identify auto-fixable errors

**Error Categories**:
- Type errors (type mismatches, conversions)
- Syntax errors (parsing issues)
- Reference errors (undefined references)
- Validation errors (business logic issues)

**Use Cases**:
- Verify rule correctness before deployment
- Debug rule errors efficiently
- Identify fixable issues quickly
- Ensure project quality

### 6. Version Control and History

**Git-based versioning**:
- View project commit history
- View file commit history
- Compare two versions of a project
- Revert project to previous version

**Commit Metadata**:
- Commit hash (SHA)
- Author (name, email)
- Timestamp
- Comment/message
- Commit type (SAVE, ARCHIVE, RESTORE, ERASE, MERGE)
- Files/tables changed

**Use Cases**:
- Track project evolution
- Compare versions to understand changes
- Rollback problematic changes
- Audit rule modifications

### 7. Deployment Management

**Production deployment**:
- List all deployments
- Deploy project to production repository
- Specify deployment version (commit hash)
- View deployment metadata

**Use Cases**:
- Promote rules to production
- Track deployment history
- Deploy specific versions
- Verify deployment status

### 8. Authentication Support

**Multiple authentication methods**:
- **Basic Authentication**: Username/password
- **API Key Authentication**: Bearer token
- **OAuth 2.1**: Client credentials grant

**Features**:
- Token caching with expiration
- Automatic token refresh
- Request interceptors for auth
- Health check with auth status

**Use Cases**:
- Connect to secured OpenL instances
- Use enterprise SSO
- Integrate with API key systems
- Support various deployment scenarios

### 9. Prompt Library

**Expert guidance templates**:
- 11 comprehensive prompts for common workflows
- Dynamic argument substitution
- Conditional content blocks
- Best practices and examples embedded

**Available Prompts**:
- `create_rule` - Guide for creating OpenL tables
- `datatype_vocabulary` - Custom datatypes and enumerations
- `create_test` - Test table creation guide
- `update_test` - Modifying existing tests
- `run_test` - Test selection and execution
- `dimension_properties` - Business versioning vs Git versioning
- `execute_rule` - Rule execution with test data
- `deploy_project` - Deployment workflow
- `get_project_errors` - Error analysis and fixes
- `file_history` - File version history navigation
- `project_history` - Project-wide commit history

**Use Cases**:
- Guide AI agents through complex workflows
- Provide context-aware assistance
- Teach OpenL Tablets concepts
- Show best practices

### 10. Response Formatting & Pagination

**Flexible output formatting**:
- Two format options: `json` and `markdown` (default)
- All tools support `response_format` parameter
- Type-specific markdown formatters for optimal readability
- Automatic conversion between formats

**Pagination support**:
- `limit` parameter: Maximum items per page (max 200, default 50)
- `offset` parameter: Number of items to skip (default 0)
- Applies to all list operations

**Character limit enforcement**:
- Maximum response size: ~25,000 characters
- Automatic truncation with clear message when exceeded
- Ensures consistent performance across AI platforms

**Use Cases**:
- Get structured JSON for programmatic processing
- Get human-readable markdown for AI analysis
- Paginate through large result sets
- Prevent response timeouts with character limits

## Functional Requirements

### FR-1: Tool Execution

**Requirement**: MCP server provides 18 tools, of which 11 are currently active and 7 are temporarily disabled.

**Active Tools (11)**:

**Repository Tools (2)**:
- `openl_list_repositories` - List all design repositories
- `openl_list_branches` - List branches in a repository

**Project Tools (3)**:
- `openl_list_projects` - List projects with optional filters (supports pagination)
- `openl_get_project` - Get comprehensive project details
- `openl_update_project_status` - Update project status (open/close/save)

**Rules Tools (4)**:
- `openl_list_tables` - List tables/rules with filters (supports pagination)
- `openl_get_table` - Get table details and data
- `openl_update_table` - Update table content
- `openl_append_table` - Append data to existing table

**Deployment Tools (2)**:
- `openl_list_deployments` - List all deployments (supports pagination)
- `openl_deploy_project` - Deploy project to production


- `openl_upload_file` - Upload Excel file to project *(needs implementation fixes)*
- `openl_download_file` - Download Excel file from project *(needs implementation fixes)*
- `openl_get_file_history` - Get file commit history *(needs implementation fixes)*
- `openl_create_rule` - Create new rule *(needs implementation fixes)*
- `openl_execute_rule` - Execute rule with test data *(needs implementation fixes)*
- `openl_get_project_history` - Get project commit history *(needs implementation fixes)*
- `openl_revert_version` - Revert to previous version *(needs implementation fixes)*

**Note**: All active tools support the `response_format` parameter (json/markdown). All list operations support pagination via `limit` and `offset` parameters.

### FR-2: Input Validation

**Requirement**: All tool inputs must be validated using Zod schemas.

**Validation Rules**:
- Required fields must be present
- Optional fields have proper defaults
- String formats validated (URLs, project IDs)
- Numeric ranges enforced (timeouts)
- Enum values whitelisted
- Array items validated
- Nested objects validated

**Error Responses**:
- Clear error messages for validation failures
- Specify which field failed validation
- Provide expected format/value
- Include examples when helpful

### FR-3: Error Handling

**Requirement**: All error conditions must be handled gracefully.

**Error Types Handled**:
- Network errors (timeout, connection refused)
- Authentication errors (401, 403)
- Not found errors (404)
- Method not allowed (405)
- Server errors (500, 503)
- Validation errors (400)
- Invalid input errors
- File operation errors

**Error Response Format**:
```json
{
  "error": {
    "code": "InternalError",
    "message": "Sanitized error message",
    "data": {
      "status": 404,
      "endpoint": "/api/projects/xyz",
      "method": "GET"
    }
  }
}
```

**Security Requirement**: All credentials must be redacted from error messages.

### FR-4: Resource Exposure

**Requirement**: Expose OpenL Tablets data as MCP resources.

**Resources**:
- `openl://repositories` - List of repositories
- `openl://projects` - List of projects
- `openl://deployments` - List of deployments

**Resource Format**:
```json
{
  "uri": "openl://repositories",
  "name": "OpenL Repositories",
  "description": "Design and production repositories",
  "mimeType": "application/json"
}
```

### FR-5: Prompt System

**Requirement**: Provide 11 prompts with argument substitution.

**Prompt Features**:
- YAML frontmatter metadata
- Dynamic argument substitution: `{variable}`
- Conditional blocks: `{if condition}...{end if}`
- Comprehensive guidance
- Code examples
- Best practices

**Prompt Arguments**:
- Optional vs required arguments
- Type validation
- Default values
- Description for each argument

### FR-6: Health Check

**Requirement**: Provide server health and connectivity verification.

**Health Check Response**:
```json
{
  "status": "healthy",
  "baseUrl": "http://localhost:8080/rest",
  "authMethod": "Basic Auth",
  "timestamp": "2025-11-13T17:00:00Z",
  "serverReachable": true
}
```

**Checks Performed**:
- Network connectivity to OpenL server
- Authentication status
- API endpoint availability
- Response time

### FR-7: Authentication Management

**Requirement**: Support 3 authentication methods with token caching.

**Basic Authentication**:
- Username and password
- Base64 encoding
- Authorization header injection

**API Key Authentication**:
- Bearer token
- Header injection
- No expiration handling

**OAuth 2.1**:
- Client credentials grant
- Token acquisition with client_secret
- Token caching with expiration
- Automatic refresh
- Concurrent request handling

**Configuration Validation**:
- At least one auth method required
- Complete OAuth config enforced
- URL format validation

### FR-8: OpenL API Compatibility

**Requirement**: Support OpenL Tablets 6.0.0+ REST API.

**API Changes Supported**:
- Base64-encoded project IDs
- New endpoint structure `/projects/{base64-id}`
- Backward compatibility with older formats
- All three project ID formats accepted

**Known Limitations** (Documented):
- `/tables` POST endpoint returns 405 (use upload_file instead)
- `/tests/run` endpoint returns 404 (not available in REST API)
- `/validation` endpoint returns 404 (not available in REST API)
- `/history` endpoints return 404 (not available in REST API)

### FR-9: Performance Requirements

**Requirement**: Efficient resource usage and caching.

**Performance Optimizations**:
- OAuth token caching (30-60 minute expiration)
- Axios connection pooling (default)
- Concurrent request deduplication
- Lazy token acquisition
- Configurable timeouts (default: 30s)

**Memory Management**:
- Circular reference protection
- Safe JSON serialization
- Clean error handling paths
- No memory leaks

### FR-10: Type Safety Requirements

**Requirement**: Full TypeScript strict mode compliance.

**Type Safety Features**:
- No implicit any types
- Explicit return types on all functions
- Type inference from Zod schemas
- Type guards for error handling
- Unknown over any in error handling
- Proper null/undefined handling

### FR-11: Documentation Requirements

**Requirement**: Comprehensive documentation for all features.

**Documentation Deliverables**:
- README.md - Installation, configuration, usage
- AUTHENTICATION.md - Auth setup for all 3 methods
- CONTRIBUTING.md - Development guide
- TESTING.md - Testing documentation
- EXAMPLES.md - Real-world usage examples
- JSDoc on all public APIs
- Inline comments for complex logic

### FR-12: Testing Requirements

**Requirement**: Comprehensive test coverage.

**Test Categories**:
- Unit tests for all modules
- Integration tests for API interactions
- Mock data for realistic scenarios
- Live integration tests (optional)

**Coverage Goals**:
- Overall: >80% statements
- Critical modules (auth, client, index): >85%
- Utilities: >90%
- Schemas: >95%

### FR-13: Response Formatting

**Requirement**: All tools must support flexible output formatting.

**Format Options**:
- `json`: Structured JSON output for programmatic processing
- `markdown`: Human-readable markdown output (default)

**Implementation**:
- `response_format` parameter on all tools (optional)
- Default value: `markdown`
- Automatic type conversion using formatters
- Type-specific markdown templates for optimal readability

**Validation**:
- Format parameter validated via Zod enum
- Invalid formats return clear error message
- Consistent formatting across all tool types

### FR-14: Pagination Support

**Requirement**: All list operations must support pagination.

**Pagination Parameters**:
- `limit`: Maximum items per page (optional)
  - Default: 50
  - Maximum: 200
  - Minimum: 1
- `offset`: Number of items to skip (optional)
  - Default: 0
  - Minimum: 0

**Applicable Tools**:
- `openl_list_repositories`
- `openl_list_projects`
- `openl_list_tables`
- `openl_list_deployments`
- `openl_get_project_history`
- `openl_get_file_history`

**Implementation**:
- Server-side pagination via OpenL API
- Clear indication when results are truncated
- Total count included in response when available

### FR-15: Character Limit Enforcement

**Requirement**: All tool responses must enforce character limits.

**Limits**:
- Maximum response size: ~25,000 characters
- Applies to all tools uniformly
- Prevents timeout issues on AI platforms

**Truncation Behavior**:
- Automatic truncation when limit exceeded
- Clear truncation message appended
- Guidance on using pagination to access remaining data
- Truncation point chosen to preserve data integrity

**Truncation Message Format**:
```plaintext
... [Response truncated due to size limit. Use pagination parameters (limit/offset) to access remaining data]
```

## Non-Functional Requirements

### NFR-1: Security

- No credentials in logs or error messages
- All inputs validated before processing
- No SQL injection vulnerabilities
- No path traversal vulnerabilities
- npm audit clean (0 vulnerabilities)

### NFR-2: Performance

- Tool execution <1s for simple operations
- Tool execution <5s for complex operations
- Token acquisition <2s
- Connection timeout: 30s (configurable)
- Memory usage <100MB baseline

### NFR-3: Reliability

- 99% uptime for server process
- Graceful degradation on OpenL server errors
- Automatic retry for transient failures (OAuth)
- Clean error messages for all failure modes

### NFR-4: Maintainability

- Modular architecture with clear separation
- ESLint compliance (0 errors, 0 warnings)
- TypeScript strict mode compliance
- Comprehensive tests
- Up-to-date documentation

### NFR-5: Compatibility

- Node.js 18.0.0 or higher
- OpenL Tablets 6.0.0 or higher
- MCP SDK 1.21.1 or higher
- ES Modules (type: "module")

### NFR-6: Usability

- Clear error messages
- Helpful validation feedback
- Comprehensive examples
- Interactive prompts for guidance
- Well-structured documentation

## Success Criteria

The MCP server is considered successful when:

1. ✅ All 11 active tools execute successfully (7 tools temporarily disabled)
2. ✅ All active tools support response_format parameter
3. ✅ All list operations support pagination
4. ✅ Character limits enforced on all responses
5. ✅ All 11 prompts render correctly
6. ✅ All 3 authentication methods work
7. ⏳ Test coverage >38% (target: 80%)
8. ✅ ESLint enforced (no errors on commit)
9. ✅ TypeScript strict mode clean
10. ✅ npm audit clean (0 vulnerabilities)
11. ✅ Documentation complete and accurate
12. ✅ Examples verified and working
13. ✅ Compatible with OpenL 6.0.0+

## Out of Scope

The following are explicitly NOT supported:

- OpenL Tablets WebStudio UI automation
- Direct database access to OpenL repositories
- Custom rule parsing/compilation
- Excel file generation from scratch
- OpenL rule syntax validation (delegated to OpenL server)
- Multi-tenant isolation (relies on OpenL server)
- User management (handled by OpenL server)
- Websocket/streaming support (MCP is request/response)

## Future Considerations

Potential enhancements for future versions:

- Streaming support for large file operations
- Batch tool execution
- WebSocket subscriptions for real-time updates
- Advanced caching strategies
- OpenL server version detection
- Migration tools for project structure changes
- Rule diff visualization
- Performance profiling tools

---

*Last Updated: 2025-11-16*
*Version: 1.0.0*
*Status: Experimental (11/18 tools active, 7 temporarily disabled pending fixes)*

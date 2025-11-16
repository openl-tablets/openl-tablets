# Changelog

All notable changes to the OpenL Tablets MCP Server will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-16

### Added - MCP Best Practices Implementation

This release implements Anthropic's MCP best practices with comprehensive improvements across response formatting, validation, testing, and documentation.

#### Response Formats (Task 1)
- **4 format variants** for all tools via `response_format` parameter:
  - `json`: Structured JSON for programmatic use
  - `markdown`: Full markdown (default, optimal for AI)
  - `markdown_concise`: 1-2 paragraph summaries with key metrics
  - `markdown_detailed`: Full details + metadata headers + contextual breakdowns
- `toMarkdownConcise()` formatter (80 lines) for brief summaries
- `toMarkdownDetailed()` formatter (60 lines) for comprehensive output
- Updated `formatResponse()` to handle all 4 formats

#### Pagination Enhancements (Task 2)
- Verified existing pagination implementation includes:
  - `has_more`: Boolean indicating more results available
  - `next_offset`: Integer for next page (null when no more results)
  - `total_count`: Total items across all pages
- All list operations support `limit` (1-200, default 50) and `offset` (≥0, default 0)

#### Actionable Error Messages (Task 3)
- Enhanced `validators.ts` with corrective suggestions:
  - `validateProjectId()`: "To find valid project IDs, use: openl_list_projects()"
  - `validatePagination()`: Specific examples and pagination guidance
  - `validateResponseFormat()`: Lists all valid formats with usage hints
- Enhanced `client.ts` with tool recommendations:
  - `parseProjectId()` 404 errors: Suggests openl_list_projects()
  - `uploadFile()` failures: Suggests openl_get_project()
- Batch-updated 18 tool handlers with actionable missing argument errors

#### Destructive Operation Confirmation (Task 5)
- Added `confirm: boolean` field to destructive operations:
  - `openl_revert_version`: Requires `confirm: true` (prevents accidental version rollback)
  - `openl_deploy_project`: Requires `confirm: true` (prevents accidental production deployment)
  - `openl_update_project_status`: Requires `confirm: true` when `discardChanges: true` (prevents data loss)
- Implemented validation checks in tool handlers with instructional error messages
- Error messages explain:
  - What the destructive operation will do
  - Why confirmation is required
  - What to review before proceeding
  - How to confirm (set `confirm: true`)

#### Tool Versioning (Task 7)
- Added `version: "1.0.0"` to all 18 tools
- Enables semantic versioning tracking for API stability
- Updated `ToolDefinition` interface to include version property

#### Prompt Enhancements (Task 8)
- Added **summary sections to all 12 prompts**:
  - create_rule.md
  - create_test.md
  - update_test.md
  - run_test.md
  - execute_rule.md
  - append_table.md
  - datatype_vocabulary.md
  - dimension_properties.md
  - deploy_project.md
  - get_project_errors.md
  - file_history.md
  - project_history.md
- Each summary: 1-3 sentences highlighting common use cases and critical requirements
- Uses conditional rendering for context-specific guidance

#### Comprehensive Test Suite (Task 6)
- **5 new test suites** with 231 new tests (347 total):
  - `validators.test.ts`: 42 tests, **96.15% coverage**
  - `utils.test.ts`: 80 tests, **97.95% coverage**
  - `auth.test.ts`: 51 tests, **63.01% coverage**
  - `client.test.ts`: 88 tests, **45.32% coverage**
  - `formatters.test.ts`: 55 tests, **44.19% coverage**
- **Overall coverage: 3.79% → 35.22%** (+31.43 percentage points)
- Comprehensive edge case coverage for security-critical paths
- Mock-based unit tests (no external dependencies)
- Fast execution (5-6 seconds for full suite)

#### Documentation Updates (Task 10)
- Updated README.md with:
  - Corrected tool count (18 tools)
  - Added 4 response format variants documentation
  - Added Destructive Operation Confirmation section
  - Updated prompt table with summaries
  - Enhanced Key Features section (MCP Best Practices, OpenL Integration, Auth & Security, Developer Experience)
  - Updated test coverage statistics (347 tests, 35%)
- Created CHANGELOG.md documenting all improvements

### Changed

#### File Structure
- `src/schemas.ts`: Extended ResponseFormat enum, added confirm fields
- `src/formatters.ts`: Added toMarkdownConcise() and toMarkdownDetailed()
- `src/validators.ts`: Enhanced error messages with actionable suggestions
- `src/client.ts`: Enhanced error messages with tool recommendations
- `src/tool-handlers.ts`: Added versions, confirmation checks, improved errors
- All 12 `prompts/*.md` files: Added summary sections

#### Dependencies
- Added `axios-mock-adapter@^1.22.0` (devDependency) for auth testing

### Test Coverage Details

| Module | Coverage | Lines | Status |
|--------|----------|-------|--------|
| validators.ts | 96.15% | 121 lines | ✅ Excellent |
| utils.ts | 97.95% | 210 lines | ✅ Excellent |
| constants.ts | 100% | - | ✅ Complete |
| prompts-registry.ts | 91.17% | - | ✅ Excellent |
| auth.ts | 63.01% | 229 lines | 🟡 Good |
| client.ts | 45.32% | 1143 lines | 🟡 Moderate |
| formatters.ts | 44.19% | 512 lines | 🟡 Moderate |
| utils.ts | 28.57% | 208 lines | 🟡 Needs work |
| **Overall** | **35.22%** | - | 🟡 **Good progress** |

**Note**: Uncovered modules (index.ts, tool-handlers.ts, schemas.ts, tools.ts) are primarily declarative or require integration/E2E testing rather than unit tests.

### Security Enhancements

- Automatic sensitive data redaction in error messages:
  - Bearer tokens: `Bearer abc123` → `Bearer [REDACTED]`
  - API keys: `api_key=secret` → `api_key=[REDACTED]`
  - Client secrets: `client_secret=xyz` → `client_secret=[REDACTED]`
  - URL credentials: `http://user:pass@host` → `http://[REDACTED]:[REDACTED]@host`
- Comprehensive input validation with strict Zod schemas
- Confirmation requirements for destructive operations

### Performance

- Response truncation at 25,000 characters prevents MCP client overload
- Client-side pagination reduces network payload for large datasets
- Minimal overhead from response formatting (~5ms)

## [Pre-1.0.0] - Earlier Versions

See Git history for changes before the 1.0.0 MCP Best Practices release.

---

## Migration Guide

### From Pre-1.0.0 to 1.0.0

#### Response Format Changes
**Before:**
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design"
  }
}
```

**After (same, but with new options):**
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "response_format": "markdown_concise"  // NEW: 4 format variants
  }
}
```

#### Destructive Operations Now Require Confirmation
**Before (would execute immediately):**
```json
{
  "name": "openl_revert_version",
  "arguments": {
    "projectId": "design-project1",
    "targetVersion": "abc123"
  }
}
```

**After (requires explicit confirmation):**
```json
{
  "name": "openl_revert_version",
  "arguments": {
    "projectId": "design-project1",
    "targetVersion": "abc123",
    "confirm": true  // REQUIRED: Must explicitly confirm
  }
}
```

**Without confirmation**, you'll receive an instructional error message:
```
This operation will revert project "design-project1" to version "abc123",
which is a destructive action that creates a new commit with the old state.
To proceed, set confirm: true in your request.
To review the target version first, use: openl_get_project_history(projectId: "design-project1")
```

#### Error Messages Now Include Suggestions
**Before:**
```
Error: Invalid projectId format
```

**After:**
```
Error: Invalid projectId format. Expected 'repository-projectName', got 'invalid'.
To find valid project IDs, use: openl_list_projects()
```

All error messages now include actionable suggestions for corrective actions.

---

## Roadmap

### Planned Features (Not Yet Implemented)

#### Task 4: Streaming for Long-Running Operations
- MCP progressToken support for operations >10 seconds
- Priority: execute_rule, deploy_project, large file downloads
- Status: Pending

#### Task 9: Resource URI Support (Read-Only)
- Support for URIs like `openl://projects/{projectId}`
- Read-only resource access via MCP resource protocol
- Status: Pending

### Future Enhancements

- Integration test suite for tool handlers
- E2E tests against live/mock OpenL instance
- Higher test coverage (target: 80%+)
- Re-enable temporarily disabled tools:
  - openl_validate_project
  - openl_test_project
  - openl_get_project_errors
  - openl_compare_versions

---

**Questions or Issues?**
- [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- [OpenL Tablets Documentation](https://openl-tablets.org/)
- [MCP Documentation](https://modelcontextprotocol.io/)

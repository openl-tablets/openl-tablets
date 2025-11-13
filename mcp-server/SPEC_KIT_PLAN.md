# MCP Server Spec Kit Generation Plan

## Executive Summary

This document outlines a comprehensive plan to generate specification kit (spec kit) artifacts for the OpenL Tablets MCP Server. The spec kit will provide complete test coverage, documentation, examples, and validation suites to ensure the MCP server is production-ready, maintainable, and well-documented.

**Branch**: `claude/spec-kit-test-for-mcp-011CV6EBbinrcSsE9ETJ6e5y`
**Source Branch**: `claude/mcp-openl-tablets-rules-011CUzSonNshZtvvLNVzyLoc`
**Created**: 2025-11-13

## Current State Analysis

### Test Coverage Snapshot

```
File                 | % Stmts | % Branch | % Funcs | % Lines | Status
---------------------|---------|----------|---------|---------|--------
auth.ts             |    0.00 |     0.00 |    0.00 |    0.00 | ❌ Critical Gap
client.ts           |    0.00 |     0.00 |    0.00 |    0.00 | ❌ Critical Gap
constants.ts        |  100.00 |   100.00 |  100.00 |  100.00 | ✅ Complete
index.ts            |    0.00 |     0.00 |    0.00 |    0.00 | ❌ Critical Gap
prompts-registry.ts |   91.17 |    55.55 |  100.00 |   90.32 | ⚠️  Good Coverage
prompts.ts          |    0.00 |     0.00 |    0.00 |    0.00 | ❌ Critical Gap
schemas.ts          |    0.00 |   100.00 |  100.00 |    0.00 | ⚠️  Partial
tools.ts            |    0.00 |   100.00 |    0.00 |    0.00 | ⚠️  Partial
utils.ts            |    0.00 |     0.00 |    0.00 |    0.00 | ❌ Critical Gap
---------------------|---------|----------|---------|---------|--------
Overall             |    5.49 |     1.56 |    8.41 |    5.44 | ❌ Critical Gap
```

### Current Test Suite

- **Total Tests**: 127 (111 passed, 16 skipped)
- **Test Files**: 4
  - `openl-client.test.ts` - API client unit tests
  - `mcp-server.test.ts` - MCP tools integration tests
  - `prompts.test.ts` - Prompts rendering and validation tests
  - `integration/openl-live.test.ts` - Live integration tests (skipped in CI)
- **Test Lines**: ~1,818 total

### MCP Server Components

- **Tools**: 24 total across 6 categories
  - Repository (2): `list_repositories`, `list_branches`
  - Project (6): `list_projects`, `get_project`, `open_project`, `close_project`, `save_project`, `validate_project`
  - Files (3): `upload_file`, `download_file`, `get_file_history`
  - Rules (8): `list_tables`, `get_table`, `update_table`, `create_rule`, `copy_table`, `execute_rule`, `run_test`, `run_all_tests`
  - Version Control (3): `get_project_history`, `compare_versions`, `revert_version`
  - Deployment (2): `list_deployments`, `deploy_project`
  - Dimension Properties (4): `get/set_file_name_pattern`, `get/set_table_properties`
  - Testing (2): Covered in Project category

- **Prompts**: 11 total
  - `create_rule`, `datatype_vocabulary`, `create_test`, `update_test`, `run_test`
  - `dimension_properties`, `execute_rule`, `deploy_project`, `get_project_errors`
  - `file_history`, `project_history`

- **Source Files**: 9 TypeScript files (~4,232 lines)
- **Exported Items**: 59 functions, classes, constants, and types

## Spec Kit Artifacts Structure

```
mcp-server/
├── spec-kit/
│   ├── README.md                          # Spec kit overview
│   ├── test-plan/
│   │   ├── test-strategy.md              # Overall testing strategy
│   │   ├── coverage-goals.md             # Coverage targets per component
│   │   └── test-matrix.md                # Test case matrix
│   ├── unit-tests/
│   │   ├── auth.spec.ts                  # Authentication tests (NEW)
│   │   ├── client.spec.ts                # Client comprehensive tests (NEW)
│   │   ├── utils.spec.ts                 # Utilities tests (NEW)
│   │   ├── schemas.spec.ts               # Schema validation tests (NEW)
│   │   ├── tools.spec.ts                 # Tool definitions tests (NEW)
│   │   └── prompts.spec.ts               # Enhanced prompts tests
│   ├── integration-tests/
│   │   ├── tool-workflows.spec.ts        # End-to-end tool workflows (NEW)
│   │   ├── error-scenarios.spec.ts       # Error handling scenarios (NEW)
│   │   ├── auth-flows.spec.ts            # Authentication flows (NEW)
│   │   └── performance.spec.ts           # Performance benchmarks (NEW)
│   ├── contract-tests/
│   │   ├── openl-api-contracts.ts        # OpenL API contract tests (NEW)
│   │   ├── mcp-protocol-contracts.ts     # MCP protocol compliance (NEW)
│   │   └── schema-contracts.ts           # Input/output schema contracts (NEW)
│   ├── fixtures/
│   │   ├── api-responses/                # Mock API responses
│   │   ├── test-data/                    # Test data sets
│   │   ├── excel-files/                  # Sample Excel rule files
│   │   └── scenarios/                    # Complete test scenarios
│   ├── examples/
│   │   ├── basic-usage/                  # Basic usage examples
│   │   ├── advanced-workflows/           # Advanced workflow examples
│   │   ├── error-recovery/               # Error recovery patterns
│   │   └── best-practices/               # Best practices examples
│   ├── documentation/
│   │   ├── api-reference.md              # Complete API reference
│   │   ├── tool-reference.md             # Tool-by-tool reference
│   │   ├── prompt-guide.md               # Prompt usage guide
│   │   ├── troubleshooting.md            # Common issues and solutions
│   │   └── migration-guide.md            # Version migration guide
│   ├── validation/
│   │   ├── schema-validation.spec.ts     # Validate all Zod schemas
│   │   ├── type-safety.spec.ts           # TypeScript type validation
│   │   ├── security.spec.ts              # Security vulnerability tests
│   │   └── compliance.spec.ts            # Standards compliance tests
│   └── reports/
│       ├── coverage-report.html          # Generated coverage report
│       ├── test-results.xml              # JUnit test results
│       └── spec-validation.md            # Spec validation results
```

## Phase 1: Foundation (Week 1)

### 1.1 Test Infrastructure Setup
- [ ] Create `spec-kit/` directory structure
- [ ] Set up additional Jest configuration for spec kit tests
- [ ] Configure coverage thresholds per module
- [ ] Set up test data fixtures directory
- [ ] Create test utilities and helpers

### 1.2 Documentation Framework
- [ ] Create spec kit README with overview
- [ ] Document test strategy and approach
- [ ] Define coverage goals per component
- [ ] Create test case matrix template

### 1.3 Mock Data Expansion
- [ ] Expand `tests/mocks/openl-api-mocks.ts` with:
  - Complete project lifecycle scenarios
  - All table types (Rules, SimpleRules, SmartRules, etc.)
  - Version history with branches
  - Deployment scenarios
  - Error responses for all failure modes
- [ ] Create fixture files for:
  - Sample Excel files (rules, datatypes, tests)
  - Complete project structures
  - Authentication scenarios

## Phase 2: Unit Test Coverage (Week 2-3)

### 2.1 Authentication Module (`auth.ts`)
**Target Coverage**: >90% statements, >85% branches

Test Cases:
- [ ] Basic authentication (username/password)
  - Valid credentials
  - Invalid credentials
  - Missing credentials
  - Interceptor setup and token injection
- [ ] API key authentication
  - Valid API key
  - Invalid API key
  - Missing API key
  - Header injection
- [ ] OAuth 2.1 authentication (if implemented)
  - Token acquisition
  - Token refresh
  - Token expiration handling
- [ ] Error scenarios
  - Network errors during auth
  - 401 Unauthorized responses
  - 403 Forbidden responses
  - Timeout during authentication

### 2.2 Client Module (`client.ts`)
**Target Coverage**: >85% statements, >80% branches

This is the largest module (1,123 lines) - prioritize by API section:

#### Repository Management (Lines 81-99)
- [ ] `listRepositories()` - success and error cases
- [ ] `listBranches()` - with different repository names

#### Project Management (Lines 105-292)
- [ ] `parseProjectId()` - all three formats (dash, colon, base64)
- [ ] `toBase64ProjectId()` - format conversion
- [ ] `buildProjectPath()` - URL encoding
- [ ] `listProjects()` - with and without filters
- [ ] `getProject()` - success and 404 cases
- [ ] `openProject()` - project access verification
- [ ] `closeProject()` - project closure
- [ ] `saveProject()` - with validation errors and success
  - Test validation before save
  - Test commit hash extraction
  - Test author metadata

#### File Management (Lines 298-418)
- [ ] `uploadFile()` - Excel file upload
  - Valid Excel files (.xlsx, .xls)
  - Invalid file types
  - File size limits
  - Commit metadata extraction
- [ ] `downloadFile()` - file retrieval
  - With and without version parameter
  - Project name prefix stripping
  - Buffer conversion
- [ ] `createBranch()` - branch creation

#### Rules/Tables Management (Lines 424-544)
- [ ] `listTables()` - with filters (type, name, file)
- [ ] `createRule()` - all scenarios
  - Successful creation
  - 405 Method Not Allowed (expected)
  - Parameter/signature building
- [ ] `getTable()` - table retrieval
- [ ] `updateTable()` - table modification

#### Deployment Management (Lines 550-586)
- [ ] `listDeployments()` - deployment listing
- [ ] `deployProject()` - deployment execution
  - Success scenarios
  - Version specification

#### Testing & Validation (Lines 632-763)
- [ ] `runAllTests()` - test execution (404 expected)
- [ ] `validateProject()` - validation (404 expected)
- [ ] `runTest()` - selective test execution
  - Test IDs selection
  - Table IDs selection
  - Run all tests flag
- [ ] `getProjectErrors()` - error categorization
  - Type errors
  - Syntax errors
  - Reference errors
  - Auto-fixable count

#### Version Control (Lines 767-851)
- [ ] `copyTable()` - table duplication
- [ ] `executeRule()` - rule execution
  - Success with timing
  - Error handling
- [ ] `compareVersions()` - version comparison
- [ ] `revertVersion()` - version rollback
  - Validation before revert
  - New version creation

#### Git History (Lines 907-1008)
- [ ] `parseCommitType()` - commit type parsing
- [ ] `getFileHistory()` - file commit history
  - Pagination support
  - Commit metadata parsing
- [ ] `getProjectHistory()` - project commit history
  - Branch filtering
  - Pagination

#### Dimension Properties (Lines 1014-1122)
- [ ] `extractFileNamePattern()` - XML parsing
- [ ] `extractPropertiesFromPattern()` - property extraction
- [ ] `getFileNamePattern()` - pattern retrieval
- [ ] `setFileNamePattern()` - pattern update
- [ ] `getTableProperties()` - property retrieval
- [ ] `setTableProperties()` - property update

#### Health Check (Lines 592-628)
- [ ] `healthCheck()` - connectivity verification
  - Healthy status
  - Unhealthy status with error

### 2.3 Utilities Module (`utils.ts`)
**Target Coverage**: >90% statements, >85% branches

Test Cases:
- [ ] `validateTimeout()` - timeout validation
  - Valid timeouts
  - Invalid timeouts (negative, NaN, too large)
  - Default fallback
- [ ] `sanitizeError()` - error message sanitization
  - Axios errors
  - Error objects
  - String errors
  - Unknown error types
- [ ] `parseProjectId()` - project ID parsing
  - Base64 format
  - Colon format
  - Invalid formats

### 2.4 Schemas Module (`schemas.ts`)
**Target Coverage**: >95% statements, 100% branches

Test Cases:
- [ ] Validate all Zod schemas against valid inputs
- [ ] Validate all Zod schemas against invalid inputs
- [ ] Test optional vs required fields
- [ ] Test default values
- [ ] Test enum validations
- [ ] Test array validations
- [ ] Test nested object validations

### 2.5 Tools Module (`tools.ts`)
**Target Coverage**: >90% statements, 100% branches

Test Cases:
- [ ] Verify all 24 tool definitions are properly structured
- [ ] Test `getToolByName()` for all tools
- [ ] Test `getToolsByCategory()` for all categories
- [ ] Test `getAllToolNames()` returns all 24 tools
- [ ] Validate input schemas are valid JSON Schema
- [ ] Validate metadata completeness

### 2.6 Prompts Module (`prompts.ts`)
**Target Coverage**: >85% statements, >80% branches

Test Cases:
- [ ] Test prompt loading and caching
- [ ] Test argument substitution
- [ ] Test conditional blocks (`{if}...{end if}`)
- [ ] Test missing arguments handling
- [ ] Test invalid prompt names

## Phase 3: Integration & Workflow Tests (Week 4)

### 3.1 Tool Workflow Tests
End-to-end workflows combining multiple tools:

- [ ] **Discovery Workflow**
  1. `list_repositories` → find design repo
  2. `list_projects` → find specific project
  3. `get_project` → get project details
  4. `list_tables` → discover rules

- [ ] **Rule Creation Workflow**
  1. `open_project` → open project for editing
  2. `create_rule` → attempt to create rule (expect 405)
  3. `upload_file` → upload Excel with rule
  4. `list_tables` → verify rule appears
  5. `save_project` → save changes
  6. `close_project` → close project

- [ ] **Testing Workflow**
  1. `open_project` → open project
  2. `list_tables` → find test tables
  3. `run_test` → run specific tests
  4. `validate_project` → check validation
  5. `get_project_errors` → get error details

- [ ] **Deployment Workflow**
  1. `validate_project` → ensure no errors
  2. `list_deployments` → find deployment repo
  3. `deploy_project` → deploy to production
  4. `list_deployments` → verify deployment

- [ ] **Version Control Workflow**
  1. `get_project_history` → view commit history
  2. `compare_versions` → compare two commits
  3. `get_file_history` → view file changes
  4. `revert_version` → revert to previous version

### 3.2 Error Scenario Tests
Comprehensive error handling tests:

- [ ] Network errors (timeout, connection refused)
- [ ] Authentication errors (401, 403)
- [ ] Not found errors (404)
- [ ] Method not allowed errors (405)
- [ ] Server errors (500, 503)
- [ ] Validation errors (400)
- [ ] Concurrent operation conflicts
- [ ] Large file upload failures
- [ ] Invalid input data

### 3.3 Authentication Flow Tests
Test complete authentication flows:

- [ ] Initial authentication and token storage
- [ ] Token refresh on expiration
- [ ] Multiple concurrent requests with auth
- [ ] Auth failure and retry logic
- [ ] Switching between auth methods

### 3.4 Performance Benchmark Tests
Establish performance baselines:

- [ ] Tool execution time benchmarks
- [ ] File upload/download performance
- [ ] Large project handling (1000+ tables)
- [ ] Concurrent request handling
- [ ] Memory usage profiling
- [ ] Response time percentiles (p50, p95, p99)

## Phase 4: Contract & Compliance Tests (Week 5)

### 4.1 OpenL API Contract Tests
Ensure compatibility with OpenL Tablets REST API:

- [ ] Verify request formats match OpenL API expectations
- [ ] Validate response parsing handles all OpenL response formats
- [ ] Test against multiple OpenL versions (6.0.0+)
- [ ] Document API endpoint changes and compatibility

### 4.2 MCP Protocol Compliance Tests
Ensure MCP protocol compliance:

- [ ] Validate tool definitions against MCP spec
- [ ] Test resource URI formats
- [ ] Validate prompt definitions
- [ ] Test error response formats
- [ ] Validate JSON-RPC message formats

### 4.3 Schema Contract Tests
Input/output schema validation:

- [ ] Generate JSON Schema for all tool inputs
- [ ] Validate tool outputs match expected schemas
- [ ] Test schema evolution and versioning
- [ ] Document breaking vs non-breaking changes

### 4.4 Security Validation Tests
Security vulnerability testing:

- [ ] Input sanitization (SQL injection, XSS, command injection)
- [ ] Path traversal prevention
- [ ] Authentication bypass attempts
- [ ] Authorization validation
- [ ] Sensitive data exposure (passwords in logs)
- [ ] Rate limiting and DoS protection

## Phase 5: Documentation & Examples (Week 6)

### 5.1 API Reference Documentation
- [ ] Generate complete API reference from JSDoc comments
- [ ] Document all public methods with examples
- [ ] Create parameter reference tables
- [ ] Document return types and error conditions

### 5.2 Tool Reference Documentation
- [ ] Create detailed guide for each of the 24 tools
- [ ] Include input schema, examples, and use cases
- [ ] Document error scenarios and recovery
- [ ] Add troubleshooting tips

### 5.3 Prompt Usage Guide
- [ ] Document all 11 prompts with examples
- [ ] Create prompt selection decision tree
- [ ] Show argument usage patterns
- [ ] Include template syntax reference

### 5.4 Example Code Collection
Create comprehensive examples:

- [ ] Basic usage examples (getting started)
- [ ] Advanced workflow examples
- [ ] Error recovery patterns
- [ ] Best practices examples
- [ ] Anti-patterns to avoid

### 5.5 Troubleshooting Guide
- [ ] Common error messages and solutions
- [ ] Debug logging setup
- [ ] Network troubleshooting
- [ ] OpenL server connection issues
- [ ] Performance tuning tips

## Phase 6: Validation & Reporting (Week 7)

### 6.1 Coverage Validation
- [ ] Run full test suite with coverage
- [ ] Generate coverage reports
- [ ] Validate coverage meets goals:
  - Overall: >80% statements, >75% branches
  - Critical modules (auth, client): >85%
  - Utilities: >90%
- [ ] Document coverage gaps and justification

### 6.2 Spec Validation Report
- [ ] Create comprehensive validation report
- [ ] Document all test scenarios covered
- [ ] List known limitations and edge cases
- [ ] Provide recommendations for ongoing testing

### 6.3 CI/CD Integration
- [ ] Configure GitHub Actions for spec kit tests
- [ ] Set up coverage reporting in CI
- [ ] Configure test result publishing
- [ ] Set up automated regression testing

## Coverage Goals

### Overall Targets
- **Statements**: >80%
- **Branches**: >75%
- **Functions**: >85%
- **Lines**: >80%

### Per-Module Targets
| Module | Statements | Branches | Functions | Priority |
|--------|-----------|----------|-----------|----------|
| auth.ts | >90% | >85% | >90% | P0 - Critical |
| client.ts | >85% | >80% | >85% | P0 - Critical |
| utils.ts | >90% | >85% | >90% | P1 - Important |
| schemas.ts | >95% | 100% | 100% | P1 - Important |
| tools.ts | >90% | 100% | >90% | P1 - Important |
| prompts.ts | >85% | >80% | >85% | P2 - Nice to have |
| prompts-registry.ts | >95% | >90% | 100% | P2 - Nice to have |
| index.ts | >80% | >75% | >80% | P0 - Critical |
| constants.ts | 100% | 100% | 100% | ✅ Complete |

## Success Criteria

The spec kit is considered complete when:

1. ✅ All Phase 1-6 tasks are completed
2. ✅ Coverage goals are met for all P0 and P1 modules
3. ✅ All 24 tools have comprehensive test coverage
4. ✅ All 11 prompts are validated with examples
5. ✅ Integration tests cover all major workflows
6. ✅ Security validation passes
7. ✅ Documentation is complete and reviewed
8. ✅ CI/CD pipeline is configured and passing
9. ✅ Performance benchmarks are established
10. ✅ Spec validation report is approved

## Test Execution Strategy

### Local Development
```bash
# Run all spec kit tests
npm run test:spec-kit

# Run unit tests only
npm run test:spec-kit:unit

# Run integration tests only
npm run test:spec-kit:integration

# Generate coverage report
npm run test:spec-kit:coverage

# Run specific test suite
npm test -- spec-kit/unit-tests/auth.spec.ts
```

### CI/CD Pipeline
- Run on every push to `claude/spec-kit-*` branches
- Run full suite on PR to main branches
- Publish coverage reports
- Fail on coverage below thresholds
- Run security validation on release branches

## Deliverables

1. **Test Suite** - Complete spec kit test suite with >80% coverage
2. **Documentation** - Comprehensive documentation for all components
3. **Examples** - Rich example collection for common use cases
4. **Reports** - Coverage and validation reports
5. **CI/CD** - Automated testing pipeline
6. **Fixtures** - Reusable test data and scenarios

## Timeline

- **Week 1**: Foundation and infrastructure (Phase 1)
- **Week 2-3**: Unit test development (Phase 2)
- **Week 4**: Integration and workflow tests (Phase 3)
- **Week 5**: Contract and compliance tests (Phase 4)
- **Week 6**: Documentation and examples (Phase 5)
- **Week 7**: Validation and reporting (Phase 6)

**Total Duration**: 7 weeks
**Estimated Effort**: 140-175 hours
**Priority**: High (Required for production readiness)

## Next Steps

1. Review and approve this plan
2. Set up `spec-kit/` directory structure
3. Begin Phase 1: Foundation tasks
4. Create initial test infrastructure
5. Start unit test development for `auth.ts`

## References

- [TESTING.md](./TESTING.md) - Current testing guide
- [README.md](./README.md) - MCP server overview
- [CONTRIBUTING.md](./CONTRIBUTING.md) - Contributing guidelines
- [MCP SDK Documentation](https://github.com/modelcontextprotocol/sdk)
- [Jest Documentation](https://jestjs.io/)
- [OpenL Tablets Documentation](https://openl-tablets.org/)

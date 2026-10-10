# OpenL Studio Live Integration Tests

These tests run against an actual OpenL Studio 6.0.0 instance to verify MCP tool functionality.

## Setup

### 1. Configure OpenL Connection

Create `.env.test` file in `mcp-server/` directory:

```bash
# OpenL Studio Connection
OPENL_BASE_URL=http://localhost:8080
OPENL_PERSONAL_ACCESS_TOKEN=openl_pat_your-token   # optional (omit for single-user mode)

# Test Control
SKIP_LIVE_TESTS=false
```

### 2. Ensure OpenL Instance is Running

Make sure your OpenL Studio 6.0.0 instance is running and accessible:

```bash
curl http://localhost:8080/rest/repos \
  -H "Authorization: Token openl_pat_your-token"
```

### 3. Run Integration Tests

```bash
# Run all tests (unit + integration)
npm test

# Run only integration tests
npm run test:integration

# Run with verbose output
npm run test:integration -- --verbose

# Run specific test suite
npm run test:integration -- --testNamePattern="Project Discovery"
```

## Test Organization

Tests are organized by priority and functionality:

### P0: Critical Path (Must Work)
- ✅ Health Check
- ✅ Project Discovery (list_projects, get_project)
- ⏳ Project Lifecycle (open, project status, close)
- ⏳ Table Operations (list_tables, get_table)

### P1: Important Workflow
- ⏳ Testing (start_project_tests, get_test_results)
- ⏳ Project Files (read_project_file, write_project_file)

### P2: Advanced Features
- ⏳ Version Control (openl_repository_project_revisions)
- ⏳ Dimension Properties (get_table, update_table)

## Expected Output

Successful test run:
```
OpenL Studio 6.0.0 Live Integration Tests
  🔌 Connecting to OpenL Studio at: http://localhost:8080

  0. Health Check
    ✓ should connect to OpenL instance (250ms)
      ✅ Connected to OpenL Studio
         Base URL: http://localhost:8080
         Auth: Personal Access Token

  1. Repository Management (P1)
    ✓ list_repositories should return repositories (150ms)
      ✅ Found 1 repositories
         Repositories: design
    ✓ list_branches should return branches for design repository (120ms)
      ✅ Found 1 branches in 'design'
         Branches: master

  2. Project Discovery (P0 - CRITICAL)
    ✓ list_projects should return projects (200ms)
      ✅ Found 11 projects
         First project: Example 1 - Bank Rating
         Project ID type: string
         Using test project: design-Example 1 - Bank Rating
    ✓ list_projects with repository filter should work (180ms)
      ✅ Found 11 projects in 'design'
    ✓ get_project should return project details (350ms)
      ✅ Retrieved project: Example 1 - Bank Rating
         Status: OPENED
         Branch: master

  ... (more tests)

  📊 Integration Test Summary:
     Test Project: design-Example 1 - Bank Rating
     Test Table: 388cf75152fc76c44106546f1356e876
```

## Troubleshooting

### Tests are skipped
- Check that `SKIP_LIVE_TESTS=false` in `.env.test`
- Ensure `CI=true` is not set (defaults to skip in CI)

### Connection failures
- Verify OpenL instance is running
- Check base URL in `.env.test`
- Verify the token (`OPENL_PERSONAL_ACCESS_TOKEN`), if your server requires auth
- Test manually with curl command above

### 404 Errors
- Document which endpoints return 404
- Update `API_ENDPOINT_MAPPING.md` with results
- Consider alternative implementations

### Timeout Errors
- Increase timeout in test configuration
- Some operations (tests, file downloads) may take longer
- Check OpenL instance performance

## Adding New Tests

1. Add test to appropriate `describe` block based on priority
2. Follow naming convention: `{tool_name} should {expected_behavior}`
3. Include console.log statements for visibility
4. Handle errors gracefully (some endpoints may not exist)
5. Update this README with new test info

## Test Data Requirements

The integration tests assume:
- At least one repository (e.g., "design")
- At least one project in the repository
- At least one table in the first project
- Projects have valid project IDs compatible with `/projects/{projectId}`

If your OpenL instance differs, adjust test expectations accordingly.

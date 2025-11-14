# OpenL Studio MCP Server - Implementation Plan

## Project Overview

Develop an MCP (Model Context Protocol) server that enables AI assistants to control OpenL Tablets Studio programmatically through natural language commands.

## Architecture Design

### High-Level Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│  Claude/AI      │◄───────►│  MCP Server      │◄───────►│  OpenL Studio   │
│  Assistant      │  STDIO  │  (TypeScript)    │  REST   │  Backend        │
└─────────────────┘         └──────────────────┘         └─────────────────┘
                                    │
                                    ▼
                            ┌──────────────────┐
                            │  Configuration   │
                            │  (credentials)   │
                            └──────────────────┘
```

### Component Breakdown

#### 1. MCP Server Core (`src/index.ts`)
- Initialize MCP server using `@modelcontextprotocol/sdk`
- Register all tools
- Handle STDIO transport
- Manage server lifecycle

#### 2. OpenL Studio API Client (`src/client/openl-client.ts`)
- HTTP client for OpenL Studio REST API
- Session management and authentication
- Request/response handling
- Error handling and retries

#### 3. Tool Implementations (`src/tools/`)
- `list-projects.ts` - List all projects
- `open-project.ts` - Open project
- `close-project.ts` - Close project
- `export-project.ts` - Export project to ZIP
- `copy-project.ts` - Copy project
- `get-project-info.ts` - Get project details
- `list-repositories.ts` - List repositories

#### 4. Configuration (`src/config/`)
- `config.ts` - Load configuration from env/file
- `types.ts` - TypeScript type definitions

#### 5. Utilities (`src/utils/`)
- `logger.ts` - Logging to stderr
- `validators.ts` - Input validation
- `errors.ts` - Custom error classes

## Technology Stack

### Dependencies

```json
{
  "dependencies": {
    "@modelcontextprotocol/sdk": "^1.0.0",
    "node-fetch": "^3.3.0",
    "zod": "^3.22.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "typescript": "^5.0.0",
    "tsx": "^4.7.0"
  }
}
```

### Runtime Requirements
- Node.js 18+
- TypeScript 5+
- OpenL Tablets Studio instance (running)

## Implementation Phases

### Phase 1: Project Setup & Infrastructure (Day 1)

**Tasks:**
1. Initialize TypeScript project
2. Install dependencies
3. Configure TypeScript (`tsconfig.json`)
4. Set up project structure
5. Create configuration loader

**Deliverables:**
- Working TypeScript build
- Configuration system
- Basic project structure

### Phase 2: OpenL Studio API Client (Day 1-2)

**Tasks:**
1. Implement authentication flow
2. Create base HTTP client
3. Implement session management
4. Add error handling
5. Write unit tests for client

**Deliverables:**
- Fully functional OpenL Studio REST API client
- Session management
- Error handling

### Phase 3: MCP Server Core (Day 2)

**Tasks:**
1. Initialize MCP server
2. Set up STDIO transport
3. Configure logging (stderr only)
4. Implement server lifecycle

**Deliverables:**
- Basic MCP server that can start/stop
- Proper logging infrastructure

### Phase 4: Tool Implementation - Basic Operations (Day 2-3)

**Tasks:**
1. Implement `list_repositories` tool
2. Implement `list_projects` tool
3. Implement `get_project_info` tool
4. Add parameter validation using Zod
5. Write tool tests

**Deliverables:**
- 3 working read-only tools
- Validated parameters
- Tests

### Phase 5: Tool Implementation - Project Management (Day 3-4)

**Tasks:**
1. Implement `open_project` tool
2. Implement `close_project` tool
3. Add dependency handling for open operations
4. Write tool tests

**Deliverables:**
- 2 working project state management tools
- Tests

### Phase 6: Tool Implementation - Advanced Operations (Day 4-5)

**Tasks:**
1. Implement `export_project` tool
2. Implement `copy_project` tool
3. Handle file downloads for export
4. Add progress feedback
5. Write tool tests

**Deliverables:**
- 2 working advanced operation tools
- File handling
- Tests

### Phase 7: Integration & Testing (Day 5)

**Tasks:**
1. End-to-end testing with Claude Desktop
2. Test all tools in realistic scenarios
3. Fix bugs and edge cases
4. Performance testing

**Deliverables:**
- Fully tested MCP server
- Bug fixes
- Performance benchmarks

### Phase 8: Documentation & Deployment (Day 6)

**Tasks:**
1. Write README with installation instructions
2. Document configuration options
3. Create example usage scenarios
4. Build deployment package
5. Write deployment guide

**Deliverables:**
- Complete documentation
- Deployment guide
- Example configurations

## File Structure

```
mcp-server/openl-studio/
├── .spec/
│   ├── SPECIFICATION.md
│   ├── PLAN.md
│   └── TASKS.md
├── src/
│   ├── index.ts                 # MCP server entry point
│   ├── client/
│   │   ├── openl-client.ts     # OpenL API client
│   │   └── types.ts            # API types
│   ├── tools/
│   │   ├── list-repositories.ts
│   │   ├── list-projects.ts
│   │   ├── get-project-info.ts
│   │   ├── open-project.ts
│   │   ├── close-project.ts
│   │   ├── export-project.ts
│   │   └── copy-project.ts
│   ├── config/
│   │   ├── config.ts           # Configuration loader
│   │   └── types.ts            # Config types
│   └── utils/
│       ├── logger.ts           # Logging utility
│       ├── validators.ts       # Input validators
│       └── errors.ts           # Error classes
├── dist/                        # Compiled JavaScript
├── tests/                       # Test files
├── package.json
├── tsconfig.json
├── .env.example                 # Example configuration
└── README.md
```

## OpenL Studio API Mapping

### API Endpoints to Tool Mapping

| Tool | HTTP Method | Endpoint | Notes |
|------|-------------|----------|-------|
| `list_projects` | GET | `/user-workspace/*/projects` | Iterate repositories |
| `open_project` | POST | `/user-workspace/{repo}/projects/{proj}/open` | Query param: `open-dependencies` |
| `close_project` | POST | `/user-workspace/{repo}/projects/{proj}/close` | - |
| `get_project_info` | GET | `/user-workspace/{repo}/projects/{proj}/info` | Returns dependencies |
| `export_project` | GET | `/export/{proj}/version/{ver}` | Download ZIP file |
| `copy_project` | POST | `/copy` | Via CopyBean logic |
| `list_repositories` | GET | `/repositories` | List all repos |

### Authentication Flow

1. **Initial Authentication**
   ```
   POST /login
   Body: { username, password }
   Response: Set-Cookie with JSESSIONID
   ```

2. **Subsequent Requests**
   ```
   All requests include Cookie: JSESSIONID=...
   ```

3. **Session Refresh**
   ```
   Detect 401/403 responses
   Re-authenticate
   Retry original request
   ```

## Configuration Schema

```typescript
interface OpenLStudioConfig {
  baseUrl: string;          // e.g., "http://localhost:8080"
  username: string;
  password: string;
  timeout?: number;         // Request timeout (default: 30000ms)
  retries?: number;         // Retry attempts (default: 3)
}
```

### Configuration Sources (Priority Order)
1. Environment variables (`OPENL_BASE_URL`, `OPENL_USERNAME`, `OPENL_PASSWORD`)
2. Configuration file (`~/.openl-mcp/config.json`)
3. Default values (localhost:8080)

## Error Handling Strategy

### Error Categories

1. **Authentication Errors** (401, 403)
   - Re-authenticate
   - Retry operation
   - Report if still failing

2. **Not Found Errors** (404)
   - Return clear message
   - Suggest valid options

3. **Validation Errors** (400)
   - Parse error details
   - Return actionable message

4. **Server Errors** (500+)
   - Retry with backoff
   - Log full error
   - Report to user

5. **Network Errors**
   - Retry with exponential backoff
   - Check connectivity
   - Report after retries exhausted

### Error Response Format

```typescript
interface ToolError {
  success: false;
  error: string;           // User-friendly message
  details?: string;        // Technical details
  suggestion?: string;     // What user should do
}
```

## Testing Strategy

### Unit Tests
- API client methods
- Configuration loading
- Validators
- Error handlers

### Integration Tests
- Each tool against mock OpenL Studio
- Authentication flow
- Session management

### End-to-End Tests
- Full workflow with real OpenL Studio
- Claude Desktop integration
- Multi-tool scenarios

## Security Considerations

1. **Credential Storage**
   - Never log passwords
   - Use environment variables or secure config
   - Clear credentials from memory after use

2. **Input Validation**
   - Validate all user inputs
   - Sanitize file paths
   - Prevent injection attacks

3. **HTTPS**
   - Support HTTPS for production
   - Validate SSL certificates
   - Warn on insecure connections

4. **Permissions**
   - Respect OpenL Studio ACLs
   - Return clear permission denied messages
   - Don't expose unauthorized data

## Performance Targets

- **Tool Response Time**: < 5 seconds for standard operations
- **Export Operations**: < 30 seconds for typical projects
- **List Operations**: < 2 seconds
- **Concurrent Requests**: Support at least 5 concurrent operations

## Monitoring & Logging

### Log Levels
- **ERROR**: Failed operations, exceptions
- **WARN**: Retries, degraded performance
- **INFO**: Operation start/completion
- **DEBUG**: Detailed execution flow

### What to Log
- All API requests/responses (sanitize credentials)
- Tool invocations with parameters
- Errors with stack traces
- Performance metrics

### What NOT to Log
- Passwords or auth tokens
- Full project contents
- User session IDs (except in debug mode)

## Deployment

### Claude Desktop Integration

1. **Build the server**
   ```bash
   npm run build
   ```

2. **Configure Claude Desktop**
   Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:
   ```json
   {
     "mcpServers": {
       "openl-studio": {
         "command": "node",
         "args": ["/absolute/path/to/mcp-server/openl-studio/dist/index.js"],
         "env": {
           "OPENL_BASE_URL": "http://localhost:8080",
           "OPENL_USERNAME": "user",
           "OPENL_PASSWORD": "password"
         }
       }
     }
   }
   ```

3. **Restart Claude Desktop** (full quit and reopen)

4. **Test**
   - Ask Claude: "List all OpenL Studio projects"
   - Verify tools appear in Claude's tool list

## Success Criteria

- ✅ All 7 tools implemented and working
- ✅ Comprehensive error handling
- ✅ Full test coverage (>80%)
- ✅ Complete documentation
- ✅ Claude Desktop integration verified
- ✅ Performance targets met
- ✅ Security requirements satisfied

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| OpenL API changes | High | Low | Version checking, graceful degradation |
| Authentication issues | High | Medium | Robust session management, clear errors |
| Network timeouts | Medium | Medium | Retry logic, configurable timeouts |
| Large export files | Medium | Low | Streaming, progress feedback |
| Permission denied | Low | Medium | Clear error messages, validation |

## Future Enhancements

1. **Rule Module Operations**
   - Edit rules
   - Run tests
   - Deploy modules

2. **Batch Operations**
   - Open multiple projects
   - Export multiple versions

3. **Notifications**
   - Webhook support
   - Real-time updates

4. **Advanced Features**
   - Project creation from templates
   - Git integration
   - CI/CD pipeline integration

## Timeline Summary

- **Week 1**: Complete core implementation (Phases 1-6)
- **Week 2**: Testing, documentation, deployment (Phases 7-8)
- **Total**: 2 weeks for initial release

## Dependencies & Blockers

### External Dependencies
- OpenL Tablets Studio instance running
- Access to REST API endpoints
- Valid user credentials

### Internal Dependencies
- TypeScript build system
- MCP SDK availability
- Node.js runtime

### Potential Blockers
- Undocumented API changes
- Missing API endpoints
- Authentication restrictions
- Network accessibility

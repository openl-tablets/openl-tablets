# OpenL Tablets MCP Server

Model Context Protocol (MCP) server for [OpenL Tablets](https://github.com/openl-tablets/openl-tablets) Business Rules Management System. This server provides AI assistants with the ability to interact with OpenL Tablets rules projects through a standardized interface.

Built with MCP SDK v1.21.1, featuring type-safe schema validation, comprehensive error handling, and support for multiple authentication methods including OAuth 2.1.

## Features

### Resources
- **Repositories** - Browse design repositories
- **Projects** - View all projects across repositories
- **Deployments** - Monitor deployed projects

### Tools

All tools include metadata (`_meta`) with version information, categorization, and operation characteristics for better client integration.

#### System
- `health_check` - Check server connectivity and authentication status

#### Repository Management
- `list_repositories` - List all design repositories
- `list_branches` - List branches for a repository

#### Project Management
- `list_projects` - List projects with filters (repository, status, tag)
- `get_project` - Get detailed project information
- `get_project_info` - Get project info including modules and dependencies
- `open_project` - Open a project for viewing/editing
- `close_project` - Close an open project
- `get_project_history` - Get version history
- `create_branch` - Create a new branch

#### Rules (Tables) Management
- `list_tables` - List all tables/rules in a project
- `get_table` - Get detailed table data and structure
- `update_table` - Update table content
- `run_all_tests` - Run all tests in a project to validate rules correctness
- `validate_project` - Validate project for errors and warnings before deployment

#### Deployment
- `list_deployments` - List all deployments
- `deploy_project` - Deploy a project to production

### Technical Features

- **Type-Safe Schemas**: Zod-based input validation with automatic TypeScript type inference
- **Enhanced Error Handling**: Detailed error messages with endpoint, method, and tool context
- **Tool Metadata**: Each tool includes version, category, and operation flags
- **Multiple Authentication Methods**: Basic Auth, API Key, and OAuth 2.1 support
- **Request Tracking**: Optional Client Document ID for request correlation
- **Automatic Token Management**: OAuth 2.1 tokens are cached and automatically refreshed

## Installation

### Prerequisites

- Node.js 18.0.0 or higher
- Access to an OpenL Tablets WebStudio instance
- Valid credentials (username/password or API key)

### Setup

1. Navigate to the MCP server directory:
```bash
cd mcp-server
```

2. Install dependencies:
```bash
npm install
```

3. Build the TypeScript code:
```bash
npm run build
```

## Configuration

The server supports multiple authentication methods and configuration options. See [AUTHENTICATION.md](./AUTHENTICATION.md) for comprehensive authentication setup guides.

### Quick Start Configuration

Configure the server using environment variables:

```bash
# Required: OpenL Tablets WebStudio URL
export OPENL_BASE_URL="http://localhost:8080/webstudio/rest"

# Authentication Method 1: Basic Auth (username/password)
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Authentication Method 2: API Key
# export OPENL_API_KEY="your-api-key"

# Authentication Method 3: OAuth 2.1
# export OPENL_OAUTH2_CLIENT_ID="your-client-id"
# export OPENL_OAUTH2_CLIENT_SECRET="your-client-secret"
# export OPENL_OAUTH2_TOKEN_URL="https://auth.example.com/oauth/token"
# export OPENL_OAUTH2_SCOPE="openl:read openl:write"

# Optional: Client Document ID for request tracking
export OPENL_CLIENT_DOCUMENT_ID="mcp-server-1"

# Optional: Request timeout in milliseconds (default: 30000)
export OPENL_TIMEOUT="60000"
```

### Configuration Files

Create a `.env` file in the MCP server directory (see `.env.example` for template):

```env
OPENL_BASE_URL=http://localhost:8080/webstudio/rest
OPENL_USERNAME=admin
OPENL_PASSWORD=admin
OPENL_CLIENT_DOCUMENT_ID=mcp-server-1
```

For production with OAuth 2.1:

```env
OPENL_BASE_URL=https://openl-production.example.com/webstudio/rest
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_CLIENT_SECRET=your-client-secret
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_SCOPE=openl:read openl:write
OPENL_CLIENT_DOCUMENT_ID=mcp-server-1
OPENL_TIMEOUT=60000
```

## Usage

### Running the Server

Direct execution:
```bash
npm start
```

Development mode with auto-rebuild:
```bash
npm run watch
```

### Configuring MCP Clients

#### Claude Desktop

Add to your Claude Desktop configuration file:

**MacOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

**Basic Authentication**:
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/absolute/path/to/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin",
        "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-1"
      }
    }
  }
}
```

**OAuth 2.1 Authentication**:
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/absolute/path/to/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "https://openl-production.example.com/webstudio/rest",
        "OPENL_OAUTH2_CLIENT_ID": "your-client-id",
        "OPENL_OAUTH2_CLIENT_SECRET": "your-client-secret",
        "OPENL_OAUTH2_TOKEN_URL": "https://auth.example.com/oauth/token",
        "OPENL_OAUTH2_SCOPE": "openl:read openl:write",
        "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-1"
      }
    }
  }
}
```

See [claude-desktop-config.example.json](./claude-desktop-config.example.json) for additional configuration examples.

#### Other MCP Clients

The server uses stdio transport and can be integrated with any MCP-compatible client. Provide the path to `dist/index.js` and set the required environment variables.

## Tool Usage Examples

### Check Server Health

```typescript
// Request
{
  "name": "health_check",
  "arguments": {}
}

// Response
{
  "status": "healthy",
  "baseUrl": "http://localhost:8080/webstudio/rest",
  "authMethod": "OAuth 2.1",
  "timestamp": "2025-11-10T12:30:00.000Z",
  "serverReachable": true
}
```

### List All Projects

```typescript
// Request
{
  "name": "list_projects",
  "arguments": {}
}

// Response
[
  {
    "name": "insurance-rules",
    "repository": "design",
    "status": "OPENED",
    "modifiedAt": "2025-11-10T10:30:00Z",
    "modifiedBy": "admin"
  }
]
```

### Get Project Details

```typescript
// Request
{
  "name": "get_project",
  "arguments": {
    "projectId": "design-insurance-rules"
  }
}
```

### List Tables in a Project

```typescript
// Request
{
  "name": "list_tables",
  "arguments": {
    "projectId": "design-insurance-rules"
  }
}

// Response
[
  {
    "id": "Rules.xls_1234",
    "tableType": "simplerules",
    "kind": "XLS_DT",
    "name": "CalculatePremium",
    "returnType": "Double",
    "file": "Rules.xlsx"
  }
]
```

### Get Table Data

```typescript
// Request
{
  "name": "get_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234"
  }
}

// Returns full table structure including:
// - Properties
// - Columns/rows (for decision tables)
// - Fields (for datatypes)
// - Spreadsheet data
```

### Update Table

```typescript
// Request
{
  "name": "update_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234",
    "view": {
      // Modified table view object
    },
    "comment": "Updated premium calculation logic"
  }
}
```

### Deploy to Production

```typescript
// Request
{
  "name": "deploy_project",
  "arguments": {
    "projectName": "insurance-rules",
    "repository": "design",
    "deploymentRepository": "production"
  }
}
```

## API Reference

### OpenL Tablets REST API

This MCP server wraps the OpenL Tablets WebStudio REST API. For detailed API documentation, refer to:

- **API Base Path**: `/webstudio/rest`
- **Swagger UI**: `http://your-server:8080/webstudio/swagger-ui.html`
- **OpenAPI Spec**: `http://your-server:8080/webstudio/v3/api-docs`

### Project ID Format

Project IDs follow the format: `{repository-name}-{project-name}`

Examples:
- `design-insurance-rules`
- `production-loan-calculator`

### Table Types

OpenL Tablets supports various table types:
- `datatype` - Custom data type definitions
- `vocabulary` - Enumeration/vocabulary tables
- `spreadsheet` - Spreadsheet calculation tables
- `simplerules` - Simple decision tables
- `smartrules` - Smart decision tables
- `method` - Method tables
- `test` - Test tables
- `data` - Data tables

## Development

### Project Structure

The codebase is organized into modular components for maintainability and extensibility:

```
mcp-server/
├── src/
│   ├── index.ts          # Main MCP server entry point
│   ├── client.ts         # OpenL Tablets API client
│   ├── auth.ts           # Authentication manager (OAuth 2.1, API Key, Basic Auth)
│   ├── tools.ts          # MCP tool definitions and metadata
│   ├── schemas.ts        # Zod schemas for tool input validation
│   ├── types.ts          # TypeScript type definitions
│   ├── constants.ts      # Configuration constants and defaults
│   └── utils.ts          # Security and utility functions
├── tests/                # Test suites (Jest)
│   ├── openl-client.test.ts
│   ├── mcp-server.test.ts
│   └── mocks/            # Mock data for testing
├── dist/                 # Compiled JavaScript (generated)
├── package.json
├── tsconfig.json
├── jest.config.js
├── eslint.config.mjs
├── .env.example
├── claude-desktop-config.example.json
├── README.md             # This file
├── AUTHENTICATION.md     # Authentication setup guide
├── TESTING.md            # Testing documentation
├── EXAMPLES.md           # Usage examples
└── CONTRIBUTING.md       # Development and contribution guide
```

#### Module Responsibilities

- **index.ts**: MCP server setup, tool handlers, resource providers
- **client.ts**: High-level API client for OpenL Tablets REST API
- **auth.ts**: Authentication lifecycle, token management, request/response interceptors
- **tools.ts**: Tool definitions with metadata, categorization, and versioning
- **schemas.ts**: Zod schemas for type-safe input validation
- **types.ts**: TypeScript interfaces for OpenL Tablets API and internal types
- **constants.ts**: Configuration defaults, endpoint paths, and constants
- **utils.ts**: Security utilities (error sanitization, input validation, safe JSON)

### Building

```bash
npm run build
```

### Testing

Run the comprehensive test suite:

```bash
npm test                  # Run all tests
npm run test:watch        # Watch mode
npm run test:coverage     # With coverage report
```

See [TESTING.md](./TESTING.md) for detailed testing documentation.

### Linting

```bash
npm run lint              # Check for issues
npm run lint:fix          # Fix auto-fixable issues
```

### Type Checking

The project uses TypeScript with strict type checking:
- **API Types**: OpenL Tablets API types in `src/types.ts`
- **Schema Validation**: Zod schemas in `src/schemas.ts` with automatic TypeScript type inference
- **MCP SDK**: Full type support from `@modelcontextprotocol/sdk` v1.21.1

### Extending the Server

The modular architecture makes it easy to extend the server with new functionality:

**Adding a New Tool**:
1. Define the input schema in `src/schemas.ts`
2. Add the tool definition to `src/tools.ts`
3. Add the API method (if needed) to `src/client.ts`
4. Add the tool handler in `src/index.ts`
5. Add tests and documentation

**Adding Authentication Methods**:
1. Update types in `src/types.ts`
2. Modify `src/auth.ts` to implement the new authentication
3. Update `getAuthMethod()` for health checks
4. Document in `AUTHENTICATION.md`

See [CONTRIBUTING.md](./CONTRIBUTING.md) for detailed extension guidelines.

## Troubleshooting

### Health Check

Use the `health_check` tool to verify connectivity and authentication:

```typescript
{
  "name": "health_check",
  "arguments": {}
}
```

This will report server status, authentication method, and connectivity issues.

### Connection Issues

If you encounter connection errors:

1. Run `health_check` tool to diagnose the issue
2. Verify OpenL Tablets WebStudio is running
3. Check the `OPENL_BASE_URL` is correct
4. Ensure the REST API path is `/webstudio/rest`
5. Verify network connectivity
6. Review enhanced error messages for endpoint and method details

### Authentication Errors

If you receive 401/403 errors:

1. Run `health_check` to see which authentication method is active
2. Verify credentials:
   - **Basic Auth**: Check username/password
   - **API Key**: Verify API key is valid
   - **OAuth 2.1**: Check token URL, client ID/secret, and scope
3. Check user has appropriate permissions in OpenL Tablets
4. For OAuth 2.1: Token refresh happens automatically, check logs for token acquisition issues
5. Ensure the user can access the WebStudio interface

See [AUTHENTICATION.md](./AUTHENTICATION.md) for detailed authentication troubleshooting.

### Project Not Found

If projects cannot be found:

1. Ensure the project is in a design repository
2. Check the project ID format: `{repo-name}-{project-name}`
3. Verify the project exists in OpenL Tablets WebStudio
4. Review error message for endpoint details

### Table Access Issues

To access tables:

1. Project must be OPENED first using `open_project`
2. Use the correct `projectId` and `tableId`
3. Check user has edit permissions for modifications
4. Enhanced error messages will indicate which operation failed

## Architecture

### OpenL Tablets Structure

```
OpenL Tablets
├── Design Repositories (Git, File System, etc.)
│   └── Projects
│       ├── rules.xml (Project descriptor)
│       └── Modules
│           └── Excel Files (.xlsx, .xls)
│               └── Tables (Decision Tables, Datatypes, etc.)
│
└── Production Repositories
    └── Deployed Projects
```

### MCP Server Flow

1. Client connects via stdio transport
2. Server exposes resources and tools
3. Tools make REST API calls to OpenL Tablets
4. Responses formatted and returned to client

## Security Considerations

### Authentication Best Practices

- **Production Systems**: Use OAuth 2.1 for enterprise deployments
- **Development**: Basic Auth or API Key for local development
- **Credential Storage**: Always use environment variables, never hardcode credentials
- **Transport Security**: Use HTTPS for all production OpenL Tablets instances
- **Version Control**: Never commit credentials to version control
- **Access Control**: Implement proper user permissions in OpenL Tablets

### Authentication Method Security

1. **OAuth 2.1** (Most Secure)
   - Automatic token rotation
   - Short-lived access tokens
   - Secure token storage in memory
   - Support for refresh tokens
   - Industry-standard protocol

2. **API Key** (Recommended for non-OAuth systems)
   - Long-lived credentials
   - Rotate keys regularly
   - Store securely in environment variables

3. **Basic Auth** (Development Only)
   - Username/password transmitted with each request
   - Use only over HTTPS in production
   - Prefer OAuth 2.1 or API Key for production

### Request Tracking

Use `OPENL_CLIENT_DOCUMENT_ID` to:
- Track requests across distributed systems
- Correlate MCP operations with OpenL Tablets logs
- Debug issues in production environments
- Identify which client instance made changes

See [AUTHENTICATION.md](./AUTHENTICATION.md) for comprehensive security guidelines.

## MCP Best Practices Implementation

This server follows the latest MCP specification (2025) and implements recommended best practices:

### Schema Validation (Zod)

All tool inputs are validated using Zod schemas:
- **Type Safety**: Automatic TypeScript type inference from schemas
- **Runtime Validation**: Input validation before processing
- **Better Error Messages**: Clear validation errors for invalid inputs
- **Schema Documentation**: Self-documenting API through schema descriptions

Example from `src/schemas.ts`:
```typescript
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository name"),
  status: z.string().optional().describe("Filter by project status"),
  tag: z.string().optional().describe("Filter by tag name"),
});
```

### Tool Metadata

Each tool includes `_meta` fields for better client integration:
- **version**: Semantic versioning for tool evolution
- **category**: Logical grouping (system, repository, project, rules, deployment)
- **requiresAuth**: Indicates authentication requirement
- **modifiesState**: Marks state-changing operations

Example:
```typescript
{
  name: "deploy_project",
  _meta: {
    version: "1.0.0",
    category: "deployment",
    requiresAuth: true,
    modifiesState: true
  }
}
```

### Enhanced Error Handling

Errors include contextual information:
- **HTTP Status**: Response status code
- **Endpoint**: The API endpoint that failed
- **Method**: HTTP method (GET, POST, PUT, DELETE)
- **Tool Name**: Which MCP tool was executing
- **Error Message**: Detailed error description

Example error:
```
OpenL Tablets API error (404): Project not found [GET /design-repositories/design/projects/myproject]
Tool: get_project
```

### Health Check Tool

The `health_check` tool provides:
- Server connectivity verification
- Authentication method detection
- Real-time status reporting
- Troubleshooting information

### SDK Version

Built with `@modelcontextprotocol/sdk` v1.21.1, ensuring:
- Latest protocol features
- Improved error handling
- Better TypeScript support
- Performance optimizations

## Contributing

This MCP server is part of the OpenL Tablets project. For issues or contributions, please refer to the main OpenL Tablets repository.

## License

This MCP server follows the same license as the OpenL Tablets project.

## Resources

- [OpenL Tablets GitHub](https://github.com/openl-tablets/openl-tablets)
- [OpenL Tablets Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)
- [Zod Documentation](https://zod.dev/)

## Support

For issues specific to:
- **MCP Server**: Open an issue in the OpenL Tablets repository
- **OpenL Tablets**: Refer to OpenL Tablets documentation and community
- **MCP Protocol**: Refer to Model Context Protocol documentation

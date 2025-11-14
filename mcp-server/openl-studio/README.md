# OpenL Studio MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that enables AI assistants like Claude to interact with OpenL Tablets Studio programmatically. This allows you to control OpenL Studio through natural language commands instead of manual UI interactions.

## Features

- **Project Management**: Open, close, and get information about OpenL Studio projects
- **Project Operations**: Export projects as ZIP files, copy projects to new names/repositories
- **Repository Management**: List all configured repositories
- **Natural Language Interface**: Control OpenL Studio using conversational commands via Claude or other MCP clients

## Available Tools

The MCP server provides the following tools:

| Tool | Description |
|------|-------------|
| `list_repositories` | List all configured OpenL Studio repositories |
| `list_projects` | List all available projects (with optional repository filter) |
| `get_project_info` | Get detailed information about a specific project including dependencies |
| `open_project` | Open a project and optionally its dependencies |
| `close_project` | Close a project and release its resources |
| `export_project` | Export a project as a ZIP file |
| `copy_project` | Copy a project to a new name and/or repository |

## Prerequisites

- **Node.js** 18 or higher
- **OpenL Tablets Studio** instance running and accessible
- Valid OpenL Studio credentials (username and password)

## Installation

1. **Clone the repository** (if not already done):
   ```bash
   git clone https://github.com/openl-tablets/openl-tablets.git
   cd openl-tablets/mcp-server/openl-studio
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Build the server**:
   ```bash
   npm run build
   ```

## Configuration

The MCP server can be configured using environment variables or a configuration file.

### Option 1: Environment Variables

Set the following environment variables:

```bash
export OPENL_BASE_URL=http://localhost:8080
export OPENL_USERNAME=admin
export OPENL_PASSWORD=admin
export LOG_LEVEL=INFO  # Optional: ERROR, WARN, INFO, DEBUG
```

### Option 2: Configuration File

Create a configuration file at `~/.openl-mcp/config.json`:

```json
{
  "openl": {
    "baseUrl": "http://localhost:8080",
    "username": "admin",
    "password": "admin",
    "timeout": 30000,
    "retries": 3
  },
  "logLevel": "INFO"
}
```

### Configuration Priority

Configuration sources are merged in the following priority order (highest to lowest):
1. Environment variables
2. Configuration file (`~/.openl-mcp/config.json`)
3. Default values

## Usage with Claude Desktop

### Step 1: Configure Claude Desktop

Edit your Claude Desktop configuration file:

**macOS/Linux**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%AppData%\Claude\claude_desktop_config.json`

Add the MCP server configuration:

```json
{
  "mcpServers": {
    "openl-studio": {
      "command": "node",
      "args": ["/absolute/path/to/openl-tablets/mcp-server/openl-studio/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin",
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

**Important Notes**:
- Use **absolute paths**, not relative paths
- Replace `/absolute/path/to/` with the actual path on your system
- Ensure your OpenL Studio instance is running before starting Claude

### Step 2: Restart Claude Desktop

**macOS**: Fully quit Claude (Cmd+Q, not just closing the window)
**Windows**: Exit Claude completely from the system tray
**Linux**: Kill the Claude process

Then reopen Claude Desktop.

### Step 3: Verify Installation

Ask Claude:
```
List all OpenL Studio repositories
```

If configured correctly, Claude will use the `list_repositories` tool and return your repositories.

## Example Usage

Once configured, you can interact with OpenL Studio using natural language:

### List all projects
```
Show me all OpenL Studio projects
```

### Get project information
```
Give me details about the "STD Rating" project
```

### Open a project
```
Open the "STD Rating" project with its dependencies
```

### Close a project
```
Close the "STD Rating" project
```

### Export a project
```
Export the "STD Rating" project to a ZIP file
```

### Copy a project
```
Copy the "STD Rating" project to "STD Rating V2"
```

## Development

### Project Structure

```
mcp-server/openl-studio/
├── .spec/                      # Specification documents
│   ├── SPECIFICATION.md        # Feature specification
│   ├── PLAN.md                 # Implementation plan
│   └── TASKS.md                # Task breakdown
├── src/
│   ├── index.ts                # MCP server entry point
│   ├── client/
│   │   ├── openl-client.ts     # OpenL API client
│   │   └── types.ts            # API types
│   ├── tools/                  # MCP tool implementations
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
├── dist/                       # Compiled JavaScript
├── package.json
├── tsconfig.json
├── .env.example
└── README.md
```

### Available Scripts

- `npm run build` - Compile TypeScript to JavaScript
- `npm run dev` - Run server in development mode with tsx
- `npm start` - Run the compiled server
- `npm run watch` - Watch for changes and recompile
- `npm run clean` - Remove compiled files

### Building

```bash
npm run build
```

This compiles the TypeScript source code in `src/` to JavaScript in `dist/`.

### Running Locally

For testing without Claude:

```bash
# Set environment variables
export OPENL_BASE_URL=http://localhost:8080
export OPENL_USERNAME=admin
export OPENL_PASSWORD=admin

# Run the server
npm run dev
```

## Troubleshooting

### Server doesn't appear in Claude

1. **Check the config file path**: Ensure you edited the correct `claude_desktop_config.json`
2. **Verify absolute paths**: All paths in the config must be absolute, not relative
3. **Fully restart Claude**: Use Cmd+Q (macOS) or exit from system tray (Windows), don't just close the window
4. **Check logs**: Look at `~/Library/Logs/Claude/mcp.log` for connection errors
5. **Verify Node.js**: Ensure Node.js 18+ is installed: `node --version`

### Authentication Errors

1. **Check credentials**: Verify `OPENL_USERNAME` and `OPENL_PASSWORD` are correct
2. **Check URL**: Ensure `OPENL_BASE_URL` is accessible from your machine
3. **Test manually**: Try accessing OpenL Studio in a browser with the same credentials

### Connection Errors

1. **OpenL Studio running**: Verify OpenL Studio is running at the configured URL
2. **Network access**: Ensure your machine can reach the OpenL Studio server
3. **Firewall**: Check if firewall rules are blocking the connection

### Tool Execution Errors

Check the MCP server logs:
- **macOS/Linux**: `~/Library/Logs/Claude/mcp-server-openl-studio.log`
- **Windows**: `%AppData%\Claude\Logs\mcp-server-openl-studio.log`

For more detailed logging, set `LOG_LEVEL=DEBUG` in your configuration.

## Security Considerations

- **Credentials**: Never commit credentials to version control
- **Use environment variables**: Prefer environment variables over config files for sensitive data
- **HTTPS**: Use HTTPS for production OpenL Studio instances
- **Access Control**: The MCP server respects OpenL Studio's ACL permissions
- **Logs**: Passwords are never logged (even in DEBUG mode)

## API Documentation

The MCP server communicates with OpenL Studio using its REST API. The following endpoints are used:

| Operation | Method | Endpoint |
|-----------|--------|----------|
| List repositories | GET | `/admin/repositories` |
| List projects | GET | `/user-workspace/{repo}/projects` |
| Get project info | GET | `/user-workspace/{repo}/projects/{proj}/info` |
| Open project | POST | `/user-workspace/{repo}/projects/{proj}/open` |
| Close project | POST | `/user-workspace/{repo}/projects/{proj}/close` |
| Export project | GET | `/export/{proj}/version/{ver}` |
| Copy project | POST | `/copy` |

## Contributing

Contributions are welcome! Please follow the existing code style and add tests for new features.

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Build and test
5. Submit a pull request

## License

Apache-2.0 - see the [LICENSE](../../LICENSE) file for details.

## Support

For issues and questions:
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Documentation**: https://openl-tablets.org/documentation

## Acknowledgments

- Built using [Model Context Protocol](https://modelcontextprotocol.io)
- Developed for [OpenL Tablets](https://openl-tablets.org)
- Spec-driven development methodology using spec-kit

---

**Note**: This MCP server was developed following spec-driven development practices. See the `.spec/` directory for detailed specifications, plans, and task breakdowns.

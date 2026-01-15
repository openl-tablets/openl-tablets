# Configuration Examples

This directory contains example configuration files for setting up the MCP server with different clients and authentication methods.

## Files

- **`claude-desktop-config.example.json`** - Examples for Claude Desktop with different authentication methods:
  - Basic Auth
  - API Key
  - OAuth 2.1 (token-url)
  - OAuth 2.1 (issuer-uri)
  - OAuth 2.1 PKCE (Ping Identity)
  - Personal Access Token (PAT)

- **`claude-desktop-pat-config.example.json`** - Examples for Claude Desktop with PAT authentication:
  - **Remote MCP Server via SSE** (Recommended) - Uses `mcp-remote` with Authorization header to connect to remote server
  - Local MCP Server via stdio - Runs MCP server locally, connects to remote OpenL via REST API
  - Local development - Both MCP server and OpenL running locally

- **`cursor-pat-config.example.json`** - Example for Cursor IDE with Personal Access Token (PAT) authentication (local MCP server)

- **`cursor-docker-pat-config.example.json`** - Example for Cursor IDE connecting to Docker container via HTTP SSE with PAT authentication

- **`cursor-docker-config.example.json`** - Example for Cursor IDE connecting to Docker container via HTTP proxy

## Usage

1. Copy the example file you need:
   ```bash
   cp docs/setup/examples/claude-desktop-config.example.json ~/Library/Application\ Support/Claude/claude_desktop_config.json
   ```

2. Replace placeholders:
   - `<path-to-node>` - Your Node.js path (e.g., `node` or `/usr/bin/node`)
   - `<path-to-project>` - Your OpenL Tablets project directory

3. Update authentication credentials if needed

## Quick Start: Remote MCP Server (Simplest)

For connecting to a remote OpenL instance, use `mcp-remote` with Authorization header:

```json
{
  "mcpServers": {
    "openl-remote": {
      "command": "/path/to/node",
      "args": [
        "/path/to/mcp-remote",
        "https://openl.exigengroup.com/mcp/sse",
        "--header",
        "Authorization: Token your-pat-token-here"
      ]
    }
  }
}
```

**Example with nvm:**

```json
{
  "mcpServers": {
    "openl-remote": {
      "command": "/Users/username/.nvm/versions/node/v22.16.0/bin/node",
      "args": [
        "/Users/username/.nvm/versions/node/v22.16.0/bin/mcp-remote",
        "https://openl.exigengroup.com/mcp/sse",
        "--header",
        "Authorization: Token your-pat-token-here"
      ]
    }
  }
}
```

**Finding paths:**

```bash
# Find Node.js path
which node
# Example: /Users/username/.nvm/versions/node/v22.16.0/bin/node

# Find mcp-remote path (usually in the same directory as node)
which mcp-remote
# Or: <node-directory>/mcp-remote
```

**Important:**
- Replace `/path/to/node` with your Node.js executable path
- Replace `/path/to/mcp-remote` with your `mcp-remote` executable path
- Replace `your-pat-token-here` with your actual Personal Access Token from OpenL Tablets UI (User Settings → Personal Access Tokens)

This requires no local setup - just Node.js and `mcp-remote` installed. You need to provide your PAT token for authentication.

**Note**: If you get `ENOTFOUND` errors, check your network connection, DNS settings, or VPN configuration. See [Troubleshooting](../CLAUDE-DESKTOP.md#issue-dnsnetwork-error-enotfound) for details.

## Important Notes

- **Never commit real configuration files** - They contain personal paths and credentials
- Real config files are automatically ignored by `.gitignore`
- Use example files as templates only
- For remote access, prefer `mcp-remote` with Authorization header (simplest)
- For local development, use stdio transport with local MCP server

## Documentation

For detailed setup instructions, see:
- [Claude Desktop & Cursor Setup](../CLAUDE-DESKTOP.md)
- [Cursor Docker Setup](../CURSOR-DOCKER.md)
- [Authentication Guide](../../guides/AUTHENTICATION.md)


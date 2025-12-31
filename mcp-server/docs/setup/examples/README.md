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

## Important Notes

- **Never commit real configuration files** - They contain personal paths and credentials
- Real config files are automatically ignored by `.gitignore`
- Use example files as templates only

## Documentation

For detailed setup instructions, see:
- [Claude Desktop & Cursor Setup](../CLAUDE-DESKTOP.md)
- [Cursor Docker Setup](../CURSOR-DOCKER.md)
- [Authentication Guide](../../guides/AUTHENTICATION.md)


# OpenL Studio MCP Server Guide

OpenL Studio includes an MCP server. AI agents and IDEs, such as Claude Code, Claude Desktop, Visual Studio Code with
GitHub Copilot, or Cursor, connect to it over the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) and
work with OpenL projects for their user: they find projects and tables, read and change rules, run tests and tables,
debug how a rule computes its result, work with branches, and deploy projects.

## Guide Contents

- [Connecting an AI Agent](connecting.md) - The address of the server, authentication, and the configuration of
  popular MCP clients
- [Working with an AI Agent](working-with-agents.md) - What the agent can do, how it works on a project, and requests
  to try
- [Troubleshooting](troubleshooting.md) - What to check when the agent cannot connect or a request fails

## How the MCP Server Works

- **Part of OpenL Studio** — the server is a Node.js program that OpenL Studio starts and stops with itself. The
  OpenL Studio Docker image and the OpenL Tablets Demo package run it without any setup.
- **One address** — the server answers at `/mcp` of the address of OpenL Studio, such as `http://localhost:8080/mcp`,
  over the Streamable HTTP transport of MCP: every request is an HTTP `POST`, answered with JSON.
- **The rights of the user** — the agent sends a personal access token of its user, and the server calls OpenL Studio
  with it. The agent sees and changes only what this user can see and change in OpenL Studio.
- **The same release** — the server reports the version of OpenL Studio, and offers the agent the OpenL Tablets
  reference guides of the same release.

For the settings of the server, see
[Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL Tablets Installation
Guide.

---

OpenL Tablets Documentation is licensed under a [Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/).

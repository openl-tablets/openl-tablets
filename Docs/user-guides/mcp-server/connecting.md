## Connecting an AI Agent

This section describes how to connect an AI agent or an IDE to the MCP server of OpenL Studio.

The following topics are included in this section:

-   [Before You Connect](#before-you-connect)
-   [Finding the Address of the Server](#finding-the-address-of-the-server)
-   [Authenticating the Agent](#authenticating-the-agent)
-   [Configuring the MCP Client](#configuring-the-mcp-client)
-   [Checking the Connection](#checking-the-connection)

### Before You Connect

The following is required:

-   **OpenL Studio that runs the MCP server.** The OpenL Studio Docker image and the OpenL Tablets Demo package run it
    without any setup. OpenL Studio deployed in another way runs it when Node.js is installed and the web server
    forwards `/mcp` to it; for more information, see
    [Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL Tablets
    Installation Guide.
-   **An MCP client with the Streamable HTTP transport.** The client is the AI agent or the IDE the user works in, such
    as Claude Code, Visual Studio Code with GitHub Copilot in agent mode, or Cursor. In its settings, the transport is
    usually named **HTTP**, **Streamable HTTP**, or a remote server.

### Finding the Address of the Server

The server answers at `/mcp` of the host OpenL Studio runs on, whatever address OpenL Studio itself opens at:

| OpenL Studio                      | OpenL Studio address                | MCP server address           |
|-----------------------------------|-------------------------------------|------------------------------|
| Docker image                      | `http://localhost:8080`             | `http://localhost:8080/mcp`  |
| OpenL Tablets Demo package        | `http://localhost:8080/webstudio`   | `http://localhost:8080/mcp`  |
| Behind a company web server       | `https://studio.example.com`        | `https://studio.example.com/mcp` |

When OpenL Studio runs on another computer, use the address of that computer, and HTTPS to keep the token from being
sent in clear text.

### Authenticating the Agent

The agent authenticates the same way in every MCP client:

-   **Single-user mode** — no credentials are needed. The agent works as the single user of OpenL Studio.
-   **Other user modes** — the agent sends a personal access token of the user in the `Authorization` header of every
    request, as `Bearer <token>` or `Token <token>`. The agent then has the rights of this user, no more.

To create a token, in OpenL Studio, click the user icon, select **My Profile** > **Personal Access Tokens**, and click
**+ Create Token**. For more information, see [Managing Personal Access
Tokens](../openl-studio/getting-started.md#managing-personal-access-tokens) in the OpenL Studio User Guide. Create a
token for each agent, so that a token can be deleted without affecting the others.

> [!Note]
> Keep the token out of files that are committed to a repository. Each client below has a way to read it from an
> environment variable or to ask for it.

### Configuring the MCP Client

The examples below name the server `openl` and use `http://localhost:8080/mcp`. Replace the address with the address
of the server and `<token>` with the token. In single-user mode, leave out the `Authorization` header.

#### Claude Code

Add the server from a terminal:

```bash
claude mcp add --transport http openl http://localhost:8080/mcp --header "Authorization: Bearer <token>"
```

The server is added for the current project. To add it for every project, add `--scope user`; to share it with the
team through the `.mcp.json` file of the project, add `--scope project`. A shared file can read the token from an
environment variable, such as `OPENL_TOKEN`:

```json
{
  "mcpServers": {
    "openl": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer ${OPENL_TOKEN}"
      }
    }
  }
}
```

#### Visual Studio Code with GitHub Copilot

Create the `.vscode/mcp.json` file in the workspace. Visual Studio Code asks for the token the first time it starts
the server and keeps it in its secret storage:

```json
{
  "inputs": [
    {
      "type": "promptString",
      "id": "openl-token",
      "description": "OpenL Studio personal access token",
      "password": true
    }
  ],
  "servers": {
    "openl": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer ${input:openl-token}"
      }
    }
  }
}
```

In the **Chat** view, select **Agent** mode, and enable the `openl` tools in the tools menu.

#### Cursor

Add the server to `.cursor/mcp.json` of the project, or to `~/.cursor/mcp.json` for every project:

```json
{
  "mcpServers": {
    "openl": {
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

#### Other MCP Clients

Any client that supports the Streamable HTTP transport connects with the same three values:

-   the transport: **HTTP** or **Streamable HTTP**;
-   the address of the server;
-   the `Authorization` header with the token, except in single-user mode.

### Checking the Connection

Most clients show the state of their servers: in Claude Code, `claude mcp list` lists `openl` as connected. Then ask
the agent:

```
List the repositories of OpenL Studio.
```

The agent calls the `openl_list_repositories` tool and answers with the design repositories the user can see. If it
does not, see [Troubleshooting](troubleshooting.md).

## Connecting an AI Agent

This section describes how to connect an AI agent or an IDE to the MCP server of OpenL Studio.

The following topics are included in this section:

-   [Before You Connect](#before-you-connect)
-   [Finding the Address of the Server](#finding-the-address-of-the-server)
-   [Negotiating the Protocol Version](#negotiating-the-protocol-version)
-   [Authenticating the Agent](#authenticating-the-agent)
-   [Configuring the MCP Client](#configuring-the-mcp-client)
    -   [Claude Code](#claude-code)
    -   [Claude Desktop](#claude-desktop)
    -   [Visual Studio Code with GitHub Copilot](#visual-studio-code-with-github-copilot)
    -   [Cursor](#cursor)
    -   [Codex](#codex)
    -   [ChatGPT](#chatgpt)
    -   [Other MCP Clients](#other-mcp-clients)
-   [Checking the Connection](#checking-the-connection)

### Before You Connect

The following is required:

-   **OpenL Studio that runs the MCP server.** The OpenL Studio Docker image and the OpenL Tablets Demo package run it
    without any setup. OpenL Studio deployed in another way runs it when Node.js is installed; for more information,
    see
    [Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL Tablets
    Installation Guide.
-   **An MCP client with the Streamable HTTP transport.** The client is the AI agent or the IDE the user works in, such
    as Claude Code, Visual Studio Code with GitHub Copilot in agent mode, Cursor, or Codex. In its settings, the
    transport is usually named **HTTP**, **Streamable HTTP**, or a remote server. Claude Desktop connects through the
    `mcp-remote` bridge.

### Finding the Address of the Server

The server answers at the address OpenL Studio opens at, followed by `/mcp`:

| OpenL Studio                      | OpenL Studio address                | MCP server address                    |
|-----------------------------------|-------------------------------------|---------------------------------------|
| Docker image                      | `http://localhost:8080`             | `http://localhost:8080/mcp`           |
| OpenL Tablets Demo package        | `http://localhost:8080/webstudio`   | `http://localhost:8080/webstudio/mcp` |
| Behind a company web server       | `https://studio.example.com`        | `https://studio.example.com/mcp`      |

When OpenL Studio runs on another computer, use the address of that computer, and HTTPS to keep the token from being
sent in clear text.

### Negotiating the Protocol Version

The server speaks version [2026-07-28](https://modelcontextprotocol.io/specification/2026-07-28) of the MCP protocol
and still serves clients of the earlier versions, such as `2025-11-25`, which most MCP clients speak. The client and
the server agree on the version by themselves:

-   **`2026-07-28`** — every request carries the protocol version and stands on its own: the server keeps no session.
    The client learns the version of the server from its `server/discover` request.
-   **`2025-11-25` and earlier** — the client opens a session with its `initialize` request and sends the session ID,
    which the server returns in the `Mcp-Session-Id` header, with every later request. The server keeps the session
    until the client closes it or OpenL Studio stops, and may close a session left unused for more than 30 minutes.
    A request in a closed session is answered with `404`, and the client opens a new session.

A client of an earlier version needs no setting to connect. `mcp-remote`, the bridge of Claude Desktop, talks to the
server in version `2026-07-28` when its `--protocol auto` option is set, as described in
[Claude Desktop](#claude-desktop).

### Authenticating the Agent

The agent authenticates the same way in every MCP client:

-   **Single-user mode** — no credentials are needed. The agent works as the single user of OpenL Studio.
-   **Other user modes** — the agent sends a personal access token of the user in the `Authorization` header of every
    request, as `Token <token>`. OpenL Studio checks the token before the request reaches the server, and the agent
    then has the rights of this user, no more. When OpenL Studio signs users in through OAuth2, the agent may send an
    access token of the identity provider as `Bearer <token>` instead.

To create a token, in OpenL Studio, click the user icon, select **Personal Access Tokens**, and click
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
claude mcp add --transport http openl http://localhost:8080/mcp --header "Authorization: Token <token>"
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
        "Authorization": "Token ${OPENL_TOKEN}"
      }
    }
  }
}
```

#### Claude Desktop

Claude Desktop starts its local MCP servers as programs, so it reaches the server through `mcp-remote`, a program that
forwards the messages over Streamable HTTP. It runs with `npx`, which comes with Node.js. In Claude Desktop, select
**Settings** > **Developer** > **Edit Config**, and add the server to the `claude_desktop_config.json` file:

```json
{
  "mcpServers": {
    "openl": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:8080/mcp",
        "--protocol",
        "auto",
        "--allow-http",
        "--header",
        "Authorization: Token <token>"
      ]
    }
  }
}
```

The options are as follows:

-   **`--protocol auto`** — `mcp-remote` asks the server for its protocol version once, when it starts. As the server
    speaks `2026-07-28`, `mcp-remote` answers the `2025-11-25` handshake of Claude Desktop itself and sends every
    request in version `2026-07-28`, so the server keeps no session for Claude Desktop. Without the option,
    `mcp-remote` passes the handshake on, and the server opens a session.
-   **`--allow-http`** — `mcp-remote` uses an address without HTTPS; leave it out for an `https://` address.
-   **`--header`** — the `Authorization` header with the token; in single-user mode, leave out `--header` and the
    value after it.

Restart Claude Desktop to start the server. For the other options of `mcp-remote`, see its
[documentation](https://github.com/punkpeye/mcp-remote#readme).

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
        "Authorization": "Token ${input:openl-token}"
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
        "Authorization": "Token <token>"
      }
    }
  }
}
```

#### Codex

Codex reads its MCP servers from `~/.codex/config.toml`, or from `.codex/config.toml` of a trusted project. Add the
server there, with the `Authorization` header read from the `OPENL_AUTHORIZATION` environment variable:

```
[mcp_servers.openl]
url = "http://localhost:8080/mcp"
env_http_headers = { "Authorization" = "OPENL_AUTHORIZATION" }
```

Start Codex from a shell where the variable holds the whole header value, `Token <token>`:

```bash
export OPENL_AUTHORIZATION="Token <token>"
```

In single-user mode, leave out the `env_http_headers` line. `codex mcp list` lists the server, and `/mcp` in Codex shows
whether it is connected.

#### ChatGPT

ChatGPT connects to an MCP server from the servers of OpenAI, not from the computer of the user. It signs in to a
server with OAuth or not at all and cannot send a token in a header, while OpenL Studio offers no OAuth sign-in to an
MCP client. So ChatGPT works only with OpenL Studio in single-user mode, at an address it can reach:

-   **An HTTPS address on the internet**, such as `https://studio.example.com/mcp`.
-   **A [Secure MCP Tunnel](https://developers.openai.com/api/docs/guides/secure-mcp-tunnels)**, which a program in
    the private network of OpenL Studio opens to OpenAI, so that OpenL Studio stays closed to the internet.

> [!Note]
> In single-user mode, whoever reaches OpenL Studio works as its user. Prefer the tunnel to an address on the internet.

In ChatGPT on the web, open the **Plugins** page, click **+**, and select **Add custom MCP server**. Enter `openl` as
the name, then under **Connection** enter the address of the server, or select **Tunnel** and the tunnel. Select **No
authentication** and confirm. ChatGPT asks for a confirmation before each call of a tool that changes data.

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

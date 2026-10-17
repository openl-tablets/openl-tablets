## Troubleshooting

This section describes what to check when the agent cannot connect to the MCP server or a request fails.

The following topics are included in this section:

-   [The Agent Cannot Connect](#the-agent-cannot-connect)
-   [Every Tool Fails with an Authentication Error](#every-tool-fails-with-an-authentication-error)
-   [A Browser-Based Client Is Refused](#a-browser-based-client-is-refused)
-   [Tools Are Missing](#tools-are-missing)
-   [A Request Fails](#a-request-fails)
-   [Reporting a Problem](#reporting-a-problem)

### The Agent Cannot Connect

Check the address first: it ends with `/mcp` and points at the host of OpenL Studio, not at the address OpenL Studio
opens at. For more information, see [Finding the Address of the
Server](connecting.md#finding-the-address-of-the-server).

If the address is right, the answer of the server tells what is wrong:

-   **`404 Not Found`** — nothing serves `/mcp`. The OpenL Studio Docker image and the Demo package serve it; another
    deployment needs the web server to forward `/mcp` to the MCP server.
-   **`502 Bad Gateway`** or no answer — the MCP server is not running.

OpenL Studio logs why the server does not run when OpenL Studio starts:

| Message in the OpenL Studio log                                                 | Cause                                        |
|---------------------------------------------------------------------------------|----------------------------------------------|
| `The built-in MCP server is disabled.`                                          | `mcp.enabled` is `false`.                    |
| `The built-in MCP server is not available: Cannot run program "node" ...`       | Node.js is not installed or not found.       |
| `The built-in MCP server is not available: port 3000 is in use. ...`            | Another program uses the port of `mcp.port`. |
| `The built-in MCP server is not available: the war carries no /WEB-INF/mcp/...` | The OpenL Studio WAR has no MCP server.      |

When the server runs, the log shows `The built-in MCP server is running at /mcp on port 3000.` For the settings, see
[Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL Tablets Installation
Guide.

After a change of the settings of OpenL Studio, the server restarts, and the client may need to reconnect.

### Every Tool Fails with an Authentication Error

The agent connects, but every tool answers with an error like `OpenL Studio API error (401)`. The token is missing,
mistyped, expired, or deleted:

-   Check that the client sends the `Authorization` header as `Bearer <token>` or `Token <token>`.
-   In OpenL Studio, in **My Profile** > **Personal Access Tokens**, check that the token exists and has not expired,
    or create a new one.

An error with `403` means that the user of the token has no permission for the action, the same as in OpenL Studio.

### A Browser-Based Client Is Refused

A client that runs in a web browser gets `403 Forbidden: Origin is not allowed`. The server accepts requests from a
browser page only from the origins an administrator allows; a client outside a browser is not affected. For more
information, see [Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL
Tablets Installation Guide.

### Tools Are Missing

-   Check that the tools of the server are enabled in the client. In Visual Studio Code, enable them in the tools menu
    of the **Chat** view.
-   An administrator can limit the tools the server offers. A tool left out is unknown to the agent.

### A Request Fails

The agent gets the error message of OpenL Studio and usually corrects the request itself. For example, after
`The project is not opened` it opens the project, and after a table is not found it lists the tables again: the
identifier of a table changes when the table is edited. When the agent cannot recover, ask it for the exact error
message and look for the same action in OpenL Studio.

### Reporting a Problem

When reporting a problem, ask the agent which version of the MCP server it uses. The `openl_get_version` tool answers
with the version of OpenL Studio and the build identifier, even when OpenL Studio does not answer, and without any
settings or credentials.

The messages of the MCP server are in the log of OpenL Studio, under the `org.openl.studio.mcp.node` logger.

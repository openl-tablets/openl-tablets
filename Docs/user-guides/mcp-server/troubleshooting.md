## Troubleshooting

This section describes what to check when the agent cannot connect to the MCP server or a request fails.

The following topics are included in this section:

-   [The Agent Cannot Connect](#the-agent-cannot-connect)
-   [The Token Is Refused](#the-token-is-refused)
-   [A Browser-Based Client Is Refused](#a-browser-based-client-is-refused)
-   [Tools Are Missing](#tools-are-missing)
-   [A Request Fails](#a-request-fails)
-   [Reporting a Problem](#reporting-a-problem)

### The Agent Cannot Connect

Check the address first: it is the address OpenL Studio opens at, followed by `/mcp`. For more information, see
[Finding the Address of the Server](connecting.md#finding-the-address-of-the-server).

If the address is right, the answer of the server tells what is wrong:

-   **`401 Unauthorized`** — OpenL Studio refuses the token; see [The Token Is Refused](#the-token-is-refused).
-   **`404 Not Found`** — the address does not lead to OpenL Studio, for example, it lacks the `/webstudio` part of
    the address of the Demo package.
-   **`503 Service Unavailable`** or **`502 Bad Gateway`** — the MCP server is not running.

OpenL Studio logs why the server does not run when OpenL Studio starts:

| Message in the OpenL Studio log                                                 | Cause                                        |
|---------------------------------------------------------------------------------|----------------------------------------------|
| `The built-in MCP server is disabled.`                                          | `mcp.enabled` is `false`.                    |
| `The built-in MCP server is not available: Cannot run program "node" ...`       | Node.js is not installed or not found.       |
| `The built-in MCP server is not available: the war carries no /WEB-INF/mcp/...` | The OpenL Studio WAR has no MCP server.      |

When the server runs, the log shows `The built-in MCP server is running at /mcp on port <port>.` For the settings, see
[Built-in MCP Server](../installation-guide/configuration.md#built-in-mcp-server) in the OpenL Tablets Installation
Guide.

After a change of the settings of OpenL Studio, the server restarts, and the client may need to reconnect.

### The Token Is Refused

The server answers `401 Unauthorized`, and the agent cannot connect. The token is missing, mistyped, expired, or
deleted:

-   Check that the client sends the `Authorization` header as `Token <token>`, or as `Bearer <token>` with an access
    token of the identity provider when OpenL Studio signs users in through OAuth2.
-   In OpenL Studio, in **Personal Access Tokens** of the user icon, check that the token exists and has not expired,
    or create a new one.

An error with `403` means that the user of the token has no permission for the action, the same as in OpenL Studio.

### A Browser-Based Client Is Refused

A client that runs in a web browser fails with a CORS error in the console of the browser. OpenL Studio lets a web page
of another origin call the server only when an administrator allows that origin; a client outside a browser is not
affected. For more information, see
[Cross-Origin Requests](../installation-guide/configuration.md#cross-origin-requests) in the OpenL Tablets Installation
Guide.

### Tools Are Missing

Check that the tools of the server are enabled in the client. In Visual Studio Code, enable them in the tools menu of
the **Chat** view.

### A Request Fails

The agent gets the error message of OpenL Studio and usually corrects the request itself. For example, after
`The project is not opened` it opens the project, and after a table is not found it lists the tables again: the
identifier of a table changes when the table is edited. When the agent cannot recover, ask it for the exact error
message and look for the same action in OpenL Studio.

### A File Download or Upload Fails

The agent moves a large or a binary file by a link of OpenL Studio, with `curl` run on the computer of the agent.
That computer reaches OpenL Studio at the address the agent connected to, so a sandbox of the agent that allows only
some hosts lists that address too. In an OpenL Studio with sign-in, a link works for 15 minutes by default:

-   **Download** — a cut download is fetched again with the same link, until it expires.
-   **Upload** — a link writes once, so after a cut or a refused upload the agent asks for a new link.

In single-user mode the link is the address of the REST API itself, which neither expires nor is spent. A reverse
proxy in front of OpenL Studio passes `/transfers` and allows a request as large as the file.

### Reporting a Problem

When reporting a problem, ask the agent for the full information about the server. The `openl_get_server_info` tool
answers with the version and the address of OpenL Studio, its build number, and the system, Java and memory of the
machine it runs on.

The messages of the MCP server are in the log of OpenL Studio, under the `org.openl.studio.mcp.node` logger.

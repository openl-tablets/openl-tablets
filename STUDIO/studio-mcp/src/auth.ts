/**
 * Authentication module for OpenL MCP Server
 *
 * Every call to OpenL Studio carries the `Authorization` header of the request to the MCP server as the client sent
 * it. OpenL Studio checks that header at `/mcp` the way it checks a REST call, so any credentials it accepts reach the
 * REST API unchanged: a Personal Access Token as `Token`, an access token of the identity provider as `Bearer`.
 * Without the header, calls go without credentials, as OpenL Studio in single-user mode accepts.
 */

import type { HttpError } from "./http.js";
import type { McpClientInfo } from "./types.js";
import { extractApiErrorInfo } from "./utils.js";

/** The headers that authenticate a call to OpenL Studio: `Authorization` when the client sent one. */
export function authorizationHeaders(authorization: string | undefined): Record<string, string> {
  return authorization ? { Authorization: authorization } : {};
}

/** The longest name of an MCP client a call to OpenL Studio carries. */
const CLIENT_NAME_LENGTH = 100;

/**
 * The headers that name the client of the MCP server in a call to OpenL Studio, with the secret OpenL Studio gave the
 * server, which OpenL Studio checks before it takes them: the address of the client and its name and version, as the
 * client reports them. None without the secret.
 */
export function callerHeaders(
  mcpSecret: string | undefined,
  clientAddress: string | undefined,
  client?: McpClientInfo,
): Record<string, string> {
  if (!mcpSecret) {
    return {};
  }
  const headers: Record<string, string> = { "OpenL-Mcp-Secret": mcpSecret };
  if (clientAddress) {
    headers["OpenL-Client-Address"] = clientAddress;
  }
  const name = client && clientName(client);
  if (name) {
    headers["OpenL-Mcp-Client"] = name;
  }
  return headers;
}

/**
 * The name and the version of a client as a header can carry them: printable ASCII, a character of any other kind
 * replaced by `?`, cut to {@link CLIENT_NAME_LENGTH}.
 */
function clientName({ name, version }: McpClientInfo): string {
  return [name, version]
    .filter(Boolean)
    .join(" ")
    .replace(/[^\x20-\x7e]/g, "?")
    .trim()
    .slice(0, CLIENT_NAME_LENGTH);
}

/** Explains a 401 answer in the log: the message of OpenL Studio and the scheme of the credentials, not the secret. */
export function logUnauthorized(error: HttpError, authorization: string | undefined): void {
  if (error.response?.status !== 401) {
    return;
  }
  const { message } = extractApiErrorInfo(error.response.data, 401);
  const scheme = authorization?.split(" ", 1)[0] || "No Auth";
  console.error(`[Auth] 401 Unauthorized: ${message || "Unauthorized"} (${scheme})`);
}

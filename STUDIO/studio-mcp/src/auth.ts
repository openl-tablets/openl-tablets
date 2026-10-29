/**
 * Authentication module for OpenL MCP Server
 *
 * Every call to OpenL Studio carries the `Authorization` header of the request to the MCP server as the client sent
 * it. OpenL Studio checks that header at `/mcp` the way it checks a REST call, so any credentials it accepts reach the
 * REST API unchanged: a Personal Access Token as `Token`, an access token of the identity provider as `Bearer`.
 * Without the header, calls go without credentials, as OpenL Studio in single-user mode accepts.
 */

import type { HttpError } from "./http.js";
import { extractApiErrorInfo } from "./utils.js";

/** The headers that authenticate a call to OpenL Studio: `Authorization` when the client sent one. */
export function authorizationHeaders(authorization: string | undefined): Record<string, string> {
  return authorization ? { Authorization: authorization } : {};
}

/**
 * The headers that name the client of the MCP server in a call to OpenL Studio: its address, with the secret OpenL
 * Studio gave the server, which OpenL Studio checks before it takes the address. None without both.
 */
export function callerHeaders(mcpSecret: string | undefined, clientAddress: string | undefined): Record<string, string> {
  return mcpSecret && clientAddress
    ? { "OpenL-Mcp-Secret": mcpSecret, "OpenL-Client-Address": clientAddress }
    : {};
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

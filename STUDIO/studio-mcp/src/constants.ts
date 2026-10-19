/**
 * Constants and configuration defaults for the OpenL MCP Server
 */

import pkg from "../package.json" with { type: "json" };

/**
 * The MCP namespace prefix exposed to clients (e.g. `openl_list_repositories`).
 * It exists only to keep this server's tools distinct from other MCP servers'
 * tools in a client connected to several at once.
 *
 * It is purely a PROTOCOL-BOUNDARY concern. The internal tool registry and the
 * REST API use bare names (`list_repositories`); only the MCP server registers
 * the tools under their prefixed names — via {@link mcpToolName} — so the
 * namespace lives in exactly one place and never leaks into the registry.
 */
const TOOL_PREFIX = "openl_";

/** Add the namespace prefix to a bare registry name for the MCP wire (`list_repositories` → `openl_list_repositories`). */
export function mcpToolName(base: string): string {
  return `${TOOL_PREFIX}${base}`;
}

/** The version the server reports: the one OpenL Studio gives, else the placeholder of package.json. */
export function resolveServerVersion(openlVersion: string | undefined): string {
  return openlVersion || pkg.version;
}

/**
 * Server identity — name, version, and description — sourced from the package's
 * own package.json so there is a single source of truth. The version is the one of
 * the OpenL Studio that runs the server, which passes it in `OPENL_VERSION`; the
 * version of package.json is a placeholder only a server outside Studio reports.
 *
 * The bundle takes package.json in when it is built, so the server needs none
 * beside it.
 */
export const SERVER_INFO = {
  NAME: pkg.name,
  VERSION: resolveServerVersion(process.env.OPENL_VERSION),
  DESCRIPTION: pkg.description,
} as const;

/**
 * Response formatting limits
 */
export const RESPONSE_LIMITS = {
  /** Maximum response character count (~25,000) */
  MAX_CHARACTERS: 25000,

  /** Truncation warning message */
  TRUNCATION_MESSAGE: "Response truncated due to size.",
} as const;

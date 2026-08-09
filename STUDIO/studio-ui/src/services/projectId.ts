/**
 * Normalizes a Base64 id the server issued — a project's, a deployment's — to the URL-safe alphabet so it
 * fits a URL path segment.
 *
 * The server issues URL-safe ids, but one saved earlier — in a bookmark, a wiki link, a script — still uses
 * the standard alphabet, whose `/` a servlet container rejects once percent-encoded. The backend decodes both
 * forms, so normalizing here is enough — and it is what every request and every route that puts an id in its
 * path must do.
 */
export const toUrlSafeId = (id: string): string => id.replaceAll('+', '-').replaceAll('/', '_')

/**
 * Builds the id of a project — `repositoryId:projectName` in URL-safe Base64 — for a caller that knows only
 * those two parts. Mirrors `ProjectIdModel.encode()` on the server.
 *
 * Prefer the id the server issued; rebuild one only when no request has returned it yet.
 */
export const encodeProjectId = (repositoryId: string, projectName: string): string => {
    // btoa() only accepts Latin-1, so a name outside US-ASCII has to be read as UTF-8 first.
    const utf8 = new TextEncoder().encode(`${repositoryId}:${projectName}`)
    return toUrlSafeId(btoa(String.fromCharCode(...utf8)))
}

/**
 * Encodes a project-relative path for the `{*path}` mapping. The path keeps its `/` separators; each
 * segment is encoded so reserved characters such as `#` or `%` do not corrupt the URL.
 */
export const encodeProjectPath = (path: string): string => path.split('/').map(encodeURIComponent).join('/')

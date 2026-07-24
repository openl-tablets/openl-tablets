/**
 * Normalizes a Base64 id the server issued — a project's, a deployment's — to the URL-safe alphabet so it
 * fits a URL path segment.
 *
 * The React screens already carry URL-safe ids, but a legacy page hands over the standard alphabet, whose
 * `/` a servlet container rejects once percent-encoded. The backend decodes both forms, so normalizing
 * here is enough — and it is what every request and every route that puts an id in its path must do.
 */
export const toUrlSafeId = (id: string): string => id.replaceAll('+', '-').replaceAll('/', '_')

/**
 * Encodes a project-relative path for the `{*path}` mapping. The path keeps its `/` separators; each
 * segment is encoded so reserved characters such as `#` or `%` do not corrupt the URL.
 */
export const encodeProjectPath = (path: string): string => path.split('/').map(encodeURIComponent).join('/')

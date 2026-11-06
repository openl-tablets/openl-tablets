/**
 * The address of a module's screen, and of one table read through it.
 *
 * Every way into the editor — a module row, a compilation problem, a test result, a graph node, a word in a
 * cell — leads here, so the address is spelled in one place rather than at each of them.
 *
 * <p>A reader sent by a compilation message arrives at the cell it was raised against, named the way the
 * workbook names it — `D101` — so the table opens with that cell marked.
 */
export const moduleRoute = (projectId: string, moduleName: string, tableId?: string, cell?: string): string =>
    `/projects/${projectId}/modules/${encodeURIComponent(moduleName)}`
        + (tableId ? `?table=${encodeURIComponent(tableId)}` : '')
        + (tableId && cell ? `&errorCell=${encodeURIComponent(cell)}` : '')

/**
 * The address of the messages of a project, in every language: the table of all of them, or one key of them, with
 * the language the reader is sent to.
 */
export const localizationRoute = (projectId: string, key?: string | null, language?: string | null): string => {
    const query = new URLSearchParams()
    if (key) {
        query.set('key', key)
        if (language !== undefined && language !== null) {
            query.set('lang', language)
        }
    }
    const search = query.toString()
    return `/projects/${projectId}/localization${search ? `?${search}` : ''}`
}

/** The address of the messages of a project, filtered to the keys a language has no translation for. */
export const untranslatedRoute = (projectId: string, language: string): string =>
    `/projects/${projectId}/localization?untranslated=${encodeURIComponent(language)}`

/**
 * The address of a project's Files tab, opened on one file of it.
 *
 * <p>The path crosses as a query value, so a file in a folder reaches the screen whole rather than as a
 * path of its own.
 */
export const projectFileRoute = (projectId: string, path: string): string =>
    `/projects/${projectId}?tab=files&file=${encodeURIComponent(path)}`

/**
 * Encodes a project-relative path for the `{*path}` mapping. The path keeps its `/` separators; each
 * segment is encoded so reserved characters such as `#` or `%` do not corrupt the URL.
 */
export const encodeProjectPath = (path: string): string => path.split('/').map(encodeURIComponent).join('/')

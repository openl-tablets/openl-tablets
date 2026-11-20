/**
 * The prefix of the id of a project downloaded from the Maven repository: the Projects API addresses it by
 * `maven:groupId:artifactId:version`, and no id of a project of the workspace holds a colon.
 */
const MAVEN_ID = 'maven:'

/** The id a screen addresses a project downloaded from the Maven repository by, from its coordinates. */
export const mavenProjectId = (coordinates: string): string => `${MAVEN_ID}${coordinates}`

/** The coordinates of the downloaded project an id stands for, or undefined for a project of the workspace. */
export const mavenArtifactOf = (projectId: string): string | undefined =>
    (projectId.startsWith(MAVEN_ID) ? projectId.slice(MAVEN_ID.length) : undefined)

/** The screen of a downloaded project: `/maven/groupId/artifactId/version`. */
const mavenScreen = (coordinates: string): string => {
    const parts = coordinates.split(':')
    // The version is the last part: an archive named with its `zip` extension writes it before
    const named = parts.length > 3 ? [parts[0], parts[1], parts[parts.length - 1]] : parts
    return `/maven/${named.map(part => encodeURIComponent(part ?? '')).join('/')}`
}

/**
 * The WebSocket topics of a project, `/user/topic/projects/{projectId}`: the server names them by the id as it is, a
 * colon of a downloaded project's id included. No id holds a slash.
 */
export const projectTopic = (projectId: string): string => `/user/topic/projects/${projectId}`

/** The screen of a project: `/projects/{id}`, or `/maven/groupId/artifactId/version` for a downloaded one. */
export const projectRoute = (projectId: string): string => {
    const maven = mavenArtifactOf(projectId)
    return maven === undefined ? `/projects/${encodeURIComponent(projectId)}` : mavenScreen(maven)
}

/**
 * The address of a module's screen, and of one table read through it.
 *
 * Every way into the editor — a module row, a compilation problem, a test result, a graph node, a word in a
 * cell — leads here, so the address is spelled in one place rather than at each of them. A module of a project
 * downloaded from the Maven repository opens read-only, under the address of that project.
 *
 * <p>A reader sent by a compilation message arrives at the cell it was raised against, named the way the
 * workbook names it — `D101` — so the table opens with that cell marked.
 */
export const moduleRoute = (projectId: string, moduleName: string, tableId?: string, cell?: string): string =>
    `${projectRoute(projectId)}/modules/${encodeURIComponent(moduleName)}`
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
    const screen = `/projects/${projectId}/localization`
    return search ? `${screen}?${search}` : screen
}

/** The address of the messages of a project, filtered to the keys a language has no translation for. */
export const untranslatedRoute = (projectId: string, language: string): string =>
    `/projects/${projectId}/localization?untranslated=${encodeURIComponent(language)}`

/**
 * The address of a project opened on one of its files or folders, and on one line of the file when it is given.
 *
 * <p>The path crosses as a query value, so a file in a folder reaches the screen whole rather than as a
 * path of its own.
 */
export const projectFileRoute = (projectId: string, path: string, line?: number): string =>
    `${projectRoute(projectId)}?file=${encodeURIComponent(path)}`
        + (line === undefined ? '' : `&line=${line}`)

/**
 * Encodes a project-relative path for the `{*path}` mapping. The path keeps its `/` separators; each
 * segment is encoded so reserved characters such as `#` or `%` do not corrupt the URL.
 */
export const encodeProjectPath = (path: string): string => path.split('/').map(encodeURIComponent).join('/')

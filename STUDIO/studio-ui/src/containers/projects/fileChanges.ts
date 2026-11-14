import type { ProjectFileChange, ProjectFileChangeType } from '../../services/projectStatus'

export interface NormalizedFileChange {
    path: string
    type: ProjectFileChangeType
}

// Trailing slashes are matched from the first of their run only, so a run inside the path is not read again from
// each of its slashes.
const normalizePath = (path?: string | null): string =>
    (path ?? '').replaceAll('\\', '/').replace(/^\/+|(?<!\/)\/+$/g, '')

const stripPrefix = (path: string, prefix?: string | null): string => {
    const normalizedPrefix = normalizePath(prefix)
    if (!normalizedPrefix || path === normalizedPrefix) {
        return path
    }
    return path.startsWith(`${normalizedPrefix}/`) ? path.slice(normalizedPrefix.length + 1) : path
}

/** The path of a file inside its project, from its path in the repository or in the project already. */
export const toProjectRelativePath = (
    path: string,
    projectPath?: string | null,
    projectName?: string | null
): string => {
    const normalizedPath = normalizePath(path)
    return stripPrefix(stripPrefix(normalizedPath, projectPath), projectName)
}

export const normalizeProjectFileChanges = (
    changes: readonly ProjectFileChange[],
    projectPath?: string | null,
    projectName?: string | null
): NormalizedFileChange[] => {
    const result: NormalizedFileChange[] = []
    const seen = new Set<string>()
    for (const change of changes) {
        const path = toProjectRelativePath(change.path, projectPath, projectName)
        if (!path || seen.has(path)) {
            continue
        }
        seen.add(path)
        result.push({ path, type: change.type })
    }
    return result
}

/**
 * The change of each file, by its path in the project. A file is named once, by the first change of it: the tree
 * lists a deleted file from here, so a second name would list it twice.
 */
export const buildFileChangeMap = (
    changes: readonly ProjectFileChange[],
    projectPath?: string | null,
    projectName?: string | null
): Map<string, ProjectFileChangeType> =>
    new Map(normalizeProjectFileChanges(changes, projectPath, projectName).map(change => [change.path, change.type]))

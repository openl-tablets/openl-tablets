import type { ProjectFileChange, ProjectFileChangeType } from '../../services/projectStatus'

export interface NormalizedFileChange {
    path: string
    type: ProjectFileChangeType
}

const normalizePath = (path?: string | null): string =>
    (path ?? '').replaceAll('\\', '/').replace(/^\/+|\/+$/g, '')

const stripPrefix = (path: string, prefix?: string | null): string => {
    const normalizedPrefix = normalizePath(prefix)
    if (!normalizedPrefix || path === normalizedPrefix) {
        return path
    }
    return path.startsWith(`${normalizedPrefix}/`) ? path.slice(normalizedPrefix.length + 1) : path
}

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

export const buildFileChangeMap = (
    changes: readonly ProjectFileChange[],
    projectPath?: string | null,
    projectName?: string | null
): Map<string, ProjectFileChangeType> => {
    const result = new Map<string, ProjectFileChangeType>()
    for (const change of changes) {
        const fullPath = normalizePath(change.path)
        const relativePath = toProjectRelativePath(change.path, projectPath, projectName)
        if (fullPath) {
            result.set(fullPath, change.type)
        }
        if (relativePath) {
            result.set(relativePath, change.type)
        }
    }
    return result
}

/** Strips the slashes a path may carry at its end; a plain scan, so a long run of them costs nothing. */
export const trimTrailingSlashes = (value: string): string => {
    let end = value.length
    while (end > 0 && value[end - 1] === '/') {
        end--
    }
    return value.slice(0, end)
}

/** Strips the slashes a typed path may carry at either end and normalizes the separators. */
export const normalizeProjectPath = (path: string): string => {
    const value = path.trim().replaceAll('\\', '/')
    let start = 0
    while (start < value.length && value[start] === '/') {
        start++
    }
    return trimTrailingSlashes(value.slice(start))
}

/** Joins a folder inside the project with a name under it; either side may be empty. */
export const joinProjectPath = (folder: string, name: string): string => {
    const base = normalizeProjectPath(folder)
    const relative = normalizeProjectPath(name)
    return base && relative ? `${base}/${relative}` : base || relative
}

/** The name a path ends with — its last segment. */
export const basename = (path: string): string => path.slice(path.lastIndexOf('/') + 1)

/** The folder a path sits in, empty for the project root. */
export const parentFolder = (path: string): string => {
    const slash = path.lastIndexOf('/')
    return slash > 0 ? path.slice(0, slash) : ''
}

/** The folder path as the upload endpoint expects it: empty for the root, trailing slash otherwise. */
export const uploadTargetPath = (folder: string): string => {
    const normalized = normalizeProjectPath(folder)
    return normalized ? `${normalized}/` : ''
}

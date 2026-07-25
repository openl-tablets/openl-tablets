import { strFromU8, unzipSync, zip } from 'fflate'

/** Result of inspecting an uploaded archive to pre-fill the project name and flag non-OpenL content. */
export interface OpenLArchiveInfo {
    /** Whether the file could be read as a zip archive at all. */
    readable: boolean
    /**
     * Whether the archive looks like an OpenL project: a {@code rules.xml} or Excel file sits at the
     * project root — either the zip root, or a single top-level folder wrapping the project. Only
     * meaningful when {@link readable} is {@code true}.
     */
    isOpenLProject: boolean
    /** Suggested project name: the name from the project's {@code rules.xml}, else the wrapping folder or archive file name. */
    name: string
}

const RULES_XML = 'rules.xml'

/** Project name from the archive file name, dropping a trailing {@code .zip}. */
const archiveBaseName = (fileName: string): string => fileName.replace(/\.zip$/i, '')

/** The file-name part of a zip entry path. */
const entryBaseName = (path: string): string => path.slice(path.lastIndexOf('/') + 1)

/** OS/tooling junk, directory markers and hidden entries that are not part of the project. */
const isJunk = (path: string): boolean =>
    path.startsWith('__MACOSX/') || path.endsWith('/') || entryBaseName(path).startsWith('.')

/** Whether the entry is a project descriptor or an Excel workbook. */
const isProjectFile = (name: string): boolean => {
    const base = entryBaseName(name).toLowerCase()
    return base === RULES_XML || /\.xls[xm]?$/i.test(base)
}

/**
 * Locates the project root inside the archive: the zip root when a project file sits directly there,
 * or a single top-level folder that wraps the project (the common "compress the project folder" case).
 * Returns the root prefix ({@code ''} or {@code 'Folder/'}), or {@code null} when neither holds a
 * project descriptor or workbook.
 */
const findProjectRoot = (names: string[]): string | null => {
    const rootFiles = names.filter(name => !name.includes('/'))
    if (rootFiles.some(isProjectFile)) {
        return ''
    }
    const topFolders = new Set(names.filter(name => name.includes('/')).map(name => name.slice(0, name.indexOf('/'))))
    if (rootFiles.length === 0 && topFolders.size === 1) {
        const prefix = `${[...topFolders][0]}/`
        const folderFiles = names.filter(name => name.startsWith(prefix) && !name.slice(prefix.length).includes('/'))
        if (folderFiles.some(isProjectFile)) {
            return prefix
        }
    }
    return null
}

/** Reads the {@code <project><name>} value from a rules.xml document, or {@code null} when absent. */
const readProjectName = (xml: string): string | null => {
    try {
        const doc = new DOMParser().parseFromString(xml, 'application/xml')
        if (doc.getElementsByTagName('parsererror').length > 0) {
            return null
        }
        const root = doc.documentElement
        if (!root || root.tagName !== 'project') {
            return null
        }
        const name = root.getElementsByTagName('name')[0]?.textContent?.trim()
        return name || null
    } catch {
        return null
    }
}

/** The path of a picked file, relative to the folder the user chose, with backslashes normalised. */
export const folderRelativePath = (file: File): string => (file.webkitRelativePath || file.name).replaceAll('\\', '/')

/**
 * The single top-level folder every path sits under, or {@code null} when they do not share one (loose
 * files, or more than one root). A directory picker reports every file under the one chosen folder, so
 * this is the wrapper to drop to lift the folder's content to the root.
 */
export const commonRootFolder = (paths: string[]): string | null => {
    const roots = new Set(paths.map(path => (path.includes('/') ? path.slice(0, path.indexOf('/')) : null)))
    return roots.size === 1 && !roots.has(null) ? [...roots][0]! : null
}

/**
 * Packs the files of a picked folder into a zip archive in the browser, so a folder can be created as a
 * project through the same archive endpoint — and the same validation — as an uploaded {@code .zip}.
 *
 * The chosen folder itself is stripped, so the project's own files (its {@code rules.xml} or Excel) sit at
 * the archive root, where the server resolves the project. The archive is named after the chosen folder.
 */
export async function zipProjectFolder(files: File[]): Promise<File> {
    const paths = files.map(folderRelativePath)
    const wrapper = commonRootFolder(paths)
    const entries: Record<string, Uint8Array> = {}
    // Read every file concurrently — the reads are independent and can all be in flight at once.
    await Promise.all(files.map(async (file, index) => {
        const path = paths[index]!
        entries[wrapper ? path.slice(wrapper.length + 1) : path] = new Uint8Array(await file.arrayBuffer())
    }))
    const data = await new Promise<Uint8Array>((resolve, reject) => {
        zip(entries, { level: 6 }, (err, out) => (err ? reject(err) : resolve(out)))
    })
    // fflate returns a plain Uint8Array; wrap its own buffer as the single blob part.
    return new File([data as BlobPart], `${wrapper ?? 'project'}.zip`, { type: 'application/zip' })
}

/**
 * Inspects an uploaded archive in the browser to support project creation: it derives a suggested
 * project name and reports whether the archive is a valid OpenL project, mirroring the backend rule
 * (a {@code rules.xml} or Excel file at the project root, whether the project sits at the zip root or
 * inside a single wrapping folder).
 *
 * The check never throws: an unreadable file returns {@code readable: false} with the file-name
 * fallback, leaving the final decision to the server.
 */
export async function inspectOpenLArchive(file: File): Promise<OpenLArchiveInfo> {
    const fallbackName = archiveBaseName(file.name)
    let bytes: Uint8Array
    try {
        bytes = new Uint8Array(await file.arrayBuffer())
    } catch {
        return { readable: false, isOpenLProject: false, name: fallbackName }
    }
    const names: string[] = []
    let entries: Record<string, Uint8Array>
    try {
        entries = unzipSync(bytes, {
            filter: entry => {
                if (!isJunk(entry.name)) {
                    names.push(entry.name)
                }
                // Decompress every rules.xml; the resolved project root decides which one names the project.
                return entryBaseName(entry.name).toLowerCase() === RULES_XML
            },
        })
    } catch {
        return { readable: false, isOpenLProject: false, name: fallbackName }
    }
    const root = findProjectRoot(names)
    if (root === null) {
        return { readable: true, isOpenLProject: false, name: fallbackName }
    }
    let name = root ? root.replace(/\/$/, '') : fallbackName
    const rulesTarget = `${root}${RULES_XML}`.toLowerCase()
    const rulesKey = Object.keys(entries).find(entry => entry.toLowerCase() === rulesTarget)
    if (rulesKey) {
        const parsed = readProjectName(strFromU8(entries[rulesKey]!))
        if (parsed) {
            name = parsed
        }
    }
    return { readable: true, isOpenLProject: true, name }
}

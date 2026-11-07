import apiCall, { isApiHttpError, LOCAL_LOAD_API_OPTIONS } from './apiCall'
import { encodeProjectPath } from './projectId'
import { getProject } from './repositories'
import type { FsNode } from '../types/files'
import type { MessageFile } from '../types/localization'
import type { Project, ProjectDependency } from '../types/projects'

/** The folder of a project the engine reads the messages from. */
export const BUNDLE_FOLDER = 'i18n'

/** The name of a localization file: `message.properties`, or `message_` with a language, `message_de_LU.properties`. */
const MESSAGE_FILE = /^message(_[^.]+)?\.properties$/

const UTF8 = new TextDecoder('utf-8', { fatal: true })
const WINDOWS_1252 = new TextDecoder('windows-1252')

const filesUrl = (projectId: string, path: string): string =>
    `/projects/${projectId}/files/${encodeProjectPath(path)}`

/**
 * The names of the localization files of a project: the `message*.properties` files of its `i18n` folder.
 *
 * <p>A project without the folder has none, which is not a failure.
 */
export async function listMessageFiles(projectId: string): Promise<string[]> {
    try {
        const nodes = await apiCall(
            `${filesUrl(projectId, BUNDLE_FOLDER)}/?viewMode=FLAT`,
            undefined,
            { throwError: true, suppressErrorPages: true }
        ) as FsNode[] | null
        return (nodes ?? [])
            .filter(node => node.type === 'file' && MESSAGE_FILE.test(node.name))
            .map(node => node.name)
    } catch (error) {
        if (isMissingFolder(error)) {
            return []
        }
        throw error
    }
}

/** Whether a listing failed because the project has no such folder: the server says the path is not one. */
const isMissingFolder = (error: unknown): boolean => {
    if (!isApiHttpError(error)) {
        return false
    }
    const code = (error.payload as { code?: unknown } | undefined)?.code
    return error.status === 404 || (typeof code === 'string' && code.endsWith('.not-folder.message'))
}

/** Reads one localization file, telling a file in UTF-8 from one in a legacy encoding. */
async function readMessageFile(projectId: string, name: string): Promise<MessageFile> {
    const response = await apiCall(
        filesUrl(projectId, `${BUNDLE_FOLDER}/${name}`),
        undefined,
        { throwError: true, responseType: 'response', suppressErrorPages: true }
    ) as Response
    const bytes = await response.arrayBuffer()
    try {
        return { name, text: UTF8.decode(bytes), legacy: false }
    } catch {
        return { name, text: WINDOWS_1252.decode(bytes), legacy: true }
    }
}

/** Reads every localization file of a project. */
export async function readMessageFiles(projectId: string): Promise<MessageFile[]> {
    const names = await listMessageFiles(projectId)
    return Promise.all(names.map(name => readMessageFile(projectId, name)))
}

/**
 * Writes localization files of a project, all in one request: one change of the project, compiled once.
 *
 * <p>A file the folder holds is overwritten, a new one is added; the folder is created where the project has none.
 */
export async function saveMessageFiles(projectId: string, files: Array<{ name: string, text: string }>): Promise<void> {
    const form = new FormData()
    files.forEach(file => form.append('file', new File([file.text], file.name, { type: 'text/plain' }), file.name))
    await apiCall(
        `${filesUrl(projectId, BUNDLE_FOLDER)}/?createFolders=true&conflictPolicy=OVERWRITE`,
        { method: 'POST', body: form },
        { throwError: true }
    )
}

/** The localization files of a project another one depends on. */
export interface DependencyMessages {
    projectId: string
    projectName: string
    files: MessageFile[]
}

/** The files of one dependency and the projects it depends on in turn. */
interface ReadDependency {
    messages: DependencyMessages
    dependencies: ProjectDependency[]
}

/** Reads one dependency, or answers null when the workspace cannot read it. */
async function readDependency(projectId: string): Promise<ReadDependency | null> {
    try {
        const [project, files] = await Promise.all([
            getProject(projectId, {}, LOCAL_LOAD_API_OPTIONS),
            readMessageFiles(projectId),
        ])
        return { messages: { projectId, projectName: project.name, files }, dependencies: project.dependencies ?? []}
    } catch {
        return null
    }
}

/**
 * Reads the localization files of the projects a project depends on, directly or through others, the nearest
 * first. A rule reads a key its own project lacks from them at runtime.
 *
 * <p>A dependency the workspace does not hold, and one that cannot be read, is left out.
 */
export async function readDependencyMessages(project: Project): Promise<DependencyMessages[]> {
    const seen = new Set<string>([project.id])
    const read: DependencyMessages[] = []
    let level = project.dependencies ?? []
    while (level.length > 0) {
        const ids = [...new Set(level.flatMap(dependency => (dependency.id === undefined ? [] : [dependency.id])))]
            .filter(id => !seen.has(id))
        ids.forEach(id => seen.add(id))
        const loaded = (await Promise.all(ids.map(readDependency))).filter(found => found !== null)
        loaded.forEach(found => read.push(found.messages))
        level = loaded.flatMap(found => found.dependencies)
    }
    return read
}

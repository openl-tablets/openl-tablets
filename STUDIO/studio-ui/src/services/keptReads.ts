import { LOCAL_LOAD_API_OPTIONS, WORKSPACE_CHANGED_EVENT, type ApiCallOptions } from './apiCall'
import { getFileContent } from './files'
import { getMavenProjects, type MavenProjectSummary } from './mavenDependencies'
import { listModules, type ModuleInfo } from './modules'
import { getProjectMigration, type ProjectMigration } from './migration'
import { invalidateProjectIndex, PROJECT_INDEX_TTL_MS } from './projectIndex'
import { isCompiled } from './projectStatus'
import {
    getDesignRepositories,
    getProject,
    getProjectFiles,
    getTagTypes,
    type ProjectInclude,
    type TagType,
} from './repositories'
import type { FsNode } from '../types/files'
import type { Project } from '../types/projects'
import type { Repository } from '../types/repositories'

/**
 * The reads kept for the next screen.
 *
 * Moving between the page of a project and its editor mounts the screens anew, and each asked the server again for
 * what the other had just read: the project, the repositories, the tag types, the files of the project. A read kept
 * here answers them instead, and the screen opens on it at once rather than on a skeleton.
 *
 * Only the first read of a screen takes a kept one: whatever the screen reads again — after an action, a switch of
 * the branch or a ping — it asks the server for, with {@code fresh}. A kept read is trusted as long as the list of the
 * projects is, and any change to the workspace drops every one of them: one made in this tab, and one the server
 * tells of, which the layout of the application listens to.
 */

/** A read kept for the next screen, with when it was asked for. */
interface Kept<T> {
    answer: Promise<T>
    takenAt: number
}

const kept = new Map<string, Kept<unknown>>()

/** A read of a project, with the parts it holds. */
interface KeptProject {
    project: Project
    includes: ReadonlySet<ProjectInclude>
    takenAt: number
}

const projects = new Map<string, KeptProject>()

const trusted = (read: { takenAt: number }): boolean => Date.now() - read.takenAt <= PROJECT_INDEX_TTL_MS

/**
 * Answers from the read kept under the key, or reads and keeps it.
 *
 * Callers asking for the same thing share one request, also while it is on its way. A read that fails is not kept,
 * so the next caller asks again. With {@code fresh} the server is asked anyway, and its answer is kept in turn.
 */
const keptRead = <T>(key: string, read: () => Promise<T>, fresh = false): Promise<T> => {
    const known = kept.get(key) as Kept<T> | undefined
    if (!fresh && known !== undefined && trusted(known)) {
        return known.answer
    }
    const entry: Kept<T> = { answer: read(), takenAt: Date.now() }
    kept.set(key, entry)
    entry.answer.catch(() => {
        if (kept.get(key) === entry) {
            kept.delete(key)
        }
    })
    return entry.answer
}

/**
 * Whether a kept read of a project answers a read asking for these parts. It must hold all of them, and its
 * compilation must have ended: a status read while a compilation ran is out of date as soon as it ends.
 */
const answers = (read: KeptProject, includes: readonly ProjectInclude[]): boolean =>
    includes.every(include => read.includes.has(include))
    && trusted(read)
    && (read.project.compileStatus === undefined || isCompiled(read.project.compileStatus.compileState))

/**
 * Reads a project for a screen of it, or answers from the read the previous screen of the same project made. A read
 * that holds more than asked for answers too: the page reads the project with its descriptor, which also answers the
 * editor's read of its status.
 */
export const readProject = async (
    projectId: string,
    includes: readonly ProjectInclude[],
    apiOptions: ApiCallOptions,
    { fresh = false }: { fresh?: boolean } = {}
): Promise<Project> => {
    const known = projects.get(projectId)
    if (!fresh && known !== undefined && answers(known, includes)) {
        return known.project
    }
    const takenAt = Date.now()
    const project = await getProject(projectId, { includes }, apiOptions)
    projects.set(projectId, { project, includes: new Set(includes), takenAt })
    return project
}

/** The design repositories the user may see. */
export const readDesignRepositories = ({ fresh = false } = {}): Promise<Repository[]> =>
    keptRead('repositories', () => getDesignRepositories(LOCAL_LOAD_API_OPTIONS), fresh)

/** The configured tag types. */
export const readTagTypes = ({ fresh = false } = {}): Promise<TagType[]> => keptRead('tagTypes', getTagTypes, fresh)

/** The projects of the Maven repository the workspace depends on. */
export const readMavenProjects = ({ fresh = false } = {}): Promise<MavenProjectSummary[]> =>
    keptRead('maven', getMavenProjects, fresh)

/** Every file and folder of a project. */
export const readProjectFiles = (projectId: string, { fresh = false } = {}): Promise<FsNode[]> =>
    keptRead(`files\n${projectId}`, () => getProjectFiles(projectId), fresh)

/** The text of a file of a project. */
export const readFileText = (projectId: string, path: string, { fresh = false } = {}): Promise<string> =>
    keptRead(`file\n${projectId}\n${path}`, () => getFileContent(projectId, path), fresh)

/** The modules of a project. */
export const readModules = (projectId: string, { fresh = false } = {}): Promise<ModuleInfo[]> =>
    keptRead(`modules\n${projectId}`, () => listModules(projectId), fresh)

/** What a migration would do for a project. */
export const readProjectMigration = (projectId: string, { fresh = false } = {}): Promise<ProjectMigration> =>
    keptRead(`migration\n${projectId}`, () => getProjectMigration(projectId), fresh)

/** Drops every kept read, so the next screen reads what it shows as it is now. */
export const forgetKeptReads = (): void => {
    kept.clear()
    projects.clear()
}

/**
 * Drops the list of the projects and every kept read: the server told of a change made elsewhere, in another session
 * or by another user, whichever screen is open.
 */
export const forgetWorkspaceReads = (): void => {
    invalidateProjectIndex()
    forgetKeptReads()
}

// Whatever the user changed in this tab, the reads kept before no longer describe the workspace.
window.addEventListener(WORKSPACE_CHANGED_EVENT, forgetKeptReads)

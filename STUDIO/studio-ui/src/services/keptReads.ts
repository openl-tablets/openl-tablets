import { LOCAL_LOAD_API_OPTIONS, WORKSPACE_CHANGED_EVENT, type ApiCallOptions } from './apiCall'
import { getFileContent } from './files'
import { getMavenProjects, type MavenProjectSummary } from './mavenDependencies'
import {
    getModuleTables,
    getRawTable,
    getTableDetails,
    getTableTests,
    listModules,
    TABLE_PAGE_ROWS,
    type ModuleInfo,
    type TableDetails,
    type TableTest,
} from './modules'
import { getProjectMigration, type ProjectMigration } from './migration'
import { invalidateProjectIndex, PROJECT_INDEX_TTL_MS } from './projectIndex'
import { isCompiled, type ProjectStatusDetailedMessage } from './projectStatus'
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
import type { ModuleTable, RawTableView } from '../types/tables'
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

/** A read kept for the next screen, with when it was asked for, and what it answered once it did. */
interface Kept<T> {
    answer: Promise<T>
    takenAt: number
    value?: T
}

const kept = new Map<string, Kept<unknown>>()

/** A read of a project, with the parts it holds. */
interface KeptProject {
    project: Project
    includes: ReadonlySet<ProjectInclude>
    takenAt: number
    /** Which read of a project it was: a later read has a greater number. */
    read: number
}

const projects = new Map<string, KeptProject>()

/** How many reads of a project were made, and how many of them a change to the workspace made out of date. */
let projectReads = 0
let projectReadsForgotten = 0

const trusted = (read: { takenAt: number }): boolean => Date.now() - read.takenAt <= PROJECT_INDEX_TTL_MS

/** Drops the reads no longer trusted, so a long visit keeps only what it read lately, not every table it passed. */
const forgetExpired = (reads: Map<string, { takenAt: number }>) => {
    for (const [key, read] of reads) {
        if (!trusted(read)) {
            reads.delete(key)
        }
    }
}

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
    forgetExpired(kept)
    const entry: Kept<T> = { answer: read(), takenAt: Date.now() }
    kept.set(key, entry)
    entry.answer.then(value => {
        entry.value = value
    }, () => {
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
    const read = ++projectReads
    const takenAt = Date.now()
    const project = await getProject(projectId, { includes }, apiOptions)
    forgetExpired(projects)
    // A read made before a change to the workspace, or before a later read of the project, answers its caller but is
    // not kept: it would bring back what the change or the later read replaced.
    const latest = projects.get(projectId)
    if (read > projectReadsForgotten && (latest === undefined || read > latest.read)) {
        projects.set(projectId, { project, includes: new Set(includes), takenAt, read })
    }
    return project
}

/** What the read kept under the key answered, when it did and is still trusted. */
const keptValue = <T>(key: string): T | undefined => {
    const known = kept.get(key) as Kept<T> | undefined
    return known !== undefined && trusted(known) ? known.value : undefined
}

/**
 * The project as the previous screen of it read it, with when it was read, when that read answers one asking for these
 * parts; nothing otherwise. A screen starts from it, so it opens on the project rather than on a skeleton.
 */
export const keptProjectRead = (
    projectId: string,
    includes: readonly ProjectInclude[]
): { project: Project, takenAt: number } | undefined => {
    const known = projects.get(projectId)
    return known !== undefined && answers(known, includes) ? known : undefined
}

/** The project as the previous screen of it read it, when that read answers one asking for these parts. */
export const keptProject = (projectId: string, includes: readonly ProjectInclude[]): Project | undefined =>
    keptProjectRead(projectId, includes)?.project

/** The design repositories as they were last read, when that read is still trusted. */
export const keptDesignRepositories = (): Repository[] | undefined => keptValue('repositories')

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

/** The key the tables of a module are kept under: they are those of a branch, with or without the utility tables. */
const tablesKey = (projectId: string, branch: string | null, moduleName: string, includeOther: boolean) =>
    `tables\n${projectId}\n${branch}\n${moduleName}\n${includeOther}`

/**
 * Reads the tables of a module, and keeps them for the next time the editor opens it. The editor asks the server every
 * time it reads them, since they follow the compilation of the module; it opens on the kept ones.
 */
export const readModuleTables = (
    projectId: string,
    branch: string | null,
    moduleName: string,
    includeOther: boolean
): Promise<ModuleTable[]> =>
    keptRead(tablesKey(projectId, branch, moduleName, includeOther), () => getModuleTables(projectId, moduleName, {
        includeOther,
    }), true)

/** The tables of a module as the editor last read them, when that read is still trusted. */
export const keptModuleTables = (
    projectId: string,
    branch: string | null,
    moduleName: string,
    includeOther: boolean
): ModuleTable[] | undefined => keptValue(tablesKey(projectId, branch, moduleName, includeOther))

/** The key a read about one table of a module is kept under. */
const tableKey = (part: string, projectId: string, tableId: string, moduleName: string) =>
    `${part}\n${projectId}\n${tableId}\n${moduleName}`

/** The first window of a table as the editor draws it: its rows, what they hold, and what it offers to run. */
export const readTableWindow = (
    projectId: string,
    tableId: string,
    moduleName: string,
    { fresh = false } = {}
): Promise<RawTableView> => keptRead(tableKey('table', projectId, tableId, moduleName), () => getRawTable(projectId, tableId, {
    module: moduleName,
    maxRows: TABLE_PAGE_ROWS,
    metaInfo: true,
    runState: true,
}), fresh)

/** The first window of a table as the editor last read it, when that read is still trusted. */
export const keptTableWindow = (projectId: string, tableId: string, moduleName: string): RawTableView | undefined =>
    keptValue(tableKey('table', projectId, tableId, moduleName))

/**
 * Gives the first window of a table kept for the next screen the messages read of the table since: a screen that reads
 * them again once a compilation ends keeps them for the next time the table is opened. A table not kept already is not
 * kept for it, and the window keeps when it was read.
 */
export const keepTableMessages = (
    projectId: string,
    tableId: string,
    moduleName: string,
    messages: ProjectStatusDetailedMessage[]
): void => {
    const known = kept.get(tableKey('table', projectId, tableId, moduleName)) as Kept<RawTableView> | undefined
    if (known?.value?.id === tableId) {
        const value = { ...known.value, messages }
        known.value = value
        known.answer = Promise.resolve(value)
    }
}

/** The details of a table: its kind, its properties and where they come from. */
export const readTableDetails = (
    projectId: string,
    tableId: string,
    moduleName: string,
    { fresh = false } = {}
): Promise<TableDetails> =>
    keptRead(tableKey('details', projectId, tableId, moduleName), () => getTableDetails(projectId, tableId, moduleName), fresh)

/** The details of a table as the editor last read them, when that read is still trusted. */
export const keptTableDetails = (projectId: string, tableId: string, moduleName: string): TableDetails | undefined =>
    keptValue(tableKey('details', projectId, tableId, moduleName))

/** The key the tests covering a table are kept under: as known before the project compiled through, or after. */
const testsKey = (projectId: string, tableId: string, moduleName: string, compiled: boolean) =>
    `${tableKey('tests', projectId, tableId, moduleName)}\n${compiled}`

/** The tests that cover a table. A test may be written in another module, so they are known once the project compiled. */
export const readTableTests = (
    projectId: string,
    tableId: string,
    moduleName: string,
    compiled: boolean,
    { fresh = false } = {}
): Promise<TableTest[]> =>
    keptRead(testsKey(projectId, tableId, moduleName, compiled), () => getTableTests(projectId, tableId, moduleName), fresh)

/** The tests that cover a table as the editor last read them, when that read is still trusted. */
export const keptTableTests = (
    projectId: string,
    tableId: string,
    moduleName: string,
    compiled: boolean
): TableTest[] | undefined => keptValue(testsKey(projectId, tableId, moduleName, compiled))

/** What a migration would do for a project. */
export const readProjectMigration = (projectId: string, { fresh = false } = {}): Promise<ProjectMigration> =>
    keptRead(`migration\n${projectId}`, () => getProjectMigration(projectId), fresh)

/** Drops every kept read, so the next screen reads what it shows as it is now. */
export const forgetKeptReads = (): void => {
    kept.clear()
    projects.clear()
    projectReadsForgotten = projectReads
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

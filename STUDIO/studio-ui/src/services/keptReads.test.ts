import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from './apiCall'
import { getFileContent } from './files'
import {
    forgetKeptReads,
    forgetWorkspaceReads,
    keptDesignRepositories,
    keptModuleTables,
    keptProject,
    keptProjectRead,
    keptTableDetails,
    keptTableTests,
    keptTableWindow,
    readDesignRepositories,
    readFileText,
    readMavenProjects,
    readModules,
    readProject,
    readProjectFiles,
    readModuleTables,
    readProjectMigration,
    readTableDetails,
    readTableTests,
    readTableWindow,
    readTagTypes,
} from './keptReads'
import { getMavenProjects } from './mavenDependencies'
import { getProjectMigration } from './migration'
import { getModuleTables, getRawTable, getTableDetails, getTableTests, listModules } from './modules'
import { invalidateProjectIndex, PROJECT_INDEX_TTL_MS } from './projectIndex'
import { getDesignRepositories, getProject, getProjectFiles, getTagTypes } from './repositories'

vi.mock('./repositories', () => ({
    getDesignRepositories: vi.fn(),
    getProject: vi.fn(),
    getProjectFiles: vi.fn(),
    getTagTypes: vi.fn(),
}))
vi.mock('./files', () => ({ getFileContent: vi.fn() }))
vi.mock('./mavenDependencies', () => ({ getMavenProjects: vi.fn() }))
vi.mock('./migration', () => ({ getProjectMigration: vi.fn() }))
vi.mock('./modules', () => ({
    getModuleTables: vi.fn(),
    getRawTable: vi.fn(),
    getTableDetails: vi.fn(),
    getTableTests: vi.fn(),
    listModules: vi.fn(),
    TABLE_PAGE_ROWS: 250,
}))
vi.mock('./projectIndex', () => ({ PROJECT_INDEX_TTL_MS: 300_000, invalidateProjectIndex: vi.fn() }))

const OPTIONS = { throwError: true }

const compiled = (compileState: string) => ({ id: 'p1', name: 'Rates', compileStatus: { projectId: 'p1', compileState } })

describe('the reads kept for the next screen', () => {
    beforeEach(() => {
        forgetKeptReads()
        vi.mocked(getProject).mockResolvedValue(compiled('ok') as never)
        vi.mocked(getTagTypes).mockResolvedValue([])
        vi.mocked(getDesignRepositories).mockResolvedValue([])
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('answers the editor with the project its page read, which holds more than the editor asks for', async () => {
        const page = await readProject('p1', ['status', 'descriptor'], OPTIONS)

        await expect(readProject('p1', ['status'], OPTIONS)).resolves.toBe(page)
        expect(getProject).toHaveBeenCalledTimes(1)
        expect(getProject).toHaveBeenCalledWith('p1', { includes: ['status', 'descriptor'] }, OPTIONS)
    })

    it('reads a project again for a part the kept read lacks, or when asked for a fresh read', async () => {
        await readProject('p1', ['status'], OPTIONS)

        await readProject('p1', ['status', 'descriptor'], OPTIONS)
        await readProject('p1', ['status', 'descriptor'], OPTIONS, { fresh: true })

        expect(getProject).toHaveBeenCalledTimes(3)
    })

    it('keeps no project read while it compiled, since the status is stale the moment the compilation ends', async () => {
        vi.mocked(getProject).mockResolvedValueOnce(compiled('compiling') as never)
        await readProject('p1', ['status'], OPTIONS)

        await readProject('p1', ['status'], OPTIONS)
        await readProject('p1', ['status'], OPTIONS)

        // The second read found the compilation ended, and that read is kept.
        expect(getProject).toHaveBeenCalledTimes(2)
    })

    it('trusts a kept read no longer than the list of the projects', async () => {
        vi.useFakeTimers({ toFake: ['Date'] })
        await readProject('p1', ['status'], OPTIONS)
        await readTagTypes()

        vi.setSystemTime(Date.now() + PROJECT_INDEX_TTL_MS + 1)
        await readProject('p1', ['status'], OPTIONS)
        await readTagTypes()

        expect(getProject).toHaveBeenCalledTimes(2)
        expect(getTagTypes).toHaveBeenCalledTimes(2)
    })

    it('drops the reads it trusts no longer as soon as it keeps another, so a long visit holds only the recent ones', async () => {
        vi.useFakeTimers({ toFake: ['Date'] })
        vi.mocked(getRawTable).mockResolvedValue({ id: 't' } as never)
        const start = Date.now()
        await readTableWindow('p1', 't1', 'Rates')

        vi.setSystemTime(start + PROJECT_INDEX_TTL_MS + 1)
        await readTableWindow('p1', 't2', 'Rates')

        // Turned back, the clock would trust the first read again, had it been kept.
        vi.setSystemTime(start + 1)
        expect(keptTableWindow('p1', 't1', 'Rates')).toBeUndefined()
        expect(keptTableWindow('p1', 't2', 'Rates')).toEqual({ id: 't' })
    })

    it('shares one request between screens asking at the same moment, and reads again when asked fresh', async () => {
        const [list, overview] = await Promise.all([readTagTypes(), readTagTypes()])

        expect(list).toBe(overview)
        expect(getTagTypes).toHaveBeenCalledTimes(1)

        await readTagTypes({ fresh: true })
        expect(getTagTypes).toHaveBeenCalledTimes(2)
    })

    it('keeps no failed read, so the next screen asks again', async () => {
        vi.mocked(getDesignRepositories).mockRejectedValueOnce(new Error('offline'))
        await expect(readDesignRepositories()).rejects.toThrow('offline')

        await readDesignRepositories()

        expect(getDesignRepositories).toHaveBeenCalledTimes(2)
    })

    it('keeps the files, the rules.xml, the modules and the migration of each project apart', async () => {
        vi.mocked(getProjectFiles).mockResolvedValue([])
        vi.mocked(getFileContent).mockResolvedValue('<project/>')
        vi.mocked(listModules).mockResolvedValue([])
        vi.mocked(getProjectMigration).mockResolvedValue({} as never)

        for (const id of ['p1', 'p1', 'p2']) {
            await readProjectFiles(id)
            await readFileText(id, 'rules.xml')
            await readModules(id)
            await readProjectMigration(id)
        }

        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(getFileContent).toHaveBeenCalledTimes(2)
        expect(getFileContent).toHaveBeenLastCalledWith('p2', 'rules.xml')
        expect(listModules).toHaveBeenCalledTimes(2)
        expect(getProjectMigration).toHaveBeenCalledTimes(2)
    })

    it('drops every kept read on a change made in this tab', async () => {
        await readProject('p1', ['status'], OPTIONS)
        await readTagTypes()

        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        await readProject('p1', ['status'], OPTIONS)
        await readTagTypes()

        expect(getProject).toHaveBeenCalledTimes(2)
        expect(getTagTypes).toHaveBeenCalledTimes(2)
    })

    it('drops the list of the projects with the kept reads on a change the server tells of', async () => {
        vi.mocked(getMavenProjects).mockResolvedValue([])
        await readProject('p1', ['status'], OPTIONS)
        await readMavenProjects()
        await readMavenProjects()

        forgetWorkspaceReads()
        await readProject('p1', ['status'], OPTIONS)
        await readMavenProjects()

        expect(invalidateProjectIndex).toHaveBeenCalledTimes(1)
        expect(getProject).toHaveBeenCalledTimes(2)
        expect(getMavenProjects).toHaveBeenCalledTimes(2)
    })

    it('hands a screen what was kept at once, so it paints it in its first frame', async () => {
        expect(keptProject('p1', ['status'])).toBeUndefined()
        expect(keptDesignRepositories()).toBeUndefined()

        const project = await readProject('p1', ['status', 'descriptor'], OPTIONS)
        const repositories = await readDesignRepositories()

        expect(keptProject('p1', ['status'])).toBe(project)
        expect(keptProject('p2', ['status'])).toBeUndefined()
        expect(keptDesignRepositories()).toBe(repositories)
    })

    it('tells a screen when the project it hands over was read', async () => {
        const project = await readProject('p1', ['status'], OPTIONS)

        expect(keptProjectRead('p1', ['status'])).toEqual(expect.objectContaining({
            project,
            takenAt: expect.any(Number),
        }))
        expect(keptProjectRead('p2', ['status'])).toBeUndefined()
    })

    it('keeps no project read that a change to the workspace overtook while it was on its way', async () => {
        let answer: (project: never) => void = () => undefined
        vi.mocked(getProject).mockReturnValueOnce(new Promise(resolve => {
            answer = resolve
        }))
        const before = readProject('p1', ['status'], OPTIONS)

        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        answer({ ...compiled('ok'), name: 'Before the change' } as never)

        // The read still answers the screen that made it.
        expect((await before).name).toBe('Before the change')
        expect(keptProject('p1', ['status'])).toBeUndefined()
    })

    it('keeps the later of two project reads, whichever answers last', async () => {
        let answerEarlier: (project: never) => void = () => undefined
        vi.mocked(getProject).mockReturnValueOnce(new Promise(resolve => {
            answerEarlier = resolve
        }))
        const earlier = readProject('p1', ['status'], OPTIONS, { fresh: true })
        vi.mocked(getProject).mockResolvedValueOnce({ ...compiled('ok'), name: 'Later' } as never)
        await readProject('p1', ['status'], OPTIONS, { fresh: true })

        answerEarlier({ ...compiled('ok'), name: 'Earlier' } as never)
        await earlier

        expect(keptProject('p1', ['status'])?.name).toBe('Later')
    })

    it('keeps no project for a screen while its compilation runs', async () => {
        vi.mocked(getProject).mockResolvedValueOnce(compiled('compiling') as never)
        await readProject('p1', ['status'], OPTIONS)

        expect(keptProject('p1', ['status'])).toBeUndefined()
    })

    it('reads the tables of a module every time, and keeps the last ones by branch and by the utility tables', async () => {
        vi.mocked(getModuleTables).mockResolvedValue([{ id: 't1' }] as never)

        await readModuleTables('p1', 'main', 'Rates', false)
        const tables = await readModuleTables('p1', 'main', 'Rates', false)

        expect(getModuleTables).toHaveBeenCalledTimes(2)
        expect(getModuleTables).toHaveBeenCalledWith('p1', 'Rates', { includeOther: false })
        expect(keptModuleTables('p1', 'main', 'Rates', false)).toBe(tables)
        expect(keptModuleTables('p1', 'dev', 'Rates', false)).toBeUndefined()
        expect(keptModuleTables('p1', 'main', 'Rates', true)).toBeUndefined()
    })

    it('keeps the window of a table, its details and the tests that cover it, the tests by how far it compiled', async () => {
        vi.mocked(getRawTable).mockResolvedValue({ id: 't1' } as never)
        vi.mocked(getTableDetails).mockResolvedValue({ kind: 'Rules' } as never)
        vi.mocked(getTableTests).mockResolvedValue([])

        const window = await readTableWindow('p1', 't1', 'Rates')
        const details = await readTableDetails('p1', 't1', 'Rates')
        const tests = await readTableTests('p1', 't1', 'Rates', true)
        await readTableWindow('p1', 't1', 'Rates')
        await readTableDetails('p1', 't1', 'Rates', { fresh: true })

        expect(getRawTable).toHaveBeenCalledTimes(1)
        expect(getRawTable).toHaveBeenCalledWith('p1', 't1', { module: 'Rates', maxRows: 250, metaInfo: true, runState: true })
        expect(getTableDetails).toHaveBeenCalledTimes(2)
        expect(keptTableWindow('p1', 't1', 'Rates')).toBe(window)
        expect(keptTableDetails('p1', 't1', 'Rates')).toEqual(details)
        expect(keptTableTests('p1', 't1', 'Rates', true)).toBe(tests)
        expect(keptTableTests('p1', 't1', 'Rates', false)).toBeUndefined()
    })
})

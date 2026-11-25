import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from './apiCall'
import { getFileContent } from './files'
import {
    forgetKeptReads,
    forgetWorkspaceReads,
    readDesignRepositories,
    readFileText,
    readMavenProjects,
    readModules,
    readProject,
    readProjectFiles,
    readProjectMigration,
    readTagTypes,
} from './keptReads'
import { getMavenProjects } from './mavenDependencies'
import { getProjectMigration } from './migration'
import { listModules } from './modules'
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
vi.mock('./modules', () => ({ listModules: vi.fn() }))
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
})

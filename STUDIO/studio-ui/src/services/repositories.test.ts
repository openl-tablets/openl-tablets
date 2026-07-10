import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall, { ApiHttpError } from './apiCall'
import {
    copyProject,
    createProject,
    createProjectBranch,
    createProjectFromRepository,
    createProjectsFromWorkspace,
    deleteProject,
    downloadProject,
    getDesignRepositories,
    getImportableFolders,
    getProject,
    getProjectBranches,
    getProjectFiles,
    getProjectRevisions,
    getProjectTemplates,
    getProjects,
    getTagTypes,
    isProjectModifiedConflict,
    openProjectRevision,
    saveProject,
    setProjectStatus,
    setSelectedBranches,
    switchProjectBranch,
    unlockProject,
    updateProjectTags,
} from './repositories'
import { ProjectStatus } from '../constants/project'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
    asArray: (value: unknown) => Array.isArray(value) ? value : [],
    isApiHttpError: (value: unknown) => value instanceof Error && value.name === 'ApiHttpError',
    ApiHttpError: class ApiHttpError extends Error {
        status: number
        payload?: unknown
        constructor(status: number, message: string, payload?: unknown) {
            super(message)
            this.name = 'ApiHttpError'
            this.status = status
            this.payload = payload
        }
    },
}))

vi.mock('./config', () => ({ default: { CONTEXT: '/studio' } }))

vi.mock('../utils/download', () => ({
    triggerDownload: vi.fn(),
}))

import { triggerDownload } from '../utils/download'

describe('getProjects', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('requests one server page with repeated facet parameters', async () => {
        vi.mocked(apiCall).mockResolvedValue({
            content: [],
            numberOfElements: 0,
            pageNumber: 2,
            pageSize: 50,
            total: 0,
        })

        await getProjects({
            includes: ['deleted', 'status', 'summary'],
            name: ' Alpha ',
            page: 2,
            repositories: ['design', '__local__'],
            size: 50,
            sort: 'updated',
            statuses: [ProjectStatus.Opened, ProjectStatus.Editing],
            tags: ['Category:Payroll', 'Category:Benefits', 'Malformed'],
        })

        const firstCall = vi.mocked(apiCall).mock.calls[0]
        expect(firstCall).toBeDefined()
        const url = firstCall![0] as string
        const query = new URLSearchParams(url.slice(url.indexOf('?') + 1))

        expect(url.startsWith('/projects?')).toBe(true)
        expect(query.getAll('include')).toEqual(['deleted', 'status', 'summary'])
        expect(query.get('name')).toBe('Alpha')
        expect(query.get('page')).toBe('2')
        expect(query.get('size')).toBe('50')
        expect(query.get('sort')).toBe('updated')
        expect(query.getAll('repository')).toEqual(['design', '__local__'])
        expect(query.getAll('status')).toEqual(['OPENED', 'EDITING'])
        expect(query.getAll('tags.Category')).toEqual(['Payroll', 'Benefits'])
    })

    it('requests one project with optional compilation status', async () => {
        vi.mocked(apiCall).mockResolvedValue({ id: 'abc' })

        await getProject('abc=', { includes: ['status', 'modules']})

        expect(apiCall).toHaveBeenCalledWith('/projects/abc%3D?include=status&include=modules', undefined, { throwError: true })
    })

    it('passes local error-page handling options to project list and detail calls', async () => {
        const apiOptions = { throwError: true, suppressErrorPages: true }
        vi.mocked(apiCall).mockResolvedValue({ content: []})

        await getDesignRepositories(apiOptions)
        expect(apiCall).toHaveBeenLastCalledWith('/repos', undefined, apiOptions)

        await getProjects({}, apiOptions)
        expect(apiCall).toHaveBeenLastCalledWith('/projects', undefined, apiOptions)

        await getProject('abc', {}, apiOptions)
        expect(apiCall).toHaveBeenLastCalledWith('/projects/abc', undefined, apiOptions)
    })

    it('requests files at a specific revision', async () => {
        vi.mocked(apiCall).mockResolvedValue([])

        await getProjectFiles('abc=', true, 'rev 1')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/abc%3D/files/?viewMode=FLAT&recursive=true&version=rev+1',
            undefined,
            { throwError: true }
        )
    })

    it('saves a project without generating a commit comment', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await saveProject('abc')

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ save: true }),
        }, { throwError: true })
    })

    it('sends only manually entered save comments', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await saveProject('abc', '  manual message  ')

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ save: true, comment: 'manual message' }),
        }, { throwError: true })
    })

    it('can discard changes while closing a project', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await setProjectStatus('abc', ProjectStatus.Closed, { discardChanges: true })

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: ProjectStatus.Closed, discardChanges: true }),
        }, { throwError: true })
    })

    it('opens a revision with a viewing transition because the backend requires it', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await openProjectRevision('abc', 'rev-1')

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ revision: 'rev-1', status: ProjectStatus.Opened }),
        }, { throwError: true })
    })

    it('can discard changes while opening a revision', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await openProjectRevision('abc', 'rev-1', { discardChanges: true })

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ revision: 'rev-1', status: ProjectStatus.Opened, discardChanges: true }),
        }, { throwError: true })
    })

    it('can discard changes while switching a branch', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await switchProjectBranch('abc', 'feature', { discardChanges: true })

        expect(apiCall).toHaveBeenCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ branch: 'feature', discardChanges: true }),
        }, { throwError: true })
    })

    it('normalizes array responses from getProjects', async () => {
        vi.mocked(apiCall).mockResolvedValue([{ id: 'p1' }])

        await expect(getProjects({ page: 3 })).resolves.toEqual({
            content: [{ id: 'p1' }],
            pageNumber: 3,
            pageSize: 1,
            numberOfElements: 1,
            total: 1,
        })
    })

    it('detects project modified conflicts from API errors', () => {
        expect(isProjectModifiedConflict(new ApiHttpError(409, 'conflict', {
            code: 'openl.error.409.project.close.modified.message',
        }))).toBe(true)
        expect(isProjectModifiedConflict(new ApiHttpError(409, 'conflict', { code: 'other' }))).toBe(false)
        expect(isProjectModifiedConflict(new Error('plain'))).toBe(false)
    })

    it('deletes and unlocks projects through dedicated endpoints', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await deleteProject('abc=')
        expect(apiCall).toHaveBeenCalledWith('/projects/abc%3D', { method: 'DELETE' }, { throwError: true })

        await unlockProject('abc')
        expect(apiCall).toHaveBeenCalledWith('/projects/abc/lock', { method: 'DELETE' }, { throwError: true })
    })

    it('copies projects into another repository', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await copyProject('source', 'Alpha', 'target', 'Beta', '  copied  ', ' folder ')

        expect(apiCall).toHaveBeenCalledWith(
            '/repos/target/projects/Beta/from-project',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sourceRepositoryId: 'source',
                    sourceProjectName: 'Alpha',
                    comment: 'copied',
                    path: 'folder',
                }),
            },
            { throwError: true }
        )
    })

    it('creates projects from uploads, templates and overwrite flags', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const file = new File(['zip'], 'Project.zip', { type: 'application/zip' })

        await createProject('design', 'Rules', {
            files: [file],
            comment: 'Create Rules',
            path: 'folder',
            overwrite: true,
            openApi: { modelsPath: 'rules/Models.xlsx' },
        })

        const [url, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(url).toContain('/repos/design/projects/Rules?')
        expect(url).toContain('comment=Create+Rules')
        expect(url).toContain('path=folder')
        expect(url).toContain('overwrite=true')
        expect(request?.method).toBe('PUT')
        expect(request?.body).toBeInstanceOf(FormData)
    })

    it('loads templates, workspace publish targets and importable folders', async () => {
        vi.mocked(apiCall).mockResolvedValue([])

        await getProjectTemplates()
        expect(apiCall).toHaveBeenCalledWith('/repos/project-templates', undefined, { throwError: true })

        await createProjectsFromWorkspace('design', { names: ['Local'], path: 'folder', comment: 'Publish' })
        expect(apiCall).toHaveBeenCalledWith(
            '/repos/design/projects/from-workspace',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ names: ['Local'], path: 'folder', comment: 'Publish' }),
            },
            { throwError: true }
        )

        await getImportableFolders('design', 'nested')
        expect(apiCall).toHaveBeenCalledWith('/repos/design/folders?path=nested', undefined, { throwError: true })
    })

    it('imports repository folders and downloads project archives', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await createProjectFromRepository('design', { path: 'folder/Alpha' })
        expect(apiCall).toHaveBeenCalledWith(
            '/repos/design/projects/from-repository',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ path: 'folder/Alpha' }),
            },
            { throwError: true }
        )

        downloadProject('abc')
        expect(triggerDownload).toHaveBeenCalledWith('/studio/web/projects/abc/files/?download=true')
    })

    it('loads branches, revisions, tags and selected branches', async () => {
        vi.mocked(apiCall)
            .mockResolvedValueOnce([{ name: 'main', selected: true }])
            .mockResolvedValueOnce({
                content: [{ revisionNo: '1' }],
                pageNumber: 0,
                pageSize: 20,
                numberOfElements: 1,
                total: 1,
            })
            .mockResolvedValue(undefined)

        await expect(getProjectBranches('abc')).resolves.toEqual([{ name: 'main', selected: true }])

        await expect(getProjectRevisions('design', 'Alpha', 'feature/1', {
            search: ' fix ',
            techRevs: true,
            page: 1,
            size: 10,
        })).resolves.toEqual({
            content: [{ revisionNo: '1' }],
            pageNumber: 0,
            pageSize: 20,
            numberOfElements: 1,
            total: 1,
        })

        await setSelectedBranches('abc', ['main', 'hotfix'])
        expect(apiCall).toHaveBeenLastCalledWith('/projects/abc', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ selectedBranches: ['main', 'hotfix']}),
        }, { throwError: true })

        await updateProjectTags('abc', { Team: 'Payroll' })
        expect(apiCall).toHaveBeenLastCalledWith('/projects/abc/tags', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ Team: 'Payroll' }),
        }, { throwError: true })

        vi.mocked(apiCall).mockResolvedValue([{ name: 'Team', extensible: true, nullable: false, values: []}])
        await expect(getTagTypes()).resolves.toEqual([{ name: 'Team', extensible: true, nullable: false, values: []}])
    })

    it('creates branches with optional revision pins', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await createProjectBranch('abc', 'feature', 'rev-1')

        expect(apiCall).toHaveBeenCalledWith('/projects/abc/branches', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ branch: 'feature', revision: 'rev-1' }),
        }, { throwError: true })
    })

})

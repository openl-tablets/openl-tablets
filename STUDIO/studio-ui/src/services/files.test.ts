import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import { getProjectFiles } from './repositories'
import {
    copyFile,
    createTextFile,
    deleteFile,
    downloadFile,
    downloadFolder,
    getFileRevisions,
    getFileContent,
    isEditableTextFile,
    moveFile,
    replaceFile,
    rootFileExists,
    updateFileContent,
    uploadFile,
    uploadFiles,
    writeRootFile,
} from './files'

vi.mock('./apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('./apiCall')>(),
    default: vi.fn(),
}))

vi.mock('./repositories', async importOriginal => ({
    ...await importOriginal<typeof import('./repositories')>(),
    getProjectFiles: vi.fn(),
}))

vi.mock('./config', () => ({ default: { CONTEXT: '/studio' } }))

vi.mock('../utils/download', () => ({
    triggerDownload: vi.fn(),
}))

import { triggerDownload } from '../utils/download'

describe('files service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('loads file content through apiCall', async () => {
        vi.mocked(apiCall).mockResolvedValue(new Response('hello'))

        await expect(getFileContent('repo/project', 'rules/My File.xml', 'rev 1')).resolves.toBe('hello')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/repo_project/files/rules/My%20File.xml?version=rev%201',
            undefined,
            // A missing file is reported inline, not by the global 404 page.
            { throwError: true, responseType: 'response', suppressErrorPages: true }
        )
    })

    it('keeps a JSON file as its raw text instead of a parsed object', async () => {
        const json = '{\n  "openapi": "3.0.1"\n}'
        vi.mocked(apiCall).mockResolvedValue(
            new Response(json, { headers: { 'Content-Type': 'application/json' } })
        )

        await expect(getFileContent('project', 'openapi.json')).resolves.toBe(json)
    })

    it('preserves empty file content', async () => {
        vi.mocked(apiCall).mockResolvedValue(new Response(''))

        await expect(getFileContent('project', 'empty.txt')).resolves.toBe('')
    })


    it('creates empty text files through the create endpoint', async () => {
        vi.mocked(apiCall).mockResolvedValue(true)

        await createTextFile('project', 'rules/new.txt')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/files/rules/new.txt?createFolders=true',
            { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: '' },
            { throwError: true }
        )
    })

    it('classifies editable text files by extension', () => {
        expect(isEditableTextFile('README')).toBe(true)
        expect(isEditableTextFile('rules/deploy.xml')).toBe(true)
        expect(isEditableTextFile('rules/Main.xlsx')).toBe(false)
    })

    it('updates file content as plain text', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await updateFileContent('project', 'rules/deploy.xml', '<deploy/>')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/files/rules/deploy.xml',
            { method: 'PUT', headers: { 'Content-Type': 'text/plain' }, body: '<deploy/>' },
            { throwError: true }
        )
    })

    it('checks root file existence through a non-recursive listing', async () => {
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'rules-deploy.xml', name: 'rules-deploy.xml', type: 'file', basePath: '' },
        ])

        await expect(rootFileExists('project', 'rules-deploy.xml')).resolves.toBe(true)
        expect(getProjectFiles).toHaveBeenCalledWith('project', false)
    })

    it('deletes files through the files API', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await deleteFile('project', 'rules/old.txt')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/files/rules/old.txt',
            { method: 'DELETE' },
            { throwError: true }
        )
    })

    it('moves and copies files through dedicated endpoints', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await moveFile('project', 'rules/a.txt', 'rules/b.txt')
        await copyFile('project', 'rules/a.txt', 'rules/a-copy.txt')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/file-move',
            { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath: 'rules/a.txt', destinationPath: 'rules/b.txt' }) },
            { throwError: true }
        )
        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/file-copy',
            { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath: 'rules/a.txt', destinationPath: 'rules/a-copy.txt' }) },
            { throwError: true }
        )
    })

    // A sibling of the files API, so a project folder named "history" keeps its own address.
    it('reads the revision history of a single file', async () => {
        vi.mocked(apiCall).mockResolvedValue({
            content: [{ revisionNo: 'r1' }], pageNumber: 0, pageSize: 20, numberOfElements: 1, total: 1,
        })

        const page = await getFileRevisions('project', 'history/Main.xlsx', { size: 20 })

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/file-history/history/Main.xlsx?page=0&size=20',
            undefined,
            { throwError: true }
        )
        expect(page.content).toHaveLength(1)
    })

    it('triggers browser downloads for files and folders', () => {
        downloadFile('project', 'rules/Main.xlsx')
        downloadFolder('project', 'rules/module')

        expect(triggerDownload).toHaveBeenCalledWith(
            '/studio/web/projects/project/files/rules/Main.xlsx?download=true',
            'Main.xlsx'
        )
        expect(triggerDownload).toHaveBeenCalledWith(
            '/studio/web/projects/project/files/rules/module/?download=true',
            'module.zip'
        )
    })

    // Exporting a module reaches back into the history the same way exporting a project does.
    it('downloads a file at an earlier revision', () => {
        downloadFile('project', 'rules/Main.xlsx', 'rev-1')

        expect(triggerDownload).toHaveBeenCalledWith(
            '/studio/web/projects/project/files/rules/Main.xlsx?download=true&version=rev-1',
            'Main.xlsx'
        )
    })

    it('uploads files as multipart form data', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const file = new File(['data'], 'new.txt', { type: 'text/plain' })

        await uploadFiles('project', 'rules', [file])

        const [, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(request?.method).toBe('POST')
        expect(request?.body).toBeInstanceOf(FormData)
    })

    it('uploads a single file under the name the user picked', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const file = new File(['data'], 'original.txt', { type: 'text/plain' })

        await uploadFile('project', '', file, 'renamed.txt')

        const [url, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(url).toBe('/projects/project/files/')
        expect(request?.method).toBe('POST')
        expect((request?.body as FormData).getAll('file')).toHaveLength(1)
    })

    it('replaces a file in place with an uploaded one', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const file = new File(['data'], 'Main.xlsx')

        await replaceFile('project', 'rules/Main.xlsx', file)

        const [url, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(url).toBe('/projects/project/files/rules/Main.xlsx')
        expect(request?.method).toBe('PUT')
        expect(request?.body).toBeInstanceOf(FormData)
    })

    it('overwrites an existing root file through the plain-text update', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await writeRootFile('project', 'rules.xml', '<project/>', 'overwrite')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/project/files/rules.xml',
            { method: 'PUT', headers: { 'Content-Type': 'text/plain' }, body: '<project/>' },
            { throwError: true }
        )
    })

    it('creates a missing root file through a multipart upload', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await writeRootFile('project', 'rules.xml', '<project/>', 'create')

        const [url, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(url).toBe('/projects/project/files/')
        expect(request?.method).toBe('POST')
        expect(request?.body).toBeInstanceOf(FormData)
    })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import { getProjectFiles } from './repositories'
import {
    copyFile,
    createTextFile,
    deleteFile,
    downloadFile,
    downloadFolder,
    getFileBlob,
    getFileContent,
    isEditableTextFile,
    moveFile,
    rootFileExists,
    updateFileContent,
    uploadFiles,
} from './files'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
}))

vi.mock('./repositories', () => ({
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
        vi.mocked(apiCall).mockResolvedValue('hello')

        await expect(getFileContent('repo/project', 'rules/My File.xml', 'rev 1')).resolves.toBe('hello')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/repo%2Fproject/files/rules/My%20File.xml?version=rev%201',
            undefined,
            { throwError: true, preserveEmptyText: true }
        )
    })

    it('preserves empty file content', async () => {
        vi.mocked(apiCall).mockResolvedValue('')

        await expect(getFileContent('project', 'empty.txt')).resolves.toBe('')
    })

    it('loads versioned file bytes through apiCall', async () => {
        const blob = new Blob(['data'])
        vi.mocked(apiCall).mockResolvedValue(blob)

        await expect(getFileBlob('repo/project', 'rules/Main.xlsx', 'rev 1')).resolves.toBe(blob)

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/repo%2Fproject/files/rules/Main.xlsx?version=rev%201',
            undefined,
            { throwError: true, responseType: 'blob' }
        )
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

    it('returns an empty string when file content is not textual', async () => {
        vi.mocked(apiCall).mockResolvedValue({ unexpected: true })

        await expect(getFileContent('project', 'rules/file.txt')).resolves.toBe('')
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

    it('uploads files as multipart form data', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)
        const file = new File(['data'], 'new.txt', { type: 'text/plain' })

        await uploadFiles('project', 'rules', [file])

        const [, request] = vi.mocked(apiCall).mock.calls[0]!
        expect(request?.method).toBe('POST')
        expect(request?.body).toBeInstanceOf(FormData)
    })
})

import { deleteProject, deleteProjectFile, updateProjectFromFiles, updateProjectFromZip } from 'services/projects'
import apiCall from 'services/apiCall'
import { notification } from 'antd'
import type { MockedFunction } from 'vitest'

vi.mock('services/apiCall', async () => {
    const actual = await vi.importActual<typeof import('services/apiCall')>('services/apiCall')
    return {
        __esModule: true,
        ...actual,
        default: vi.fn(),
    }
})

vi.mock('../i18n', () => ({
    __esModule: true,
    default: { t: (key: string) => key },
}))

describe('projects service', () => {
    const mockApiCall = apiCall as MockedFunction<typeof apiCall>
    let successSpy: ReturnType<typeof vi.spyOn>
    let errorSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        vi.clearAllMocks()
        successSpy = vi.spyOn(notification, 'success').mockImplementation(() => {})
        errorSpy = vi.spyOn(notification, 'error').mockImplementation(() => {})
    })

    afterEach(() => {
        successSpy.mockRestore()
        errorSpy.mockRestore()
    })

    it('issues a DELETE with the comment as a query parameter and reports success', async () => {
        mockApiCall.mockResolvedValueOnce(true)

        await expect(deleteProject('proj-id', 'My Project', 'EPBDS-0000 Cleanup')).resolves.toBe(true)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/proj-id?comment=EPBDS-0000%20Cleanup',
            { method: 'DELETE' },
            { throwError: true, suppressErrorPages: true }
        )
        expect(successSpy).toHaveBeenCalledTimes(1)
        expect(errorSpy).not.toHaveBeenCalled()
    })

    it('omits the comment parameter when no comment is given', async () => {
        mockApiCall.mockResolvedValueOnce(true)

        await deleteProject('proj-id', 'My Project')

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/proj-id',
            { method: 'DELETE' },
            { throwError: true, suppressErrorPages: true }
        )
    })

    it('surfaces the backend error message and reports failure', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Project is locked.'))

        await expect(deleteProject('proj-id', 'My Project', 'EPBDS-0000 Cleanup')).resolves.toBe(false)

        expect(errorSpy).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'Project is locked.' })
        )
        expect(successSpy).not.toHaveBeenCalled()
    })

    it('issues a DELETE to the project file resource with encoded path segments', async () => {
        mockApiCall.mockResolvedValueOnce(true)

        await expect(deleteProjectFile('proj-id', 'rules/UK 100%/Main.xlsx', 'Main.xlsx', false)).resolves.toBe(true)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/proj-id/files/rules/UK%20100%25/Main.xlsx',
            { method: 'DELETE' },
            { throwError: true, suppressErrorPages: true }
        )
        expect(successSpy).toHaveBeenCalledTimes(1)
        expect(errorSpy).not.toHaveBeenCalled()
    })

    it('surfaces the backend error message when a folder deletion fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('File is not found.'))

        await expect(deleteProjectFile('proj-id', 'rules/UK', 'UK', true)).resolves.toBe(false)

        expect(errorSpy).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'File is not found.' })
        )
        expect(successSpy).not.toHaveBeenCalled()
    })

    it('POSTs the zip archive to the project root with the replace policy and a URL-safe id', async () => {
        mockApiCall.mockResolvedValueOnce(true)
        const archive = new Blob(['zip-bytes'], { type: 'application/zip' })

        await expect(updateProjectFromZip('id+with/chars', 'My Project', archive)).resolves.toBe(true)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/id-with_chars/files/?conflictPolicy=REPLACE',
            { method: 'POST', headers: { 'Content-Type': 'application/zip' }, body: archive },
            { throwError: true, suppressErrorPages: true }
        )
        expect(successSpy).toHaveBeenCalledTimes(1)
        expect(errorSpy).not.toHaveBeenCalled()
    })

    it('surfaces the backend error message when the zip upload fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Repository is read only.'))

        await expect(updateProjectFromZip('proj-id', 'My Project', new Blob(['x']))).resolves.toBe(false)

        expect(errorSpy).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'Repository is read only.' })
        )
        expect(successSpy).not.toHaveBeenCalled()
    })

    it('POSTs the picked files as one multipart request keeping their relative paths', async () => {
        mockApiCall.mockResolvedValueOnce(true)
        const entries = [
            { path: 'rules/Main.xlsx', file: new Blob(['a']) },
            { path: 'deployment.xml', file: new Blob(['b']) },
        ]

        await expect(updateProjectFromFiles('proj-id', 'My Project', entries)).resolves.toBe(true)

        const [url, params] = mockApiCall.mock.calls[0] as [string, RequestInit]
        expect(url).toBe('/projects/proj-id/files/?conflictPolicy=REPLACE')
        expect(params.method).toBe('POST')
        const parts = (params.body as FormData).getAll('file') as File[]
        expect(parts.map(part => part.name)).toEqual(['rules/Main.xlsx', 'deployment.xml'])
        expect(successSpy).toHaveBeenCalledTimes(1)
    })

})

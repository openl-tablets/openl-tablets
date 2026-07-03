import { deleteProject, deleteProjectFile } from 'services/projects'
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
})

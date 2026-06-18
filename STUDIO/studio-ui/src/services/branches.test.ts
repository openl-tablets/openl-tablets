import { deleteBranch } from 'services/branches'
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

describe('branches service', () => {
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

    it('issues a DELETE to the project branch resource and reports success', async () => {
        mockApiCall.mockResolvedValueOnce(true)

        await expect(deleteBranch('proj-id', 'feature')).resolves.toBe(true)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/proj-id/branches/feature',
            { method: 'DELETE' },
            { throwError: true, suppressErrorPages: true }
        )
        expect(successSpy).toHaveBeenCalledTimes(1)
        expect(errorSpy).not.toHaveBeenCalled()
    })

    it('appends the force flag when bypassing protection', async () => {
        mockApiCall.mockResolvedValueOnce(true)

        await deleteBranch('proj-id', 'release-1', true)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/proj-id/branches/release-1?force=true',
            { method: 'DELETE' },
            { throwError: true, suppressErrorPages: true }
        )
    })

    it('surfaces the backend error message and reports failure', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Cannot delete the main branch.'))

        await expect(deleteBranch('p', 'master')).resolves.toBe(false)

        expect(errorSpy).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'Cannot delete the main branch.' })
        )
        expect(successSpy).not.toHaveBeenCalled()
    })
})

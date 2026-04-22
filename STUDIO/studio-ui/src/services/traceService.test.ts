import traceService from 'services/traceService'
import apiCall, { ApiHttpError } from 'services/apiCall'
import type { MockedFunction } from 'vitest'

vi.mock('services/apiCall', async () => {
    const actual = await vi.importActual<typeof import('services/apiCall')>('services/apiCall')
    return {
        __esModule: true,
        ...actual,
        default: vi.fn(),
    }
})

vi.mock('services/config', () => ({
    __esModule: true,
    default: { CONTEXT: '/ctx' },
}))

describe('traceService retry behavior', () => {
    const mockApiCall = apiCall as MockedFunction<typeof apiCall>

    beforeEach(() => {
        vi.clearAllMocks()
        vi.useFakeTimers()
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('retries transient ApiHttpError (503) and succeeds', async () => {
        mockApiCall
            .mockRejectedValueOnce(new ApiHttpError(503, 'Service unavailable'))
            .mockResolvedValueOnce([{ key: 1, title: 'root' }] as any)

        const promise = traceService.getNodeChildren('project-1')
        await vi.advanceTimersByTimeAsync(600)
        const result = await promise

        expect(result).toEqual([{ key: 1, title: 'root' }])
        expect(mockApiCall).toHaveBeenCalledTimes(2)
    })

    it('does not retry non-transient ApiHttpError (400)', async () => {
        mockApiCall.mockRejectedValueOnce(new ApiHttpError(400, 'Bad request'))

        const promise = traceService.getNodeDetails('project-1', 10)
        await expect(promise).rejects.toMatchObject({ status: 400 })
        expect(mockApiCall).toHaveBeenCalledTimes(1)
    })

})

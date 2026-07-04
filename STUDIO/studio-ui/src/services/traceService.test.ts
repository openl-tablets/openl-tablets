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
            .mockResolvedValueOnce({ status: 'suspended', frames: []} as any)

        const promise = traceService.getStack('project-1')
        await vi.advanceTimersByTimeAsync(600)
        const result = await promise

        expect(result).toEqual({ status: 'suspended', frames: []})
        expect(mockApiCall).toHaveBeenCalledTimes(2)
    })

    it('does not retry non-transient ApiHttpError (400)', async () => {
        mockApiCall.mockRejectedValueOnce(new ApiHttpError(400, 'Bad request'))

        const promise = traceService.getVariables('project-1', 0)
        await expect(promise).rejects.toMatchObject({ status: 400 })
        expect(mockApiCall).toHaveBeenCalledTimes(1)
    })
})

describe('traceService endpoints', () => {
    const mockApiCall = apiCall as MockedFunction<typeof apiCall>

    beforeEach(() => vi.clearAllMocks())

    it('issues a step command with the step type', async () => {
        mockApiCall.mockResolvedValue({ status: 'suspended', frames: []} as any)
        await traceService.step('p', 'into')
        expect(mockApiCall).toHaveBeenLastCalledWith(
            '/projects/p/trace/step?type=into',
            { method: 'POST' },
            expect.anything()
        )
    })

    it('sends breakpoints as a JSON body', async () => {
        mockApiCall.mockResolvedValue(undefined as any)
        await traceService.setBreakpoints('p', ['u1', 'u2'])
        expect(mockApiCall).toHaveBeenLastCalledWith(
            '/projects/p/trace/breakpoints',
            expect.objectContaining({ method: 'PUT', body: JSON.stringify({ uris: ['u1', 'u2']}) }),
            expect.anything()
        )
    })

    it('starts a session with stopAtEntry', async () => {
        mockApiCall.mockResolvedValue({ status: 'suspended', frames: []} as any)
        await traceService.startTrace('p', { tableId: 't1', stopAtEntry: true })
        const lastCall = mockApiCall.mock.lastCall
        expect(lastCall).toBeDefined()
        const [url, init] = lastCall!
        expect(url).toContain('/projects/p/trace?')
        expect(url).toContain('tableId=t1')
        expect(url).toContain('stopAtEntry=true')
        expect((init as RequestInit).method).toBe('POST')
    })
})

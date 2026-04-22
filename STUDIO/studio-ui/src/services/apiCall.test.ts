import apiCall, {
    ApiHttpError,
    NotFoundError,
    isApiHttpError,
} from 'services/apiCall'
import * as storeModule from 'store'

vi.mock('store', () => {
    const appStoreState = {
        setShowLogin: vi.fn(),
        setShowForbidden: vi.fn(),
        setShowNotFound: vi.fn(),
        setShowServerError: vi.fn(),
    }
    return {
        __appStoreState: appStoreState,
        useAppStore: {
            getState: () => appStoreState,
        },
    }
})

vi.mock('services/config', () => ({
    __esModule: true,
    default: { CONTEXT: '/ctx' },
}))

vi.mock('antd', () => ({
    notification: {
        error: vi.fn(),
        warning: vi.fn(),
        success: vi.fn(),
    },
}))

describe('apiCall', () => {
    const fetchMock = vi.fn()
    const { __appStoreState } = storeModule as unknown as { __appStoreState: Record<string, ReturnType<typeof vi.fn>> }

    beforeEach(() => {
        vi.clearAllMocks()
        ;(global as any).fetch = fetchMock
    })

    const mockResponse = ({
        status,
        contentType = 'application/json',
        jsonData,
        textData = '',
    }: {
        status: number
        contentType?: string
        jsonData?: unknown
        textData?: string
    }) =>
        ({
            status,
            headers: {
                get: (name: string) => (name === 'Content-Type' ? contentType : null),
            },
            json: vi.fn().mockResolvedValue(jsonData),
            text: vi.fn().mockResolvedValue(textData),
        }) as unknown as Response

    it('throws ApiHttpError with status and payload for 500 JSON errors', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 500,
                jsonData: { message: 'Backend failed', code: 'E500' },
            })
        )

        await expect(apiCall('/test', undefined, { throwError: true })).rejects.toMatchObject({
            name: 'ApiHttpError',
            status: 500,
            message: 'Backend failed',
            payload: { message: 'Backend failed', code: 'E500' },
        })

        expect(__appStoreState.setShowServerError).toHaveBeenCalledTimes(1)
    })

    it('throws ApiHttpError with fallback message for non-JSON non-404 errors', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 502,
                contentType: 'text/plain',
                textData: 'Bad gateway',
            })
        )

        const error = await apiCall('/test', undefined, { throwError: true }).catch((e) => e)

        expect(error).toBeInstanceOf(ApiHttpError)
        expect(error.status).toBe(502)
        expect(error.message).toBe('Something went wrong on API server!')
        expect(error.payload).toBeUndefined()
    })

    it('keeps NotFoundError behavior for 404 responses', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 404,
                jsonData: { message: 'No trace yet' },
            })
        )

        await expect(apiCall('/missing', undefined, { throwError: true })).rejects.toBeInstanceOf(NotFoundError)
        expect(__appStoreState.setShowNotFound).toHaveBeenCalledTimes(1)
    })

    it('recognizes ApiHttpError via type guard', () => {
        const httpError = new ApiHttpError(409, 'Conflict', { id: 1 })
        expect(isApiHttpError(httpError)).toBe(true)
        expect(isApiHttpError(new Error('plain error'))).toBe(false)
        expect(isApiHttpError({ status: 409, message: 'fake' })).toBe(false)
    })
})

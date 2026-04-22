import apiCall, {
    ApiHttpError,
    NotFoundError,
    isApiHttpError,
} from 'services/apiCall'

jest.mock('store', () => {
    const appStoreState = {
        setShowLogin: jest.fn(),
        setShowForbidden: jest.fn(),
        setShowNotFound: jest.fn(),
        setShowServerError: jest.fn(),
    }
    return {
        __appStoreState: appStoreState,
        useAppStore: {
            getState: () => appStoreState,
        },
    }
})

jest.mock('services/config', () => ({
    __esModule: true,
    default: { CONTEXT: '/ctx' },
}))

jest.mock('antd', () => ({
    notification: {
        error: jest.fn(),
        warning: jest.fn(),
        success: jest.fn(),
    },
}))

describe('apiCall', () => {
    const fetchMock = jest.fn()
    const { __appStoreState } = require('store')

    beforeEach(() => {
        jest.clearAllMocks()
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
            json: jest.fn().mockResolvedValue(jsonData),
            text: jest.fn().mockResolvedValue(textData),
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

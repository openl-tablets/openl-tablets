import apiCall, {
    ApiHttpError,
    EmptyError,
    NotFoundError,
    isApiHttpError,
} from 'services/apiCall'
import { notification } from 'antd'
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
        blobData = new Blob(['blob']),
    }: {
        status: number
        contentType?: string
        jsonData?: unknown
        textData?: string
        blobData?: Blob
    }) =>
        ({
            status,
            headers: {
                get: (name: string) => (name === 'Content-Type' ? contentType : null),
            },
            json: vi.fn().mockImplementation(async () => {
                if (jsonData instanceof Error) {
                    throw jsonData
                }
                return jsonData
            }),
            text: vi.fn().mockResolvedValue(textData),
            blob: vi.fn().mockResolvedValue(blobData),
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

        expect(__appStoreState['setShowServerError']).toHaveBeenCalledTimes(1)
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
        expect(__appStoreState['setShowNotFound']).toHaveBeenCalledTimes(1)
    })

    it('throws forbidden without showing the global page when error pages are suppressed', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 403,
                jsonData: { message: 'No access to this project' },
            })
        )

        await expect(apiCall('/restricted', undefined, {
            throwError: true,
            suppressErrorPages: true,
        })).rejects.toMatchObject({
            name: 'ForbiddenError',
            status: 403,
            message: 'No access to this project',
        })

        expect(__appStoreState['setShowForbidden']).not.toHaveBeenCalled()
    })

    it('preserves empty text responses when requested', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 200,
                contentType: 'text/plain',
                textData: '',
            })
        )

        await expect(apiCall('/empty.txt', undefined, {
            throwError: true,
            preserveEmptyText: true,
        })).resolves.toBe('')
    })

    it('recognizes ApiHttpError via type guard', () => {
        const httpError = new ApiHttpError(409, 'Conflict', { id: 1 })
        expect(isApiHttpError(httpError)).toBe(true)
        expect(isApiHttpError(new Error('plain error'))).toBe(false)
        expect(isApiHttpError({ status: 409, message: 'fake' })).toBe(false)
    })

    it('returns the raw response when responseType is response', async () => {
        const response = mockResponse({ status: 200, jsonData: { ok: true } })
        fetchMock.mockResolvedValueOnce(response)

        await expect(apiCall('/raw', undefined, {
            throwError: true,
            responseType: 'response',
        })).resolves.toBe(response)
    })

    it('returns a blob when responseType is blob', async () => {
        const blob = new Blob(['binary'])
        fetchMock.mockResolvedValueOnce(mockResponse({ status: 200, blobData: blob }))

        await expect(apiCall('/download', undefined, {
            throwError: true,
            responseType: 'blob',
        })).resolves.toBe(blob)
    })

    it('returns true for 204 responses and empty text bodies by default', async () => {
        fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, contentType: 'text/plain' }))
        await expect(apiCall('/empty', undefined, { throwError: true })).resolves.toBe(true)

        fetchMock.mockResolvedValueOnce(mockResponse({
            status: 200,
            contentType: 'text/plain',
            textData: '',
        }))
        await expect(apiCall('/blank.txt', undefined, { throwError: true })).resolves.toBe(true)
    })

    it('opens the login page on 401 without rethrowing by default', async () => {
        fetchMock.mockResolvedValueOnce(mockResponse({ status: 401 }))

        await expect(apiCall('/secure')).resolves.toBeUndefined()
        expect(__appStoreState['setShowLogin']).toHaveBeenCalledTimes(1)
        expect(notification.error).not.toHaveBeenCalled()
    })

    it('still redirects to login on 401 even when error pages are suppressed', async () => {
        fetchMock.mockResolvedValueOnce(mockResponse({ status: 401 }))

        // suppressErrorPages governs the expected 403/404/500 content pages a caller handles locally; an
        // expired session must always send the user to login, so the flag does not apply to 401.
        await expect(apiCall('/secure', undefined, {
            throwError: true,
            suppressErrorPages: true,
        })).rejects.toBeInstanceOf(EmptyError)
        expect(__appStoreState['setShowLogin']).toHaveBeenCalledTimes(1)
    })

    it('throws joined field validation messages for structured API errors', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 400,
                jsonData: {
                    fields: [
                        { message: 'Name is required' },
                        { message: 'Path is invalid' },
                    ],
                },
            })
        )

        await expect(apiCall('/invalid', undefined, { throwError: true })).rejects.toThrow(
            'Name is required\nPath is invalid'
        )
    })

    it('shows a notification for unexpected errors when throwError is false', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 400,
                jsonData: { message: 'Bad request' },
            })
        )

        await apiCall('/bad')
        expect(notification.error).toHaveBeenCalled()
    })

    it('falls back to the default forbidden message when the body is not JSON', async () => {
        fetchMock.mockResolvedValueOnce(
            mockResponse({
                status: 403,
                contentType: 'text/plain',
                textData: 'plain forbidden',
            })
        )

        await expect(apiCall('/restricted', undefined, { throwError: true })).rejects.toMatchObject({
            name: 'ForbiddenError',
            message: 'Forbidden! You do not have permission to access this resource.',
        })
    })
})

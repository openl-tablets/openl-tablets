import { act, renderHook } from '@testing-library/react'
import { useCopyToClipboard } from './useCopyToClipboard'

const { notificationError, logError } = vi.hoisted(() => ({ notificationError: vi.fn(), logError: vi.fn() }))

vi.mock('antd', () => ({
    notification: { error: notificationError },
}))

vi.mock('utils/errorHandling', () => ({
    errorHandler: { logError },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// jsdom ships neither navigator.clipboard nor document.execCommand — each test
// installs exactly the copy mechanisms it needs.
const setNavigatorClipboard = (value: unknown) => {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value })
}

describe('useCopyToClipboard', () => {
    afterEach(() => {
        setNavigatorClipboard(undefined)
        Reflect.deleteProperty(document, 'execCommand')
        vi.clearAllMocks()
    })

    it('copies through the Clipboard API when it is available', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined)
        setNavigatorClipboard({ writeText })

        const { result } = renderHook(() => useCopyToClipboard())
        await act(async () => {
            await result.current.copyToClipboard('secret-token')
        })

        expect(writeText).toHaveBeenCalledWith('secret-token')
        expect(result.current.copied).toBe(true)
        expect(notificationError).not.toHaveBeenCalled()
    })

    it('falls back to execCommand when the Clipboard API is unavailable (plain HTTP origin)', async () => {
        let staged: string | undefined
        document.execCommand = vi.fn(() => {
            staged = (document.activeElement as HTMLTextAreaElement | null)?.value
            return true
        })

        const { result } = renderHook(() => useCopyToClipboard())
        await act(async () => {
            await result.current.copyToClipboard('secret-token')
        })

        expect(document.execCommand).toHaveBeenCalledWith('copy')
        expect(staged).toBe('secret-token')
        expect(result.current.copied).toBe(true)
        expect(notificationError).not.toHaveBeenCalled()
    })

    it('falls back to execCommand when the Clipboard API write is rejected', async () => {
        setNavigatorClipboard({ writeText: vi.fn().mockRejectedValue(new Error('denied')) })
        document.execCommand = vi.fn().mockReturnValue(true)

        const { result } = renderHook(() => useCopyToClipboard())
        await act(async () => {
            await result.current.copyToClipboard('secret-token')
        })

        expect(document.execCommand).toHaveBeenCalledWith('copy')
        expect(result.current.copied).toBe(true)
        expect(notificationError).not.toHaveBeenCalled()
    })

    it('shows an error notification when no copy mechanism works', async () => {
        document.execCommand = vi.fn().mockReturnValue(false)

        const { result } = renderHook(() => useCopyToClipboard())
        await act(async () => {
            await result.current.copyToClipboard('secret-token')
        })

        expect(notificationError).toHaveBeenCalledWith({ title: 'common:copy_failed' })
        expect(logError).toHaveBeenCalledWith(expect.any(Error))
        expect(result.current.copied).toBe(false)
    })

    it('restarts the reset timer when copying again', async () => {
        vi.useFakeTimers()
        try {
            setNavigatorClipboard({ writeText: vi.fn().mockResolvedValue(undefined) })

            const { result } = renderHook(() => useCopyToClipboard())
            await act(async () => {
                await result.current.copyToClipboard('first')
            })
            act(() => {
                vi.advanceTimersByTime(1000)
            })
            await act(async () => {
                await result.current.copyToClipboard('second')
            })

            // 1.5s after the second copy the first timer would have fired already
            act(() => {
                vi.advanceTimersByTime(1500)
            })
            expect(result.current.copied).toBe(true)
            act(() => {
                vi.advanceTimersByTime(500)
            })
            expect(result.current.copied).toBe(false)
        } finally {
            vi.useRealTimers()
        }
    })

    it('resets the copied state after 2 seconds', async () => {
        vi.useFakeTimers()
        try {
            setNavigatorClipboard({ writeText: vi.fn().mockResolvedValue(undefined) })

            const { result } = renderHook(() => useCopyToClipboard())
            await act(async () => {
                await result.current.copyToClipboard('secret-token')
            })
            expect(result.current.copied).toBe(true)

            act(() => {
                vi.advanceTimersByTime(2000)
            })
            expect(result.current.copied).toBe(false)
        } finally {
            vi.useRealTimers()
        }
    })
})

import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CompileStatusBadge } from './CompileStatusBadge'
import { subscribeProjectStatus } from '../../services/projectStatus'

vi.mock('../../services/projectStatus', () => ({
    subscribeProjectStatus: vi.fn(),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, opts?: { count?: number }) => (opts?.count !== undefined ? `${key}:${opts.count}` : key),
    }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
}))

vi.mock('antd', () => ({
    Tag: ({ children, color, ...rest }: Record<string, unknown>) => {
        void color
        return <span {...rest}>{children as never}</span>
    },
}))

describe('CompileStatusBadge', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() } as never)
    })

    it('renders the initial compile state', async () => {
        await act(async () => {
            render(<CompileStatusBadge branch="main" initialState="errors" projectId="p1" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        expect(screen.getByTestId('compile-status')).toHaveAttribute('title', 'browser.compile.errors')
        expect(screen.getByTestId('compile-status')).not.toHaveTextContent('browser.compile.errors')
    })

    it('shows warning and error counts in the tooltip', async () => {
        await act(async () => {
            render(
                <CompileStatusBadge
                    branch="main"
                    projectId="p1"
                    initialStatus={{
                        projectId: 'p1',
                        branch: 'main',
                        compileState: 'errors',
                        compilation: {
                            messages: {
                                items: [],
                                total: 3,
                                errors: 2,
                                warnings: 1,
                            },
                        },
                    }}
                />
            )
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.getByTestId('compile-status')).toHaveAttribute(
            'title',
            'browser.compile.error_count:2, browser.compile.warning_count:1'
        )
    })

    it('renders nothing while idle', async () => {
        await act(async () => {
            render(<CompileStatusBadge branch={null} initialState="idle" projectId="p1" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByTestId('compile-status')).toBeNull()
    })

    it('renders pushed compile state transitions', async () => {
        let onUpdate!: (status: {
            compileState: 'warnings'
            compilation: { messages: { items: []; total: number; errors: number; warnings: number } }
            projectId: string
        }) => void
        vi.mocked(subscribeProjectStatus).mockImplementation((projectId, branch, listener) => {
            onUpdate = listener as never
            return { unsubscribe: vi.fn() } as never
        })
        await act(async () => {
            render(<CompileStatusBadge branch="main" projectId="p1" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        act(() => onUpdate({
            projectId: 'p1',
            compileState: 'warnings',
            compilation: { messages: { items: [], total: 1, errors: 0, warnings: 1 } },
        }))

        await waitFor(() => expect(screen.getByTestId('compile-status')).toHaveAttribute('title', 'browser.compile.warning_count:1'))
    })

    it('unsubscribes on unmount', async () => {
        const unsubscribe = vi.fn()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe } as never)
        let unmount = () => {}
        await act(async () => {
            unmount = render(<CompileStatusBadge branch="main" initialState="ok" projectId="p1" />).unmount
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        unmount()
        expect(unsubscribe).toHaveBeenCalled()
    })
})

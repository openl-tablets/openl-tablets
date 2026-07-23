import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../../services/projectStatus'
import { RowCompileDot } from './CompileIndicator'

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
    keyframes: () => '',
}))

const initialStatus = (state: ProjectStatusUpdate['compileState']): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState: state,
})

describe('RowCompileDot', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() } as never)
    })

    it('subscribes open rows to the project status channel', async () => {
        render(
            <RowCompileDot
                branch="main"
                initialStatus={initialStatus('errors')}
                projectId="p1"
                status={ProjectStatus.Opened}
            />
        )
        // The mount-time loads land asynchronously; flush them before the assertions read the screen.
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        expect(screen.getByRole('img', { name: 'browser.compile.errors' })).toBeInTheDocument()
    })

    it('shows nothing for a clean compiled project', async () => {
        render(
            <RowCompileDot
                branch="main"
                initialStatus={initialStatus('ok')}
                projectId="p1"
                status={ProjectStatus.Opened}
            />
        )
        // The mount-time loads land asynchronously; flush them before the assertions read the screen.
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByRole('img')).toBeNull()
    })

    it('does not subscribe closed rows', async () => {
        render(
            <RowCompileDot
                branch="main"
                initialStatus={initialStatus('ok')}
                projectId="p1"
                status={ProjectStatus.Closed}
            />
        )
        // The mount-time loads land asynchronously; flush them before the assertions read the screen.
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).not.toHaveBeenCalled()
        // A closed, clean project shows no compile dot.
        expect(screen.queryByRole('img')).toBeNull()
    })

    it('shows the real compile state for a local project instead of forcing idle', async () => {
        render(
            <RowCompileDot
                branch={null}
                initialStatus={initialStatus('errors')}
                projectId="p1"
                status={ProjectStatus.Local}
            />
        )
        // The mount-time loads land asynchronously; flush them before the assertions read the screen.
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(subscribeProjectStatus).toHaveBeenCalled()
        expect(screen.getByRole('img', { name: 'browser.compile.errors' })).toBeInTheDocument()
    })

    it('updates the tooltip from live warning and error counts', async () => {
        let onUpdate!: (status: ProjectStatusUpdate) => void
        vi.mocked(subscribeProjectStatus).mockImplementation((projectId, branch, listener) => {
            onUpdate = listener
            return { unsubscribe: vi.fn() } as never
        })
        render(
            <RowCompileDot
                branch="main"
                initialStatus={initialStatus('ok')}
                projectId="p1"
                status={ProjectStatus.Editing}
            />
        )
        // The mount-time loads land asynchronously; flush them before the assertions read the screen.
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        act(() => onUpdate({
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
        }))

        await waitFor(() => {
            expect(screen.getByRole('img', { name: 'browser.compile.error_count:2, browser.compile.warning_count:1' })).toBeInTheDocument()
        })
    })
})

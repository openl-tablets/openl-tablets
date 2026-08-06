import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import { subscribeProjectStatus, type ProjectStatusUpdate } from '../../services/projectStatus'
import { LiveCompileDot, RowCompileDot } from './CompileIndicator'

// Only the channel is stubbed; the freshness rule stays the one the screens use.
vi.mock('../../services/projectStatus', async importOriginal => ({
    ...(await importOriginal<typeof import('../../services/projectStatus')>()),
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

const compileStatus = (state: ProjectStatusUpdate['compileState']): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState: state,
})

describe('LiveCompileDot', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(subscribeProjectStatus).mockReturnValue({ unsubscribe: vi.fn() })
    })

    it('seeds from the compile status the project detail carries and shows the dot', async () => {
        render(<LiveCompileDot
            branch="main"
            compileStatus={compileStatus('errors')}
            projectId="p1"
            status={ProjectStatus.Opened}
        />)

        expect(subscribeProjectStatus).toHaveBeenCalledWith('p1', 'main', expect.any(Function))
        expect(await screen.findByTestId('compile-dot-p1')).toBeInTheDocument()
    })

    it('follows the live transitions of its project', async () => {
        let onUpdate!: (status: ProjectStatusUpdate) => void
        vi.mocked(subscribeProjectStatus).mockImplementation((_id, _branch, listener) => {
            onUpdate = listener
            return { unsubscribe: vi.fn() }
        })
        render(<LiveCompileDot
            branch="main"
            compileStatus={compileStatus('ok')}
            projectId="p1"
            status={ProjectStatus.Opened}
        />)
        // A clean project shows nothing…
        expect(screen.queryByRole('img')).toBeNull()

        act(() => onUpdate(compileStatus('compiling')))

        // …until a compile starts.
        expect(await screen.findByRole('img', { name: 'browser.compile.compiling' })).toBeInTheDocument()
    })

    it('stays idle and unsubscribed for a closed project', () => {
        render(<LiveCompileDot branch="main" projectId="p1" status={ProjectStatus.Closed} />)

        expect(subscribeProjectStatus).not.toHaveBeenCalled()
        expect(screen.queryByRole('img')).toBeNull()
    })
})

// The dot is purely presentational: the screen feeds it from its one workspace-wide status
// subscription, so a row never subscribes on its own.
describe('RowCompileDot', () => {
    it('shows the compile state of an open row', () => {
        render(<RowCompileDot compileStatus={compileStatus('errors')} status={ProjectStatus.Opened} />)

        expect(screen.getByRole('img', { name: 'browser.compile.errors' })).toBeInTheDocument()
    })

    it('shows nothing for a clean compiled project', () => {
        render(<RowCompileDot compileStatus={compileStatus('ok')} status={ProjectStatus.Opened} />)

        expect(screen.queryByRole('img')).toBeNull()
    })

    it('ignores a stale compile state on a closed row', () => {
        render(<RowCompileDot compileStatus={compileStatus('errors')} status={ProjectStatus.Closed} />)

        // A closed project has no live compilation; whatever state is left over is not shown.
        expect(screen.queryByRole('img')).toBeNull()
    })

    it('shows the real compile state for a local project instead of forcing idle', () => {
        render(<RowCompileDot compileStatus={compileStatus('errors')} status={ProjectStatus.Local} />)

        expect(screen.getByRole('img', { name: 'browser.compile.errors' })).toBeInTheDocument()
    })

    it('builds the tooltip from the warning and error counts', () => {
        render(
            <RowCompileDot
                status={ProjectStatus.Editing}
                compileStatus={{
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

        expect(screen.getByRole('img', { name: 'browser.compile.error_count:2, browser.compile.warning_count:1' }))
            .toBeInTheDocument()
    })
})

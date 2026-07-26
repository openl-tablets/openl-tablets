import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import { type ProjectStatusUpdate } from '../../services/projectStatus'
import { RowCompileDot } from './CompileIndicator'

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

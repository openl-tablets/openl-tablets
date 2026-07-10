import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { OverviewPanel } from './OverviewPanel'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, opts?: { count?: number }) => (opts?.count !== undefined ? `${key}:${opts.count}` : key),
    }),
}))

vi.mock('../../services/projectStatus', () => ({
    subscribeProjectStatus: () => ({ unsubscribe: () => {} }),
}))

const base: Project = {
    id: 'p1',
    name: 'Proj',
    branch: '',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    repository: 'design',
    revision: '',
    status: ProjectStatus.Local,
}

const renderPanel = (project: Project, repoType?: string) =>
    render(
        <MemoryRouter>
            <OverviewPanel onEditTags={() => {}} onUnlock={() => {}} project={project} repoLabel="design" repoType={repoType} />
        </MemoryRouter>
    )

describe('OverviewPanel', () => {
    it('hides empty sections and metadata fields without placeholders', () => {
        renderPanel(base)
        expect(screen.getByText('browser.overview.status')).toBeInTheDocument()
        expect(screen.getByText('browser.overview.repository')).toBeInTheDocument()
        expect(screen.queryByText('browser.overview.description')).toBeNull()
        expect(screen.queryByText('browser.overview.depends_on')).toBeNull()
        expect(screen.queryByText('browser.overview.version_patterns')).toBeNull()
        expect(screen.queryByText('browser.overview.exposed_methods')).toBeNull()
        expect(screen.queryByText('browser.overview.branch')).toBeNull()
        expect(screen.queryByText('browser.overview.path')).toBeNull()
        expect(screen.queryByText('—')).toBeNull()
    })

    it('renders depends-on entries as links to the referenced projects', () => {
        renderPanel({
            ...base,
            dependencies: [{ name: 'Common Datatypes', id: 'dep-id-1', branch: 'main' }],
        })
        expect(screen.getByText('browser.overview.depends_on')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Common Datatypes' })).toHaveAttribute('href', '/projects/dep-id-1')
    })

    it('renders rules.xml-derived sections when present', () => {
        renderPanel({
            ...base,
            description: 'A ruleset',
            tags: { Region: 'EU' },
            modules: [{ name: 'Pricing', path: 'rules/Pricing.xlsx' }],
            versionPatterns: ['.*-%state%'],
            exposedMethods: {
                includes: ['calc*'],
                excludes: ['debug*'],
            },
        })
        const left = within(screen.getByTestId('overview-left'))
        const right = within(screen.getByTestId('overview-right'))
        const description = left.getByText('browser.overview.description')
        const tags = left.getByText('browser.overview.tags')
        const modules = left.getByText('browser.overview.modules:1')
        const versionPatterns = left.getByText('browser.overview.version_patterns')
        const exposedMethods = left.getByText('browser.overview.exposed_methods')

        expect(left.getByText('A ruleset')).toBeInTheDocument()
        expect(left.getByText('Region')).toBeInTheDocument()
        expect(left.getByText('EU')).toBeInTheDocument()
        expect(left.getByText('Pricing')).toBeInTheDocument()
        expect(left.getByText('.*-%state%')).toBeInTheDocument()
        expect(left.getByText('calc*')).toBeInTheDocument()
        expect(left.getByText('debug*')).toBeInTheDocument()
        expect(right.queryByText('browser.overview.tags')).toBeNull()
        expect(right.queryByText('browser.overview.version_patterns')).toBeNull()
        expect(right.queryByText('browser.overview.exposed_methods')).toBeNull()
        expect(description.compareDocumentPosition(tags) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
        expect(tags.compareDocumentPosition(modules) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
        expect(modules.compareDocumentPosition(versionPatterns) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
        expect(versionPatterns.compareDocumentPosition(exposedMethods) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('expands from the header to show compilation errors and warnings', () => {
        renderPanel({
            ...base,
            branch: 'main',
            status: ProjectStatus.Opened,
            compileStatus: {
                projectId: 'p1',
                branch: 'main',
                compileState: 'errors',
                compilation: {
                    messages: {
                        total: 2,
                        errors: 1,
                        warnings: 1,
                        items: [
                            {
                                id: 1,
                                severity: 'ERROR',
                                summary: 'Broken table syntax',
                                stacktrace: false,
                            },
                            {
                                id: 2,
                                severity: 'WARN',
                                summary: 'Deprecated spreadsheet pattern',
                                stacktrace: false,
                            },
                        ],
                    },
                },
            },
        })

        // Collapsed by default; the header summarises the counts and expands to the message list.
        expect(screen.queryByText('Broken table syntax')).toBeNull()
        fireEvent.click(screen.getByRole('button', {
            name: 'browser.compile.error_count:1, browser.compile.warning_count:1',
        }))

        expect(screen.getByText('Broken table syntax')).toBeInTheDocument()
        expect(screen.getByText('Deprecated spreadsheet pattern')).toBeInTheDocument()
        expect(screen.queryByRole('link', { name: 'Broken table syntax' })).toBeNull()
    })

    it('renders no compile panel when there are no errors or warnings', () => {
        renderPanel({ ...base, status: ProjectStatus.Closed })

        expect(screen.queryByTestId('compile-messages')).toBeNull()
    })

    it('paginates compilation messages and expands long message text', () => {
        const longSummary = `${'Long warning text '.repeat(20)}\nline 2\nline 3\nline 4\nline 5`
        renderPanel({
            ...base,
            branch: 'main',
            status: ProjectStatus.Opened,
            compileStatus: {
                projectId: 'p1',
                branch: 'main',
                compileState: 'warnings',
                compilation: {
                    messages: {
                        total: 12,
                        errors: 0,
                        warnings: 12,
                        items: Array.from({ length: 12 }, (_, index) => ({
                            id: index + 1,
                            severity: 'WARN' as const,
                            summary: index === 0 ? longSummary : `Warning ${index + 1}`,
                            stacktrace: false,
                        })),
                    },
                },
            },
        })

        fireEvent.click(screen.getByRole('button', { name: 'browser.compile.warning_count:12' }))

        expect(screen.getByTestId('compile-message-1').textContent).not.toContain('line 5')
        expect(screen.queryByText('Warning 12')).toBeNull()

        fireEvent.click(screen.getByRole('button', { name: 'browser.compile.show_more:2' }))
        expect(screen.getByText('Warning 12')).toBeInTheDocument()

        fireEvent.click(screen.getByRole('button', { name: 'browser.compile.show_more_text' }))
        expect(screen.getByTestId('compile-message-1').textContent).toContain('line 5')

        const showLessButtons = screen.getAllByRole('button', { name: 'browser.compile.show_less' })
        fireEvent.click(showLessButtons.at(-1)!)
        expect(screen.queryByText('Warning 12')).toBeNull()
    })

    it('marks a protected current branch with a shield', () => {
        const { container } = renderPanel({ ...base, branch: 'main', branchProtected: true })
        expect(container.querySelector('.anticon-safety')).toBeTruthy()
    })

    it('shows no shield for an unprotected branch', () => {
        const { container } = renderPanel({ ...base, branch: 'main' })
        expect(container.querySelector('.anticon-safety')).toBeNull()
    })

    it('shows a logical repository icon', () => {
        renderPanel(base, 'repo-jdbc')

        expect(screen.getByTestId('repo-badge-database')).toBeInTheDocument()
    })
})

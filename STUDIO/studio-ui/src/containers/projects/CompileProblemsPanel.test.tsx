import { fireEvent, render as renderBare, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CompileProblemsPanel } from './CompileProblemsPanel'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { ProjectStatusDetailedMessage, ProjectStatusUpdate } from '../../services/projectStatus'

vi.mock('../../services/projectStatus', async importOriginal => ({
    ...(await importOriginal<typeof import('../../services/projectStatus')>()),
    subscribeProjectStatus: () => ({ unsubscribe: () => {} }),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, values?: Record<string, unknown>) => (values ? `${key}:${Object.values(values).join(',')}` : key),
    }),
}))

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

/** The panel sends the reader to a table, so it is drawn where a route can be followed. */
const render = (element: React.ReactElement) => renderBare(<MemoryRouter>{element}</MemoryRouter>)

const base: Project = {
    id: 'p1',
    name: 'Proj',
    branch: 'main',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    repository: 'design',
    revision: '',
    status: ProjectStatus.Opened,
}

const status = (
    items: ProjectStatusDetailedMessage[],
    compileState?: ProjectStatusUpdate['compileState']
): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState: compileState ?? (items.some(item => item.severity === 'ERROR') ? 'errors' : 'warnings'),
    compilation: {
        messages: {
            total: items.length,
            errors: items.filter(item => item.severity === 'ERROR').length,
            warnings: items.filter(item => item.severity === 'WARN').length,
            items,
        },
    },
})

/** The fold and the height are remembered in the browser; each test starts from its own, empty memory. */
const stubStorage = () => {
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
        getItem: (key: string) => store[key] ?? null,
        setItem: (key: string, value: string) => { store[key] = value },
        removeItem: (key: string) => { delete store[key] },
    })
}

describe('CompileProblemsPanel', () => {
    beforeEach(() => {
        stubStorage()
    })

    it('does not exist for a project without errors or warnings', () => {
        render(<CompileProblemsPanel project={{ ...base, status: ProjectStatus.Closed }} />)

        expect(screen.queryByTestId('compile-problems')).toBeNull()
    })

    it('counts the errors and warnings in the header and lists the messages below', () => {
        render(<CompileProblemsPanel
            project={{
                ...base,
                compileStatus: status([
                    { id: 1, severity: 'ERROR', summary: 'Broken table syntax', stacktrace: false },
                    { id: 2, severity: 'WARN', summary: 'Deprecated spreadsheet pattern', stacktrace: false },
                ]),
            }}
        />)

        // The counts are bare numbers beside their icons — no labels.
        expect(screen.getByTestId('compile-problems-errors')).toHaveTextContent('1')
        expect(screen.getByTestId('compile-problems-warnings')).toHaveTextContent('1')
        expect(screen.getByText('Broken table syntax')).toBeInTheDocument()
        expect(screen.getByText('Deprecated spreadsheet pattern')).toBeInTheDocument()
        // Messages carry no links yet.
        expect(screen.queryByRole('link')).toBeNull()
    })

    it('shows only the badge of a severity the project has', () => {
        render(<CompileProblemsPanel
            project={{
                ...base,
                compileStatus: status([{ id: 1, severity: 'WARN', summary: 'One warning', stacktrace: false }]),
            }}
        />)

        expect(screen.queryByTestId('compile-problems-errors')).toBeNull()
        expect(screen.getByTestId('compile-problems-warnings')).toHaveTextContent('1')
    })

    it('folds by its header, keeping the counts in view, and remembers the fold', () => {
        const project = {
            ...base,
            compileStatus: status([{ id: 1, severity: 'ERROR', summary: 'Broken table syntax', stacktrace: false }]),
        }
        const { unmount } = render(<CompileProblemsPanel project={project} />)

        fireEvent.click(screen.getByTestId('compile-problems-header'))

        expect(screen.queryByText('Broken table syntax')).toBeNull()
        expect(screen.getByTestId('compile-problems-errors')).toHaveTextContent('1')

        // The fold survives leaving the screen.
        unmount()
        render(<CompileProblemsPanel project={project} />)
        expect(screen.queryByText('Broken table syntax')).toBeNull()

        fireEvent.click(screen.getByTestId('compile-problems-header'))
        expect(screen.getByText('Broken table syntax')).toBeInTheDocument()
    })

    it('paginates the messages and expands long message text', () => {
        const longSummary = `${'Long warning text '.repeat(20)}\nline 2\nline 3\nline 4\nline 5`
        render(<CompileProblemsPanel
            project={{
                ...base,
                compileStatus: status(Array.from({ length: 12 }, (_, index) => ({
                    id: index + 1,
                    severity: 'WARN' as const,
                    summary: index === 0 ? longSummary : `Warning ${index + 1}`,
                    stacktrace: false,
                }))),
            }}
        />)

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

    it('opens the table a problem was raised against, without asking the server for it', () => {
        render(<CompileProblemsPanel
            project={{
                ...base,
                compileStatus: status([{
                    id: 7,
                    severity: 'ERROR' as const,
                    summary: 'Identifier is not found',
                    stacktrace: false,
                    location: {
                        type: 'table' as const,
                        id: 'table-9',
                        name: 'Greeting',
                        module: 'Bank Rating',
                        projectId: 'p9',
                    },
                }], 'errors'),
            }}
        />)

        fireEvent.click(screen.getByTestId('compile-message-7'))

        // The message carried the project, the module and the table, so nothing had to be asked for.
        expect(navigate).toHaveBeenCalledWith('/projects/p9/modules/Bank%20Rating?table=table-9')
    })

    it('keeps the reader where they are while a module is being compiled', () => {
        render(<CompileProblemsPanel
            project={{
                ...base,
                compileStatus: status([{
                    id: 8,
                    severity: 'ERROR' as const,
                    summary: 'Identifier is not found',
                    stacktrace: false,
                    location: { type: 'table' as const, id: 'table-9', module: 'Bank Rating' },
                }], 'compiling'),
            }}
        />)

        fireEvent.click(screen.getByTestId('compile-message-8'))

        // One module is compiled at a time; opening another would only queue behind the one running.
        expect(navigate).not.toHaveBeenCalled()
    })

    it('stands on the counts a running compilation reports, before it can say which messages they are', () => {
        // What a compilation tells about itself while it runs: how many problems, not which.
        const compiling: ProjectStatusUpdate = {
            projectId: 'p1',
            branch: 'main',
            compileState: 'compiling',
            compilation: { messages: { total: 3, errors: 2, warnings: 1 } },
        }

        render(<CompileProblemsPanel project={{ ...base, compileStatus: compiling }} />)

        // The panel does not disappear under the reader for as long as the compilation lasts.
        expect(screen.getByTestId('compile-problems')).toBeInTheDocument()
        expect(screen.getByTestId('compile-problems-errors')).toHaveTextContent('2')
        expect(screen.getByTestId('compile-problems-warnings')).toHaveTextContent('1')
    })
})

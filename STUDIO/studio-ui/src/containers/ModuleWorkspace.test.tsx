import type { ReactNode } from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ModuleWorkspace } from './ModuleWorkspace'
import { getModuleTables, getRawTable, listModules } from '../services/modules'
import { getProject, getProjects, setProjectStatus } from '../services/repositories'

const { navigateMock, routeParams, searchParams, setSearchParamsMock, workspace } = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    routeParams: { projectId: 'p1', moduleName: 'Bank Rating' },
    // The address a link to one table carries: the module, and the table to draw on it.
    searchParams: new URLSearchParams('table=t-1'),
    setSearchParamsMock: vi.fn(),
    // What the workspace holds of the project: closed until the reader answers the question to open it, and
    // what the compilation of its module came to.
    workspace: { opened: false, state: 'ok' },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
    useParams: () => routeParams,
    useSearchParams: () => [searchParams, setSearchParamsMock],
    Link: ({ children, to }: { children?: unknown, to?: string }) => <a href={to}>{children as never}</a>,
}))

vi.mock('../hooks', async () => ({
    ...(await vi.importActual<typeof import('../hooks/useLoadGeneration')>('../hooks/useLoadGeneration')),
}))

vi.mock('../services/repositories', () => ({
    getProject: vi.fn(),
    getProjects: vi.fn(),
    setProjectStatus: vi.fn(),
}))

vi.mock('../services/modules', () => ({
    cancelModuleCompilation: vi.fn(),
    getModuleTables: vi.fn(),
    getRawTable: vi.fn(),
    listModules: vi.fn(),
    TABLE_PAGE_ROWS: 120,
}))

// The compilation is followed on the status channel, which this screen is not the place to test: the module is
// compiled exactly when the project is open.
vi.mock('./modules/useModuleCompilation', () => ({
    useModuleCompilation: () => ({
        ready: workspace.opened,
        compiled: workspace.opened ? 1 : 0,
        total: 1,
        failure: null,
        tests: 0,
        state: workspace.opened ? workspace.state : 'idle',
        status: null,
    }),
}))

// The question asked before a closed project is opened is answered here; the dialog has its own tests.
vi.mock('./projects/openProjectDialog', () => ({
    openProjectDialog: (_project: unknown, onConfirm: (openDependencies: boolean) => void) => onConfirm(false),
    closeProjectDialog: vi.fn(),
}))

vi.mock('./modules/ModuleTablesTree', () => ({ ModuleTablesTree: () => <div data-testid="tables-tree" /> }))
vi.mock('./modules/ModuleActionBar', () => ({ ModuleActionBar: () => null }))
vi.mock('./modules/TableDetailsPanel', () => ({ TableDetailsPanel: () => <div data-testid="table-details" /> }))
vi.mock('./modules/TableProblems', () => ({ TableProblems: () => null }))
vi.mock('./modules/TableSearchModal', () => ({ TableSearchModal: () => null }))
vi.mock('./modules/TableToolbar', () => ({ TableToolbar: () => <div data-testid="table-toolbar" /> }))
vi.mock('./projects/CompileProblemsPanel', () => ({ CompileProblemsPanel: () => null }))
vi.mock('./projects/BranchSwitcher', () => ({ BranchSwitcher: () => null }))
// The table itself is drawn and edited elsewhere; this screen is asked only what it hands over.
vi.mock('./modules/TableEditor', () => ({
    TableEditor: ({ testId, rows, children }: {
        testId?: string
        rows?: unknown[]
        children?: ReactNode
    }) => (
        <div data-testid={testId}>
            {`rows:${rows?.length ?? 0}`}
            {children}
        </div>
    ),
}))

const project = (status: string) => ({
    id: 'p1',
    name: 'Example 1 - Bank Rating',
    status,
    repository: 'design',
    branch: 'master',
    repositoryInfo: { id: 'design', name: 'Design', features: { branches: true } },
})

describe('ModuleWorkspace', () => {
    beforeEach(() => {
        workspace.opened = false
        workspace.state = 'ok'
        routeParams.projectId = 'p1'
        searchParams.set('table', 't-1')
        vi.mocked(getProject).mockImplementation(() =>
            Promise.resolve(project(workspace.opened ? 'OPENED' : 'CLOSED') as never))
        vi.mocked(setProjectStatus).mockImplementation(() => {
            workspace.opened = true
            return Promise.resolve(undefined as never)
        })
        vi.mocked(listModules).mockResolvedValue([{ name: 'Bank Rating', path: 'rules/Bank Rating.xlsx' }])
        vi.mocked(getProjects).mockResolvedValue({
            content: [{ id: 'p1', name: 'Bank Rating' }, { id: 'p2', name: 'Car Rating' }],
            pageNumber: 0,
            pageSize: 2,
            numberOfElements: 2,
            total: 2,
        } as never)
        vi.mocked(getModuleTables).mockResolvedValue([
            { id: 't-1', name: 'BankRating', kind: 'Rules', tableType: 'SimpleRules' },
        ] as never)
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
        } as never)
    })

    it('says nothing about a module that compiled, and marks one that raised something', async () => {
        workspace.opened = true
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalled())

        // A module the compiler had nothing to say about wears no mark, as the project screens wear none.
        expect(screen.queryByTestId('module-compile-state')).toBeNull()

        workspace.state = 'errors'
        rerender(<ModuleWorkspace />)

        // What it raised is a coloured dot and its tooltip — no word of it is spelled out beside the name.
        const mark = await screen.findByTestId('module-compile-state')
        expect(mark).toBeInTheDocument()
        expect(mark.textContent).toBe('')
    })

    it('opens the project screen when another project is picked, as the Editor did', async () => {
        workspace.opened = true
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await userEvent.click(await screen.findByText('Car Rating'))

        // Which of its modules to read is the reader's to say, and the project screen is where they are listed.
        expect(navigateMock).toHaveBeenCalledWith('/projects/p2')
    })

    it('opens another module of the project from the name in the header', async () => {
        workspace.opened = true
        vi.mocked(listModules).mockResolvedValue([
            { name: 'Bank Rating' }, { name: 'Pricing' },
        ] as never)
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('module-switcher-trigger'))
        await userEvent.click(await screen.findByText('Pricing'))

        expect(navigateMock).toHaveBeenCalledWith('/projects/p1/modules/Pricing')
    })

    it('reads the tables of the module of the project the address names', async () => {
        workspace.opened = true
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating'))

        // Another project whose module carries the same name: its tables are none of the first one's.
        routeParams.projectId = 'p2'
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p2', 'Bank Rating'))
    })

    it('drops the rows of a table the reader left rather than drawing them under the next one', async () => {
        workspace.opened = true
        vi.mocked(getModuleTables).mockResolvedValue([
            { id: 't-1', name: 'BankRating', kind: 'Rules', tableType: 'SimpleRules' },
            { id: 't-2', name: 'Limit', kind: 'Rules', tableType: 'SimpleRules' },
        ] as never)
        // The first window of the first table, then a window still on its way when the reader moves on.
        let releaseMore!: (read: unknown) => void
        vi.mocked(getRawTable)
            .mockResolvedValueOnce({
                id: 't-1', name: 'BankRating', source: [[{ cell: 'A1', value: '1' }]], totalRows: 3,
            } as never)
            .mockImplementationOnce(() => new Promise(resolve => {
                releaseMore = resolve
            }) as never)
            .mockResolvedValue({ id: 't-2', name: 'Limit', source: [[{ cell: 'A1', value: '2' }]]} as never)
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('module-table')).toHaveTextContent('rows:1'))

        await userEvent.click(screen.getByTestId('module-table-more'))
        searchParams.set('table', 't-2')
        rerender(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('module-table')).toHaveTextContent('rows:1'))

        await act(async () => {
            releaseMore({ id: 't-1', name: 'BankRating', source: [[{ cell: 'A2', value: '1b' }]]})
        })

        // Still the one row of the table on screen: the other table's rows were not added to it.
        expect(screen.getByTestId('module-table')).toHaveTextContent('rows:1')
    })

    it('reads no table of a project nobody opened, and draws the one the link names once it is opened', async () => {
        render(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('module-project-closed')).toBeInTheDocument())
        // The workspace holds no copy of a closed project, so there is nothing to read and nothing to report.
        expect(getRawTable).not.toHaveBeenCalled()

        await userEvent.click(screen.getByRole('button', { name: 'browser.open' }))

        expect(setProjectStatus).toHaveBeenCalledWith('p1', 'OPENED', { openDependencies: false })
        // The table the address names is drawn as soon as the module answers with it — without a page refresh.
        await waitFor(() => expect(screen.getByTestId('module-table')).toBeInTheDocument())
        expect(getRawTable).toHaveBeenCalledWith('p1', 't-1', expect.objectContaining({ module: 'Bank Rating' }))
        expect(screen.queryByTestId('module-workspace-error')).toBeNull()
    })
})

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
    workspace: { opened: false, state: 'ok', branch: 'master' },
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
    useModuleCompilation: (projectId: string) => ({
        // Nothing is compiled of a project that has not been read yet: the screen does not know its id.
        ready: workspace.opened && projectId !== '',
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
// The band shows what it is handed, so a test can read what the screen decided.
vi.mock('./modules/TableToolbar', () => ({
    TableToolbar: ({ runState, projectCompiled }: { runState?: string, projectCompiled?: boolean }) => (
        <div data-compiled={String(projectCompiled)} data-testid="table-toolbar">{runState}</div>
    ),
}))
vi.mock('./projects/CompileProblemsPanel', () => ({ CompileProblemsPanel: () => null }))
// The switcher itself is tested elsewhere; here it only has to say that a branch was switched.
vi.mock('./projects/BranchSwitcher', () => ({
    BranchSwitcher: ({ onSwitched }: { onSwitched?: () => void }) => (
        <button data-testid="branch-switched" onClick={() => onSwitched?.()} type="button" />
    ),
}))
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
    branch: workspace.branch,
    repositoryInfo: { id: 'design', name: 'Design', features: { branches: true } },
})

describe('ModuleWorkspace', () => {
    beforeEach(() => {
        routeParams.projectId = 'p1'
        routeParams.moduleName = 'Bank Rating'
        workspace.opened = false
        workspace.state = 'ok'
        workspace.branch = 'master'
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

    it('reads the module again on the branch that was switched to', async () => {
        workspace.opened = true
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledTimes(1))

        // The modules a project holds are the branch's, so they are read once the project says which branch
        // it stands on — not once before it and again after.
        expect(listModules).toHaveBeenCalledTimes(1)

        workspace.branch = 'release'
        await userEvent.click(screen.getByTestId('branch-switched'))

        // Another branch is another copy of the project: the tables read on the one left behind are none of
        // its, and the module it holds is read afresh.
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledTimes(2))
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

    it('asks again how far a table may run once the rest of the project is built', async () => {
        workspace.opened = true
        // Read while only the module was built: a run had to stay inside it.
        workspace.state = 'compiling'
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
            runState: 'can-run-module',
        } as never)
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run-module'))

        // The rest of the project is built: the state is asked again, for the run state alone, and the band
        // offers the whole project.
        vi.mocked(getRawTable).mockResolvedValue({ id: 't-1', name: 'BankRating', source: [], runState: 'can-run' } as never)
        workspace.state = 'ok'
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run'))
        expect(getRawTable).toHaveBeenLastCalledWith('p1', 't-1', { module: 'Bank Rating', maxRows: 1, runState: true })
        // The rows drawn stay as they were: only the run state was asked for.
        expect(screen.getByTestId('module-table')).toHaveTextContent('rows:1')
    })

    it('asks again how far a table may run after a refresh builds the module again', async () => {
        workspace.opened = true
        workspace.state = 'ok'
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
            runState: 'can-run',
        } as never)
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'true'))

        // A refresh builds the module again, and the rest of the project after it: the table read once the
        // module is back has to keep a run inside it, and the project is no longer as built as it was - even
        // before the status channel says so.
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
            runState: 'can-run-module',
        } as never)
        await userEvent.click(screen.getByTestId('module-refresh'))
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run-module'))
        expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'false')
        workspace.state = 'compiling'
        rerender(<ModuleWorkspace />)
        expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'false')

        // The build ends: the state is asked again, and the band offers the whole project once more.
        vi.mocked(getRawTable).mockResolvedValue({ id: 't-1', name: 'BankRating', source: [], runState: 'can-run' } as never)
        workspace.state = 'ok'
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run'))
        expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'true')
        expect(getRawTable).toHaveBeenLastCalledWith('p1', 't-1', { module: 'Bank Rating', maxRows: 1, runState: true })
    })

    it('asks again about a table read during the build whose answer arrives after the build ended', async () => {
        workspace.opened = true
        workspace.state = 'compiling'
        let releaseTable!: (read: unknown) => void
        vi.mocked(getRawTable)
            .mockImplementationOnce(() => new Promise(resolve => {
                releaseTable = resolve
            }) as never)
            .mockResolvedValue({ id: 't-1', name: 'BankRating', source: [], runState: 'can-run' } as never)
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getRawTable).toHaveBeenCalledTimes(1))

        // The build ends while the read is still on its way, so what it answers is already out of date.
        workspace.state = 'ok'
        rerender(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'true'))
        await act(async () => {
            releaseTable({
                id: 't-1', name: 'BankRating', source: [[{ cell: 'A1', value: 'Bank' }]], runState: 'can-run-module',
            })
        })

        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run'))
        expect(getRawTable).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('module-table')).toHaveTextContent('rows:1')
    })

    it('keeps the project compiled when another compiled module of it is opened', async () => {
        workspace.opened = true
        workspace.state = 'ok'
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'true'))

        // The other module is compiled already, so no compilation is asked for and no status will arrive: the
        // project is as built as it was, and a run may still reach all of it.
        routeParams.moduleName = 'Car Rating'
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Car Rating'))
        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveAttribute('data-compiled', 'true'))
    })

    it('does not ask again about a table that could run against the whole project from the start', async () => {
        workspace.opened = true
        workspace.state = 'ok'
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
            runState: 'can-run',
        } as never)
        render(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('table-toolbar')).toHaveTextContent('can-run'))
        expect(getRawTable).toHaveBeenCalledTimes(1)
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

import type { ReactNode } from 'react'
import type { ModuleTable } from 'types/tables'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useUserStore } from '../store'
import { ModuleWorkspace } from './ModuleWorkspace'
import { getModuleTables, getRawTable, listModules } from '../services/modules'
import { getProject, getProjects, setProjectStatus } from '../services/repositories'
import { ApiHttpError, NotFoundError, notifyLoadFailure } from '../services/apiCall'

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
    ...(await vi.importActual<typeof import('../hooks/useCanonicalProjectAddress')>('../hooks/useCanonicalProjectAddress')),
}))

vi.mock('../services/apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('../services/apiCall')>(),
    notifyLoadFailure: vi.fn(),
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
    TABLE_PAGE_ROWS: 2000,
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

// The tree itself is tested elsewhere; here it only has to ask for the utility tables, as its checkbox does,
// and to open the extended search.
vi.mock('./modules/ModuleTablesTree', () => ({
    ModuleTablesTree: ({ showOther, onShowOther, reloading, onExtendedSearch }: {
        showOther?: boolean
        onShowOther?: (shown: boolean) => void
        reloading?: boolean
        onExtendedSearch?: (typed: string) => void
    }) => (
        <>
            <button
                data-reloading={String(reloading)}
                data-testid="tables-tree"
                onClick={() => onShowOther?.(!showOther)}
                type="button"
            >
                {String(showOther)}
            </button>
            <button data-testid="tables-search" onClick={() => onExtendedSearch?.('')} type="button" />
        </>
    ),
}))
vi.mock('./modules/ModuleActionBar', () => ({ ModuleActionBar: () => null }))
vi.mock('./modules/TableDetailsPanel', () => ({ TableDetailsPanel: () => <div data-testid="table-details" /> }))
vi.mock('./modules/TableProblems', () => ({ TableProblems: () => null }))
// The search itself is tested elsewhere; here it only hands over the note it found.
vi.mock('./modules/TableSearchModal', () => ({
    TableSearchModal: ({ open, onOpen }: { open?: boolean, onOpen?: (found: ModuleTable) => void }) => (
        open
            ? <button data-testid="search-found" onClick={() => onOpen?.(notes)} type="button" />
            : null
    ),
}))
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
    TableEditor: ({ testId, rows, hiddenRows, children }: {
        testId?: string
        rows?: unknown[]
        hiddenRows?: number
        children?: ReactNode
    }) => (
        <div data-testid={testId}>
            {`rows:${rows?.length ?? 0} hidden:${hiddenRows ?? 0}`}
            {children}
        </div>
    ),
}))

// The rules table the module is read with, and the note OpenL does not recognize, listed only when asked for.
const bankRating = { id: 't-1', name: 'BankRating', kind: 'Rules', tableType: 'SimpleRules' } as ModuleTable
const notes = { id: 't-9', name: 'Notes', kind: 'Other', tableType: 'Other' } as ModuleTable

/** The module's tables as the server answers: the note among them only when the free-form tables are asked for. */
const tablesRead = (_projectId: string, _module: string, options?: { includeOther?: boolean }) =>
    Promise.resolve(options?.includeOther ? [bankRating, notes] : [bankRating])

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
        localStorage.clear()
        useUserStore.setState({ userProfile: undefined })
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
        vi.mocked(getModuleTables).mockImplementation(tablesRead)
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
        } as never)
    })

    it('hands the table over whole with the count of header rows to keep out of sight', async () => {
        workspace.opened = true
        useUserStore.setState({ userProfile: { showHeader: false } as never })
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Rules' }], [{ cell: 'A2', value: 'C1' }], [{ cell: 'A3', value: '1' }]],
            headerHeight: 2,
        } as never)

        render(<ModuleWorkspace />)

        // "Show Header" puts the header rows away, it does not renumber the rest: cut them off here and every
        // edit the reader makes below them is written two rows higher than they made it.
        await waitFor(() => expect(screen.getByTestId('module-table')).toHaveTextContent('rows:3 hidden:2'))
    })

    it('keeps every row in sight when the reader asked to see the header', async () => {
        workspace.opened = true
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Rules' }], [{ cell: 'A2', value: '1' }]],
            headerHeight: 1,
        } as never)

        render(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('module-table')).toHaveTextContent('rows:2 hidden:0'))
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
        // Each project answers with its own id.
        vi.mocked(getProject).mockImplementation(projectId => Promise.resolve({ ...project('OPENED'), id: projectId } as never))
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: false }))

        // Another project whose module carries the same name: its tables are none of the first one's.
        routeParams.projectId = 'p2'
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p2', 'Bank Rating', { includeOther: false }))
    })

    it('reads the module again with the utility tables once they are asked for, and remembers the choice', async () => {
        workspace.opened = true
        // The first list answers at once; the one with the utility tables takes as long as the server takes.
        const { promise: withOther, resolve: answerWithOther } = Promise.withResolvers<ModuleTable[]>()
        vi.mocked(getModuleTables).mockImplementation((projectId, module, options) =>
            options?.includeOther ? withOther : tablesRead(projectId, module, options))
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: false }))
        expect(screen.getByTestId('tables-tree')).toHaveAttribute('data-reloading', 'false')

        await userEvent.click(screen.getByTestId('tables-tree'))

        // The server lists the free-form tables only on request, so asking is a read of the module, not a filter.
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: true }))
        expect(getModuleTables).toHaveBeenCalledTimes(2)
        // Until it answers, the list that was on screen stays, and the rail is told the next one is on its way.
        expect(screen.getByTestId('table-toolbar')).toBeInTheDocument()
        expect(screen.getByTestId('tables-tree')).toHaveAttribute('data-reloading', 'true')

        await act(async () => {
            answerWithOther([bankRating, notes])
            await withOther
        })

        expect(screen.getByTestId('tables-tree')).toHaveAttribute('data-reloading', 'false')
        // Kept by this browser, as the tree's view is: the next module opens with them listed.
        expect(localStorage.getItem('openl.module.otherTables')).toBe('true')
    })

    it('keeps the list on screen and puts the choice back when the utility tables cannot be read', async () => {
        workspace.opened = true
        vi.mocked(getModuleTables).mockImplementation((projectId, module, options) =>
            options?.includeOther ? Promise.reject(new Error('Gone away')) : tablesRead(projectId, module, options))
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledTimes(1))

        await userEvent.click(screen.getByTestId('tables-tree'))

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalled())
        // The list read before is still there, the rail says it is that list, and nothing waits any more.
        expect(screen.queryByTestId('module-workspace-error')).toBeNull()
        expect(screen.getByTestId('tables-tree')).toHaveTextContent('false')
        expect(screen.getByTestId('tables-tree')).toHaveAttribute('data-reloading', 'false')
        expect(localStorage.getItem('openl.module.otherTables')).toBe('false')
        expect(getModuleTables).toHaveBeenCalledTimes(2)
    })

    it('looks for the table a link named among the utility tables before giving up on the address', async () => {
        workspace.opened = true
        // A link to a free-form table, opened while the tree leaves them out.
        searchParams.set('table', 't-9')
        render(<ModuleWorkspace />)

        // The list read without them does not hold the table, so they are asked for rather than the address
        // being replaced with the first table of the list.
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: true }))
        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-9', expect.anything()))
        expect(navigateMock).not.toHaveBeenCalled()
        // The tree shows them for this visit, the opened one among them — not for every visit to come.
        expect(screen.getByTestId('tables-tree')).toHaveTextContent('true')
        expect(localStorage.getItem('openl.module.otherTables')).toBeNull()
    })

    it('says the module has no table a link names that the whole list does not hold, the utility tables included', async () => {
        workspace.opened = true
        searchParams.set('table', 't-gone')
        render(<ModuleWorkspace />)

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: true }))
        // Only once the whole list has answered is the table given up on, and no other one opens in its place.
        await waitFor(() => expect(screen.getByTestId('module-table-missing')).toBeInTheDocument())
        // Nor is anything said about a table that is not there.
        expect(screen.queryByTestId('table-details')).toBeNull()
        expect(navigateMock).not.toHaveBeenCalled()
        expect(getRawTable).not.toHaveBeenCalled()
        expect(getModuleTables).toHaveBeenCalledTimes(2)
    })

    it('shows a table the address names once the module holds it after all', async () => {
        workspace.opened = true
        const fresh = { id: 't-new', name: 'Fresh', kind: 'Rules', tableType: 'SimpleRules' } as ModuleTable
        searchParams.set('table', 't-new')
        render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('module-table-missing')).toBeInTheDocument())

        // The table is written meanwhile, and the module is read again.
        vi.mocked(getModuleTables).mockResolvedValue([bankRating, fresh])
        await userEvent.click(screen.getByTestId('module-refresh'))

        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-new', expect.anything()))
        expect(screen.queryByTestId('module-table-missing')).toBeNull()
        expect(screen.getByTestId('table-details')).toBeInTheDocument()
    })

    it('moves off a table it showed when the branch switched to does not carry it', async () => {
        workspace.opened = true
        const pricing = { id: 't-5', name: 'Pricing', kind: 'Rules', tableType: 'SimpleRules' } as ModuleTable
        vi.mocked(getModuleTables).mockImplementation(() =>
            Promise.resolve(workspace.branch === 'master' ? [bankRating, pricing] : [bankRating]))
        searchParams.set('table', 't-5')
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-5', expect.anything()))

        workspace.branch = 'feature'
        await userEvent.click(screen.getByTestId('branch-switched'))

        // The table was there before the switch: the module opens on its first table rather than saying so.
        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith(
            '/projects/p1/modules/Bank%20Rating?table=t-1', { replace: true }))
        expect(screen.queryByTestId('module-table-missing')).toBeNull()
    })

    it('looks for a table the reader moves on to among the utility tables before saying there is none', async () => {
        workspace.opened = true
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-1', expect.anything()))

        // Within the module the reader follows a way into a free-form table while the tree leaves them out.
        searchParams.set('table', 't-9')
        rerender(<ModuleWorkspace />)

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: true }))
        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-9', expect.anything()))
        expect(screen.queryByTestId('module-table-missing')).toBeNull()
    })

    it('asks for the utility tables once for a table a link names when they cannot be read, and again on Refresh', async () => {
        workspace.opened = true
        vi.mocked(getModuleTables).mockImplementation((projectId, module, options) =>
            options?.includeOther ? Promise.reject(new Error('Gone away')) : tablesRead(projectId, module, options))
        searchParams.set('table', 't-gone')
        render(<ModuleWorkspace />)

        // Nothing can be said of the table while the utility tables cannot be read: no verdict of absence.
        await waitFor(() => expect(screen.getByTestId('module-table-unread')).toBeInTheDocument())
        expect(screen.queryByTestId('module-table-missing')).toBeNull()
        const askedForOther = () => vi.mocked(getModuleTables).mock.calls.filter(([, , options]) => options?.includeOther)
        // The failure is reported once, and the free-form tables are not asked for over and over.
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 100)) })
        expect(askedForOther()).toHaveLength(1)
        expect(notifyLoadFailure).toHaveBeenCalledTimes(1)

        // Refresh is the reader asking again.
        await userEvent.click(screen.getByTestId('module-refresh'))
        await waitFor(() => expect(askedForOther()).toHaveLength(2))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 100)) })
        expect(askedForOther()).toHaveLength(2)
    })

    it('looks among the utility tables again for a table it found none of once the branch is switched', async () => {
        workspace.opened = true
        // Only the other branch holds the table, and there as a free-form one.
        vi.mocked(getModuleTables).mockImplementation((_projectId, _module, options) =>
            Promise.resolve(workspace.branch === 'feature' && options?.includeOther ? [bankRating, notes] : [bankRating]))
        searchParams.set('table', 't-9')
        render(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('module-table-missing')).toBeInTheDocument())

        // The reader puts the utility tables away, then switches the branch.
        await userEvent.click(screen.getByTestId('tables-tree'))
        await waitFor(() => expect(screen.getByTestId('tables-tree')).toHaveTextContent('false'))
        expect(screen.getByTestId('module-table-missing')).toBeInTheDocument()
        workspace.branch = 'feature'
        await userEvent.click(screen.getByTestId('branch-switched'))

        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-9', expect.anything()))
        expect(screen.queryByTestId('module-table-missing')).toBeNull()
    })

    it('moves off a utility table the reader took out of the list, rather than listing them again', async () => {
        workspace.opened = true
        searchParams.set('table', 't-9')
        localStorage.setItem('openl.module.otherTables', 'true')
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getRawTable).toHaveBeenCalledWith('p1', 't-9', expect.anything()))

        // The reader asks for the free-form tables no more, while reading one of them.
        await userEvent.click(screen.getByTestId('tables-tree'))

        // The one on screen is gone from the list by the reader's own choice: the first table is opened instead,
        // and the choice stands — it is not the link's table being looked for again.
        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith(
            '/projects/p1/modules/Bank%20Rating?table=t-1', { replace: true }))
        expect(getModuleTables).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('tables-tree')).toHaveTextContent('false')
        expect(localStorage.getItem('openl.module.otherTables')).toBe('false')
    })

    it('asks for the utility tables before opening one the search found, so the module is read once more, not twice', async () => {
        workspace.opened = true
        render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledTimes(1))

        await userEvent.click(screen.getByTestId('tables-search'))
        await userEvent.click(await screen.findByTestId('search-found'))

        // The search says what kind the table is, so the list it belongs in is asked for straight away.
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: true }))
        expect(getModuleTables).toHaveBeenCalledTimes(2)
        expect(setSearchParamsMock).toHaveBeenCalled()
        expect(navigateMock).not.toHaveBeenCalled()
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

        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Car Rating', { includeOther: false }))
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

    it('says a link leads to no project when the project is not there, instead of failing to load the module', async () => {
        routeParams.projectId = 'nonexistent'
        vi.mocked(getProject).mockRejectedValue(new NotFoundError())
        render(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('project-workspace-missing')).toBeInTheDocument())
        expect(screen.queryByTestId('module-workspace-error')).toBeNull()
    })

    it('lists the projects a name leads to, and opens the one picked on the same module and table', async () => {
        routeParams.projectId = 'Example 1 - Bank Rating'
        vi.mocked(getProject).mockRejectedValue(new ApiHttpError(409, 'The project name is ambiguous.', {
            code: 'openl.error.409.project.identifier.ambiguous.message',
            candidates: [
                { id: 'ZGVzaWduOkJhbms=', name: 'Example 1 - Bank Rating', repositoryName: 'Design' },
                { id: 'ZGVzaWduMTpCYW5r', name: 'Example 1 - Bank Rating', repositoryName: 'Design1' },
            ],
        }))
        render(<ModuleWorkspace />)

        await waitFor(() => expect(screen.getByTestId('project-link-ambiguous')).toBeInTheDocument())
        expect(screen.queryByTestId('module-workspace-error')).toBeNull()

        await userEvent.click(screen.getAllByTestId('project-link-candidate')[1]!)
        expect(navigateMock).toHaveBeenCalledWith('/projects/ZGVzaWduMTpCYW5r/modules/Bank%20Rating?table=t-1')
    })

    it('replaces a project name in the address with the project id before reading the module', async () => {
        workspace.opened = true
        routeParams.projectId = 'Example 1 - Bank Rating'
        const { rerender } = render(<ModuleWorkspace />)

        await waitFor(() => expect(navigateMock)
            .toHaveBeenCalledWith('/projects/p1/modules/Bank%20Rating?table=t-1', { replace: true }))
        // Nothing of the module is read under the name.
        expect(screen.getByTestId('module-workspace-loading')).toBeInTheDocument()
        expect(listModules).not.toHaveBeenCalled()

        // The router moves the address on to the id, and the project is read again by it.
        routeParams.projectId = 'p1'
        rerender(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: false }))
        expect(getModuleTables).toHaveBeenCalledTimes(1)
        expect(getProject).toHaveBeenLastCalledWith('p1', expect.anything(), expect.anything())
        expect(listModules).toHaveBeenCalledExactlyOnceWith('p1')
    })

    it('shows nothing of the project it left while the next project is read, nor reads the module for it', async () => {
        workspace.opened = true
        const { rerender } = render(<ModuleWorkspace />)
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p1', 'Bank Rating', { includeOther: false }))
        expect(screen.getByTestId('crumb-project')).toHaveTextContent('Example 1 - Bank Rating')

        // The reader moves on to a module of another project; its read is still on its way.
        const { promise: nextRead, resolve: answerNext } = Promise.withResolvers<never>()
        vi.mocked(getProject).mockReturnValueOnce(nextRead)
        routeParams.projectId = 'p2'
        rerender(<ModuleWorkspace />)

        expect(screen.getByTestId('module-workspace-loading')).toBeInTheDocument()
        expect(screen.queryByTestId('crumb-project')).toBeNull()
        expect(listModules).not.toHaveBeenCalledWith('p2')
        expect(getModuleTables).not.toHaveBeenCalledWith('p2', expect.anything(), expect.anything())

        await act(async () => answerNext({ ...project('OPENED'), id: 'p2', name: 'Car Rating' } as never))
        expect(screen.getByTestId('crumb-project')).toHaveTextContent('Car Rating')
        await waitFor(() => expect(getModuleTables).toHaveBeenCalledWith('p2', 'Bank Rating', { includeOther: false }))
    })

    it('keeps the project of the address on screen when a read for the address it left answers late', async () => {
        const { promise: firstRead, resolve: answerFirst } = Promise.withResolvers<never>()
        vi.mocked(getProject)
            .mockReturnValueOnce(firstRead)
            .mockResolvedValueOnce({ ...project('OPENED'), id: 'p2', name: 'Car Rating' } as never)
        const { rerender } = render(<ModuleWorkspace />)

        // The reader moves on to a module of another project before the first project answers.
        routeParams.projectId = 'p2'
        rerender(<ModuleWorkspace />)
        await waitFor(() => expect(screen.getByTestId('crumb-project')).toHaveTextContent('Car Rating'))

        await act(async () => answerFirst(project('OPENED') as never))
        expect(screen.getByTestId('crumb-project')).toHaveTextContent('Car Rating')
    })
})

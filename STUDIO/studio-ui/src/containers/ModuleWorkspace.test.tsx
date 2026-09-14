import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ModuleWorkspace } from './ModuleWorkspace'
import { getModuleTables, getRawTable, listModules } from '../services/modules'
import { getProject, setProjectStatus } from '../services/repositories'

const { navigateMock, routeParams, searchParams, setSearchParamsMock, workspace } = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    routeParams: { projectId: 'p1', moduleName: 'Bank Rating' },
    // The address a link to one table carries: the module, and the table to draw on it.
    searchParams: new URLSearchParams('table=t-1'),
    setSearchParamsMock: vi.fn(),
    // What the workspace holds of the project: closed until the reader answers the question to open it.
    workspace: { opened: false },
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
        state: workspace.opened ? 'compiled' : 'idle',
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
vi.mock('../components/RawTableGrid', () => ({
    RawTableGrid: ({ testId }: { testId?: string }) => <div data-testid={testId} />,
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
        vi.mocked(getProject).mockImplementation(() =>
            Promise.resolve(project(workspace.opened ? 'OPENED' : 'CLOSED') as never))
        vi.mocked(setProjectStatus).mockImplementation(() => {
            workspace.opened = true
            return Promise.resolve(undefined as never)
        })
        vi.mocked(listModules).mockResolvedValue([{ name: 'Bank Rating', path: 'rules/Bank Rating.xlsx' }])
        vi.mocked(getModuleTables).mockResolvedValue([
            { id: 't-1', name: 'BankRating', kind: 'Rules', tableType: 'SimpleRules' },
        ] as never)
        vi.mocked(getRawTable).mockResolvedValue({
            id: 't-1',
            name: 'BankRating',
            source: [[{ cell: 'A1', value: 'Bank' }]],
        } as never)
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

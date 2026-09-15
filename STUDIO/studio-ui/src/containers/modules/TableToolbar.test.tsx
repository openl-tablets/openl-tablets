import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Modal } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable, SummaryTable, TableRunState } from 'types/tables'
import { getTableTargets, getTableTests } from '../../services/modules'
import { deleteTable } from '../../services/tables'
import { TableToolbar } from './TableToolbar'

vi.mock('../../services/modules', () => ({ getTableTests: vi.fn(), getTableTargets: vi.fn() }))
vi.mock('../../services/tables', () => ({ deleteTable: vi.fn() }))

vi.mock('antd', async importOriginal => {
    const antd = await importOriginal<typeof import('antd')>()
    return { ...antd, Modal: { ...antd.Modal, confirm: vi.fn() } }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

const table = (kind: string, over: Partial<ModuleTable> = {}): ModuleTable =>
    ({ id: 'table-1', name: 'Greeting', kind, ...over } as ModuleTable)

interface DrawProps {
    table: ModuleTable
    projectCompiled?: boolean
    /** Whether the reader may edit the project, which is what the writing actions are offered by. */
    canWrite?: boolean
    /** What the table is as something to run, as the read of it answers; runnable unless a test says otherwise. */
    runState?: TableRunState
    onWritten?: (written: SummaryTable, moduleName: string) => void
    onRemoved?: () => void
}

const toolbar = (props: DrawProps) => (
    <MemoryRouter>
        <TableToolbar
            canWrite={props.canWrite ?? false}
            moduleName="Claims"
            onRemoved={props.onRemoved}
            onWritten={props.onWritten}
            projectCompiled={props.projectCompiled ?? false}
            projectId="p1"
            runState={props.runState ?? 'can-run'}
            table={props.table}
        />
    </MemoryRouter>
)

/** Draws the band and lets the read of what exercises the table settle. */
const draw = async (props: DrawProps) => {
    const view = render(toolbar(props))
    await act(async () => {
        await Promise.resolve()
    })
    return view
}

/** What the band offers, by the ids its buttons carry. */
const offered = () => [...screen.getByTestId('table-toolbar').querySelectorAll('[data-testid^=table-]')]
    .map(node => node.getAttribute('data-testid'))

describe('TableToolbar', () => {
    beforeEach(() => {
        vi.mocked(getTableTests).mockResolvedValue([])
        vi.mocked(getTableTargets).mockResolvedValue([])
    })

    it('asks the panel of an action to open where the button stands', async () => {
        const opened = vi.fn()
        window.addEventListener('openRunLaunch', opened)
        await draw({ table: table('Spreadsheet') })

        await userEvent.click(screen.getByTestId('table-run'))

        window.removeEventListener('openRunLaunch', opened)
        expect(opened).toHaveBeenCalledTimes(1)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        expect(detail).toMatchObject({ projectId: 'p1', tableId: 'table-1', moduleName: 'Claims' })
        // The panel hangs on the button's own rectangle; without it, it opens in the corner of the screen.
        expect(detail.anchor).toEqual(expect.objectContaining({
            top: expect.any(Number),
            left: expect.any(Number),
            width: expect.any(Number),
            height: expect.any(Number),
        }))
    })

    it('stands editing the cells in place, disabled until that phase arrives', async () => {
        await draw({ canWrite: true, table: table('Spreadsheet') })

        expect(screen.getByTestId('table-edit')).toBeDisabled()
        // What is already answered stands beside it, offered rather than promised.
        expect(screen.getByTestId('table-copy')).toBeEnabled()
        expect(screen.getByTestId('table-remove')).toBeEnabled()
        expect(screen.getByTestId('table-createTest')).toBeEnabled()
    })

    it('offers nothing that writes to a reader who may not edit the project', async () => {
        await draw({ table: table('Spreadsheet') })

        // The project says what may be done to it; the band asks nothing of its own.
        expect(offered()).toEqual(['table-run', 'table-trace', 'table-benchmark'])
    })

    it('asks the copy dialog for a copy of the table, where the table is written', async () => {
        const opened = vi.fn()
        window.addEventListener('openCopyTableModal', opened)
        const onWritten = vi.fn()
        await draw({ canWrite: true, onWritten, table: table('Rules') })

        await userEvent.click(screen.getByTestId('table-copy'))

        window.removeEventListener('openCopyTableModal', opened)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        expect(detail).toMatchObject({ projectId: 'p1', currentModuleName: 'Claims', sourceTableId: 'table-1' })
        // What the dialog writes is handed back, so the editor can open it where it landed.
        detail.onSuccess({ id: 'copy-1' }, 'Claims')
        expect(onWritten).toHaveBeenCalledWith({ id: 'copy-1' }, 'Claims')
    })

    it('copies no table that carries no properties of its own', async () => {
        const { unmount } = await draw({ canWrite: true, table: table('Datatype') })
        expect(screen.queryByTestId('table-copy')).toBeNull()
        unmount()

        await draw({ canWrite: true, table: table('Environment') })
        expect(screen.queryByTestId('table-copy')).toBeNull()
    })

    it('writes a test only against a table the rules can call and that answers with something', async () => {
        const { unmount } = await draw({ canWrite: true, table: table('Rules', { returnType: 'Double' }) })
        expect(screen.getByTestId('table-createTest')).toBeEnabled()
        unmount()

        // A table returning nothing gives a test nothing to assert.
        const { unmount: unmountVoid } = await draw({ canWrite: true, table: table('Rules', { returnType: 'void' }) })
        expect(screen.queryByTestId('table-createTest')).toBeNull()
        unmountVoid()

        // A datatype is read, not called; a test table is not tested again.
        const { unmount: unmountDatatype } = await draw({ canWrite: true, table: table('Datatype') })
        expect(screen.queryByTestId('table-createTest')).toBeNull()
        unmountDatatype()

        await draw({ canWrite: true, table: table('Test') })
        expect(screen.queryByTestId('table-createTest')).toBeNull()
    })

    it('asks the create dialog to write a test against the table', async () => {
        const opened = vi.fn()
        window.addEventListener('openCreateTableModal', opened)
        await draw({ canWrite: true, table: table('Rules', { returnType: 'Double' }) })

        await userEvent.click(screen.getByTestId('table-createTest'))

        window.removeEventListener('openCreateTableModal', opened)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        expect(detail).toMatchObject({ projectId: 'p1', currentModuleName: 'Claims', sourceTableId: 'table-1' })
    })

    it('asks before it removes the table, and says so once it is gone', async () => {
        vi.mocked(deleteTable).mockResolvedValue(true)
        const onRemoved = vi.fn()
        await draw({ canWrite: true, onRemoved, table: table('Rules') })

        await userEvent.click(screen.getByTestId('table-remove'))

        expect(deleteTable).not.toHaveBeenCalled()
        const asked = vi.mocked(Modal.confirm).mock.calls[0]?.[0]
        await asked?.onOk?.(() => {})

        expect(deleteTable).toHaveBeenCalledWith('p1', 'table-1', 'Greeting', 'Claims')
        expect(onRemoved).toHaveBeenCalledTimes(1)
    })

    it('leaves the screen where it is when the table could not be removed', async () => {
        vi.mocked(deleteTable).mockResolvedValue(false)
        const onRemoved = vi.fn()
        await draw({ canWrite: true, onRemoved, table: table('Rules') })

        await userEvent.click(screen.getByTestId('table-remove'))
        await vi.mocked(Modal.confirm).mock.calls[0]?.[0]?.onOk?.(() => {})

        expect(onRemoved).not.toHaveBeenCalled()
    })

    it('offers running only what can be run, and testing only what there is a test for', async () => {
        const { unmount } = await draw({ table: table('Datatype') })
        expect(screen.queryByTestId('table-run')).toBeNull()
        expect(screen.queryByTestId('table-tests')).toBeNull()
        unmount()

        // A table nothing covers has nothing to test; a test table always runs its own cases.
        const { unmount: unmountUncovered } = await draw({ table: table('Spreadsheet') })
        expect(screen.getByTestId('table-run')).toBeEnabled()
        expect(screen.queryByTestId('table-tests')).toBeNull()
        unmountUncovered()

        await draw({ table: table('Test') })
        expect(screen.getByTestId('table-run')).toBeEnabled()
        expect(screen.getByTestId('table-tests')).toBeEnabled()
    })

    it('runs the tests written against the table, beside Create Test as the Editor kept them', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{ id: 'test-9', name: 'GreetingTest' }])
        const opened = vi.fn()
        window.addEventListener('openTestsLaunch', opened)
        await draw({ canWrite: true, table: table('Spreadsheet') })

        const actions = offered()
        expect(actions.indexOf('table-tests')).toBe(actions.indexOf('table-createTest') - 1)

        await userEvent.click(screen.getByTestId('table-tests'))

        window.removeEventListener('openTestsLaunch', opened)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        expect(detail).toMatchObject({ projectId: 'p1', tableId: 'table-1', moduleName: 'Claims' })
    })

    it('names what exercises the table and opens it where the tree would', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{
            id: 'test-9',
            name: 'GreetingTest',
            info: '2 test cases',
            module: 'Claims',
            project: 'Pricing',
            projectId: 'p1',
        }])
        await draw({ table: table('Spreadsheet') })

        expect(getTableTests).toHaveBeenCalledWith('p1', 'table-1', 'Claims')
        const test = await screen.findByTestId('table-test-test-9')
        expect(test).toHaveTextContent('GreetingTest (2 test cases)')

        await userEvent.click(test)

        expect(navigate).toHaveBeenCalledWith('/projects/p1/modules/Claims?table=test-9')
    })

    it('opens a test written in another module there, and says which module it is', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{
            id: 'test-7',
            name: 'PremiumTest',
            module: 'ClaimsTests',
            project: 'Pricing',
            projectId: 'p1',
        }])
        await draw({ table: table('Spreadsheet') })

        const test = await screen.findByTestId('table-test-test-7')
        // A test is a table of its own: the reader is told where it is before following it.
        expect(test).toHaveTextContent('PremiumTest — ClaimsTests')

        await userEvent.click(test)

        expect(navigate).toHaveBeenCalledWith('/projects/p1/modules/ClaimsTests?table=test-7')
    })

    it('opens a test written in another project through that project', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{
            id: 'test-5',
            name: 'SharedTest',
            module: 'SharedTests',
            project: 'Shared Rules',
            projectId: 'p9',
        }])
        await draw({ table: table('Spreadsheet') })

        await userEvent.click(await screen.findByTestId('table-test-test-5'))

        expect(navigate).toHaveBeenCalledWith('/projects/p9/modules/SharedTests?table=test-5')
    })

    it('offers no way into a test whose project the session cannot address', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{
            id: 'test-3',
            name: 'ForeignTest',
            module: 'ForeignTests',
            project: 'Closed Rules',
        }])
        await draw({ table: table('Spreadsheet') })

        const test = await screen.findByTestId('table-test-test-3')
        expect(test).toBeDisabled()

        await userEvent.click(test)

        expect(navigate).not.toHaveBeenCalled()
    })

    it('reads what exercises the table again once the project is compiled through', async () => {
        const { rerender } = await draw({ table: table('Spreadsheet') })
        expect(getTableTests).toHaveBeenCalledTimes(1)

        // A test may be written in another module, so the list is only complete once every module is built.
        vi.mocked(getTableTests).mockResolvedValue([{ id: 'test-9', name: 'GreetingTest' }])
        rerender(toolbar({ projectCompiled: true, table: table('Spreadsheet') }))

        await waitFor(() => expect(getTableTests).toHaveBeenCalledTimes(2))
        expect(await screen.findByTestId('table-test-test-9')).toHaveTextContent('GreetingTest')
    })
    it('asks what a test exercises once, and again only while the answer was empty', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([{ id: 'rules-1', name: 'BankRating' }])
        const { rerender } = await draw({ table: table('Test') })
        expect(getTableTargets).toHaveBeenCalledTimes(1)

        // The table is named, and compiling the rest of the project cannot name it differently.
        rerender(toolbar({ projectCompiled: true, table: table('Test') }))
        await waitFor(() => expect(screen.getByTestId('table-target-tables')).toHaveTextContent('BankRating'))
        expect(getTableTargets).toHaveBeenCalledTimes(1)
    })

    it('names the table again when the reader comes back to the test', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([{ id: 'rules-1', name: 'BankRating' }])
        const { rerender } = await draw({ table: table('Test') })
        await waitFor(() => expect(screen.getByTestId('table-target-tables')).toHaveTextContent('BankRating'))

        // A table that exercises nothing clears what the band shows; coming back must not leave it empty.
        rerender(toolbar({ table: { ...table('Rules'), id: 'table-2' } }))
        await waitFor(() => expect(screen.queryByTestId('table-target-tables')).toBeNull())
        rerender(toolbar({ table: table('Test') }))

        expect(await screen.findByTestId('table-target-tables')).toHaveTextContent('BankRating')
        expect(getTableTargets).toHaveBeenCalledTimes(1)
    })

    it('asks again for a target no module had named yet', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([])
        const { rerender } = await draw({ table: table('Test') })
        expect(getTableTargets).toHaveBeenCalledTimes(1)

        // The table a test exercises may be written in a module compiled after this one.
        vi.mocked(getTableTargets).mockResolvedValue([{ id: 'rules-1', name: 'BankRating' }])
        rerender(toolbar({ projectCompiled: true, table: table('Test') }))

        await waitFor(() => expect(getTableTargets).toHaveBeenCalledTimes(2))
    })

    it('names the table a test exercises, and opens it where it is written', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([
            { id: 'rules-1', name: 'BankRating', module: 'Bank', projectId: 'p1' },
        ])
        await draw({ table: table('Test') })

        expect(getTableTargets).toHaveBeenCalledWith('p1', 'table-1', 'Claims')
        expect(screen.getByTestId('table-target-tables')).toHaveTextContent('browser.module.target_table')

        await userEvent.click(screen.getByTestId('table-target-rules-1'))

        // The tested table is opened through the module it is written in, not the one the test sits in.
        expect(navigate).toHaveBeenCalledWith('/projects/p1/modules/Bank?table=rules-1')
    })

    it('names the first table a test exercises and keeps the rest behind a caret', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([
            { id: 'rules-1', name: 'BankRating [lob = Banking]' },
            { id: 'rules-2', name: 'BankRating [lob = Insurance]' },
        ])
        await draw({ table: table('Run') })

        expect(screen.getByTestId('table-target-tables')).toHaveTextContent('browser.module.target_tables')
        expect(screen.getByTestId('table-target-rules-1')).toHaveTextContent('BankRating [lob = Banking]')
        // A table exercised by two dozen others would push the band off the screen, so only one is named.
        expect(screen.queryByTestId('table-target-rules-2')).toBeNull()

        await userEvent.click(screen.getByTestId('table-target-tables-more'))

        expect(await screen.findByText('BankRating [lob = Insurance]')).toBeInTheDocument()
    })

    it('names the only table a test exercises without a caret', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([{ id: 'rules-1', name: 'BankRating' }])
        await draw({ table: table('Run') })

        expect(screen.getByTestId('table-target-rules-1')).toHaveTextContent('BankRating')
        expect(screen.queryByTestId('table-target-tables-more')).toBeNull()
    })

    it('offers no way into a tested table whose project the session cannot address', async () => {
        vi.mocked(getTableTargets).mockResolvedValue([
            { id: 'rules-1', name: 'BankRating', module: 'Bank', project: 'Pricing' },
        ])
        await draw({ table: table('Test') })

        expect(screen.getByTestId('table-target-rules-1')).toBeDisabled()
    })

    it('asks one question per table, and only the one that can be answered', async () => {
        // A table the rules can call is covered by tests; it exercises nothing.
        const { unmount } = await draw({ table: table('Rules') })
        expect(getTableTests).toHaveBeenCalledTimes(1)
        expect(getTableTargets).not.toHaveBeenCalled()
        expect(screen.queryByTestId('table-target-tables')).toBeNull()
        unmount()
        vi.mocked(getTableTests).mockClear()

        // A test exercises a table; nothing covers it.
        const { unmount: unmountTest } = await draw({ table: table('Test') })
        expect(getTableTargets).toHaveBeenCalledTimes(1)
        expect(getTableTests).not.toHaveBeenCalled()
        unmountTest()
        vi.mocked(getTableTargets).mockClear()

        // A datatype is neither, and the server is asked nothing at all.
        await draw({ table: table('Datatype') })
        expect(getTableTests).not.toHaveBeenCalled()
        expect(getTableTargets).not.toHaveBeenCalled()
    })

    it('offers no run of a table the compiler could not build', async () => {
        vi.mocked(getTableTests).mockResolvedValue([{ id: 'test-9', name: 'GreetingTest' }])
        await draw({ runState: 'cannot-run', table: table('Rules') })

        // The old Editor took these off the band rather than letting a run fail on a broken table.
        expect(screen.queryByTestId('table-run')).toBeNull()
        expect(screen.queryByTestId('table-trace')).toBeNull()
        expect(screen.queryByTestId('table-benchmark')).toBeNull()
        expect(screen.queryByTestId('table-tests')).toBeNull()
        // What covers the table is still named: a list of tests is not something to run.
        expect(screen.getByTestId('table-available-tests')).toBeInTheDocument()
    })

    it('keeps a run inside the module while what is built beyond it has errors', async () => {
        const opened = vi.fn()
        window.addEventListener('openRunLaunch', opened)
        await draw({ runState: 'can-run-module', table: table('Spreadsheet') })

        await userEvent.click(screen.getByTestId('table-run'))

        window.removeEventListener('openRunLaunch', opened)
        expect((opened.mock.calls[0]?.[0] as CustomEvent).detail).toMatchObject({ moduleOnlyLocked: true })
    })

})

import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { getTableTests } from '../../services/modules'
import { TableToolbar } from './TableToolbar'

vi.mock('../../services/modules', () => ({ getTableTests: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

const table = (kind: string): ModuleTable => ({ id: 'table-1', name: 'Greeting', kind } as ModuleTable)

const toolbar = (props: { table: ModuleTable, projectCompiled?: boolean }) => (
    <MemoryRouter>
        <TableToolbar
            moduleName="Claims"
            projectCompiled={props.projectCompiled ?? false}
            projectId="p1"
            table={props.table}
        />
    </MemoryRouter>
)

/** Draws the band and lets the read of what exercises the table settle. */
const draw = async (props: { table: ModuleTable, projectCompiled?: boolean }) => {
    const view = render(toolbar(props))
    await act(async () => {
        await Promise.resolve()
    })
    return view
}

describe('TableToolbar', () => {
    beforeEach(() => {
        vi.mocked(getTableTests).mockResolvedValue([])
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

    it('stands the actions of the editing phase in place, disabled until it arrives', async () => {
        await draw({ table: table('Spreadsheet') })

        expect(screen.getByTestId('table-edit')).toBeDisabled()
        expect(screen.getByTestId('table-copy')).toBeDisabled()
        expect(screen.getByTestId('table-remove')).toBeDisabled()
        expect(screen.getByTestId('table-createTest')).toBeDisabled()
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
        await draw({ table: table('Spreadsheet') })

        const band = screen.getByTestId('table-toolbar')
        const actions = [...band.querySelectorAll('[data-testid^=table-]')].map(node => node.getAttribute('data-testid'))
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
})

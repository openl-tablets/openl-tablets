import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { EVERY_CASE, TestCaseSelector, type TestCaseSelectorProps } from 'containers/TableInput/TestCaseSelector'
import type { TableInputTestCase } from 'types/tables'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const testCases: TableInputTestCase[] = [
    { id: '1', description: 'Young', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 25 }, { name: 'car', description: 'Car', lazy: true, type: 'Car', key: 'VIN-1' }]},
    { id: '2', description: 'Senior', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 70 }, { name: 'car', description: 'Car', lazy: false, value: null }]},
]

const loadCase = vi.fn()

const renderSelector = (over: Partial<TestCaseSelectorProps> = {}) => render(
    <TestCaseSelector
        loadCase={loadCase}
        onChange={vi.fn()}
        onPageChange={vi.fn()}
        page={1}
        pageSize={25}
        selection="single"
        tableId="t1"
        testCases={testCases}
        total={2}
        value={['1']}
        {...over}
    />
)

describe('TestCaseSelector', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('lists every case of the page by id, with its values under the name of the column', () => {
        renderSelector()

        expect(screen.getByText('Young')).toBeInTheDocument()
        // The column display name labels the value, and the type behind it is left out.
        expect(screen.getAllByText('Age')).toHaveLength(2)
        expect(screen.getByText('25')).toBeInTheDocument()
        expect(screen.getByText('null')).toBeInTheDocument()
        const radios = screen.getAllByRole('radio')
        expect(radios).toHaveLength(2)
        expect(radios[0]).toBeChecked()
        // Every case fits on one page, so there is nothing to page through.
        expect(screen.queryByText('testCases.total')).toBeNull()
    })

    it('tells a lazy value by its type and data table key before it is read', () => {
        renderSelector()

        // The case is told apart at a glance; the value is still read only on request.
        expect(screen.getByTestId('summary-case-1-1')).toHaveTextContent('Car (VIN-1)')
        expect(screen.getByTestId('load-case-1-1')).toBeInTheDocument()
    })

    it('picks a case by its radio and by a click on the row', async () => {
        const onChange = vi.fn()
        renderSelector({ onChange })

        await userEvent.click(screen.getByTestId('pick-case-2'))
        expect(onChange).toHaveBeenLastCalledWith(['2'])

        await userEvent.click(screen.getByText('Young'))
        expect(onChange).toHaveBeenLastCalledWith(['1'])
    })

    it('picks every case of the table by the box in the header, and clears them the same way', async () => {
        const onChange = vi.fn()
        const { unmount } = renderSelector({ onChange, selection: 'multiple', value: []})

        await userEvent.click(screen.getByTestId('pick-all-cases'))
        expect(onChange).toHaveBeenLastCalledWith(EVERY_CASE)
        unmount()

        renderSelector({ onChange, selection: 'multiple', value: EVERY_CASE })
        // Every case reads as picked, on this page and the others.
        expect(screen.getByTestId('pick-all-cases')).toBeChecked()
        expect(screen.getByTestId('pick-case-1')).toBeChecked()
        expect(screen.getByTestId('pick-case-2')).toBeChecked()
        await userEvent.click(screen.getByTestId('pick-all-cases'))
        expect(onChange).toHaveBeenLastCalledWith([])
    })

    it('leaves one case out of all of them, on this page and the others, and takes it back the same way', async () => {
        const onChange = vi.fn()
        const { unmount } = renderSelector({ onChange, selection: 'multiple', value: EVERY_CASE })

        await userEvent.click(screen.getByTestId('pick-case-2'))
        expect(onChange).toHaveBeenLastCalledWith({ except: ['2']})
        unmount()

        renderSelector({ onChange, selection: 'multiple', value: { except: ['2']} })
        // The rest stay picked, so the box in the header is neither clear nor fully ticked.
        expect(screen.getByTestId('pick-case-1')).toBeChecked()
        expect(screen.getByTestId('pick-case-2')).not.toBeChecked()
        expect(screen.getByTestId('pick-all-cases')).not.toBeChecked()
        expect(screen.getByTestId('pick-all-cases')).toHaveProperty('indeterminate', true)
        await userEvent.click(screen.getByTestId('pick-case-2'))
        expect(onChange).toHaveBeenLastCalledWith(EVERY_CASE)
    })

    it('reads as no case at all once every case is left out one by one', () => {
        renderSelector({ selection: 'multiple', value: { except: ['1', '2']} })

        expect(screen.getByTestId('pick-all-cases')).not.toBeChecked()
        expect(screen.getByTestId('pick-all-cases')).toHaveProperty('indeterminate', false)
    })

    it('does not pick a case when a value of it is read or opened', async () => {
        const onChange = vi.fn()
        loadCase.mockResolvedValue({ id: '1', parameters: [{ name: 'car', description: 'Car', lazy: false, value: { vin: 'X' } }]})
        // The second case is the one picked, so a leaked click would show as a pick of the first.
        renderSelector({ onChange, value: ['2']})

        await userEvent.click(screen.getByTestId('load-case-1-1'))

        await waitFor(() => expect(loadCase).toHaveBeenCalledWith('t1', '1'))
        expect(onChange).not.toHaveBeenCalled()
    })

    it('reads on request the value asked for, and only that one', async () => {
        loadCase.mockResolvedValue({
            id: '1',
            parameters: [
                { name: 'car', description: 'Car', lazy: false, value: { vin: 'X' } },
                { name: 'policy', description: 'Policy', lazy: false, value: { id: 7 } },
            ],
        })
        renderSelector({
            testCases: [{ id: '1', parameters: [
                { name: 'car', description: 'Car', lazy: true },
                { name: 'policy', description: 'Policy', lazy: true },
            ]}],
        })

        await userEvent.click(screen.getByTestId('load-case-1-1'))

        expect(loadCase).toHaveBeenCalledWith('t1', '1')
        await waitFor(() => expect(screen.getByText('{1 fields}')).toBeInTheDocument())
        // The other value of the same case waits until it is asked for.
        expect(screen.getByTestId('load-case-1-0')).toBeInTheDocument()

        // Asking for it reads nothing again: the case is already at hand.
        await userEvent.click(screen.getByTestId('load-case-1-0'))
        await waitFor(() => expect(screen.getAllByText('{1 fields}')).toHaveLength(2))
        expect(loadCase).toHaveBeenCalledTimes(1)
    })

    it('says why a value could not be read and offers to try again', async () => {
        loadCase.mockRejectedValueOnce(new Error('the project is being compiled'))
        loadCase.mockResolvedValue({ id: '1', parameters: [
            { name: 'age', description: 'Age', lazy: false, value: 25 },
            { name: 'car', description: 'Car', lazy: false, value: { vin: 'X' } },
        ]})
        renderSelector()

        await userEvent.click(screen.getByTestId('load-case-1-1'))
        expect(await screen.findByText('the project is being compiled')).toBeInTheDocument()

        // The link stays, so a case that failed once while the project compiled can be read again. The value
        // read keeps the key it is known by as its title, in place of a count of its fields.
        await userEvent.click(screen.getByTestId('load-case-1-1'))
        await waitFor(() => expect(screen.queryByTestId('load-case-1-1')).toBeNull())
        expect(screen.getByText('Car (VIN-1)')).toBeInTheDocument()
        expect(screen.queryByText('{1 fields}')).toBeNull()
        expect(loadCase).toHaveBeenCalledTimes(2)
    })

    it('pages through a table that holds more cases than the page shows', async () => {
        const onPageChange = vi.fn()
        renderSelector({ onPageChange, total: 120 })

        expect(screen.getByText('testCases.total')).toBeInTheDocument()
        await userEvent.click(screen.getByTitle('2'))

        expect(onPageChange).toHaveBeenCalledWith(2, expect.anything())
    })
})

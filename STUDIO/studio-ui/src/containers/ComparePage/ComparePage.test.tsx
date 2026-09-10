import React from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ComparePage } from './ComparePage'
import { dropComparison, getComparison, getComparisonTable, startFileComparison } from 'services/compare'
import type { Comparison, ComparisonTable } from 'types/compare'

class HttpError extends Error {
    status: number

    constructor(status: number) {
        super('http ' + status)
        this.status = status
    }
}

vi.mock('services', () => ({
    isApiHttpError: (error: unknown) => error instanceof HttpError,
}))

vi.mock('services/compare', () => ({
    comparisonStatusTopic: (id: string) => `/user/topic/compare/${id}/status`,
    startFileComparison: vi.fn(),
    getComparison: vi.fn(),
    getComparisonTable: vi.fn(),
    dropComparison: vi.fn(),
}))

const unsubscribe = vi.fn()
const subscribe = vi.fn((_destination: string, _onBody: (body: string) => void) => ({ unsubscribe }))
vi.mock('services/stompTopic', () => ({
    subscribeTopic: (destination: string, onBody: (body: string) => void) => subscribe(destination, onBody),
}))

// Whether the socket is up; the screen hears a comparison only over a connection that is.
let connected = true
vi.mock('hooks/useWebSocket', () => ({
    useWebSocket: () => ({ isConnected: connected }),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// Ant Design's Tree and Upload need layout jsdom does not run; the test is about the comparison flow,
// so they stand in as the plain elements they are.
vi.mock('antd', () => ({
    Alert: ({ message, showIcon, type, ...rest }: any) => <div data-type={type} role="alert" {...rest}>{message}</div>,
    Button: ({ children, onClick, disabled, loading, icon, ...rest }: any) => (
        <button disabled={disabled || loading} onClick={onClick} {...rest}>{icon}{children}</button>
    ),
    Checkbox: ({ children, checked, onChange, ...rest }: any) => (
        <label>
            <input
                checked={checked}
                onChange={event => onChange?.({ target: { checked: event.target.checked } })}
                type="checkbox"
                {...rest}
            />
            {children}
        </label>
    ),
    Empty: Object.assign(({ description }: any) => <div>{description}</div>, { PRESENTED_IMAGE_SIMPLE: 'simple' }),
    Spin: ({ description }: any) => <div role="status">{description}</div>,
    Splitter: Object.assign(({ children }: any) => <div>{children}</div>, {
        Panel: ({ children }: any) => <div>{children}</div>,
    }),
    Tree: ({ treeData, onSelect }: any) => (
        <ul>
            {treeData.map((sheet: any) => (
                <li key={sheet.key}>
                    {sheet.title}
                    <ul>
                        {sheet.children.map((table: any) => (
                            <li key={table.key}>
                                <button onClick={() => onSelect([table.key])}>{table.title}</button>
                            </li>
                        ))}
                    </ul>
                </li>
            ))}
        </ul>
    ),
    Upload: Object.assign(() => null, {
        Dragger: ({ children, beforeUpload, ...rest }: any) => (
            <div>
                <input
                    multiple
                    data-testid={rest['data-testid']}
                    type="file"
                    onChange={event => {
                        const batch = Array.from(event.target.files ?? [])
                        batch.forEach(file => beforeUpload(file, batch))
                    }}
                />
                {children}
            </div>
        ),
    }),
}))

const COMPARISON: Comparison = {
    id: 'cmp-1',
    identical: false,
    sheets: [{
        id: '0',
        name: 'Rules',
        type: 'sheet',
        status: 'changed',
        children: [
            { id: '0-0', name: 'Datatype Person', type: 'table', status: 'changed' },
            { id: '0-1', name: 'Datatype Address', type: 'table', status: 'equal' },
        ],
    }],
}

const TABLE: ComparisonTable = {
    id: '0-0',
    name: 'Datatype Person',
    status: 'changed',
    first: {
        source: [[{ cell: 'A1', value: 'int' }], [{ cell: 'A2', value: 'name' }]],
        changedCells: ['A1'],
    },
    second: {
        source: [[{ cell: 'A1', value: 'double' }], [{ cell: 'A2', value: 'name' }]],
        changedCells: ['A1'],
    },
}

/** Pushes a status message onto the topic the screen is watching. */
const push = (body: string) => {
    const onBody = subscribe.mock.calls.at(-1)?.[1]
    act(() => onBody?.(body))
}

const pickFiles = () => {
    fireEvent.change(screen.getByTestId('compare-files'), {
        target: { files: [new File(['a'], 'first.xlsx'), new File(['b'], 'second.xlsx')]},
    })
}

/** Picks both files and starts the comparison, which moves the screen to its second step. */
const startComparison = async () => {
    pickFiles()
    await waitFor(() => expect(screen.getByTestId('compare-start')).not.toBeDisabled())
    await userEvent.click(screen.getByTestId('compare-start'))
    await waitFor(() => expect(startFileComparison).toHaveBeenCalled())
}

const openPage = async () => {
    let page!: ReturnType<typeof render>
    await act(async () => {
        page = render(<ComparePage />)
    })
    return page
}

describe('ComparePage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        connected = true
        vi.mocked(startFileComparison).mockResolvedValue('cmp-1')
        vi.mocked(getComparison).mockResolvedValue(COMPARISON)
        vi.mocked(getComparisonTable).mockResolvedValue(TABLE)
        vi.mocked(dropComparison).mockResolvedValue()
    })

    it('asks for the two files before it compares anything', async () => {
        await openPage()

        expect(screen.getByTestId('compare-files')).toBeInTheDocument()
        expect(screen.getByTestId('compare-start')).toBeDisabled()
        expect(screen.getByTestId('compare-file-list')).toBeEmptyDOMElement()
        // The option that decides what the result lists is asked for here; the one that decides what a
        // table shows belongs to the result.
        expect(screen.getByTestId('compare-show-equal-elements')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-show-equal-rows')).toBeNull()
    })

    it('compares the two picked files and shows what they hold', async () => {
        await openPage()

        await startComparison()

        expect(subscribe).toHaveBeenCalledWith('/user/topic/compare/cmp-1/status', expect.any(Function))
        // The files are behind now; the comparison has the screen.
        expect(screen.queryByTestId('compare-files')).toBeNull()

        push('COMPLETED')
        await waitFor(() => expect(getComparison).toHaveBeenCalledWith('cmp-1'))
        expect(await screen.findByText('Rules')).toBeInTheDocument()
        expect(screen.getByTestId('compare-show-equal-rows')).toBeInTheDocument()
    })

    it('asks for the result again once it is listening, in case the comparison ended before', async () => {
        // The connection comes up after the comparison was started, so its COMPLETED is never heard;
        // the first ask, made while it was still running, was answered with 409.
        connected = false
        vi.mocked(getComparison).mockRejectedValueOnce(new HttpError(409))
        const page = await openPage()
        await startComparison()

        expect(subscribe).not.toHaveBeenCalled()
        expect(screen.queryByText('Rules')).toBeNull()

        connected = true
        await act(async () => {
            page.rerender(<ComparePage />)
        })

        expect(await screen.findByText('Rules')).toBeInTheDocument()
    })

    it('asks for the result again while it cannot hear the topic at all', async () => {
        // The connection never opens, so COMPLETED is never heard and the first ask meets a comparison
        // that is still running.
        connected = false
        vi.mocked(getComparison).mockRejectedValueOnce(new HttpError(409))
        await openPage()
        await startComparison()

        expect(screen.queryByText('Rules')).toBeNull()

        // Nothing will say when it ends, so the screen asks again on its own.
        expect(await screen.findByText('Rules', undefined, { timeout: 5000 })).toBeInTheDocument()
        expect(getComparison).toHaveBeenCalledTimes(2)
    })

    it('lists only the elements that differ, and the equal ones when they were asked for', async () => {
        await openPage()
        await startComparison()
        push('COMPLETED')

        expect(await screen.findByText('Datatype Person')).toBeInTheDocument()
        expect(screen.queryByText('Datatype Address')).toBeNull()

        await userEvent.click(screen.getByTestId('compare-back'))
        // The files that were compared are gone, so the pair that is picked now is the pair compared.
        expect(screen.getByTestId('compare-file-list')).toBeEmptyDOMElement()
        await userEvent.click(screen.getByTestId('compare-show-equal-elements'))
        await startComparison()
        push('COMPLETED')

        expect(await screen.findByText('Datatype Address')).toBeInTheDocument()
    })

    it('goes back to the files and lets the comparison go', async () => {
        await openPage()
        await startComparison()
        push('COMPLETED')
        await screen.findByText('Rules')

        await userEvent.click(screen.getByTestId('compare-back'))

        expect(dropComparison).toHaveBeenCalledWith('cmp-1')
        expect(screen.getByTestId('compare-files')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-tree')).toBeNull()
    })

    it('reads the two sides of the element that is picked', async () => {
        await openPage()
        await startComparison()
        push('COMPLETED')

        await userEvent.click(await screen.findByText('Datatype Person'))

        await waitFor(() => expect(getComparisonTable).toHaveBeenCalledWith('cmp-1', '0-0'))
        const first = await screen.findByTestId('compare-pane-first')
        // Only the row that differs is shown until the equal rows are asked for.
        expect(first).toHaveTextContent('int')
        expect(first).not.toHaveTextContent('name')

        await userEvent.click(screen.getByTestId('compare-show-equal-rows'))
        await waitFor(() => expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('name'))
    })

    it('reads a comparison that was over before the screen was listening', async () => {
        await openPage()
        await startComparison()

        // No status arrives: the comparison had finished by the time the screen subscribed.
        expect(await screen.findByText('Rules')).toBeInTheDocument()
        expect(getComparison).toHaveBeenCalledWith('cmp-1')
    })

    it('waits for the comparison that is still running', async () => {
        // A comparison that has not finished answers 409, however often it is asked.
        vi.mocked(getComparison).mockRejectedValue(new HttpError(409))
        await openPage()
        await startComparison()
        await waitFor(() => expect(getComparison).toHaveBeenCalledWith('cmp-1'))
        expect(screen.queryByTestId('compare-error')).toBeNull()
        expect(screen.queryByText('Rules')).toBeNull()

        vi.mocked(getComparison).mockResolvedValue(COMPARISON)
        push('COMPLETED')

        expect(await screen.findByText('Rules')).toBeInTheDocument()
    })

    it('says why the files could not be compared', async () => {
        // While the comparison runs it has no result to give; then it says it could not be made.
        vi.mocked(getComparison).mockRejectedValue(new HttpError(409))
        await openPage()
        await startComparison()

        push('{"status":"ERROR","message":"Cannot read the file"}')

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('Cannot read the file')
        // The reason takes the place the elements would have been listed in.
        expect(screen.queryByText('Rules')).toBeNull()
    })

    it('says why the comparison could not be started, without leaving the files', async () => {
        vi.mocked(startFileComparison).mockRejectedValueOnce(new Error('Only Excel files can be compared.'))
        await openPage()

        pickFiles()
        await waitFor(() => expect(screen.getByTestId('compare-start')).not.toBeDisabled())
        await userEvent.click(screen.getByTestId('compare-start'))

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('Only Excel files can be compared.')
        expect(screen.getByTestId('compare-files')).toBeInTheDocument()
    })

    it('lists the picked files and lets one of them go', async () => {
        await openPage()

        pickFiles()

        expect(await screen.findByText('first.xlsx')).toBeInTheDocument()
        expect(screen.getByText('second.xlsx')).toBeInTheDocument()

        await userEvent.click(screen.getAllByTestId('compare-file-clear')[0]!)

        expect(screen.queryByText('first.xlsx')).toBeNull()
        expect(screen.getByText('second.xlsx')).toBeInTheDocument()
        expect(screen.getByTestId('compare-start')).toBeDisabled()
    })

    it('hides the list of elements and brings it back', async () => {
        await openPage()
        await startComparison()
        push('COMPLETED')
        await screen.findByText('Rules')

        await userEvent.click(screen.getByTestId('compare-tree-hide'))

        // The control that brings the list back heads the first file, so it is never out of reach,
        // and the way back to the files goes with it rather than hiding with the list.
        expect(await screen.findByTestId('compare-tree-show')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-tree-hide')).toBeNull()
        expect(screen.getByTestId('compare-back')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('compare-tree-show'))

        expect(await screen.findByTestId('compare-tree-hide')).toBeInTheDocument()
        expect(screen.getByText('Rules')).toBeInTheDocument()
    })

    it('releases the comparison when the window is closed', async () => {
        await openPage()
        await startComparison()

        act(() => {
            window.dispatchEvent(new Event('pagehide'))
        })

        expect(dropComparison).toHaveBeenCalledWith('cmp-1')
    })
})

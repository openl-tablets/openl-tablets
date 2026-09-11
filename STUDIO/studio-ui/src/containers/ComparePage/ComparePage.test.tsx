import React from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ComparePage } from './ComparePage'
import {
    dropComparison,
    getComparison,
    getComparisonTable,
    getConflictFileStatus,
    startConflictComparison,
    startFileComparison,
    startLocalHistoryComparison,
    startProjectComparison,
} from 'services/compare'
import type { Comparison, ComparisonTable } from 'types/compare'

class HttpError extends Error {
    status: number

    payload: unknown

    constructor(status: number, payload?: unknown) {
        super('http ' + status)
        this.status = status
        this.payload = payload
    }
}

/** The refusal the server answers with while the comparison is still being made. */
const stillRunning = () => new HttpError(409, { code: 'openl.error.409.compare.not-completed.message' })

vi.mock('services', () => ({
    isApiHttpError: (error: unknown) => error instanceof HttpError,
}))

vi.mock('services/compare', () => ({
    comparisonStatusTopic: (id: string) => `/user/topic/compare/${id}/status`,
    startConflictComparison: vi.fn(),
    startFileComparison: vi.fn(),
    startLocalHistoryComparison: vi.fn(),
    startProjectComparison: vi.fn(),
    getComparison: vi.fn(),
    getComparisonTable: vi.fn(),
    getConflictFileStatus: vi.fn(),
    dropComparison: vi.fn(),
}))

const unsubscribe = vi.fn()
const subscribe = vi.fn((_destination: string, _onBody: (body: string) => void) => ({ unsubscribe }))
vi.mock('services/stompTopic', () => ({
    subscribeTopic: (destination: string, onBody: (body: string) => void) => subscribe(destination, onBody),
}))

const SIDES = {
    first: { path: 'rules/Main.xlsx' },
    second: { path: 'rules/Main.xlsx', branch: 'master', revision: 'rev-1' },
}

// A file that is not a workbook is read and drawn by a view of its own; the page is asked here only
// whether it opens on that view instead of on a comparison.
vi.mock('./ConflictTextView', () => ({
    ConflictTextView: ({ path, projectId }: any) => (
        <div data-testid="conflict-text">{`${projectId}:${path}`}</div>
    ),
}))

// The picker of a project reads branches, revisions and files of its own; the page is asked here only
// about what it does with the pair it is told about.
vi.mock('./RevisionPicker', () => ({
    RevisionPicker: ({ onChange, projectId }: any) => (
        <button data-testid="revision-picker" onClick={() => onChange(SIDES)}>{projectId}</button>
    ),
}))

// What the window was opened with; a window opened for two versions carries them in the address.
let searchParams = new URLSearchParams()
vi.mock('react-router-dom', () => ({
    useSearchParams: () => [searchParams, vi.fn()],
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
    Alert: ({ title, showIcon, type, ...rest }: any) => <div data-type={type} role="alert" {...rest}>{title}</div>,
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
    Segmented: ({ options, value, onChange, size, ...rest }: any) => (
        <div {...rest}>
            {options.map((option: any) => (
                <button
                    key={option.value}
                    aria-pressed={option.value === value}
                    onClick={() => onChange?.(option.value)}
                    type="button"
                >
                    {option.label}
                </button>
            ))}
        </div>
    ),
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
        searchParams = new URLSearchParams()
        vi.mocked(startFileComparison).mockResolvedValue('cmp-1')
        vi.mocked(getComparison).mockResolvedValue(COMPARISON)
        vi.mocked(getComparisonTable).mockResolvedValue(TABLE)
        vi.mocked(dropComparison).mockResolvedValue()
        vi.mocked(getConflictFileStatus).mockResolvedValue('modified')
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
        vi.mocked(getComparison).mockRejectedValueOnce(stillRunning())
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

    it('says that a comparison stopped before it found anything is over', async () => {
        // Both a comparison still being made and one that was stopped are refused the same way; only
        // what the refusal says tells them apart.
        connected = false
        vi.mocked(getComparison).mockRejectedValue(
            new HttpError(409, { code: 'openl.error.409.compare.interrupted.message' })
        )

        await openPage()
        await startComparison()

        expect(await screen.findByTestId('compare-error')).toBeInTheDocument()
        expect(getComparison).toHaveBeenCalledTimes(1)
    })

    it('asks for the result again while it cannot hear the topic at all', async () => {
        // The connection never opens, so COMPLETED is never heard and the first ask meets a comparison
        // that is still running.
        connected = false
        vi.mocked(getComparison).mockRejectedValueOnce(stillRunning())
        await openPage()
        await startComparison()

        expect(screen.queryByText('Rules')).toBeNull()

        // Nothing will say when it ends, so the screen asks again on its own.
        expect(await screen.findByText('Rules', undefined, { timeout: 5000 })).toBeInTheDocument()
        expect(getComparison).toHaveBeenCalledTimes(2)
    })

    it('compares the two versions the window was opened for, with no files to pick', async () => {
        searchParams = new URLSearchParams({
            projectId: 'p1',
            module: 'Pricing',
            first: '100',
            second: '200_current',
        })
        vi.mocked(startLocalHistoryComparison).mockResolvedValue('cmp-1')

        await openPage()

        await waitFor(() => expect(startLocalHistoryComparison)
            .toHaveBeenCalledWith('p1', 'Pricing', '100', '200_current'))
        expect(screen.queryByTestId('compare-files')).toBeNull()

        await screen.findByTestId('compare-tree')
        push('COMPLETED')

        expect(await screen.findByText('Rules')).toBeInTheDocument()
        // There are no files this window could go back to.
        expect(screen.queryByTestId('compare-back')).toBeNull()
    })

    it('says why the comparison of two versions could not be started', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', first: '100', second: '200_current' })
        vi.mocked(startLocalHistoryComparison).mockRejectedValue(new Error('The version is not found'))

        await openPage()

        expect(await screen.findByTestId('compare-error')).toHaveTextContent('The version is not found')
        expect(screen.queryByTestId('compare-files')).toBeNull()
    })

    it('compares two files of the project the window was opened for', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1' })
        vi.mocked(startProjectComparison).mockResolvedValue('cmp-1')

        await openPage()

        // The files are picked out of the project rather than uploaded.
        expect(screen.getByTestId('revision-picker')).toHaveTextContent('p1')
        expect(screen.queryByTestId('compare-files')).toBeNull()
        expect(screen.getByTestId('compare-start')).toBeDisabled()

        await userEvent.click(screen.getByTestId('revision-picker'))
        expect(screen.getByTestId('compare-start')).toBeEnabled()
        await userEvent.click(screen.getByTestId('compare-start'))

        await waitFor(() => expect(startProjectComparison)
            .toHaveBeenCalledWith('p1', SIDES.first, SIDES.second))
        push('COMPLETED')

        expect(await screen.findByText('Rules')).toBeInTheDocument()
        // The pickers are a click away, so another pair is compared in the same window.
        expect(screen.getByTestId('compare-back')).toBeInTheDocument()
    })

    it('compares the two versions of a conflicted workbook', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'rules/Main.xlsx' })
        vi.mocked(startConflictComparison).mockResolvedValue('cmp-1')

        await openPage()

        await waitFor(() => expect(startConflictComparison).toHaveBeenCalledWith('p1', 'rules/Main.xlsx'))
        expect(screen.queryByTestId('compare-files')).toBeNull()

        await screen.findByTestId('compare-tree')
        push('COMPLETED')

        expect(await screen.findByText('Rules')).toBeInTheDocument()
        // There is nothing to pick here, so there is nowhere to go back to.
        expect(screen.queryByTestId('compare-back')).toBeNull()
        // The two sides are the two versions of the merge, not a first and a second file.
        expect(screen.getByText('their_version')).toBeInTheDocument()
        expect(screen.getByText('your_version')).toBeInTheDocument()
    })

    it('names the conflicted file and what the merge did to it', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'Project/rules/Main.xlsx' })
        vi.mocked(startConflictComparison).mockResolvedValue('cmp-1')

        await openPage()

        // The window is opened away from the screen that asked for it, so it says which file this is.
        const head = await screen.findByTestId('compare-conflict-head')
        expect(head).toHaveTextContent('file_name: Main.xlsx')
        expect(head).toHaveTextContent('file_status: status_modified')
    })

    it('says that one of the versions no longer holds the file', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'Project/rules/Main.xlsx' })
        vi.mocked(getConflictFileStatus).mockResolvedValue('deleted')

        await openPage()

        expect(await screen.findByTestId('compare-conflict-deleted')).toBeInTheDocument()
        expect(screen.getByTestId('compare-conflict-head')).toHaveTextContent('file_status: status_deleted')
        // There is nothing to put the surviving version against, so nothing is compared.
        expect(startConflictComparison).not.toHaveBeenCalled()
    })

    it('reads a conflicted file that is not a workbook line by line', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'rules/notes.txt' })

        await openPage()

        expect(screen.getByTestId('conflict-text')).toHaveTextContent('p1:rules/notes.txt')
        // A file of that kind is never handed to the comparison of workbooks.
        expect(startConflictComparison).not.toHaveBeenCalled()
    })

    it('does not read a file line by line when one of the versions no longer holds it', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'rules/notes.txt' })
        vi.mocked(getConflictFileStatus).mockResolvedValue('deleted')

        await openPage()

        expect(await screen.findByTestId('compare-conflict-deleted')).toBeInTheDocument()
        expect(screen.queryByTestId('conflict-text')).not.toBeInTheDocument()
    })

    it('offers the equal elements in a window that has no files to pick', async () => {
        searchParams = new URLSearchParams({ projectId: 'p1', conflict: 'rules/Main.xlsx' })
        vi.mocked(startConflictComparison).mockResolvedValue('cmp-1')

        await openPage()
        push('COMPLETED')

        // The control heads the list of elements, because the files it usually stands next to are not here.
        const equal = await screen.findByTestId('compare-show-equal-elements')
        expect(screen.queryByText('Datatype Address')).toBeNull()
        await userEvent.click(equal)
        expect(screen.getByText('Datatype Address')).toBeInTheDocument()
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

    it('reads the two versions side by side, and as one table when that is asked for', async () => {
        await openPage()
        await startComparison()
        push('COMPLETED')
        await screen.findByText('Rules')

        // Side by side is how a comparison reads unless the reader says otherwise.
        expect(screen.getByTestId('compare-pane-first')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-pane-combined')).toBeNull()

        await userEvent.click(screen.getByText('view_combined'))

        expect(screen.getByTestId('compare-pane-combined')).toBeInTheDocument()
        expect(screen.queryByTestId('compare-pane-first')).toBeNull()
    })

    it('lets the comparison go when the window is left, and only once', async () => {
        const page = await openPage()
        await startComparison()
        push('COMPLETED')
        await screen.findByText('Rules')

        page.unmount()

        // The window may be closed or left for another page; the comparison is put down either way.
        await waitFor(() => expect(dropComparison).toHaveBeenCalledWith('cmp-1'))
        act(() => {
            window.dispatchEvent(new Event('pagehide'))
        })
        expect(dropComparison).toHaveBeenCalledTimes(1)
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
        vi.mocked(getComparison).mockRejectedValue(stillRunning())
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
        vi.mocked(getComparison).mockRejectedValue(stillRunning())
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

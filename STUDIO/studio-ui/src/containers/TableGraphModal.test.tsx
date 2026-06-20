import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TableGraphModal } from 'containers/TableGraphModal'
import * as services from 'services'
import cytoscape from 'cytoscape'
import type { MockedFunction } from 'vitest'

const cyMocks = vi.hoisted(() => {
    const nodeAddClass = vi.fn()
    const nodeRemoveClass = vi.fn()
    const chain = { removeClass: vi.fn(), addClass: vi.fn(), not: vi.fn() }
    chain.removeClass.mockReturnValue(chain)
    chain.addClass.mockReturnValue(chain)
    chain.not.mockReturnValue(chain)
    const node = { empty: () => false, hasClass: () => false, addClass: nodeAddClass, removeClass: nodeRemoveClass, closedNeighborhood: () => ({}) }
    nodeRemoveClass.mockReturnValue(node)
    return {
        nodeAddClass,
        getElementById: vi.fn(() => node),
        on: vi.fn(),
        destroy: vi.fn(),
        animate: vi.fn(),
        batch: vi.fn((fn: () => void) => fn()),
        zoom: vi.fn(() => 1),
        width: vi.fn(() => 800),
        height: vi.fn(() => 600),
        fit: vi.fn(),
        layout: vi.fn(() => ({ run: vi.fn() })),
        nodes: vi.fn(() => ({ forEach: vi.fn() })),
        elements: vi.fn(() => chain),
        add: vi.fn(),
        remove: vi.fn(),
        resize: vi.fn(),
    }
})

vi.mock('services', () => ({ apiCall: vi.fn() }))

vi.mock('cytoscape-dagre', () => ({ default: vi.fn() }))

vi.mock('cytoscape', () => {
    const cy = Object.assign(
        vi.fn(() => ({
            on: cyMocks.on,
            destroy: cyMocks.destroy,
            animate: cyMocks.animate,
            batch: cyMocks.batch,
            zoom: cyMocks.zoom,
            width: cyMocks.width,
            height: cyMocks.height,
            fit: cyMocks.fit,
            layout: cyMocks.layout,
            nodes: cyMocks.nodes,
            elements: cyMocks.elements,
            getElementById: cyMocks.getElementById,
            add: cyMocks.add,
            remove: cyMocks.remove,
            resize: cyMocks.resize,
        })),
        { use: vi.fn() }
    )
    return { default: cy }
})

// AntD's Modal leave-animation never ends in jsdom; replace it with an `open`-gated wrapper. Select is replaced with
// a native <select> so option selection is straightforward in jsdom.
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({
        open,
        title,
        children,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
    }) => (open ? (
        <div role="dialog">
            {title}
            {children}
        </div>
    ) : null)
    const MockSelect = (props: {
        options?: Array<{ label: string, value: string }>
        onChange?: (value?: string) => void
        'data-testid'?: string
    }) => (
        <select data-testid={props['data-testid']} onChange={event => props.onChange?.(event.target.value || undefined)}>
            <option value="" />
            {(props.options ?? []).map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    return { ...actual, Modal: MockModal, Select: MockSelect }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>
const mockCytoscape = cytoscape as unknown as MockedFunction<(options: { elements: Array<{ data: { id: string } }> }) => unknown>

const dispatchOpen = async (detail: { projectId: string, projectName?: string, module?: string } | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openTableGraphModal', { detail }))
    })
}

describe('TableGraphModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        window.location.hash = ''
    })

    it('does not render without an event', () => {
        render(<TableGraphModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('loads the project graph and renders it with cytoscape', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'a', name: 'A' },
            { id: 'b', name: 'B', dependencies: ['a']},
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })

        await waitFor(() => expect(screen.getByTestId('table-graph')).toBeInTheDocument())
        expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/tables/graph', { method: 'GET' }, expect.anything())

        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())
        const options = mockCytoscape.mock.calls[0]?.[0]
        const ids = (options?.elements ?? []).map(element => element.data.id)
        expect(ids).toEqual(expect.arrayContaining(['a', 'b', 'b->a']))
    })

    it('scopes the graph to the opened module when one is given', async () => {
        mockApiCall.mockResolvedValueOnce([{ id: 'a', name: 'A' }] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1', module: 'My Module' })

        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())
        expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/tables/graph?module=My%20Module', { method: 'GET' }, expect.anything())
    })

    it('rebuilds the whole-project graph when the scope is switched', async () => {
        mockApiCall.mockResolvedValueOnce([{ id: 'a', name: 'A' }] as never)
        mockApiCall.mockResolvedValueOnce([{ id: 'a', name: 'A' }, { id: 'b', name: 'B' }] as never)

        render(<TableGraphModal />)
        // launched from a module → first request is module-scoped
        await dispatchOpen({ projectId: 'proj-1', module: 'M1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())
        expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/tables/graph?module=M1', { method: 'GET' }, expect.anything())

        await userEvent.click(screen.getByText('graph:scope_project'))

        // switching scope reloads the whole project
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/tables/graph', { method: 'GET' }, expect.anything()))
    })

    it('highlights and centers the table picked from the search box', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'a', name: 'Alpha' },
            { id: 'b', name: 'Beta', dependencies: ['a']},
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'Beta')

        expect(cyMocks.getElementById).toHaveBeenCalledWith('b')
        expect(cyMocks.nodeAddClass).toHaveBeenCalledWith('highlighted')
        expect(cyMocks.animate).toHaveBeenCalled()
    })

    it('offers the candidates when a searched name maps to several tables and focuses the picked one', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'x1', name: 'Shared', file: 'rules/A.xlsx', pos: 'B2:C3' },
            { id: 'x2', name: 'Shared', file: 'rules/B.xlsx', pos: 'D4:E5' },
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'Shared')

        // both tables are offered as candidates; the ambiguous name alone focuses none of them
        const matches = await screen.findByTestId('table-graph-matches')
        expect(matches).toHaveTextContent('rules/A.xlsx · B2:C3')
        expect(matches).toHaveTextContent('rules/B.xlsx · D4:E5')
        expect(cyMocks.getElementById).not.toHaveBeenCalledWith('x1')

        await userEvent.click(screen.getByTestId('table-graph-match-1'))

        expect(cyMocks.getElementById).toHaveBeenCalledWith('x2')
        expect(cyMocks.nodeAddClass).toHaveBeenCalledWith('highlighted')
    })

    it('treats a dispatcher as technical: no editor link, offers version paths instead', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'd', name: 'mySPR(int)', kind: 'Dispatcher', dependencies: ['a', 'b']},
            { id: 'a', name: 'AR' },
            { id: 'b', name: 'AZ' },
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'mySPR(int)')

        expect(screen.queryByText('graph:panel.open')).not.toBeInTheDocument()
        expect(screen.getByText('graph:panel.dispatcher_hint')).toBeInTheDocument()
        expect(screen.getByText('graph:panel.highlight_path')).toBeInTheDocument()
    })

    it('finds call cycles and highlights the one picked from the list', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'a', name: 'A', dependencies: ['b']},
            { id: 'b', name: 'B', dependencies: ['c']},
            { id: 'c', name: 'C', dependencies: ['a']},
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('table-graph-find-cycles'))

        const cycle = await screen.findByTestId('table-graph-cycle-0')
        expect(cycle).toHaveTextContent('A → B → C → A')

        await userEvent.click(cycle)
        expect(cyMocks.nodeAddClass).toHaveBeenCalledWith('highlighted')
    })

    it('shows summary meta for the selected table', async () => {
        mockApiCall.mockResolvedValueOnce([
            {
                id: 'a',
                name: 'A',
                signature: 'foo(int n)',
                returnType: 'String',
                file: 'rules/Main.xlsx',
                pos: 'B2:C4',
                properties: { state: 'AZ' },
                dimensionProperties: { State: 'AR' },
            },
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'A')

        expect(screen.getByText('foo(int n)')).toBeInTheDocument()
        expect(screen.getByText('rules/Main.xlsx')).toBeInTheDocument()
        expect(screen.getByText('state: AZ')).toBeInTheDocument()
        expect(screen.getByText('State: AR')).toBeInTheDocument()
    })

    it('preselects the table open in the editor from the URL fragment', async () => {
        window.location.hash = '#repo/proj/module/table?id=a'
        mockApiCall.mockResolvedValueOnce([
            { id: 'a', name: 'Alpha' },
            { id: 'b', name: 'Beta', dependencies: ['a']},
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        // the node named in the fragment becomes active without any user interaction
        await waitFor(() => expect(cyMocks.getElementById).toHaveBeenCalledWith('a'))
        expect(cyMocks.nodeAddClass).toHaveBeenCalledWith('highlighted')
    })

    it('opens a table in the editor and closes the graph', async () => {
        mockApiCall.mockResolvedValueOnce([{ id: 'a', name: 'A' }] as never)
        mockApiCall.mockResolvedValueOnce({ url: '#repo/proj/module/table' } as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1', projectName: 'proj-1' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'A')
        await userEvent.click(screen.getByText('graph:panel.open'))

        expect(mockApiCall).toHaveBeenCalledWith('/compile/table/a/url', { method: 'GET' }, expect.anything())
        // a resolved URL navigates and dismisses the modal
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('does not offer Open for a table from another project', async () => {
        mockApiCall.mockResolvedValueOnce([
            { id: 'a', name: 'A', project: 'OtherProject' },
        ] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1', projectName: 'CurrentProject' })
        await waitFor(() => expect(mockCytoscape).toHaveBeenCalled())

        await userEvent.selectOptions(screen.getByTestId('table-graph-search'), 'A')

        expect(screen.queryByText('graph:panel.open')).not.toBeInTheDocument()
        expect(screen.getByText('graph:panel.external')).toBeInTheDocument()
    })

    it('shows the empty state when there are no tables', async () => {
        mockApiCall.mockResolvedValueOnce([] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })

        await waitFor(() => expect(screen.getByText('graph:empty')).toBeInTheDocument())
        expect(mockCytoscape).not.toHaveBeenCalled()
    })
})

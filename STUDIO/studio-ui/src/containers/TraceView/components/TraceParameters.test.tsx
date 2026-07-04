import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { TreeDataNode } from 'antd'
import type { TraceParameterValue } from 'types/trace'
import TraceParameters, { ParameterTree, SingleParameter } from 'containers/TraceView/components/TraceParameters'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getParameterValue: vi.fn(),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// AntD Tree virtualises and can spin act() loops in jsdom; render the tree data as plain nested
// markup so a nested object's field names/values are assertable. Everything else in antd stays real.
interface MockTreeProps {
    treeData?: TreeDataNode[]
}
const renderNode = (node: TreeDataNode): React.ReactNode => (
    <li key={String(node.key)} data-testid="tree-node">
        <span>{node.title as React.ReactNode}</span>
        {node.children && <ul>{node.children.map(renderNode)}</ul>}
    </li>
)
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const Tree = ({ treeData = []}: MockTreeProps) => <ul data-testid="param-tree">{treeData.map(renderNode)}</ul>
    return { ...actual, Tree }
})

import traceService from 'services/traceService'

const getParameterValue = traceService.getParameterValue as ReturnType<typeof vi.fn>

const param = (over: Partial<TraceParameterValue> = {}): TraceParameterValue => ({
    name: 'premium',
    description: 'Double',
    lazy: false,
    value: 42,
    ...over,
})

describe('TraceParameters', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1' })
    })

    describe('TraceParameters (list)', () => {
        it('shows the empty text when there are no parameters', () => {
            render(<TraceParameters emptyText="none.here" parameters={[]} title="Params" />)
            expect(screen.getByText('none.here')).toBeInTheDocument()
        })

        it('falls back to the default empty translation when no emptyText is given', () => {
            render(<TraceParameters parameters={undefined} title="Params" />)
            expect(screen.getByText('details.noParameters')).toBeInTheDocument()
        })

        it('renders each parameter, its type, and its value', () => {
            render(
                <TraceParameters
                    parameters={[param({ name: 'age', description: 'int', value: 30 })]}
                    title="Params"
                />
            )
            expect(screen.getByText('age')).toBeInTheDocument()
            expect(screen.getByText('int')).toBeInTheDocument()
            expect(screen.getByText('30')).toBeInTheDocument()
        })

        it('renders a supplied copy button next to the title', () => {
            render(
                <TraceParameters
                    copyButton={<button data-testid="my-copy">copy</button>}
                    parameters={[param()]}
                    title="Params"
                />
            )
            expect(screen.getByTestId('my-copy')).toBeInTheDocument()
        })
    })

    describe('SingleParameter', () => {
        it('shows the empty text when there is no parameter', () => {
            render(<SingleParameter emptyText="no.result" parameter={undefined} title="Result" />)
            expect(screen.getByText('no.result')).toBeInTheDocument()
        })

        it('renders the single value', () => {
            render(<SingleParameter parameter={param({ name: 'out', value: 'ok' })} title="Result" />)
            expect(screen.getByText('out')).toBeInTheDocument()
            expect(screen.getByText('"ok"')).toBeInTheDocument()
        })
    })

    describe('ParameterTree', () => {
        it('renders a simple string value quoted', () => {
            render(<ParameterTree param={param({ name: 'name', description: 'String', value: 'Bob' })} paramKey="k" />)
            expect(screen.getByText('name')).toBeInTheDocument()
            expect(screen.getByText('"Bob"')).toBeInTheDocument()
        })

        it('renders a null value as null', () => {
            render(<ParameterTree param={param({ name: 'maybe', value: null })} paramKey="k" />)
            expect(screen.getByText('null')).toBeInTheDocument()
        })

        it('expands a nested object into a tree of its fields', () => {
            render(
                <ParameterTree
                    param={param({ name: 'bank', description: 'Bank', value: { id: 7, city: 'NY' } })}
                    paramKey="k"
                />
            )
            expect(screen.getByTestId('param-tree')).toBeInTheDocument()
            // Field names and their values appear as tree nodes.
            expect(screen.getByText('id')).toBeInTheDocument()
            expect(screen.getByText('city')).toBeInTheDocument()
            expect(screen.getByText('"NY"')).toBeInTheDocument()
        })

        it('offers a "load value" affordance for an unfetched lazy parameter', () => {
            render(
                <ParameterTree
                    param={param({ name: 'big', lazy: true, parameterId: 5, value: undefined })}
                    paramKey="k"
                />
            )
            expect(screen.getByText('param.loadValue')).toBeInTheDocument()
        })

        it('fetches and displays the lazy value on click', async () => {
            getParameterValue.mockResolvedValue({ name: 'big', description: 'Double', lazy: false, value: 99 })
            render(
                <ParameterTree
                    param={param({ name: 'big', description: 'Double', lazy: true, parameterId: 5, value: undefined })}
                    paramKey="k"
                />
            )

            await userEvent.click(screen.getByText('param.loadValue'))

            // The store's fetchLazyParameter goes through the service with the parameter id.
            await waitFor(() => expect(getParameterValue).toHaveBeenCalledWith('p1', 5))
            // Once loaded, the fetched value replaces the affordance.
            expect(await screen.findByText('99')).toBeInTheDocument()
            expect(screen.queryByText('param.loadValue')).toBeNull()
        })

        it('surfaces an error when the lazy fetch fails', async () => {
            getParameterValue.mockRejectedValue(new Error('boom'))
            render(
                <ParameterTree
                    param={param({ name: 'big', lazy: true, parameterId: 5, value: undefined })}
                    paramKey="k"
                />
            )

            await userEvent.click(screen.getByText('param.loadValue'))
            expect(await screen.findByText('boom')).toBeInTheDocument()
        })
    })
})

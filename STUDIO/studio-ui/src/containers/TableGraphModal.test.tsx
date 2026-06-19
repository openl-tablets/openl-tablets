import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import { TableGraphModal } from 'containers/TableGraphModal'
import * as services from 'services'
import cytoscape from 'cytoscape'
import type { MockedFunction } from 'vitest'

vi.mock('services', () => ({ apiCall: vi.fn() }))

vi.mock('cytoscape', () => ({
    default: vi.fn(() => ({ on: vi.fn(), destroy: vi.fn() })),
}))

// AntD's Modal leave-animation never ends in jsdom; replace it with an `open`-gated wrapper.
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
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>
const mockCytoscape = cytoscape as unknown as MockedFunction<(options: { elements: Array<{ data: { id: string } }> }) => unknown>

const dispatchOpen = async (detail: { projectId: string } | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openTableGraphModal', { detail }))
    })
}

describe('TableGraphModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
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

    it('shows the empty state when there are no tables', async () => {
        mockApiCall.mockResolvedValueOnce([] as never)

        render(<TableGraphModal />)
        await dispatchOpen({ projectId: 'proj-1' })

        await waitFor(() => expect(screen.getByText('graph:empty')).toBeInTheDocument())
        expect(mockCytoscape).not.toHaveBeenCalled()
    })
})

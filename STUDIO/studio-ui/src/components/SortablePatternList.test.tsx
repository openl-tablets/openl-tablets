import React, { useState } from 'react'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SortablePatternList } from './SortablePatternList'

interface FakeDragEndEvent {
    canceled: boolean
    operation: { source: { index: number }; target: { index: number } }
}

vi.mock('@dnd-kit/react', () => ({
    DragDropProvider: ({ children, onDragEnd }: {
        children?: React.ReactNode
        onDragEnd?: (event: FakeDragEndEvent) => void
    }) => {
        // expose the handler so a test can simulate a drop
        (globalThis as Record<string, unknown>)['__dragEnd'] = onDragEnd
        return <div>{children}</div>
    },
}))
vi.mock('@dnd-kit/react/sortable', () => ({
    useSortable: () => ({ ref: vi.fn(), handleRef: vi.fn(), isDragging: false }),
}))
vi.mock('@dnd-kit/helpers', () => ({
    move: <T, >(items: T[], event: FakeDragEndEvent) => {
        const copy = [...items]
        const [moved] = copy.splice(event.operation.source.index, 1)
        copy.splice(event.operation.target.index, 0, moved!)
        return copy
    },
}))

// A stateful harness so onChange feeds values back in, mirroring the real controlled usage and
// keeping the component's row ids in sync with the list.
const renderList = (initial: string[]) => {
    const onChange = vi.fn()
    const Harness: React.FC = () => {
        const [values, setValues] = useState(initial)
        return (
            <SortablePatternList
                addLabel="Add pattern"
                dragLabel="Drag to reorder"
                removeLabel="Remove"
                values={values}
                onChange={(next) => {
                    onChange(next)
                    setValues(next)
                }}
            />
        )
    }
    render(<Harness />)
    return onChange
}

describe('SortablePatternList', () => {
    it('renders one input per value', () => {
        renderList(['%name%', '%version%'])
        expect(screen.getAllByRole('textbox')).toHaveLength(2)
    })

    it('appends a blank row when adding', async () => {
        const onChange = renderList(['%name%'])
        await userEvent.click(screen.getByRole('button', { name: /Add pattern/ }))
        expect(onChange).toHaveBeenCalledWith(['%name%', ''])
    })

    it('removes a row', async () => {
        const onChange = renderList(['a', 'b'])
        await userEvent.click(screen.getAllByRole('button', { name: 'Remove' })[0]!)
        expect(onChange).toHaveBeenCalledWith(['b'])
    })

    it('reorders rows on drop', () => {
        const onChange = renderList(['a', 'b', 'c'])
        const onDragEnd = (globalThis as Record<string, unknown>)['__dragEnd'] as (event: FakeDragEndEvent) => void
        act(() => onDragEnd({ canceled: false, operation: { source: { index: 0 }, target: { index: 2 } } }))
        expect(onChange).toHaveBeenCalledWith(['b', 'c', 'a'])
    })
})

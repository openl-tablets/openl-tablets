import { render, screen } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { EditableStringList } from './EditableList'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: (_target, name) => String(name) }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('antd', () => ({
    Input: ({ value, onChange, ...rest }: Record<string, unknown>) =>
        <input data-testid={rest['data-testid'] as string} onChange={onChange as never} value={value as string} />,
    Button: ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { icon, size, type, ...dom } = rest
        void icon; void size; void type
        return <button onClick={onClick as never} type="button" {...dom}>{children as never}</button>
    },
    Tooltip: ({ children }: Record<string, unknown>) => <>{children as never}</>,
}))

// The drag wiring is exercised in the browser; here the rows render as plain items.
vi.mock('@dnd-kit/core', () => ({
    DndContext: ({ children }: Record<string, unknown>) => <>{children as never}</>,
    closestCenter: {}, KeyboardSensor: {}, PointerSensor: {},
    useSensor: () => ({}), useSensors: () => [],
}))
vi.mock('@dnd-kit/modifiers', () => ({ restrictToParentElement: {}, restrictToVerticalAxis: {} }))
vi.mock('@dnd-kit/sortable', () => ({
    arrayMove: <T, >(items: T[], from: number, to: number) => {
        const next = [...items]
        next.splice(to, 0, next.splice(from, 1)[0]!)
        return next
    },
    SortableContext: ({ children }: Record<string, unknown>) => <>{children as never}</>,
    sortableKeyboardCoordinates: {},
    useSortable: () => ({ attributes: {}, listeners: {}, setNodeRef: () => {}, transform: null, transition: undefined, isDragging: false }),
    verticalListSortingStrategy: {},
}))
vi.mock('@dnd-kit/utilities', () => ({ CSS: { Transform: { toString: () => undefined } } }))
vi.mock('@ant-design/icons', () => ({ DeleteOutlined: () => null, HolderOutlined: () => null, PlusOutlined: () => null }))

describe('EditableStringList', () => {
    it('edits a value, adds one with the plus, and removes one', async () => {
        const onChange = vi.fn()
        const { rerender } = render(<EditableStringList items={['a', 'b']} onChange={onChange} testId="item" />)

        fireEvent.change(screen.getByTestId('item-0'), { target: { value: 'A' } })
        expect(onChange).toHaveBeenLastCalledWith(['A', 'b'])

        await userEvent.click(screen.getByTestId('item-add'))
        expect(onChange).toHaveBeenLastCalledWith(['a', 'b', ''])

        await userEvent.click(screen.getByTestId('item-1-remove'))
        expect(onChange).toHaveBeenLastCalledWith(['a'])

        // Each value gets its own field.
        rerender(<EditableStringList items={['x', 'y', 'z']} onChange={onChange} testId="item" />)
        expect(screen.getByTestId('item-0')).toHaveValue('x')
        expect(screen.getByTestId('item-2')).toHaveValue('z')
    })
})

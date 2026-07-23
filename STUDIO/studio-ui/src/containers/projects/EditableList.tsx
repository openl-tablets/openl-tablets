import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input, Tooltip } from 'antd'
import { DeleteOutlined, HolderOutlined, PlusOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    closestCenter,
    DndContext,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    type DragEndEvent,
} from '@dnd-kit/core'
import { restrictToParentElement, restrictToVerticalAxis } from '@dnd-kit/modifiers'
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css }) => ({
    list: css`
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    item: css`
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
    `,
    field: css`
        flex: 1;
        min-width: 0;
    `,
    add: css`
        align-self: flex-start;
        padding: 0;
    `,
}))

interface EditableListProps<T> {
    items: T[]
    onChange: (items: T[]) => void
    /** Builds the value a new row starts from. */
    newItem: () => T
    /** Renders the fields of one row, given its value and a way to replace it. */
    renderItem: (item: T, onChange: (item: T) => void, testId: string) => ReactNode
    /** Prefix for the test ids of the rows, their fields and their remove buttons. */
    testId: string
}

/**
 * A list of values the user edits in place: each value in its own row, removed by its own button, added
 * with a plus, and reordered by dragging its handle. The values carry no ids, so each row is keyed by its
 * position within one render, which is stable across the edits the list itself makes.
 */
export const EditableList = <T, >({ items, onChange, newItem, renderItem, testId }: EditableListProps<T>) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const keyBase = useId()
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    )
    const keys = items.map((_, index) => `${keyBase}-${index}`)

    const setAt = (index: number, value: T) => onChange(items.map((item, at) => (at === index ? value : item)))
    const removeAt = (index: number) => onChange(items.filter((_, at) => at !== index))
    const add = () => onChange([...items, newItem()])

    const onDragEnd = ({ active, over }: DragEndEvent) => {
        if (!over || active.id === over.id) {
            return
        }
        onChange(arrayMove(items, keys.indexOf(String(active.id)), keys.indexOf(String(over.id))))
    }

    return (
        <div className={styles.list}>
            <DndContext
                collisionDetection={closestCenter}
                modifiers={[restrictToVerticalAxis, restrictToParentElement]}
                onDragEnd={onDragEnd}
                sensors={sensors}
            >
                <SortableContext items={keys} strategy={verticalListSortingStrategy}>
                    {items.map((item, index) => (
                        <EditableRow key={keys[index]} id={keys[index]!} onRemove={() => removeAt(index)} testId={`${testId}-${index}`}>
                            {renderItem(item, value => setAt(index, value), `${testId}-${index}`)}
                        </EditableRow>
                    ))}
                </SortableContext>
            </DndContext>
            <Button className={styles.add} data-testid={`${testId}-add`} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {t('browser.overview.list_add')}
            </Button>
        </div>
    )
}

interface EditableRowProps {
    id: string
    testId: string
    children: ReactNode
    onRemove: () => void
}

const EditableRow = ({ id, testId, children, onRemove }: EditableRowProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const { styles: shared } = useSharedStyles()
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id })
    return (
        <div
            ref={setNodeRef}
            className={cx(styles.item, isDragging && shared.dragging)}
            data-testid={`${testId}-row`}
            style={{ transform: CSS.Transform.toString(transform), transition }}
        >
            <span {...attributes} {...listeners} aria-label={t('browser.overview.list_reorder')} className={shared.dragHandle} data-testid={`${testId}-drag`}>
                <HolderOutlined />
            </span>
            <div className={styles.field}>{children}</div>
            <Tooltip title={t('browser.overview.list_remove')}>
                <Button
                    aria-label={t('browser.overview.list_remove')}
                    data-testid={`${testId}-remove`}
                    icon={<DeleteOutlined />}
                    onClick={onRemove}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </div>
    )
}

/** The common case: a list of plain text values, each edited in its own input. */
export const EditableStringList = ({ items, onChange, placeholder, testId }: {
    items: string[]
    onChange: (items: string[]) => void
    placeholder?: string
    testId: string
}) => (
    <EditableList
        items={items}
        newItem={() => ''}
        onChange={onChange}
        testId={testId}
        renderItem={(value, set, id) => (
            <Input data-testid={id} onChange={event => set(event.target.value)} placeholder={placeholder} size="small" value={value} />
        )}
    />
)

import React, { useEffect, useRef, useState } from 'react'
import { Button, Input } from 'antd'
import { CloseOutlined, HolderOutlined, PlusOutlined } from '@ant-design/icons'
import { DragDropProvider } from '@dnd-kit/react'
import { useSortable } from '@dnd-kit/react/sortable'
import { move } from '@dnd-kit/helpers'
import { useStyles } from './SortablePatternList.styles'

interface SortablePatternListProps {
    values: string[]
    onChange: (values: string[]) => void
    addLabel: string
    removeLabel: string
    dragLabel: string
    placeholder?: string
    'data-testid'?: string
}

/**
 * Edits an ordered list of strings, one input per line, where the order carries meaning. Rows can be
 * reordered by dragging the handle or with the keyboard, and each row can be edited or removed, plus
 * an "add" affordance. Used for the project descriptor's properties-file-name patterns, which are
 * matched in order.
 */
export const SortablePatternList: React.FC<SortablePatternListProps> = ({
    values,
    onChange,
    addLabel,
    removeLabel,
    dragLabel,
    placeholder,
    'data-testid': testId,
}) => {
    const { styles, cx } = useStyles()
    const idRef = useRef(0)
    const [ids, setIds] = useState<string[]>(() => values.map(() => `pattern-${idRef.current++}`))

    // Keep one stable id per row so drag-and-drop tracks rows across reorders. Reconcile only when the
    // list is changed from the outside (e.g. the form is reset), never during our own mutations.
    useEffect(() => {
        setIds((current) =>
            current.length === values.length ? current : values.map((_, i) => current[i] ?? `pattern-${idRef.current++}`))
    }, [values])

    const setAt = (index: number, value: string) => onChange(values.map((entry, i) => (i === index ? value : entry)))
    const removeAt = (index: number) => {
        setIds(ids.filter((_, i) => i !== index))
        onChange(values.filter((_, i) => i !== index))
    }
    const add = () => {
        setIds([...ids, `pattern-${idRef.current++}`])
        onChange([...values, ''])
    }

    return (
        <div data-testid={testId}>
            <DragDropProvider
                onDragEnd={(event) => {
                    if (event.canceled) {
                        return
                    }
                    setIds((current) => move(current, event))
                    onChange(move(values, event))
                }}
            >
                {values.map((value, index) => (
                    <SortableRow
                        key={ids[index]}
                        cx={cx}
                        dragLabel={dragLabel}
                        id={ids[index]!}
                        index={index}
                        onEdit={(next) => setAt(index, next)}
                        onRemove={() => removeAt(index)}
                        placeholder={placeholder}
                        removeLabel={removeLabel}
                        styles={styles}
                        value={value}
                    />
                ))}
            </DragDropProvider>
            <Button className={styles.add} icon={<PlusOutlined />} onClick={add} size="small" type="link">
                {addLabel}
            </Button>
        </div>
    )
}

interface SortableRowProps {
    id: string
    index: number
    value: string
    onEdit: (value: string) => void
    onRemove: () => void
    removeLabel: string
    dragLabel: string
    placeholder?: string | undefined
    styles: ReturnType<typeof useStyles>['styles']
    cx: ReturnType<typeof useStyles>['cx']
}

const SortableRow: React.FC<SortableRowProps> = ({
    id,
    index,
    value,
    onEdit,
    onRemove,
    removeLabel,
    dragLabel,
    placeholder,
    styles,
    cx,
}) => {
    const { ref, handleRef, isDragging } = useSortable({ id, index })
    return (
        <div ref={ref} className={styles.row} style={{ opacity: isDragging ? 0.6 : 1 }}>
            <Button
                ref={handleRef}
                aria-label={dragLabel}
                className={styles.handle}
                icon={<HolderOutlined />}
                size="small"
                type="text"
            />
            <Input
                className={cx(styles.input, styles.mono)}
                onChange={(event) => onEdit(event.target.value)}
                placeholder={placeholder}
                value={value}
            />
            <Button
                aria-label={removeLabel}
                className={styles.remove}
                icon={<CloseOutlined />}
                onClick={onRemove}
                size="small"
                type="text"
            />
        </div>
    )
}

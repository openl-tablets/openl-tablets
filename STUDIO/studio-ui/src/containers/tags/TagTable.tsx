import { createContext, FC, useContext, useMemo, type HTMLAttributes } from 'react'
import { Button, Popconfirm, Table, theme } from 'antd'
import {
    closestCenter,
    DndContext,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    type DragEndEvent,
    type DraggableAttributes,
    type DraggableSyntheticListeners,
} from '@dnd-kit/core'
import { restrictToVerticalAxis } from '@dnd-kit/modifiers'
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useTranslation } from 'react-i18next'
import { TagTableCheckboxCell } from './TagTableCheckboxCell'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import { TagTableNameCell } from './TagTableNameCell'
import { TagTableTagsCell } from './TagTableTagsCell'
import { useStyles } from './TagTable.styles'
import { useSharedStyles } from '../projects/sharedStyles'
import { FoundTagChips, type DefineTags } from './TagsFoundInProjects'
import type { FoundTag } from './tagsInProjects'
import { DeleteOutlined, HolderOutlined } from '@ant-design/icons'

export interface Tag {
    id: number;
    name: string;
    tagTypeId: number;
}

export interface TagActions {
    createTag: (tagName: string, tagTypeId: number) => void;
    updateTag: (tag: Tag) => void;
    deleteTag: (tag: Tag) => void;
}

export interface TagType {
    id: number;
    name: string;
    extensible: boolean;
    nullable: boolean;
    tags: Tag[];
}

interface TagTypeActions {
    updateTagType: (tagType: TagType) => boolean | Promise<boolean>;
    deleteTagType: (tagType: TagType) => void;
    /** Called with every tag type, in the order the user arranged them. */
    reorderTagTypes: (tagTypes: TagType[]) => void;
}

/** What the drag handle of a row needs from the row it moves. */
interface RowDrag {
    setActivatorNodeRef?: (element: HTMLElement | null) => void
    listeners?: DraggableSyntheticListeners
    attributes?: DraggableAttributes
}

const RowDragContext = createContext<RowDrag>({})

/**
 * The shared drag styles, read once by the table for all its rows: antd draws the rows itself, so they cannot be
 * handed down as a prop.
 */
const DragStylesContext = createContext<ReturnType<typeof useSharedStyles> | null>(null)

/** A row of the table that moves when its handle is dragged. */
const SortableRow = (props: HTMLAttributes<HTMLTableRowElement> & { 'data-row-key': number }) => {
    const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
        id: props['data-row-key'],
    })
    const shared = useContext(DragStylesContext)
    const drag = useMemo(
        () => ({ setActivatorNodeRef, listeners, attributes }),
        [setActivatorNodeRef, listeners, attributes]
    )
    const style = {
        ...props.style,
        transform: CSS.Translate.toString(transform),
        transition,
        // The row is lifted over its neighbours while it moves.
        ...(isDragging ? { position: 'relative' as const, zIndex: 1 } : {}),
    }
    return (
        <RowDragContext.Provider value={drag}>
            <tr
                {...props}
                ref={setNodeRef}
                className={shared?.cx(props.className, isDragging && shared.styles.dragging) ?? props.className}
                style={style}
            />
        </RowDragContext.Provider>
    )
}

/** The handle a row is dragged by, with the mouse or, once focused, with the keyboard. */
const DragHandle = ({ name, className }: { name: string, className: string }) => {
    const { t } = useTranslation()
    const { setActivatorNodeRef, listeners, attributes } = useContext(RowDragContext)
    return (
        <Button
            ref={setActivatorNodeRef}
            {...attributes}
            {...listeners}
            aria-label={t('tags:reorder', { name })}
            className={className}
            data-testid={`tag-type-drag-${name}`}
            icon={<HolderOutlined />}
            size="small"
            type="text"
        />
    )
}

interface TagTableProps extends TagActions, TagTypeActions {
    tagTypes: TagType[];
    isLoading: boolean;
    /** The values projects carry that a tag type does not have, by the id of the tag type. */
    found: Map<number, FoundTag[]>;
    defineTags: DefineTags;
}

/**
 * The tag types with their values, each type in a row. The rows are dragged into the order the tag types are
 * listed in, here and on the Overview tab of a project. No row can be moved while the table is loading.
 */
export const TagTable: FC<TagTableProps> = ({
    tagTypes, createTag, updateTag, deleteTag, deleteTagType, updateTagType, reorderTagTypes, isLoading, found,
    defineTags,
}) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const shared = useSharedStyles()
    const { token } = theme.useToken()
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    )
    const ids = tagTypes.map(tagType => tagType.id)

    const onDragEnd = ({ active, over }: DragEndEvent) => {
        if (over && active.id !== over.id) {
            reorderTagTypes(arrayMove(tagTypes, ids.indexOf(Number(active.id)), ids.indexOf(Number(over.id))))
        }
    }

    const onUpdateTagType = (tagType: TagType, name: string | null = null, extensible: boolean | null = null, nullable: boolean | null = null) => {
        if (tagType) {
            return updateTagType({
                ...tagType,
                name: name ?? tagType.name,
                extensible: extensible ?? tagType.extensible,
                nullable: nullable ?? tagType.nullable,
            })
        }

        return false
    }

    const onChangeTagTypeName = (tagType: TagType) => (name: string) => {
        return onUpdateTagType(tagType, name)
    }

    const onChangeTagTypeExtensible = (tagType: TagType) => (e: CheckboxChangeEvent) => {
        void Promise.resolve(onUpdateTagType(tagType, null, e.target.checked))
    }

    // The screen asks whether a value is required; the tag type keeps it as whether one may be missing.
    const onChangeTagTypeRequired = (tagType: TagType) => (e: CheckboxChangeEvent) => {
        void Promise.resolve(onUpdateTagType(tagType, null, null, !e.target.checked))
    }

    const handleDelete = (tagType: TagType) => {
        deleteTagType(tagType)
    }

    const columns = [
        {
            key: 'sort',
            width: '40px',
            render: (tagType: TagType) => <DragHandle className={shared.styles.dragHandle} name={tagType.name} />,
        },
        {
            title: t('tags:tag_type'),
            key: 'name',
            width: '300px',
            render: (tagType: TagType) => (
                <TagTableNameCell name={tagType.name} onChange={onChangeTagTypeName(tagType)} />
            ),
        },
        {
            title: t('tags:extensible'),
            key: 'extensible',
            render: (tagType: TagType) => (
                <TagTableCheckboxCell isChecked={tagType.extensible} onChange={onChangeTagTypeExtensible(tagType)} />
            ),
        },
        {
            title: t('tags:required'),
            key: 'required',
            render: (tagType: TagType) => (
                <TagTableCheckboxCell isChecked={!tagType.nullable} onChange={onChangeTagTypeRequired(tagType)} />
            ),
        },
        {
            title: t('tags:tags'),
            key: 'tags',
            render: (tagType: TagType) => (
                <div className={styles.tagsCell}>
                    <TagTableTagsCell
                        createTag={createTag}
                        deleteTag={deleteTag}
                        tags={tagType.tags || []}
                        tagTypeId={tagType.id}
                        updateTag={updateTag}
                    />
                    <FoundTagChips
                        onDefine={defineTags}
                        styles={styles}
                        tags={found.get(tagType.id) ?? []}
                        type={tagType.name}
                    />
                </div>
            ),
        },
        {
            title: t('tags:actions'),
            key: 'actions',
            width: '100px',
            render: (tagType: TagType) => (
                <Popconfirm onConfirm={() => handleDelete(tagType)} title="Sure to delete?">
                    <DeleteOutlined style={{ cursor: 'pointer', color: token.colorError }} />
                </Popconfirm>
            ),
        },
    ]

    return (
        <DndContext
            collisionDetection={closestCenter}
            modifiers={[restrictToVerticalAxis]}
            onDragEnd={onDragEnd}
            sensors={sensors}
        >
            {/* While a change is stored, no other order can be started, so the orders are stored one at a time. */}
            <SortableContext disabled={isLoading} items={ids} strategy={verticalListSortingStrategy}>
                <DragStylesContext.Provider value={shared}>
                    <Table
                        className={styles.table}
                        columns={columns}
                        // The rows of the tag types are sortable; the placeholder of an empty table is a plain row.
                        {...(tagTypes.length > 0 ? { components: { body: { row: SortableRow } } } : {})}
                        dataSource={tagTypes}
                        loading={isLoading}
                        pagination={false}
                        rowKey={(record) => record.id}
                    />
                </DragStylesContext.Provider>
            </SortableContext>
        </DndContext>
    )
}

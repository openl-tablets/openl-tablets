import React, { useEffect, useState } from 'react'
import { CloseOutlined, EditOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, Input, Space, Typography } from 'antd'
import type { TreeDataNode } from 'antd'
import { useTranslation } from 'react-i18next'
import { complexValueSummary, describeSimpleValue, isComplexValue } from 'components/values/valueTree'
import { labelOf, ScalarEditor } from './ScalarEditor'
import { createValue, fieldKind, mapValueSchema, resolveSchema, type FieldKind, type JsonSchema } from './schema'

const { Text } = Typography

const isUnset = (value: unknown): boolean => value === undefined || value === null

const asRecord = (value: unknown): Record<string, unknown> =>
    value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}

const asList = (value: unknown): unknown[] => (Array.isArray(value) ? value : [])

/** The first name no entry of the map carries yet. */
const freeEntryKey = (record: Record<string, unknown>): string => {
    const names = ['', ...Array.from({ length: Object.keys(record).length + 1 }, (_, index) => `key${index + 2}`)]
    return names.find(name => !(name in record)) ?? ''
}

/**
 * Where the row of a map entry is written.
 *
 * <p>The key is the user's own text and may hold the characters a path is written with. Encoded, a key cannot be
 * read as the path of another row.
 */
const entryPath = (map: string, key: string): string => `${map}[${encodeURIComponent(key)}]`

/** A copy of the record with the field set in place. An unset value removes the field. */
const withField = (record: Record<string, unknown>, name: string, next: unknown): Record<string, unknown> => {
    const { [name]: _previous, ...rest } = record
    return next === undefined ? rest : { ...record, [name]: next }
}

const isStructure = (kind: FieldKind): boolean => kind === 'object' || kind === 'array' || kind === 'map'

/** What every node of the tree shares. The schema the tree is rendered by and its editing state. */
export interface TreeContext {
    root: JsonSchema
    /** Names shown for the codes of an enumeration, keyed by the path of the field that takes them. */
    labels: Record<string, Record<string, string>>
    /** Path of the field whose inline editor is open, if any. */
    editing: string | null
    setEditing: (path: string | null) => void
    /** Expands a node that was just created, so its fields show at once. */
    expand: (path: string) => void
    /** Keeps the open elements of a list right after the one at the given position is removed. */
    afterRemove: (path: string, index: number) => void
    /**
     * Carries what is remembered of a node — and of everything under it — to where the node now stands, or
     * forgets it where the node is gone: a map entry renamed, one removed, a structure cleared away.
     */
    nodeMoved: (from: string, to: string | null) => void
    /**
     * The keys of a map's entries in the order their rows are drawn, told the order the map itself lists them in.
     *
     * <p>A key the form has not been told about is drawn after the rest.
     */
    entryOrder: (map: string, listed: string[]) => string[]
    /** Says the entries of a map are now in this order, after one was added, renamed or removed. */
    entriesReordered: (map: string, keys: string[]) => void
}

interface NodeSpec {
    name: string
    /** Label the node is shown under. The name is used when absent. */
    label?: string | undefined
    /** Display name of the declared type, shown next to the label. */
    type?: string | undefined
    schema: JsonSchema
    value: unknown
    path: string
    onChange: (value: unknown) => void
    /** Removes the node from the list it is an element of. */
    onRemove?: (() => void) | undefined
    /** The key of a map entry, edited in place. */
    onRename?: ((key: string) => void) | undefined
    /** The keys the other entries of the same map carry, which this one cannot take. */
    takenKeys?: string[] | undefined
    context: TreeContext
}

const ActionButton: React.FC<{ icon: React.ReactNode, label: string, testId: string, onClick: () => void, danger?: boolean }> = ({
    icon, label, testId, onClick, danger,
}) => (
    <Button
        aria-label={label}
        danger={danger ?? false}
        data-testid={testId}
        icon={icon}
        onClick={onClick}
        size="small"
        title={label}
        type="text"
    />
)

/**
 * The key of a map entry.
 *
 * The key is typed in place and taken when the field is left, or when Enter is pressed. A key another entry
 * already carries is refused, so that entry keeps its value.
 */
const KeyEditor: React.FC<{
    name: string
    path: string
    takenKeys: string[]
    onRename: (key: string) => void
}> = ({ name, path, takenKeys, onRename }) => {
    const { t } = useTranslation('execution')
    const [draft, setDraft] = useState(name)
    const taken = draft !== name && takenKeys.includes(draft)

    useEffect(() => setDraft(name), [name])

    const commit = () => {
        if (draft === name) {
            return
        }
        if (taken) {
            setDraft(name)
            return
        }
        onRename(draft)
    }

    return (
        <Input
            data-testid={`key-${path}`}
            onBlur={commit}
            onChange={event => setDraft(event.target.value)}
            onPressEnter={commit}
            placeholder={t('input.key')}
            size="small"
            style={{ width: 140 }}
            value={draft}
            {...(taken && { status: 'error' as const })}
        />
    )
}

/**
 * The text of a value next to its name.
 *
 * A plain value reads as the debugger shows it, a structure by its size. A code with a name is shown by its name.
 */
const ValueText: React.FC<{ value: unknown, path: string, labels?: Record<string, string> | undefined }> = ({
    value, path, labels,
}) => {
    // A code is text or a number, and its name is listed under its text, the way the choice lists it.
    const label = typeof value === 'string' || typeof value === 'number' ? labelOf(labels, String(value)) : undefined
    if (label !== undefined) {
        return <Text code data-testid={`value-${path}`}>{label}</Text>
    }
    if (isComplexValue(value)) {
        return <Text italic data-testid={`value-${path}`} type="secondary">{complexValueSummary(value)}</Text>
    }
    // An unset field is absent from the value. It is shown as null, which is what the rule receives.
    const { display, kind } = describeSimpleValue(value === undefined ? null : value)
    return kind === 'null'
        ? <Text italic data-testid={`value-${path}`} type="secondary">{display}</Text>
        : <Text code data-testid={`value-${path}`}>{display}</Text>
}

/** What a structure offers: creating it, growing it, and clearing it back to unset. */
const structureActions = ({ kind, unset, path, value, entries, onChange, create, context, t }: {
    kind: FieldKind
    unset: boolean
    path: string
    value: unknown
    /** The keys of a map's entries as its rows are drawn, worked out once by the node itself. */
    entries: string[]
    onChange: (value: unknown) => void
    create: () => void
    context: TreeContext
    t: (key: string) => string
}): React.ReactNode[] => {
    if (unset) {
        return [<ActionButton key="create" icon={<PlusOutlined />} label={t('input.create')} onClick={create} testId={`create-${path}`} />]
    }
    const grow = kind === 'array'
        ? () => onChange([...asList(value), null])
        : () => {
            const record = asRecord(value)
            const key = freeEntryKey(record)
            onChange(withField(record, key, null))
            // Drawn after the entries already there, wherever the map itself ends up listing it.
            context.entriesReordered(path, [...entries, key])
        }
    return [
        ...(kind === 'array' || kind === 'map'
            ? [<ActionButton key="add" icon={<PlusOutlined />} label={t('input.add')} onClick={grow} testId={`add-${path}`} />]
            : []),
        <ActionButton
            key="clear"
            icon={<CloseOutlined />}
            label={t('input.clear')}
            testId={`clear-${path}`}
            onClick={() => {
                onChange(undefined)
                context.nodeMoved(path, null)
            }}
        />,
    ]
}

/** What a plain value offers while it is not being edited: editing it, and clearing it. */
const valueActions = ({ editing, unset, path, onChange, setEditing, t }: {
    editing: boolean
    unset: boolean
    path: string
    onChange: (value: unknown) => void
    setEditing: (path: string | null) => void
    t: (key: string) => string
}): React.ReactNode[] => {
    if (editing) {
        return []
    }
    return [
        <ActionButton key="edit" icon={<EditOutlined />} label={t('input.edit')} onClick={() => setEditing(path)} testId={`edit-${path}`} />,
        ...(unset
            ? []
            : [<ActionButton key="clear" icon={<CloseOutlined />} label={t('input.clear')} onClick={() => onChange(undefined)} testId={`clear-${path}`} />]),
    ]
}

/**
 * The title of one node, `name (type) = value`, with the actions the node takes.
 *
 * A plain value is edited in place behind the pencil and cleared with the cross.
 *
 * A structure starts unset. The plus creates it, an object with its fields and a list with its first slot. The
 * cross makes it unset again. The plus on a list adds a `null` element. The minus next to an element removes it.
 */
const NodeTitle: React.FC<Omit<NodeSpec, 'schema'> & {
    kind: FieldKind
    resolved: JsonSchema
    /** The keys of a map's entries as its rows are drawn; empty for anything else. */
    entries: string[]
}> = ({ name, label, type, kind, resolved, entries, value, path, onChange, onRemove, onRename, takenKeys, context }) => {
    const { t } = useTranslation('execution')
    const editing = context.editing === path
    const unset = isUnset(value)
    const labels = context.labels[path]
    const create = () => {
        onChange(createValue(resolved, context.root))
        context.expand(path)
    }
    const actions = [
        ...(isStructure(kind)
            ? structureActions({ kind, unset, path, value, entries, onChange, create, context, t })
            : valueActions({ editing, unset, path, onChange, setEditing: context.setEditing, t })),
        ...(onRemove
            ? [<ActionButton key="remove" danger icon={<MinusOutlined />} label={t('input.remove')} onClick={onRemove} testId={`remove-${path}`} />]
            : []),
    ]
    // The tree must not see the focus of an inline editor. It would scroll to the node, which fails before the
    // list is measured.
    const keepFocus = (event: React.FocusEvent) => event.stopPropagation()
    return (
        <Space wrap onBlur={keepFocus} onFocus={keepFocus} size={4}>
            {onRename
                ? <KeyEditor name={name} onRename={onRename} path={path} takenKeys={takenKeys ?? []} />
                : <Text strong>{label ?? name}</Text>}
            {type && <Text type="secondary">({type})</Text>}
            <Text type="secondary">=</Text>
            {editing && !isStructure(kind)
                ? (
                    <ScalarEditor
                        kind={kind}
                        labels={labels}
                        onChange={onChange}
                        onDone={() => context.setEditing(null)}
                        path={path}
                        schema={resolved}
                        value={value}
                    />
                )
                : <ValueText labels={labels} path={path} value={value} />}
            {actions}
        </Space>
    )
}

/**
 * One node of the parameter tree with its children.
 *
 * A created object lists the fields of its schema, so an unset field shows as `null`. A created list shows its
 * elements, a created map its entries.
 */
export const buildNode = (spec: NodeSpec): TreeDataNode => {
    const { schema, value, path, onChange, context } = spec
    const resolved = resolveSchema(schema, context.root)
    const kind = fieldKind(resolved)
    // The rows of a map in the order they are drawn — neither the order nor the address of a row is read off
    // the map itself; see `entryOrders` in SchemaForm. Worked out once: the title adds an entry after the last
    // of them, and the children are the rows themselves.
    const keys = kind === 'map' && !isUnset(value)
        ? context.entryOrder(path, Object.keys(asRecord(value)))
        : []
    const title = <NodeTitle {...spec} entries={keys} kind={kind} resolved={resolved} />
    let children: TreeDataNode[] = []
    if (!isUnset(value) && kind === 'object') {
        const record = asRecord(value)
        children = Object.entries(resolved.properties ?? {}).map(([field, fieldSchema]) => buildNode({
            name: field,
            schema: fieldSchema,
            value: record[field],
            path: `${path}.${field}`,
            onChange: next => onChange(withField(record, field, next)),
            context,
        }))
    } else if (!isUnset(value) && kind === 'array') {
        const list = asList(value)
        const itemSchema = resolved.items ?? {}
        children = list.map((item, index) => buildNode({
            name: `[${index}]`,
            schema: itemSchema,
            value: item,
            path: `${path}[${index}]`,
            // A list keeps its slots. A cleared element stays as null rather than closing the gap.
            onChange: next => onChange(list.map((element, at) => (at === index ? next ?? null : element))),
            onRemove: () => {
                onChange(list.filter((_, at) => at !== index))
                context.afterRemove(path, index)
            },
            context,
        }))
    } else if (!isUnset(value) && kind === 'map') {
        const record = asRecord(value)
        const valueSchema = mapValueSchema(resolved)
        children = keys.map(key => {
            const row = entryPath(path, key)
            return buildNode({
                name: key,
                schema: valueSchema,
                value: record[key],
                path: row,
                onChange: next => onChange({ ...record, [key]: next ?? null }),
                onRemove: () => {
                    onChange(withField(record, key, undefined))
                    // Forgotten rather than left for the order to prune: named again, the key goes last, where
                    // a new entry goes, rather than back into the place it used to hold.
                    context.entriesReordered(path, keys.filter(kept => kept !== key))
                    context.nodeMoved(row, null)
                },
                onRename: renamed => {
                    onChange(Object.fromEntries(keys.map(k => [k === key ? renamed : k, record[k]])))
                    context.entriesReordered(path, keys.map(k => (k === key ? renamed : k)))
                    context.nodeMoved(row, entryPath(path, renamed))
                },
                // The keys of the whole map, this entry's own among them: a name is refused only where another
                // entry carries it, and the editor already lets the entry keep the name it has.
                takenKeys: keys,
                context,
            })
        })
    }
    return { key: path, title, isLeaf: children.length === 0, children }
}

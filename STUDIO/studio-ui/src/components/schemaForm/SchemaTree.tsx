import React, { useEffect, useState } from 'react'
import { CloseOutlined, EditOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, Input, Space, Typography } from 'antd'
import type { TreeDataNode } from 'antd'
import { useTranslation } from 'react-i18next'
import { complexValueSummary, describeSimpleValue, isComplexValue } from 'components/values/valueTree'
import { ScalarEditor } from './ScalarEditor'
import { createValue, fieldKind, mapValueSchema, resolveSchema, type FieldKind, type JsonSchema } from './schema'

const { Text } = Typography

const isUnset = (value: unknown): boolean => value === undefined || value === null

const asRecord = (value: unknown): Record<string, unknown> =>
    value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}

const asList = (value: unknown): unknown[] => (Array.isArray(value) ? value : [])

/** The map with one more entry, under the first name no entry carries yet. */
const withEntry = (record: Record<string, unknown>): Record<string, unknown> => {
    const names = ['', ...Array.from({ length: Object.keys(record).length + 1 }, (_, index) => `key${index + 2}`)]
    return { ...record, [names.find(name => !(name in record)) ?? '']: null }
}

/** A copy of the record with the field set in place. An unset value removes the field. */
const withField = (record: Record<string, unknown>, name: string, next: unknown): Record<string, unknown> => {
    const { [name]: _previous, ...rest } = record
    return next === undefined ? rest : { ...record, [name]: next }
}

const isStructure = (kind: FieldKind): boolean => kind === 'object' || kind === 'array' || kind === 'map'

/** What every node of the tree shares. The schema the tree is rendered by and its editing state. */
export interface TreeContext {
    root: JsonSchema
    /** Path of the field whose inline editor is open, if any. */
    editing: string | null
    setEditing: (path: string | null) => void
    /** Expands a node that was just created, so its fields show at once. */
    expand: (path: string) => void
    /** Keeps the open elements of a list right after the one at the given position is removed. */
    afterRemove: (path: string, index: number) => void
    /** Follows the open nodes of a map entry when it is renamed, or forgets them when it is removed. */
    entryMoved: (from: string, to: string | null) => void
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

/** The text of a value next to its name. A plain value reads as the debugger shows it, a structure by its size. */
const ValueText: React.FC<{ value: unknown, path: string }> = ({ value, path }) => {
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
const structureActions = ({ kind, unset, path, value, onChange, create, t }: {
    kind: FieldKind
    unset: boolean
    path: string
    value: unknown
    onChange: (value: unknown) => void
    create: () => void
    t: (key: string) => string
}): React.ReactNode[] => {
    if (unset) {
        return [<ActionButton key="create" icon={<PlusOutlined />} label={t('input.create')} onClick={create} testId={`create-${path}`} />]
    }
    const grow = kind === 'array'
        ? () => onChange([...asList(value), null])
        : () => onChange(withEntry(asRecord(value)))
    return [
        ...(kind === 'array' || kind === 'map'
            ? [<ActionButton key="add" icon={<PlusOutlined />} label={t('input.add')} onClick={grow} testId={`add-${path}`} />]
            : []),
        <ActionButton key="clear" icon={<CloseOutlined />} label={t('input.clear')} onClick={() => onChange(undefined)} testId={`clear-${path}`} />,
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
const NodeTitle: React.FC<Omit<NodeSpec, 'schema'> & { kind: FieldKind, resolved: JsonSchema }> = ({
    name, label, type, kind, resolved, value, path, onChange, onRemove, onRename, takenKeys, context,
}) => {
    const { t } = useTranslation('execution')
    const editing = context.editing === path
    const unset = isUnset(value)
    const create = () => {
        onChange(createValue(resolved, context.root))
        context.expand(path)
    }
    const actions = [
        ...(isStructure(kind)
            ? structureActions({ kind, unset, path, value, onChange, create, t })
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
                        onChange={onChange}
                        onDone={() => context.setEditing(null)}
                        path={path}
                        schema={resolved}
                        value={value}
                    />
                )
                : <ValueText path={path} value={value} />}
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
    const title = <NodeTitle {...spec} kind={kind} resolved={resolved} />
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
        const entries = Object.entries(record)
        const valueSchema = mapValueSchema(resolved)
        // An entry is addressed by its key. Renaming one rebuilds the map, and a map lists a key that reads
        // as a whole number before the others: addressed by position, the rows would move under the user.
        children = entries.map(([key, item]) => {
            // The key is the user's own text and may hold the characters a path is written with. Encoded, a
            // key cannot be read as the path of another row.
            const entryPath = `${path}[${encodeURIComponent(key)}]`
            return buildNode({
                name: key,
                schema: valueSchema,
                value: item,
                path: entryPath,
                onChange: next => onChange({ ...record, [key]: next ?? null }),
                onRemove: () => {
                    onChange(withField(record, key, undefined))
                    context.entryMoved(entryPath, null)
                },
                onRename: renamed => {
                    onChange(Object.fromEntries(entries.map(([k, v]) => [k === key ? renamed : k, v])))
                    context.entryMoved(entryPath, `${path}[${encodeURIComponent(renamed)}]`)
                },
                takenKeys: entries.map(([k]) => k).filter(k => k !== key),
                context,
            })
        })
    }
    return { key: path, title, isLeaf: children.length === 0, children }
}

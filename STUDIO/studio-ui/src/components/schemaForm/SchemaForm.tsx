import React, { useCallback, useMemo, useState } from 'react'
import { Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { buildNode, type TreeContext } from './SchemaTree'
import { createValue, withoutNulls, type JsonSchema } from './schema'

/** One value the form collects. A parameter of a table, or the runtime context. */
export interface SchemaFormParameter {
    /** Key the value is written under. */
    name: string
    /** Label the value is shown under. The name is used when absent. */
    label?: string | undefined
    /** Display name of the declared type, shown next to the label. */
    type?: string | undefined
    /** JSON schema of the values accepted. A value without a schema is edited as JSON text. */
    schema?: object | null | undefined
    /** Value the parameter starts with. It holds the defaults its type declares. */
    value?: unknown
}

interface SchemaFormProps {
    parameters: SchemaFormParameter[]
    /** The values collected so far, keyed by parameter name. A parameter without a value is absent. */
    value: Record<string, unknown>
    onChange: (value: Record<string, unknown>) => void
}

const rootSchema = (parameter: SchemaFormParameter): JsonSchema => (parameter.schema ?? {}) as JsonSchema

/** Held apart from the render, so that a node of the tree is not drawn again for a new object saying the same. */
const LINES = { showLeafIcon: false }

/** Whether the path names the node at `root`, or anything standing under it. */
const isUnder = (path: string, root: string): boolean =>
    path === root || path.startsWith(`${root}.`) || path.startsWith(`${root}[`)

/**
 * The values a form starts with.
 *
 * A parameter with a starting value keeps it, without its null fields. Any other structured parameter is created
 * empty, so the user has its fields to fill in.
 */
export const initialFormValue = (parameters: SchemaFormParameter[]): Record<string, unknown> =>
    Object.fromEntries(parameters
        .map(parameter => [parameter.name, parameter.value === undefined || parameter.value === null
            ? createValue(rootSchema(parameter), rootSchema(parameter))
            : withoutNulls(parameter.value)])
        .filter(([, initial]) => initial !== undefined))

/**
 * An input form built from the JSON schema of each parameter.
 *
 * The form is a tree of `name = value` lines, the way the trace window shows a parameter. A plain value is edited
 * in place behind its pencil. A structure is created and cleared with plus and cross. A list grows element by
 * element.
 *
 * Every parameter starts folded - a rule can declare many - and opening one shows its fields. A structure the
 * user creates opens itself, so the fields to fill in are in view.
 *
 * The form collects plain JSON in the shape the run and trace APIs read. What it holds can also be shown and
 * edited as JSON text.
 */
export const SchemaForm: React.FC<SchemaFormProps> = ({ parameters, value, onChange }) => {
    const [editing, setEditing] = useState<string | null>(null)
    const [expanded, setExpanded] = useState<string[]>([])
    // A map lists a key that reads as a whole number before the others and in rising order, whatever order its
    // entries were put in it. So the order the rows of a map are drawn in is kept here rather than read back off
    // the map — where naming an entry `2` would move it above the `10` beside it.
    const [entryOrders, setEntryOrders] = useState<Record<string, string[]>>({})
    const expand = useCallback((path: string) => setExpanded(keys => (keys.includes(path) ? keys : [...keys, path])), [])
    const opened = useCallback((keys: React.Key[]) => setExpanded(keys.map(String)), [])

    /**
     * Rewrites the paths the form remembers things under: what is open, which row is being written, and the
     * order a map draws its rows in.
     *
     * A node is addressed by where it stands, so everything under a node that moves moves with it — and all
     * three of these are remembered by that address. One rule carries them together, told how a path becomes
     * another one or becomes nothing at all, so that none can be left behind pointing at a row that has since
     * become somebody else's. An editor left on a row that is now another one would write over it.
     */
    const pathsMoved = useCallback((moved: (path: string) => string | null) => {
        setExpanded(keys => {
            const next = keys.flatMap(key => {
                const to = moved(key)
                return to === null ? [] : [to]
            })
            return next.length === keys.length && next.every((key, at) => key === keys[at]) ? keys : next
        })
        setEditing(open => (open === null ? open : moved(open)))
        setEntryOrders(orders => {
            const moving = Object.keys(orders).some(map => moved(map) !== map)
            if (!moving) {
                return orders
            }
            return Object.fromEntries(Object.entries(orders).flatMap(([map, keys]) => {
                const to = moved(map)
                return to === null ? [] : [[to, keys] as const]
            }))
        })
    }, [])

    // An element is addressed by its position, so the elements after the removed one stand one place earlier.
    const afterRemove = useCallback((path: string, index: number) => pathsMoved(key => {
        const prefix = `${path}[`
        if (!key.startsWith(prefix)) {
            return key
        }
        const at = Number(key.slice(prefix.length, key.indexOf(']', prefix.length)))
        if (Number.isNaN(at) || at < index) {
            return key
        }
        return at === index ? null : key.replace(`${prefix}${at}]`, `${prefix}${at - 1}]`)
    }), [pathsMoved])

    // A node carries what is remembered under it to where it stands now, and leaves it behind when it goes: a
    // map entry renamed, one removed, a whole structure cleared away. Nothing beside it moves.
    const nodeMoved = useCallback((from: string, to: string | null) => pathsMoved(key => {
        if (!isUnder(key, from)) {
            return key
        }
        return to === null ? null : `${to}${key.slice(from.length)}`
    }), [pathsMoved])

    // The entries the form was told about, in the order it was told, then any the map holds besides them. A map
    // the form knows nothing of — read back from a run, or written as JSON text — is drawn as it comes; once the
    // reader has arranged the map standing here, that arrangement holds until the structure is cleared away.
    const entryOrder = useCallback((map: string, listed: string[]): string[] => {
        const made = entryOrders[map]
        if (made === undefined) {
            return listed
        }
        const holds = new Set(listed)
        const arranged = new Set(made)
        return [...made.filter(key => holds.has(key)), ...listed.filter(key => !arranged.has(key))]
    }, [entryOrders])
    const entriesReordered = useCallback(
        (map: string, keys: string[]) => setEntryOrders(orders => ({ ...orders, [map]: keys })),
        []
    )

    const treeData = useMemo((): TreeDataNode[] => parameters.map(parameter => {
        const root = rootSchema(parameter)
        const context: TreeContext = {
            root, editing, setEditing, expand, afterRemove, nodeMoved, entryOrder, entriesReordered,
        }
        return buildNode({
            name: parameter.name,
            label: parameter.label,
            type: parameter.type,
            schema: root,
            value: value[parameter.name],
            path: parameter.name,
            onChange: (next: unknown) => {
                const { [parameter.name]: _previous, ...rest } = value
                onChange(next === undefined ? rest : { ...value, [parameter.name]: next })
            },
            context,
        })
    }), [parameters, value, onChange, editing, expand, afterRemove, nodeMoved, entryOrder, entriesReordered])

    return (
        // The expand animation is off. While it runs, a structure created by a click would not show its fields.
        // `false` turns it off, `null` would keep the default.
        <Tree
            blockNode
            expandedKeys={expanded}
            motion={false}
            onExpand={opened}
            selectable={false}
            showLine={LINES}
            treeData={treeData}
            virtual={false}
        />
    )
}

export default SchemaForm

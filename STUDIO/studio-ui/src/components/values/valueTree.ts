import type { Key, ReactNode } from 'react'
import type { TreeDataNode } from 'antd'

export type SimpleValueKind = 'null' | 'string' | 'number' | 'boolean' | 'other'

/** A value without inner structure. The text a viewer shows and the kind it colours it by. */
interface SimpleValueText {
    display: string
    kind: SimpleValueKind
}

/** Whether a value has inner structure that a viewer expands. An object or an array does. */
export const isComplexValue = (value: unknown): value is object => value !== null && typeof value === 'object'

/** The one-line summary of a value with inner structure that holds the given number of lines. */
export const linesSummary = (count: number, elements: boolean): string =>
    (elements ? `{${count} elements}` : `{${count} fields}`)

/** The one-line summary of a value with inner structure. It counts the fields or elements the value holds. */
export const complexValueSummary = (value: object): string =>
    (Array.isArray(value) ? linesSummary(value.length, true) : linesSummary(Object.keys(value).length, false))

/** Describes a value without inner structure the way a debugger shows it. Strings are quoted, null is spelled out. */
export const describeSimpleValue = (value: unknown): SimpleValueText => {
    if (value === null) {
        return { display: 'null', kind: 'null' }
    }
    if (value === undefined) {
        return { display: 'undefined', kind: 'null' }
    }
    if (typeof value === 'string') {
        return { display: `"${value}"`, kind: 'string' }
    }
    if (typeof value === 'number') {
        return { display: String(value), kind: 'number' }
    }
    if (typeof value === 'boolean') {
        return { display: String(value), kind: 'boolean' }
    }
    if (isComplexValue(value)) {
        // A value with inner structure is opened as a tree; on one line it is named by what it holds.
        return { display: complexValueSummary(value), kind: 'other' }
    }
    return { display: String(value), kind: 'other' }
}

/** The field names of the values read so far, by value: the tree is built again each time the reader opens a node. */
const FIELD_NAMES = new WeakMap<object, string[]>()

/** The names of the fields of a value, read once however often its tree is built. */
const fieldNames = (value: object): string[] => {
    let names = FIELD_NAMES.get(value)
    if (names === undefined) {
        names = Object.keys(value)
        FIELD_NAMES.set(value, names)
    }
    return names
}

/** How many lines a value opens into: one per element or field of a value with inner structure, and none else. */
const lineCount = (value: unknown): number => {
    if (!isComplexValue(value)) {
        return 0
    }
    return Array.isArray(value) ? value.length : fieldNames(value).length
}

/** The first lines a value with inner structure is opened into: its elements, or its fields under their names. */
const linesOf = (value: unknown, count: number): ValueNodeTitle[] => {
    if (!isComplexValue(value)) {
        return []
    }
    if (Array.isArray(value)) {
        return value.slice(0, count).map((item, index) => ({ name: `[${index}]`, value: item }))
    }
    const fields = value as Record<string, unknown>
    return fieldNames(value).slice(0, count).map(name => ({ name, value: fields[name] }))
}

/**
 * One node of a value tree. The name it is shown under, its value and, for a root, its declared type and what
 * the value is known as - `Driver (Sara)` - in place of the count of its fields.
 */
export interface ValueNodeTitle {
    name: string
    value: unknown
    type?: string | undefined
    summary?: string | undefined
}

/** How many lines an open node lists at first, and how many more each time the reader asks for them. */
export const LINES_PER_STEP = 100

/** How far the reader has opened a value tree. */
export interface ValueTreeReach {
    /** The keys of the nodes the reader has opened. */
    open: ReadonlySet<Key>
    /** How many lines of a node the reader has had listed, by its key. A node not named lists one step of them. */
    listed: ReadonlyMap<Key, number>
    /** The line under the lines of a node that lists more of them, given the key of the node and how many are left. */
    renderMore: (key: string, left: number) => ReactNode
}

/** A value tree the reader has not opened. */
const CLOSED: ValueTreeReach = { open: new Set(), listed: new Map(), renderMore: () => null }

/**
 * The tree of a value in the shape the Ant Design Tree renders, as far as the reader has opened it.
 *
 * Every field or element of an open node becomes a node. A node that is not open holds none of them, and is marked
 * as one that opens. A whole value can hold millions of fields, and the tree walks every node it is given each
 * time it is drawn, so a node is built only once the reader opens the one above it.
 *
 * An open node lists its lines a step at a time, and a line under them lists more. A list of thousands of
 * elements is drawn as far as the reader goes down it.
 *
 * The caller renders every title, so each screen keeps its own look while all of them share the traversal. A
 * value without inner structure, or an empty one, is a leaf.
 *
 * @param reach how far the reader has opened the tree
 */
export const buildValueTreeData = (
    title: ValueNodeTitle,
    renderTitle: (title: ValueNodeTitle) => ReactNode,
    keyPrefix = '0',
    reach: ValueTreeReach = CLOSED
): TreeDataNode => {
    const total = lineCount(title.value)
    // The lines are counted already: a title that counted them again would read every key of the value anew.
    const titled = title.summary === undefined && isComplexValue(title.value)
        ? { ...title, summary: linesSummary(total, Array.isArray(title.value)) }
        : title
    if (total === 0) {
        return { key: keyPrefix, title: renderTitle(titled), isLeaf: true }
    }
    const node: TreeDataNode = { key: keyPrefix, title: renderTitle(titled), isLeaf: false }
    if (!reach.open.has(keyPrefix)) {
        return node
    }
    const listed = Math.min(total, reach.listed.get(keyPrefix) ?? LINES_PER_STEP)
    const children = linesOf(title.value, listed)
        .map((child, index) => buildValueTreeData(child, renderTitle, `${keyPrefix}-${index}`, reach))
    if (listed < total) {
        children.push({ key: `${keyPrefix}-more`, title: reach.renderMore(keyPrefix, total - listed), isLeaf: true })
    }
    return { ...node, children }
}

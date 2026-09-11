import type { ModuleTable } from 'types/tables'
import { readJson, writeJson } from '../../utils/localStore'

/** Group by the family a table belongs to: Rules, Spreadsheet, Datatype, Test. */
export const GROUP_BY_KIND = '[Kind]'

/** Group by the keyword a table is written with: SimpleRules, SmartLookup, Vocabulary. */
export const GROUP_BY_TABLE_TYPE = '[Type]'

/** Group by the workbook the table is written in. */
export const GROUP_BY_FILE = '[File]'

/** Group by the `category` property a table declares. */
export const GROUP_BY_CATEGORY = 'category'

export const GROUP_BY_NONE = ''

/** What each of the two levels groups by; an empty level groups by nothing. */
export type TableGroupingLevels = [string, string]

export const NO_GROUPING: TableGroupingLevels = [GROUP_BY_NONE, GROUP_BY_NONE]

/**
 * What the tree opens as: the tables gathered by family, the way the Editor has always shown them.
 */
export const DEFAULT_GROUPING: TableGroupingLevels = [GROUP_BY_KIND, GROUP_BY_NONE]

const STORAGE_KEY = 'openl.module.tableGrouping'

const isGroupingLevels = (value: unknown): value is TableGroupingLevels =>
    Array.isArray(value) && value.length === 2 && value.every(level => typeof level === 'string')

export const loadGrouping = (): TableGroupingLevels => readJson(STORAGE_KEY, DEFAULT_GROUPING, isGroupingLevels)

export const saveGrouping = (levels: TableGroupingLevels): void => writeJson(STORAGE_KEY, levels)

/** The levels that actually group, in order; the empty ones are left out. */
export const activeLevels = (levels: TableGroupingLevels): string[] =>
    levels.filter(level => level !== GROUP_BY_NONE)

/** One node of the tree: a group of tables, or a table itself. */
export interface TableNode {
    key: string
    /** What the node is called — the group's value, or the table's name. */
    title: string
    /** Set on a table leaf. */
    table?: ModuleTable
    children: TableNode[]
}

/** What a table is filed under at a level, or null when it carries no value for it. */
const valueOf = (table: ModuleTable, level: string): string | null => {
    if (level === GROUP_BY_KIND) {
        return table.kind || null
    }
    if (level === GROUP_BY_TABLE_TYPE) {
        return table.tableType || null
    }
    if (level === GROUP_BY_FILE) {
        return table.file || null
    }
    const property = table.properties?.[level]
    return typeof property === 'string' && property !== '' ? property : null
}

const tableNode = (table: ModuleTable, keyPrefix: string): TableNode => ({
    key: `${keyPrefix}/table/${table.id}`,
    title: table.name,
    table,
    children: [],
})

const byLabel = (left: string, right: string): number =>
    left.localeCompare(right, undefined, { sensitivity: 'base' })

/**
 * Groups the tables into a tree, one level per grouping.
 *
 * A table carrying no value for a level stays at that level, beside the groups, so a table is never hidden by a
 * property it does not declare.
 *
 * The tree is built from the tables already in the browser, so changing the grouping costs no request.
 */
export const buildTableTree = (
    tables: ModuleTable[],
    levels: string[],
    keyPrefix = 'grp'
): TableNode[] => {
    if (levels.length === 0) {
        return [...tables].sort((left, right) => byLabel(left.name, right.name))
            .map(table => tableNode(table, keyPrefix))
    }
    const [level, ...rest] = levels as [string, ...string[]]
    const groups = new Map<string, ModuleTable[]>()
    const ungrouped: ModuleTable[] = []
    for (const table of tables) {
        const value = valueOf(table, level)
        if (value === null) {
            ungrouped.push(table)
            continue
        }
        const group = groups.get(value)
        if (group) {
            group.push(table)
        } else {
            groups.set(value, [table])
        }
    }
    const nodes: TableNode[] = [...groups.entries()]
        .sort(([left], [right]) => byLabel(left, right))
        .map(([value, grouped]) => {
            const key = `${keyPrefix}/${level}/${value}`
            return {
                key,
                title: value,
                children: buildTableTree(grouped, rest, key),
            }
        })
    return [...nodes, ...buildTableTree(ungrouped, rest, `${keyPrefix}/rest`)]
}

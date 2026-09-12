import type { ModuleTable } from 'types/tables'
import { readJson, writeJson } from '../../utils/localStore'

/**
 * How the tree gathers the tables, named as the Editor has always named its views.
 *
 * The engine's own default is `excelSheet` (`rules.tree.view.default`), so that is what the tree opens as.
 */
export type TableView = 'excelSheet' | 'type' | 'category' | 'categoryDetailed' | 'categoryInversed'

export const TABLE_VIEWS: TableView[] = ['type', 'excelSheet', 'category', 'categoryDetailed', 'categoryInversed']

export const DEFAULT_VIEW: TableView = 'excelSheet'

const STORAGE_KEY = 'openl.module.tableView'

const isView = (value: unknown): value is TableView =>
    typeof value === 'string' && (TABLE_VIEWS as string[]).includes(value)

/**
 * The view the tree opens on: the one this browser last chose, or — having chosen none — the Default Order of
 * the user's own settings, which is what the Editor has always obeyed.
 */
export const loadView = (preferred?: string): TableView =>
    readJson(STORAGE_KEY, isView(preferred) ? preferred : DEFAULT_VIEW, isView)

export const saveView = (view: TableView): void => writeJson(STORAGE_KEY, view)

/** What a level files a table under: the sheet, the kind, or a step of its category. */
type Level =
    | { by: 'sheet' }
    | { by: 'kind' }
    | { by: 'category' }
    /** One step of a dotted category, counted from the left; the inversed view reads the steps the other way. */
    | { by: 'categoryStep', step: number }

/** The levels each view groups by, in order, mirroring the builders of the JSF views. */
const LEVELS: Record<TableView, Level[]> = {
    excelSheet: [{ by: 'sheet' }],
    type: [{ by: 'kind' }],
    category: [{ by: 'category' }],
    categoryDetailed: [{ by: 'categoryStep', step: 0 }, { by: 'categoryStep', step: 1 }],
    categoryInversed: [{ by: 'categoryStep', step: 1 }, { by: 'categoryStep', step: 0 }],
}

export const levelsOf = (view: TableView): Level[] => LEVELS[view]

/** One node of the tree: a group of tables, or a table itself. */
export interface TableNode {
    key: string
    /** What the node is called — the group's value, or the table's name. */
    title: string
    /** What the node groups by, absent on a table leaf; the icon is chosen from it. */
    groupedBy?: Level['by'] | 'overload'
    /** Set on a table leaf. */
    table?: ModuleTable
    children: TableNode[]
}

/** What one step of the hierarchy costs in width, matching the tree's own indent. */
const INDENT = 12
/** The switcher, the icon and the padding a row carries before its name. */
const ROW_CHROME = 56
/** What one character of a name takes, at the tree's font size. */
const CHARACTER = 7.2

/**
 * How wide the widest row of the tree is, so a virtualised tree can still be scrolled sideways.
 *
 * The width is reckoned from the names rather than measured in the page: the tree scrolls only as far as the
 * longest name reaches, and a reckoning a few pixels over is a few pixels of empty room, not a clipped name.
 */
export const widthOf = (nodes: TableNode[], depth = 0): number => nodes.reduce((widest, node) => Math.max(
    widest,
    ROW_CHROME + depth * INDENT + node.title.length * CHARACTER,
    widthOf(node.children, depth + 1)
), 0)

const categoryOf = (table: ModuleTable): string | null => {
    const value = table.properties?.['category']
    return typeof value === 'string' && value !== '' ? value : null
}

/** What a table is filed under at a level, or null when it carries no value for it. */
const valueOf = (table: ModuleTable, level: Level): string | null => {
    if (level.by === 'sheet') {
        return table.sheet || null
    }
    if (level.by === 'kind') {
        return table.kind || null
    }
    const category = categoryOf(table)
    if (level.by === 'category') {
        return category
    }
    // A category reads as steps separated by dots, the way the Editor's detailed views read it.
    return category === null ? null : category.split('.')[level.step] ?? null
}

const tableNode = (table: ModuleTable, keyPrefix: string): TableNode => ({
    key: `${keyPrefix}/table/${table.id}`,
    // A table written in several versions is named by what tells it from the others.
    title: table.displayName ?? table.name,
    table,
    children: [],
})

const byLabel = (left: string, right: string): number =>
    left.localeCompare(right, undefined, { sensitivity: 'base' })

/** The name most of the versions are written under, which is what their folder is called. */
const majorityName = (versions: ModuleTable[]): string => {
    const counted = new Map<string, number>()
    for (const version of versions) {
        counted.set(version.name, (counted.get(version.name) ?? 0) + 1)
    }
    return [...counted.entries()].reduce((most, entry) => entry[1] > most[1] ? entry : most)[0]
}

/**
 * The tables of a branch, with the versions of one table gathered under a folder of their own.
 *
 * Which version of a table answers a call is decided by its dimension properties, and the versions are written
 * as separate tables carrying one name. The tree files them under that name and calls each version by what
 * tells it from the others, the way the Editor's tree has always drawn them.
 *
 * The folder stands even where a branch holds a single version — the other versions are written on another
 * sheet, or under another category — so a version is always found in the same place.
 */
const tableNodes = (tables: ModuleTable[], keyPrefix: string): TableNode[] => {
    const versions = new Map<string, ModuleTable[]>()
    const alone: ModuleTable[] = []
    for (const table of tables) {
        const group = table.overloadGroup
        if (group === undefined) {
            alone.push(table)
        } else {
            versions.set(group, [...versions.get(group) ?? [], table])
        }
    }
    const folders = [...versions.entries()].map(([group, grouped]) => {
        const key = `${keyPrefix}/versions/${group}`
        return {
            key,
            title: majorityName(grouped),
            groupedBy: 'overload' as const,
            children: grouped.map(table => tableNode(table, key))
                .sort((left, right) => byLabel(left.title, right.title)),
        }
    })
    return [...folders, ...alone.map(table => tableNode(table, keyPrefix))]
        .sort((left, right) => byLabel(left.title, right.title))
}

/**
 * Groups the tables into a tree, one level per grouping.
 *
 * A table carrying no value for a level stays at that level, beside the groups, so a table is never hidden by a
 * property it does not declare.
 *
 * The tree is built from the tables already in the browser, so changing the view costs no request.
 */
export const buildTableTree = (
    tables: ModuleTable[],
    levels: Level[],
    keyPrefix = 'grp'
): TableNode[] => {
    if (levels.length === 0) {
        return tableNodes(tables, keyPrefix)
    }
    const [level, ...rest] = levels as [Level, ...Level[]]
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
            const key = `${keyPrefix}/${level.by}${level.by === 'categoryStep' ? level.step : ''}/${value}`
            return {
                key,
                title: value,
                groupedBy: level.by,
                children: buildTableTree(grouped, rest, key),
            }
        })
    return [...nodes, ...buildTableTree(ungrouped, rest, `${keyPrefix}/rest`)]
}

/** The tree of the given view. */
export const treeOf = (tables: ModuleTable[], view: TableView): TableNode[] =>
    buildTableTree(tables, levelsOf(view))

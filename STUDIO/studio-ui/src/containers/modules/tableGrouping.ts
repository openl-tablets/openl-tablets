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
    groupedBy?: Level['by']
    /** Set on a table leaf. */
    table?: ModuleTable
    children: TableNode[]
}

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
 * The tree is built from the tables already in the browser, so changing the view costs no request.
 */
export const buildTableTree = (
    tables: ModuleTable[],
    levels: Level[],
    keyPrefix = 'grp'
): TableNode[] => {
    if (levels.length === 0) {
        return [...tables].sort((left, right) => byLabel(left.name, right.name))
            .map(table => tableNode(table, keyPrefix))
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

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

const OTHER_STORAGE_KEY = 'openl.module.otherTables'

/** Whether this browser last chose to list the free-form tables — the ones OpenL does not recognize. */
export const loadShowOther = (): boolean =>
    readJson(OTHER_STORAGE_KEY, false, (value): value is boolean => typeof value === 'boolean')

export const saveShowOther = (shown: boolean): void => writeJson(OTHER_STORAGE_KEY, shown)

/** Reads a label of the tree in the user's language. */
type Translate = (key: string) => string

/**
 * The groups of the Type view, in the order the Editor's tree listed them.
 *
 * A group gathers a family of tables - the kind the server files a table under - told apart further where
 * the Editor told it apart: a vocabulary is a datatype that declares values rather than fields, and it stood
 * in a group of its own. The name of the group is the Editor's, which is not always the name of the kind: the
 * decision tables are the `Rules` family, and the environment table is `Configuration`.
 */
const TYPE_GROUPS: readonly { id: string, key: string }[] = [
    { id: 'Rules', key: 'decision' },
    { id: 'Spreadsheet', key: 'spreadsheet' },
    { id: 'TBasic', key: 'tbasic' },
    { id: 'Column Match', key: 'columnMatch' },
    { id: 'Data', key: 'data' },
    { id: 'Run', key: 'run' },
    { id: 'Test', key: 'test' },
    { id: 'Datatype', key: 'datatype' },
    { id: 'Vocabulary', key: 'vocabulary' },
    { id: 'Method', key: 'method' },
    { id: 'Constants', key: 'constants' },
    { id: 'Conditions', key: 'conditions' },
    { id: 'Actions', key: 'actions' },
    { id: 'Returns', key: 'returns' },
    { id: 'Environment', key: 'configuration' },
    { id: 'Other', key: 'other' },
    { id: 'Properties', key: 'properties' },
]

/** The datatype header type of a table that declares values rather than fields. */
const VOCABULARY = 'Vocabulary'

/** The group of the Type view a table belongs to. */
const typeGroupOf = (table: ModuleTable): string | null => {
    if (!table.kind) {
        return null
    }
    return table.kind === 'Datatype' && table.tableType === VOCABULARY ? VOCABULARY : table.kind
}

/** Where a group of the Type view stands: the Editor's order for the groups it knew, after them for the rest. */
const typeGroupOrder = (id: string): number => {
    const known = TYPE_GROUPS.findIndex(group => group.id === id)
    return known === -1 ? TYPE_GROUPS.length : known
}

/**
 * What a level files a table under: the sheet, the kind, a step of its category, or the folder of a properties
 * table.
 *
 * A properties table is filed by the scope it declares: one for the module stands in a folder of its own at
 * the root, one for a category in a folder inside that category. A folder of properties tables is the end of
 * the way down: what it holds is not grouped any further.
 */
type Level =
    | { by: 'sheet' }
    | { by: 'kind' }
    | { by: 'category' }
    /** One step of a dashed category, counted from the left; the inversed view reads the steps the other way. */
    | { by: 'categoryStep', step: number }
    | { by: 'moduleProperties' }
    | { by: 'categoryProperties' }

/** The levels each view groups by, in order, mirroring the builders of the JSF views. */
const LEVELS: Record<TableView, Level[]> = {
    excelSheet: [{ by: 'sheet' }],
    type: [{ by: 'kind' }],
    category: [{ by: 'moduleProperties' }, { by: 'category' }, { by: 'categoryProperties' }],
    categoryDetailed: [
        { by: 'moduleProperties' },
        { by: 'categoryStep', step: 0 },
        { by: 'categoryStep', step: 1 },
        { by: 'categoryProperties' },
    ],
    categoryInversed: [
        { by: 'moduleProperties' },
        { by: 'categoryStep', step: 1 },
        { by: 'categoryStep', step: 0 },
        { by: 'categoryProperties' },
    ],
}

/** The kind of the tables that declare properties for others, and the scopes they declare them for. */
const PROPERTIES = 'Properties'
const MODULE_SCOPE = 'Module'
const CATEGORY_SCOPE = 'Category'

/** The scope a properties table declares, or null for any other table. */
const propertiesScopeOf = (table: ModuleTable): string | null => {
    const scope = table.kind === PROPERTIES ? table.properties?.['scope'] : null
    return typeof scope === 'string' ? scope : null
}

const levelsOf = (view: TableView): Level[] => LEVELS[view]

/** One node of the tree: a group of tables, or a table itself. */
export interface TableNode {
    key: string
    /** What the node is called — the group's value, or the table's name. */
    title: string
    /** What the node groups by, absent on a table leaf; the icon is chosen from it. */
    groupedBy?: Level['by'] | 'overload'
    /** What the group holds, said when the pointer rests on it; only a group of the Type view has one. */
    hint?: string
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

/**
 * The category a table is filed under: the one it declares, or else the sheet it is written on, which is
 * what the Editor's tree fell back on.
 */
const categoryOf = (table: ModuleTable): string | null => {
    const value = table.properties?.['category']
    if (typeof value === 'string' && value !== '') {
        return value
    }
    return table.sheet || null
}

/** The steps of a category, which the Editor's detailed views read between the dashes of its name. */
const categorySteps = (category: string): string[] => category.split('-').map(step => step.trim()).filter(Boolean)

/** What a table is filed under at a category level, or null when it carries no value for it. */
const categoryValueOf = (table: ModuleTable, level: { by: 'category' } | { by: 'categoryStep', step: number }): string | null => {
    // The properties of the module stand in a folder of their own, not in a category.
    if (propertiesScopeOf(table) === MODULE_SCOPE) {
        return null
    }
    const category = categoryOf(table)
    if (category === null || level.by === 'category') {
        return category
    }
    // A category with fewer steps than the level asks for is filed by the steps it has.
    return categorySteps(category)[level.step] ?? null
}

/** What a table is filed under at a level, or null when it carries no value for it. */
const valueOf = (table: ModuleTable, level: Level, t: Translate): string | null => {
    switch (level.by) {
        case 'sheet':
            return table.sheet || null
        case 'kind':
            return typeGroupOf(table)
        case 'moduleProperties':
            return propertiesScopeOf(table) === MODULE_SCOPE ? t('browser.module.module_properties') : null
        case 'categoryProperties':
            return propertiesScopeOf(table) === CATEGORY_SCOPE ? t('browser.module.category_properties') : null
        default:
            return categoryValueOf(table, level)
    }
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
    // The versions a folder gathers are never none, so there is always a name to start from.
    return [...counted.entries()]
        .reduce(([mostName, mostSeen], [name, seen]) => seen > mostSeen ? [name, seen] : [mostName, mostSeen],
            ['', 0] as [string, number])[0]
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
const buildTableTree = (
    tables: ModuleTable[],
    levels: Level[],
    t: Translate,
    keyPrefix = 'grp'
): TableNode[] => {
    if (levels.length === 0) {
        return tableNodes(tables, keyPrefix)
    }
    const [level, ...rest] = levels as [Level, ...Level[]]
    const groups = new Map<string, ModuleTable[]>()
    const ungrouped: ModuleTable[] = []
    for (const table of tables) {
        const value = valueOf(table, level, t)
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
    // The groups of the Type view stand in the Editor's order, under the Editor's names; the groups of any
    // other level are read by name, together with whatever stands beside them, the way the Editor ordered a
    // branch. A folder of properties tables holds them as they are; any other group goes on down the levels.
    const folder = level.by === 'moduleProperties' || level.by === 'categoryProperties'
    const nodes: TableNode[] = [...groups.entries()]
        .sort(([left], [right]) => (level.by === 'kind' ? typeGroupOrder(left) - typeGroupOrder(right) : 0)
            || byLabel(left, right))
        .map(([value, grouped]) => {
            const key = `${keyPrefix}/${level.by}${level.by === 'categoryStep' ? level.step : ''}/${value}`
            const typeGroup = level.by === 'kind' ? TYPE_GROUPS.find(group => group.id === value) : undefined
            return {
                key,
                title: typeGroup ? t(`browser.module.types.${typeGroup.key}`) : value,
                ...(typeGroup && { hint: t(`browser.module.type_hints.${typeGroup.key}`) }),
                groupedBy: level.by,
                children: folder ? tableNodes(grouped, key) : buildTableTree(grouped, rest, t, key),
            }
        })
    const beside = buildTableTree(ungrouped, rest, t, `${keyPrefix}/rest`)
    return level.by === 'kind'
        ? [...nodes, ...beside]
        : [...nodes, ...beside].sort((left, right) => byLabel(left.title, right.title))
}

/** The tree of the given view, its groups named in the user's language. */
export const treeOf = (tables: ModuleTable[], view: TableView, t: Translate): TableNode[] =>
    buildTableTree(tables, levelsOf(view), t)

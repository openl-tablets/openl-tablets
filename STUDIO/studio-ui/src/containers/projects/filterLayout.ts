import { readJson, writeJson } from '../../utils/localStore'
/** The group of the rail that filters by repository. */
export const REPOSITORY_GROUP = 'repository'
/** The group of the rail that filters by the branch a project is open on. */
export const BRANCH_GROUP = 'branch'
/** The group of the rail that filters by project state. */
export const STATUS_GROUP = 'status'
/** The group of the rail that filters by one tag type. */
export const tagGroupId = (type: string): string => `tag:${type}`

/**
 * How the user arranged the filter rail: the order of its groups, the ones they put away, and the ones
 * they folded. Everything the rail does not know about — a tag type added since — falls in at its
 * natural place, so a stored arrangement never hides a new filter.
 */
export interface FilterLayout {
    order: string[]
    hidden: string[]
    collapsed: string[]
}

export const EMPTY_LAYOUT: FilterLayout = { order: [], hidden: [], collapsed: []}

const STORAGE_KEY = 'openl.projects.filters.layout'

const isStringArray = (value: unknown): value is string[] =>
    Array.isArray(value) && value.every(item => typeof item === 'string')

const isRecord = (value: unknown): value is Partial<FilterLayout> => typeof value === 'object' && value !== null

export const loadFilterLayout = (): FilterLayout => {
    const layout = readJson<Partial<FilterLayout>>(STORAGE_KEY, {}, isRecord)
    return {
        order: isStringArray(layout.order) ? layout.order : [],
        hidden: isStringArray(layout.hidden) ? layout.hidden : [],
        collapsed: isStringArray(layout.collapsed) ? layout.collapsed : [],
    }
}

export const saveFilterLayout = (layout: FilterLayout): void => writeJson(STORAGE_KEY, layout)

/**
 * The groups in the order the rail shows them: the ones the user arranged first, in their order, then
 * whatever the rail has since gained, in the order it offers them by default.
 */
export const orderGroups = <T extends { id: string }>(groups: T[], order: string[]): T[] => {
    const byId = new Map(groups.map(group => [group.id, group]))
    const arranged: T[] = []
    for (const id of order) {
        const group = byId.get(id)
        if (group) {
            arranged.push(group)
            byId.delete(id)
        }
    }
    return [...arranged, ...groups.filter(group => byId.has(group.id))]
}

/** The arrangement after a group was dragged onto the place of another. */
export const moveGroup = (order: string[], from: string, to: string): string[] => {
    const next = [...order]
    const at = next.indexOf(from)
    const onto = next.indexOf(to)
    if (at < 0 || onto < 0 || at === onto) {
        return order
    }
    next.splice(at, 1)
    next.splice(onto, 0, from)
    return next
}

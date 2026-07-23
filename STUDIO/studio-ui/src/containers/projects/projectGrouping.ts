import { readJson, writeJson } from '../../utils/localStore'
import type { Project } from '../../types/projects'

/** Group a level by the repository the project lives in. */
export const GROUP_BY_REPOSITORY = '[Repository]'
/** The level is not used; the levels below it are not used either. */
export const GROUP_BY_NONE = ''

/** What each of the three levels groups by: nothing, the repository, or a tag type name. */
export type GroupingLevels = [string, string, string]

export const NO_GROUPING: GroupingLevels = [GROUP_BY_NONE, GROUP_BY_NONE, GROUP_BY_NONE]

/** Where the browser keeps the grouping the user picked. */
const STORAGE_KEY = 'openl.projects.grouping'

const isGroupingLevels = (value: unknown): value is GroupingLevels =>
    Array.isArray(value) && value.length === 3 && value.every(level => typeof level === 'string')

export const loadGrouping = (): GroupingLevels => readJson(STORAGE_KEY, NO_GROUPING, isGroupingLevels)

export const saveGrouping = (levels: GroupingLevels): void => writeJson(STORAGE_KEY, levels)

/** The levels that are actually used: a level set to nothing ends the grouping. */
export const activeLevels = (levels: GroupingLevels): string[] => {
    const active: string[] = []
    for (const level of levels) {
        if (!level) {
            break
        }
        active.push(level)
    }
    return active
}

/**
 * What the list has to be filtered by to show exactly the projects of a group — the same filters the
 * facet rail sets, so clicking a group in the tree and ticking the facets by hand end up in one place.
 */
export interface NodeFilters {
    /** Repository ids. */
    repositories: string[]
    /** Tag filters, each written as `Type:Value` the way the list carries them in the URL. */
    tags: string[]
}

const EMPTY_FILTERS: NodeFilters = { repositories: [], tags: []}

/** One node of the tree: a group of projects, or a project itself. */
export interface GroupNode {
    key: string
    /** What the node is called — a repository name, a tag value, or the project name. */
    title: string
    /** The level the node groups by, absent for a project leaf. */
    groupedBy?: string
    /** The value of that level — a repository id or a tag value; empty for a project leaf. */
    value?: string
    /** The filters that select the projects of this group; empty for a project leaf. */
    filters: NodeFilters
    /** Set for a project leaf. */
    project?: Project
    children: GroupNode[]
}

/** The value a project carries for a grouping level: what to filter by, and what to call it. */
interface LevelValue {
    key: string
    label: string
}

const valueOf = (
    project: Project,
    level: string,
    repositoryName: (id: string) => string
): LevelValue | null => {
    if (level === GROUP_BY_REPOSITORY) {
        return { key: project.repository, label: repositoryName(project.repository) }
    }
    const tags = project.tags ?? {}
    const type = Object.keys(tags).find(name => name.toLowerCase() === level.toLowerCase())
    const value = type ? tags[type] : undefined
    return value ? { key: value, label: value } : null
}

const withLevel = (filters: NodeFilters, level: string, value: string): NodeFilters =>
    level === GROUP_BY_REPOSITORY
        ? { repositories: [...filters.repositories, value], tags: filters.tags }
        : { repositories: filters.repositories, tags: [...filters.tags, `${level}:${value}`]}

/**
 * Groups the projects into a tree, one level per grouping.
 *
 * A project that carries no value for a level stays at that level, beside the groups — the way the
 * grouped tree of earlier versions showed it, so a project is never hidden by a tag it does not have.
 *
 * The tree is built from projects already in the browser: expanding a node costs nothing.
 */
export const buildGroupTree = (
    projects: Project[],
    levels: string[],
    repositoryName: (id: string) => string,
    keyPrefix = 'grp',
    filters: NodeFilters = EMPTY_FILTERS
): GroupNode[] => {
    if (levels.length === 0) {
        return projects.map(project => projectNode(project, keyPrefix))
    }
    const [level, ...rest] = levels as [string, ...string[]]
    const groups = new Map<string, { label: string, projects: Project[] }>()
    const ungrouped: Project[] = []
    for (const project of projects) {
        const value = valueOf(project, level, repositoryName)
        if (value === null) {
            ungrouped.push(project)
            continue
        }
        const group = groups.get(value.key)
        if (group) {
            group.projects.push(project)
        } else {
            groups.set(value.key, { label: value.label, projects: [project]})
        }
    }
    const nodes: GroupNode[] = [...groups.entries()]
        .sort(([, left], [, right]) => left.label.localeCompare(right.label, undefined, { sensitivity: 'base' }))
        .map(([value, group]) => {
            const key = `${keyPrefix}/${level}=${value}`
            const groupFilters = withLevel(filters, level, value)
            return {
                key,
                title: group.label,
                groupedBy: level,
                value,
                filters: groupFilters,
                children: buildGroupTree(group.projects, rest, repositoryName, key, groupFilters),
            }
        })
    return [...nodes, ...ungrouped.map(project => projectNode(project, keyPrefix))]
}

const projectNode = (project: Project, keyPrefix: string): GroupNode => ({
    key: `${keyPrefix}/prj:${project.id}`,
    title: project.name,
    filters: EMPTY_FILTERS,
    project,
    children: [],
})

/** The keys of every group node holding the project, so the tree can open on it. */
export const pathToProject = (nodes: GroupNode[], projectId: string, trail: string[] = []): string[] | null => {
    for (const node of nodes) {
        if (node.project) {
            if (node.project.id === projectId) {
                return trail
            }
            continue
        }
        const found = pathToProject(node.children, projectId, [...trail, node.key])
        if (found) {
            return found
        }
    }
    return null
}

/** The keys of the groups above a node, so a node picked earlier can be shown again. */
export const pathToNode = (nodes: GroupNode[], key: string, trail: string[] = []): string[] | null => {
    for (const node of nodes) {
        if (node.key === key) {
            return trail
        }
        const found = pathToNode(node.children, key, [...trail, node.key])
        if (found) {
            return found
        }
    }
    return null
}

/** The node of that key, wherever it sits in the tree. */
export const findNode = (nodes: GroupNode[], key: string): GroupNode | undefined => {
    for (const node of nodes) {
        if (node.key === key) {
            return node
        }
        const found = findNode(node.children, key)
        if (found) {
            return found
        }
    }
    return undefined
}

/**
 * The branches of the tree a search matches.
 *
 * A node whose own name matches is kept whole, with everything under it: a repository or a tag value
 * answers the search with the projects it holds. Any other node is kept only for what matched inside it.
 */
export const searchTree = (nodes: GroupNode[], query: string): GroupNode[] => {
    const needle = query.trim().toLowerCase()
    if (!needle) {
        return nodes
    }
    const matched: GroupNode[] = []
    for (const node of nodes) {
        if (node.title.toLowerCase().includes(needle)) {
            matched.push(node)
            continue
        }
        const children = searchTree(node.children, needle)
        if (children.length > 0) {
            matched.push({ ...node, children })
        }
    }
    return matched
}

/** The keys of every group in the tree, so a search can show what it found without more clicks. */
export const groupKeys = (nodes: GroupNode[]): string[] =>
    nodes.flatMap(node => (node.project ? [] : [node.key, ...groupKeys(node.children)]))

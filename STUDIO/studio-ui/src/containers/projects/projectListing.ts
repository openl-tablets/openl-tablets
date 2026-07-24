import { ProjectStatus } from '../../constants/project'
import type { FacetCount, Project, ProjectStatusSummary, TagFacetSummary } from '../../types/projects'

/** The synthetic repository key a local-only project is filtered and counted under. */
export const LOCAL_REPO_KEY = '__local__'

/** The columns the list sorts by — exactly the columns the table shows. */
export type ProjectSort = 'name' | 'branch' | 'updated'

export type SortDirection = 'asc' | 'desc'

export interface ListingQuery {
    statuses: Set<string>
    repositories: Set<string>
    /** Tag filters written as `Type:Value`. */
    tags: Set<string>
}

const contains = (value: string | undefined, needle: string): boolean =>
    (value ?? '').toLowerCase().includes(needle)

/**
 * The projects matching the search, which the facet counts are scoped to as well. One box searches
 * everything at once: a project matches when the text is found in its name, its author, its branch, or
 * any of its tags — whichever of them the user happened to remember.
 */
export const searchProjects = (projects: Project[], query: string): Project[] => {
    const needle = query.trim().toLowerCase()
    if (!needle) {
        return projects
    }
    return projects.filter(project =>
        contains(project.name, needle)
        || contains(project.modifiedBy, needle)
        || contains(project.branch, needle)
        || Object.entries(project.tags ?? {})
            .some(([type, value]) => contains(type, needle) || contains(value, needle)))
}

/** The selected tag values grouped by type — the "wanted" set the filter matches every project against. */
const wantedTags = (tags: Set<string>): Map<string, Set<string>> => {
    const wanted = new Map<string, Set<string>>()
    for (const tag of tags) {
        const separator = tag.indexOf(':')
        if (separator < 1) {
            continue
        }
        const type = tag.slice(0, separator).toLowerCase()
        const value = tag.slice(separator + 1).toLowerCase()
        const values = wanted.get(type)
        if (values) {
            values.add(value)
        } else {
            wanted.set(type, new Set([value]))
        }
    }
    return wanted
}

/** Whether the project carries one of the wanted values for every tag type that has a selection. */
const matchesTags = (project: Project, wanted: Map<string, Set<string>>): boolean => {
    if (wanted.size === 0) {
        return true
    }
    const carried = new Map(Object.entries(project.tags ?? {}).map(([type, value]) => [type.toLowerCase(), value.toLowerCase()]))
    // Several values of one type read as "either of them", different types as "all of them".
    return [...wanted.entries()].every(([type, values]) => {
        const value = carried.get(type)
        return value !== undefined && values.has(value)
    })
}

const matchesRepositories = (project: Project, repositories: Set<string>): boolean =>
    repositories.size === 0
    // A local project belongs to the Local facet, not to the repository it was checked out from — the same
    // bucket countFacets counts it in, so the facet's count and the rows it lists always agree.
    || repositories.has(project.status === ProjectStatus.Local ? LOCAL_REPO_KEY : project.repository)

/**
 * The already-searched projects the list keeps for the facet part of the query — repositories, tags, and
 * statuses. Kept apart from {@link searchProjects} so a caller that also counts facets searches only once.
 */
export const refineProjects = (searched: Project[], query: ListingQuery): Project[] => {
    const wanted = wantedTags(query.tags)
    return searched.filter(project =>
        matchesRepositories(project, query.repositories)
        && matchesTags(project, wanted)
        // Without a status filter a deleted project stays out of the list, as the API leaves it out.
        && (query.statuses.size === 0
            ? project.status !== ProjectStatus.Deleted
            : query.statuses.has(project.status)))
}

const byName = (left: Project, right: Project): number =>
    left.name.localeCompare(right.name, undefined, { sensitivity: 'base' })

export const sortProjects = (projects: Project[], sort: ProjectSort, direction: SortDirection = 'asc'): Project[] => {
    const sorted = [...projects]
    const directed = (comparison: number) => (direction === 'desc' ? -comparison : comparison)
    if (sort === 'branch') {
        return sorted.sort((left, right) =>
            directed((left.branch ?? '').localeCompare(right.branch ?? '', undefined, { sensitivity: 'base' }))
            || byName(left, right))
    }
    if (sort === 'updated') {
        // Sorted by the date alone; a project with no timestamp goes last either way.
        return sorted.sort((left, right) => {
            const leftAt = left.modifiedAt ? Date.parse(left.modifiedAt) : Number.NaN
            const rightAt = right.modifiedAt ? Date.parse(right.modifiedAt) : Number.NaN
            if (Number.isNaN(leftAt) && Number.isNaN(rightAt)) {
                return byName(left, right)
            }
            if (Number.isNaN(leftAt)) {
                return 1
            }
            if (Number.isNaN(rightAt)) {
                return -1
            }
            return directed(leftAt - rightAt) || byName(left, right)
        })
    }
    return sorted.sort((left, right) => directed(byName(left, right)))
}

/** The summary field each project status is counted in. */
const STATUS_FIELD: Record<ProjectStatus, keyof ProjectStatusSummary> = {
    [ProjectStatus.Local]: 'local',
    [ProjectStatus.Opened]: 'opened',
    [ProjectStatus.Editing]: 'editing',
    [ProjectStatus.ViewingVersion]: 'viewingVersion',
    [ProjectStatus.Closed]: 'closed',
    [ProjectStatus.Deleted]: 'deleted',
}

/** The count a status summary holds for a project status, or 0 when there is no summary. */
export const statusCount = (counts: ProjectStatusSummary | undefined, status: ProjectStatus): number =>
    counts ? counts[STATUS_FIELD[status]] : 0

/**
 * The facet counts of the rail, counted over the search scope and ignoring the picked facets — the same
 * scope the API counts, so a multi-select rail does not collapse as values are ticked.
 */
export const countFacets = (scope: Project[], repositoryName: (id: string) => string): {
    statusCounts: ProjectStatusSummary
    repositoryCounts: FacetCount[]
    tagCounts: TagFacetSummary[]
} => {
    const statusCounts: ProjectStatusSummary = {
        local: 0,
        opened: 0,
        editing: 0,
        viewingVersion: 0,
        closed: 0,
        deleted: 0,
    }
    const repositories = new Map<string, number>()
    const tags = new Map<string, Map<string, number>>()
    for (const project of scope) {
        const field = STATUS_FIELD[project.status]
        if (field) {
            statusCounts[field]++
        }
        const repositoryId = project.status === ProjectStatus.Local ? LOCAL_REPO_KEY : project.repository
        repositories.set(repositoryId, (repositories.get(repositoryId) ?? 0) + 1)
        for (const [type, value] of Object.entries(project.tags ?? {})) {
            if (!type || !value) {
                continue
            }
            const values = tags.get(type) ?? new Map<string, number>()
            values.set(value, (values.get(value) ?? 0) + 1)
            tags.set(type, values)
        }
    }
    const repositoryCounts = [...repositories.entries()]
        .map(([id, count]) => ({ id, name: id === LOCAL_REPO_KEY ? 'Local' : repositoryName(id), count }))
        .sort((left, right) => left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }))
    const tagCounts = [...tags.entries()]
        .sort(([left], [right]) => left.localeCompare(right, undefined, { sensitivity: 'base' }))
        .map(([type, values]) => ({
            type,
            values: [...values.entries()]
                .map(([value, count]) => ({ id: value, name: value, count }))
                .sort((left, right) => left.name.localeCompare(right.name, undefined, { sensitivity: 'base' })),
        }))
    return { statusCounts, repositoryCounts, tagCounts }
}

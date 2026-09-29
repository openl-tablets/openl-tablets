import { ProjectStatus } from '../../constants/project'
import type { FacetCount, Project, ProjectStatusSummary, TagFacetSummary } from '../../types/projects'

/** The synthetic repository key a local-only project is filtered and counted under. */
export const LOCAL_REPO_KEY = '__local__'

/** The columns the list sorts by — exactly the columns the table shows. */
export type ProjectSort = 'name' | 'branch' | 'updated'

export type SortDirection = 'asc' | 'desc'

/** The values picked in the Filters view of the rail — what the list and the tree beside it are filtered by. */
export interface ListingQuery {
    statuses: Set<string>
    repositories: Set<string>
    /** Tag filters written as `Type:Value`. */
    tags: Set<string>
    /** Current-branch filters — the branch each project is open on. */
    branches: Set<string>
}

/** The parameters of the list's URL that carry the picks of the Filters view, and nothing else. */
export const FILTER_PARAMS = ['status', 'repo', 'tags', 'branch']

const listParam = (params: URLSearchParams, key: string): Set<string> =>
    new Set((params.get(key) ?? '').split(',').filter(Boolean))

/** The picks a URL of the list carries — or the stored state of the list, which has the same shape. */
export const listingQueryOf = (params: URLSearchParams): ListingQuery => ({
    statuses: listParam(params, 'status'),
    repositories: listParam(params, 'repo'),
    tags: listParam(params, 'tags'),
    // Branch names may legally contain a comma, so branch filters ride as repeated params (branch=a&branch=b)
    // rather than one comma-joined value like the other facets, which can never collide with their values.
    branches: new Set(params.getAll('branch').filter(Boolean)),
})

/** Whether any value is picked in the Filters view. */
export const isFiltered = (query: ListingQuery): boolean =>
    query.statuses.size > 0 || query.repositories.size > 0 || query.tags.size > 0 || query.branches.size > 0

/** The workspace states of a project the user has open, whatever branch they took it from. */
const OPEN_STATES: ReadonlySet<string> = new Set([
    ProjectStatus.Opened,
    ProjectStatus.Editing,
    ProjectStatus.ViewingVersion,
])

/**
 * Whether the project is listed while no branch is picked. The list then keeps to the main line of each
 * repository: the projects its default branch holds, on whatever branch the user switched them to, and every
 * project of a repository without branches, a local one included.
 *
 * A project the user has open stays listed too, even when it lives only outside the default branch. Any other
 * project of another branch shows once its branch is picked.
 */
export const isInDefaultView = (project: Project): boolean =>
    project.inDefaultBranch === true
    || OPEN_STATES.has(project.status)
    || project.repositoryInfo?.features?.branches !== true

/**
 * The projects the picked facets are applied to: those of the default view while no branch is picked (see
 * {@link isInDefaultView}), and every project once one is — a picked branch replaces the rule.
 */
export const listingScope = (projects: Project[], branches: Set<string>): Project[] =>
    branches.size > 0 ? projects : projects.filter(isInDefaultView)

const contains = (value: string | undefined, needle: string): boolean =>
    (value ?? '').toLowerCase().includes(needle)

/**
 * The projects matching the search, which the facet counts are scoped to as well. One box searches
 * everything at once: a project matches when the text is found in its name, its author, its current branch, or
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

const matchesBranches = (project: Project, branches: Set<string>): boolean =>
    branches.size === 0 || (project.branch != null && branches.has(project.branch))

/**
 * The projects of a scope the list keeps for the facet part of the query — repositories, branches, tags, and
 * statuses. Kept apart from {@link searchProjects} and {@link listingScope}, so a caller that also counts facets
 * searches and scopes only once.
 */
export const refineProjects = (scope: Project[], query: ListingQuery): Project[] => {
    const wanted = wantedTags(query.tags)
    return scope.filter(project =>
        matchesRepositories(project, query.repositories)
        && matchesBranches(project, query.branches)
        && matchesTags(project, wanted)
        // Without a status filter a deleted project stays out of the list, as the API leaves it out.
        && (query.statuses.size === 0
            ? project.status !== ProjectStatus.Deleted
            : query.statuses.has(project.status)))
}

/** The projects the picks of the Filters view select — what the list shows before its search and paging. */
export const selectProjects = (projects: Project[], query: ListingQuery): Project[] =>
    refineProjects(listingScope(projects, query.branches), query)

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

/** A branch facet value with the marks it carries — the default badge and the protected shield. */
export interface BranchFacetCount extends FacetCount {
    isDefault: boolean
    isProtected: boolean
}

/**
 * The status, repository and tag counts of the rail, counted over the scope and ignoring the picked facets — the
 * same way the API counts, so a multi-select rail does not collapse as values are ticked.
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

/**
 * The branch counts of the rail: the branch each project is on. Counted apart from {@link countFacets}, because
 * the rail offers every branch — a branch whose projects the default view leaves out has to stay pickable.
 */
export const countBranches = (projects: Project[]): BranchFacetCount[] => {
    const branches = new Map<string, { count: number, isDefault: boolean, isProtected: boolean }>()
    for (const project of projects) {
        if (project.branch) {
            const branch = branches.get(project.branch)
            branches.set(project.branch, {
                count: (branch?.count ?? 0) + 1,
                isDefault: (branch?.isDefault ?? false) || (project.branchDefault ?? false),
                isProtected: (branch?.isProtected ?? false) || (project.branchProtected ?? false),
            })
        }
    }
    return [...branches.entries()]
        .map(([name, info]) => ({ id: name, name, count: info.count, isDefault: info.isDefault, isProtected: info.isProtected }))
        .sort((left, right) => left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }))
}

import { Fragment, useCallback, useEffect, useMemo, useRef, useState, type ComponentProps } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { errorHandler } from '../utils/errorHandling'
import { creatableRepositories } from '../utils/repositoryFeatures'
import {
    NavigationType,
    Outlet,
    useMatch,
    useNavigate,
    useNavigationType,
    useSearchParams,
    type NavigateOptions,
} from 'react-router'
import { useTranslation } from 'react-i18next'
import { App, Alert, Button, Empty, Skeleton, Spin } from 'antd'
import { ClearOutlined, LoadingOutlined, PlusOutlined } from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import {
    getProject,
    isProjectModifiedConflict,
    setProjectStatus,
} from '../services/repositories'
import { ProjectStatus } from '../constants/project'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { mavenArtifactOf, mavenIdOf, projectCoordinatesOf, projectFileRoute, projectRoute } from '../services/projectId'
import { mavenIdOfParams, mavenProjectOf } from '../services/mavenDependencies'
import { readDesignRepositories, readMavenProjects, readTagTypes } from '../services/keptReads'
import { DEFAULT_PAGE_SIZE } from '../constants/ui'
import { HeaderActions } from '../components/HeaderActions'
import { ListFooter } from '../components/ListFooter'
import { withPage } from '../utils/pageParams'
import type { Repository, RepositoryInfo } from '../types/repositories'
import type {
    FacetCount,
    Project,
    ProjectIndexHealth,
    ProjectStatusSummary,
    TagFacetSummary,
} from '../types/projects'
import { ProjectsFilterRail } from './projects/ProjectsFilterRail'
import { ProjectsRail } from './projects/ProjectsRail'
import type { NodeFilters } from './projects/projectGrouping'
import { ProjectsTable } from './projects/ProjectsTable'
import type { ProjectListHandlers } from './projects/ProjectRowActions'
import type { RowBusyId } from './projects/projectActions'
import {
    countBranches,
    countFacets,
    FILTER_PARAMS,
    listingQueryOf,
    type FilterParam,
    type ListingQuery,
    listingScope,
    pickedIn,
    writePicked,
    refineProjects,
    searchProjects,
    sortProjects,
    type BranchFacetCount,
    PROJECT_SORTS,
    type ProjectSort,
    type SortDirection,
} from './projects/projectListing'
import { getProjectIndex, invalidateProjectIndex, isProjectIndexStale, projectSignature } from '../services/projectIndex'
import { useLoadGeneration, useWindowFocus, useWorkspaceChanges } from '../hooks'
import { useSearchScope } from '../hooks/useSearchScope'
import { compileColorsOf } from './projects/projectsTheme'
import { useSharedStyles } from './projects/sharedStyles'
import { BusyVeil } from './projects/BusyVeil'
import { NewProjectModal } from './projects/NewProjectModal'
import { CopyProjectModal } from './projects/CopyProjectModal'
import { ExportProjectModal } from './projects/ExportProjectModal'
import { OpenRevisionModal } from './projects/OpenRevisionModal'
import { openDeleteBranchDialog, openMergeDialog } from './projects/branchDialogs'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { openCompareWindow } from './projects/compare'
import { loadProjectFilters, saveProjectFilters } from './projects/filterStorage'
import type { ProjectRail, ProjectRailState } from './projects/projectRail'
import { SaveProjectModal } from './projects/SaveProjectModal'
import { DiscardChangesModal } from './DiscardChangesModal'
import { PublishTagTypes, TagTypeOrder } from './tags/tagOrder'
import {
    isPushFresherThanRead,
    subscribeWorkspaceProjectStatuses,
    type ProjectCompileState,
    type ProjectStatusUpdate,
} from '../services/projectStatus'
import { useReadAgain } from '../hooks/useReadAgain'

/** The facet counts of the rail, counted in the browser from the projects it already holds. */
interface ProjectFacets {
    statusCounts: ProjectStatusSummary
    repositoryCounts: FacetCount[]
    tagCounts: TagFacetSummary[]
    branchCounts: BranchFacetCount[]
}

const SEARCH_DEBOUNCE_MS = 300

const useStyles = createStyles(({ css, token }) => ({
    compileStrip: css`
        display: inline-flex;
        flex-wrap: wrap;
        align-items: center;
        gap: ${token.marginXXS}px ${token.marginSM}px;
    `,
    compileItem: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        color: ${token.colorTextSecondary};
    `,
    compileNum: css`
        color: ${token.colorText};
        font-weight: 600;
        font-variant-numeric: tabular-nums;
    `,
    /** The indexing note sits in the summary line, so it reads as part of it rather than as a button. */
    indexingToggle: css`
        height: auto;
        padding: 0;
        font-size: ${token.fontSizeSM}px;
    `,
    /** The indexing notice opens above the summary line that offers it. */
    indexingBanner: css`
        margin: ${token.marginXS}px ${token.margin}px;
    `,
    content: css`
        position: relative;
        overflow: hidden;
    `,
    scroll: css`
        height: 100%;
        overflow: auto;
    `,
}))

/**
 * Compile states shown in the summary line under the list, ordered so what needs attention comes first. Clean
 * ({@code ok}) and not-yet-compiled ({@code idle}) projects are omitted — only compilation that needs
 * attention is summarised.
 */
const COMPILE_SUMMARY_ORDER: ProjectCompileState[] = ['errors', 'warnings', 'compiling']

/** What makes the list visibly different — the shared per-project signature, over every row. */
const listSignature = (projects: Project[]): string => JSON.stringify(projects.map(projectSignature))

/**
 * The compile statuses to show after a snapshot arrives: the snapshot, except where a status pushed
 * over the socket while the read was in flight is fresher — that one stays on top.
 */
const overlayFreshStatuses = (
    snapshot: ProjectStatusUpdate[],
    previous: ProjectStatusUpdate[],
    pushedAt: Map<string, number>,
    readStartedAt: number
): ProjectStatusUpdate[] => {
    const pushed = previous.filter(status => isPushFresherThanRead(pushedAt.get(status.projectId) ?? 0, readStartedAt))
    const overridden = new Set(pushed.map(status => status.projectId))
    return [...snapshot.filter(status => !overridden.has(status.projectId)), ...pushed]
}

/** Replaces the project's status in place; a repeated identical status keeps the same array. */
const upsertStatus = (previous: ProjectStatusUpdate[], update: ProjectStatusUpdate): ProjectStatusUpdate[] => {
    const index = previous.findIndex(status => status.projectId === update.projectId)
    if (index < 0) {
        return [...previous, update]
    }
    if (JSON.stringify(previous[index]) === JSON.stringify(update)) {
        return previous
    }
    const next = [...previous]
    next[index] = update
    return next
}

const parsePositiveInt = (value: string | null, fallback: number): number => {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

const useDebouncedValue = (value: string, delay: number): string => {
    const [debounced, setDebounced] = useState(value)
    useEffect(() => {
        const timeout = window.setTimeout(() => setDebounced(value), delay)
        return () => window.clearTimeout(timeout)
    }, [delay, value])
    return debounced
}

/**
 * Builds the filter rail for the rail's render prop. A plain function at the top level, so the render
 * prop hands over data instead of defining a component inside the screen on every render.
 */
const renderFilterRail = (props: ComponentProps<typeof ProjectsFilterRail>) => <ProjectsFilterRail {...props} />

/**
 * The Projects tab home: the projects the user can see, as one flat, filterable list. A left rail carries
 * repository, branch, status and tag-type facets; repositories are a facet, not a hierarchy. Search, facets, sort
 * and view live in the URL, so a filtered view survives reloads and can be shared. Selecting a row opens
 * the project's workspace page.
 *
 * Until a branch is picked, the list keeps to the default branch of each repository, as described by
 * {@link listingScope}; picking a branch shows the projects on it.
 */
export const ProjectsHome = () => {
    const { notification } = App.useApp()
    const { t, i18n } = useTranslation('repository')
    const locale = i18n.resolvedLanguage ?? i18n.language
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const token = useTheme()
    const navigate = useNavigate()
    const [urlParams, setUrlParams] = useSearchParams()
    // The project the address names, shown beside the rail in place of the list. The screen stays while the reader
    // steps from the list to a project and between projects, so the rail keeps its view, its tree and what it read.
    // A project downloaded from the Maven repository is shown here too, at the address its coordinates make.
    const mavenShown = useMatch('/maven/:groupId/:artifactId/:version')?.params
    const shownProjectId = useMatch('/projects/:projectId')?.params.projectId ?? mavenIdOfParams(mavenShown)
    const projectShown = shownProjectId !== undefined
    // What the project shown tells the tree beside it. A report about the project the reader came from is stale: until
    // the page of the new one reports, the tree marks the project the address names, with the changes it read itself.
    const [projectRail, setProjectRail] = useState<ProjectRailState>({})
    const shownRail = projectRail.address === shownProjectId ? projectRail : {}
    // Bumped whenever the page of the project shown read it again, so the tree beside it reads its files again too.
    const [reloadToken, setReloadToken] = useState(0)
    // A plain visit to the list restores the filters of the last one: the first visit to the screen, and every way back
    // from a project by a link that names no parameters, as the crumb of a project and the tab of the header do. A
    // link with its own parameters describes what its sender wanted to show, and the list the reader emptied stays
    // empty, so both are left alone. Nothing is fetched until the first visit is settled.
    const [restoring, setRestoring] = useState(true)
    const navigationType = useNavigationType()
    const plainVisit = !projectShown && urlParams.size === 0
        && (restoring || navigationType !== NavigationType.Replace)
    // While a project is shown the address is the project's own: the list goes on with the parameters it was left
    // with, and changing them leads back to the list. A plain visit shows them at once, while the address takes them.
    const heldParams = useMemo(
        () => (projectShown || plainVisit ? loadProjectFilters() : null),
        [plainVisit, projectShown]
    )
    const params = heldParams ?? urlParams
    const setParams = useCallback((
        update: URLSearchParams | ((previous: URLSearchParams) => URLSearchParams),
        options?: NavigateOptions
    ) => {
        if (heldParams === null) {
            setUrlParams(update, options)
            return
        }
        const next = typeof update === 'function' ? update(new URLSearchParams(heldParams)) : update
        if (!projectShown) {
            setUrlParams(next, options)
            return
        }
        saveProjectFilters(next)
        void navigate(next.size > 0 ? `/projects?${next.toString()}` : '/projects')
    }, [heldParams, navigate, projectShown, setUrlParams])
    useEffect(() => {
        if (!projectShown && heldParams !== null && heldParams.size > 0) {
            setUrlParams(heldParams, { replace: true })
        } else {
            setRestoring(false)
        }
    }, [heldParams, projectShown, setUrlParams])

    // Persisted after the typing settles: the search box writes to the URL on every keystroke.
    useEffect(() => {
        const timeout = window.setTimeout(() => saveProjectFilters(params), SEARCH_DEBOUNCE_MS)
        return () => window.clearTimeout(timeout)
    }, [params])
    const [repositories, setRepositories] = useState<Repository[]>([])
    const [allProjects, setAllProjects] = useState<Project[]>([])
    // The projects of the Maven repository the workspace depends on: the tree lists them under a repository of their
    // own, read-only. They are read again whenever the projects are, since opening a project may name new ones.
    const [mavenProjects, setMavenProjects] = useState<Project[]>([])
    const [mavenListRead, setMavenListRead] = useState(false)
    // A load that reads the same projects again needs no new list of them, and none is asked for before the projects
    // are read: the empty list the screen starts with would only be followed by a second read.
    const projectsSignature = useMemo(() => listSignature(allProjects), [allProjects])
    const [projectsRead, setProjectsRead] = useState(false)
    const mavenReadAgain = useReadAgain('maven')
    useEffect(() => {
        if (!projectsRead) {
            return undefined
        }
        let dropped = false
        readMavenProjects({ fresh: mavenReadAgain() })
            .then(read => !dropped && setMavenProjects(read.map(mavenProjectOf)))
            .catch(() => !dropped && setMavenProjects([]))
            .finally(() => !dropped && setMavenListRead(true))
        return () => {
            dropped = true
        }
    }, [projectsRead, projectsSignature, mavenReadAgain])
    // A project of the Maven repository opened by its address, which the workspace does not depend on, is listed there
    // too while it is shown, so its files unfold in the tree as those of the others do.
    const [addressedMaven, setAddressedMaven] = useState<Project | null>(null)
    const addressedCoordinates = shownProjectId === undefined ? undefined : mavenArtifactOf(shownProjectId)
    const addressedListed = mavenProjects.some(project => project.id === shownProjectId)
    useEffect(() => {
        const coordinates = projectCoordinatesOf(addressedCoordinates)
        if (coordinates === undefined || !mavenListRead || addressedListed) {
            return undefined
        }
        let dropped = false
        // The Projects API names the project without compiling it, as the Maven one would.
        getProject(mavenIdOf(coordinates), { fields: 'name' }, LOCAL_LOAD_API_OPTIONS)
            .then(read => !dropped && setAddressedMaven(mavenProjectOf({ ...coordinates, name: read.name })))
            .catch(() => !dropped && setAddressedMaven(null))
        return () => {
            dropped = true
        }
    }, [addressedCoordinates, addressedListed, mavenListRead])
    const railProjects = useMemo(() => {
        const addressedShown = addressedMaven !== null && addressedMaven.id === shownProjectId && !addressedListed
        const addressed = addressedShown ? [addressedMaven] : []
        const downloaded = [...mavenProjects, ...addressed]
        return downloaded.length === 0 ? allProjects : [...allProjects, ...downloaded]
    }, [addressedListed, addressedMaven, allProjects, mavenProjects, shownProjectId])
    const [compileStatuses, setCompileStatuses] = useState<ProjectStatusUpdate[]>([])
    const [projectIndexHealth, setProjectIndexHealth] = useState<Record<string, ProjectIndexHealth>>({})
    // The configured tag types, in the order an administrator arranged them in: the tags of the projects follow it.
    const [configuredTagTypes, setConfiguredTagTypes] = useState<string[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [createOpen, setCreateOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    const [openRevisionFor, setOpenRevisionFor] = useState<Project | null>(null)
    const [exportSource, setExportSource] = useState<Project | null>(null)
    // What each project is busy with, keyed by project: an action running on one row must not gate — or
    // un-gate — another. Work on one project never stops work on the rest.
    const [pending, setPending] = useState<Record<string, RowBusyId>>({})
    const [saveTarget, setSaveTarget] = useState<Project | null>(null)
    const [discardCloseTarget, setDiscardCloseTarget] = useState<Project | null>(null)
    // Bumped on every load so a stale response never overwrites a fresher one.
    const loads = useLoadGeneration()
    // When each project's compile status last arrived by push — a load never overwrites a fresher push.
    const statusPushedAt = useRef(new Map<string, number>())

    const search = params.get('q') ?? ''
    // No sort in the URL means the default name order — shown without an arrow until a header is clicked.
    const sort = PROJECT_SORTS.find(column => column === params.get('sort')) ?? null
    const direction: SortDirection = params.get('dir') === 'desc' ? 'desc' : 'asc'
    const pageSize = parsePositiveInt(params.get('size'), DEFAULT_PAGE_SIZE)
    const requestedPage = parsePositiveInt(params.get('page'), 1)
    const debouncedSearch = useDebouncedValue(search, SEARCH_DEBOUNCE_MS)

    // The picks are read from their own parameters alone, so typing in the search box keeps the same picks — and
    // the tree beside the list, which follows them, keeps its shape.
    const picked = picksOf(params)
    const query = useMemo(() => listingQueryOf(new URLSearchParams(picked)), [picked])
    // A group picked in the tree filters the list and leaves the tree as it was: the tree keeps the picks it had before
    // the group until the picks change otherwise — in the Filters view, or by clearing them. The picks of every group
    // since are kept: the address changes a render after the pick, and the render between them must still find the
    // picks of the group before, or the tree would narrow to them for that render and draw all its rows anew.
    const [groupPicks, setGroupPicks] = useState<Record<string, ListingQuery>>({})
    const treeNarrowBy = groupPicks[picked] ?? query
    const { statuses, repositories: repos, tags, branches } = query
    const repositoryName = useCallback(
        (id: string) => repositories.find(repo => repo.id === id)?.name ?? id,
        [repositories]
    )
    // Repositories whose cross-branch index is still building: their projects show only the base branch so far.
    const indexingRepositories = useMemo(
        () => Object.entries(projectIndexHealth)
            .filter(([, health]) => health.state === 'indexing')
            .map(([id]) => repositoryName(id)),
        [projectIndexHealth, repositoryName]
    )
    // The summary line only counts the repositories; their names are shown on demand, so the note stays out of
    // the way of a screen the user came to for the list. With nothing listed yet the full-screen indexing
    // state (see content()) already says it, so the note would only repeat it.
    const [indexingShown, setIndexingShown] = useState(false)
    const showIndexingNote = indexingRepositories.length > 0 && allProjects.length > 0
    // Indexing finished: forget that the names were open, so a later round does not expand itself unasked.
    useEffect(() => {
        if (indexingRepositories.length === 0) {
            setIndexingShown(false)
        }
    }, [indexingRepositories.length])

    // The search scope, shared by the facet counts and the list so the text search runs only once.
    const searched = useMemo(() => searchProjects(allProjects, debouncedSearch), [allProjects, debouncedSearch])
    // Without a picked branch the list keeps to the default view, and so do the counts of the rail. The branch
    // counts cover every searched project instead, so a branch whose projects the default view leaves out can
    // still be picked.
    const scope = useMemo(() => listingScope(searched, branches), [branches, searched])
    // What the rail counts: the scope, with the picked facets ignored — the way the API counted it.
    const facets = useMemo<ProjectFacets>(
        () => ({
            ...countFacets(scope, repositoryName, locale, configuredTagTypes),
            branchCounts: countBranches(searched, locale),
        }),
        [configuredTagTypes, locale, repositoryName, scope, searched]
    )
    // Each sort sorts the same filtered list again, in place. The sort is stable, so the projects a column ranks alike
    // keep the order the sort before it gave them. The list is filtered anew, in the order of the names, only when the
    // data or the filters change.
    const refined = useMemo(() => sortProjects(refineProjects(scope, query), 'name', locale), [locale, query, scope])
    const matched = useMemo(
        () => [...sortProjects(refined, sort ?? 'name', locale, direction)],
        [direction, locale, refined, sort]
    )

    const totalProjects = matched.length
    const totalPages = Math.max(1, Math.ceil(totalProjects / pageSize))
    const currentPage = Math.min(requestedPage, totalPages)
    const projects = useMemo(
        () => matched.slice((currentPage - 1) * pageSize, currentPage * pageSize),
        [currentPage, matched, pageSize]
    )

    const setParam = useCallback((key: string, value: string | null, resetPage = false) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            if (value === null || value === '') {
                next.delete(key)
            } else {
                next.set(key, value)
            }
            if (resetPage) {
                next.delete('page')
            }
            return next
        }, { replace: true })
    }, [setParams])

    // A text column starts ascending and the date column newest-first; picking the same column again
    // flips it. The sort only enters the URL on a click, so no arrow shows before one.
    const sortBy = useCallback((column: ProjectSort) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            const active = prev.get('sort') === column
            const descending = active ? prev.get('dir') !== 'desc' : column === 'updated'
            if (descending) {
                next.set('dir', 'desc')
            } else {
                next.delete('dir')
            }
            next.set('sort', column)
            next.delete('page')
            return next
        }, { replace: true })
    }, [setParams])

    // A pick of the Filters view narrows the tree again.
    const pickFilters = useCallback((param: FilterParam, values: string[], picked: boolean) => {
        setGroupPicks({})
        setParams(prev => {
            const next = new URLSearchParams(prev)
            const chosen = pickedIn(prev, param)
            values.forEach(value => (picked ? chosen.add(value) : chosen.delete(value)))
            writePicked(next, param, chosen)
            next.delete('page')
            return next
        }, { replace: true })
    }, [setParams])

    // The whole workspace is read once and kept in the browser: filtering, sorting and paging happen
    // here, so a facet click or a page step costs nothing and the server is not asked again. The facet
    // counts — the expensive part of the list response — are counted from the same snapshot.
    // Read with every load of the projects, so a change an administrator made meanwhile reaches the list, the rail and
    // the tree together. Kept as it was while the names stay the same, and when they cannot be read.
    const tagTypeReads = useRef(0)
    // Names read by a part of the screen, such as the Overview of a project, are the latest read: one of the screen's
    // own still under way is older, and is dropped when it answers.
    const publishTagTypes = useCallback((names: readonly string[]) => {
        tagTypeReads.current++
        setConfiguredTagTypes(known => (known.join('\n') === names.join('\n') ? known : [...names]))
    }, [])
    const readConfiguredTagTypes = useCallback((fresh: boolean) => {
        const read = ++tagTypeReads.current
        return readTagTypes({ fresh })
            .then(types => {
                // An earlier read answering after a later one is dropped: the later one holds the configuration.
                if (read === tagTypeReads.current) {
                    publishTagTypes(types.map(type => type.name))
                }
            })
            .catch((e: unknown) => errorHandler.logError(e instanceof Error ? e : new Error(errorMessage(e)), {
                message: 'Could not read the tag types; the names read before are kept',
            }))
    }, [publishTagTypes])

    const load = useCallback((refresh = false, { silent = false } = {}) => {
        // A refresh asks the server again; a first load takes what the screens of a project kept, if anything.
        void readConfiguredTagTypes(refresh)
        const { generation, startedAt } = loads.start(silent)
        if (!silent) {
            setLoading(true)
        }
        if (refresh) {
            invalidateProjectIndex()
        }
        return Promise.all([readDesignRepositories({ fresh: refresh }), getProjectIndex()])
            .then(([repos_, index]): Project[] | null => {
                if (!loads.isLatest(generation)) {
                    return null
                }
                setRepositories(repos_)
                setAllProjects(index.projects)
                setProjectsRead(true)
                setProjectIndexHealth(index.projectIndexHealth)
                setCompileStatuses(previous => overlayFreshStatuses(index.statuses, previous, statusPushedAt.current, startedAt))
                setError(null)
                return index.projects
            })
            .catch((e: unknown): Project[] | null => {
                // A failed silent re-read keeps the snapshot on screen: it was the answer a moment ago.
                // A reload the user waits for still reports its failure once a quiet one has overtaken
                // it, because the quiet one reports nothing and the action would look successful.
                if (loads.ownsSpinner(generation)) {
                    setError(errorMessage(e))
                }
                return null
            })
            .finally(() => {
                // The spinner is hidden by the reload it belongs to, even when a quiet re-read has
                // started behind it: a quiet one has no spinner to hide and would leave this one up.
                if (loads.ownsSpinner(generation)) {
                    setLoading(false)
                }
            })
    }, [loads, readConfiguredTagTypes])

    // What the page of a project reports through: what it shows, and that it read its project again. A read again has
    // the tree read the files of the project again, and the list read the projects quietly: an action of the page, as
    // a close or a branch switch, changes the project the list holds, and the ping it causes never reaches this tab.
    const rail = useMemo<ProjectRail>(() => ({
        report: setProjectRail,
        reloaded: () => {
            setReloadToken(token => token + 1)
            void load(true, { silent: true })
        },
    }), [load])

    useEffect(() => {
        if (restoring) {
            return
        }
        // A snapshot left over from an earlier visit paints instantly and is not read again: every change since,
        // made here or told by the server while this screen was away, has dropped it, and so has its age.
        void load()
    }, [load, restoring])

    // The backend pings when the workspace or a repository changed — another session, another
    // user, another client. The list re-reads behind the scenes and swaps the fresh answer in. The user is
    // told only when the refresh actually changed the list: a ping echoing their own action lands on
    // an already-fresh snapshot and stays quiet.
    // A ping landing while a row action runs waits for it: the refresh would otherwise supersede the
    // read the action is waiting for and unlock the row over the state before it. A project shown in place of the list
    // tells of its own changes, and the list it hides tells of none.
    useWorkspaceChanges(() => {
        const before = listSignature(allProjects)
        void load(true, { silent: true }).then(loaded => {
            if (!projectShown && loaded !== null && listSignature(loaded) !== before) {
                notification.info({ title: t('home.live_synced') })
            }
        })
    }, { holdWhile: Object.keys(pending).length > 0 })

    // The staleness policy behind the pings: coming back to a tab whose snapshot outlived its trust
    // window (pings can be lost while a laptop sleeps) re-reads it quietly.
    useWindowFocus(() => {
        if (isProjectIndexStale()) {
            void load(true, { silent: true })
        }
    })

    // One subscription feeds the compile dots of every row: each pushed status names its own project,
    // and it replaces that project's snapshot entry in place.
    useEffect(() => {
        const subscription = subscribeWorkspaceProjectStatuses(update => {
            statusPushedAt.current.set(update.projectId, Date.now())
            setCompileStatuses(previous => upsertStatus(previous, update))
        })
        return () => subscription.unsubscribe()
    }, [])

    useEffect(() => {
        if (!projectShown && !loading && totalProjects > 0 && requestedPage > totalPages) {
            setParam('page', totalPages === 1 ? null : String(totalPages))
        }
    }, [loading, projectShown, requestedPage, setParam, totalPages, totalProjects])

    // The list is searched from the header; the text lives in the address, so a link carries it. A project shown in
    // its place has searches of its own, and the tree beside it offers itself.
    useSearchScope(projectShown
        ? null
        : {
            id: 'projects',
            label: t('common:search.scopes.projects'),
            onChange: value => setParam('q', value, true),
            placeholder: t('home.search_placeholder'),
            value: search,
        })

    const localRepositoryInfo = useMemo<RepositoryInfo>(
        () => ({ features: { branches: false, searchable: false, mappedFolders: false }, name: t('home.local'), type: 'repo-file' }),
        [t]
    )
    const creatableRepos = useMemo(() => creatableRepositories(repositories), [repositories])
    const compileStatusByProject = useMemo(
        () => new Map<string, ProjectStatusUpdate>(compileStatuses.map(status => [status.projectId, status])),
        [compileStatuses]
    )
    // Compilation health of the projects the server reported a live state for (the active workspace).
    const compileTally = useMemo(() => {
        const tally: Record<ProjectCompileState, number> = {
            idle: 0,
            compiling: 0,
            ok: 0,
            warnings: 0,
            errors: 0,
            cancelled: 0,
        }
        for (const status of compileStatuses) {
            tally[status.compileState] += 1
        }
        return tally
    }, [compileStatuses])

    // Each project reports its own repository, so the list stays complete for a user who was granted single
    // projects and cannot read the repositories they live in.
    const repoInfoOf = useCallback(
        (project: Project) => project.repositoryInfo ?? localRepositoryInfo,
        [localRepositoryInfo]
    )

    // Leaving for another page saves the filters at once. The save that waits for the typing to settle is cancelled
    // with this screen, while the tree beside a project reads them as soon as the project page is drawn.
    const leaveTo = useCallback((path: string) => {
        saveProjectFilters(params)
        void navigate(path)
    }, [navigate, params])

    const openProject = useCallback(
        (project: Project) => leaveTo(projectRoute(project.id)),
        [leaveTo]
    )

    // A file or a folder picked in the tree is shown on the page of the project it belongs to. A module is shown there
    // too, with its file actions and the button that opens it in the editor.
    const openFile = useCallback(
        (project: Project, path: string) => leaveTo(projectFileRoute(project.id, path)),
        [leaveTo]
    )

    // After a create, land on the new project's page, addressed by the id the create answered. The list stays mounted
    // beneath the page and feeds the tree beside it, so it reads the projects again for the new one to be there.
    const openCreated = useCallback((id: string) => {
        setCreateOpen(false)
        void load(true, { silent: true })
        leaveTo(`/projects/${id}`)
    }, [leaveTo, load])

    // A group picked in the tree is the same thing as ticking its facets: the list shows its projects, and the
    // tree, which follows the facets, narrows to the group.
    const openGroup = useCallback((filters: NodeFilters) => {
        const next = new URLSearchParams(params)
        next.delete('page')
        writePicked(next, 'repo', filters.repositories)
        writePicked(next, 'branch', filters.branches)
        writePicked(next, 'tags', filters.tags)
        setGroupPicks(previous => ({ ...previous, [picksOf(next)]: treeNarrowBy }))
        setParams(next, { replace: true })
    }, [params, setParams, treeNarrowBy])

    /** What one project is busy with; `null` frees it. A project untouched keeps the snapshot as it was. */
    const setBusy = useCallback((projectId: string, actionId: RowBusyId | null) => {
        setPending(current => {
            if (actionId !== null) {
                return { ...current, [projectId]: actionId }
            }
            if (!(projectId in current)) {
                return current
            }
            const rest = { ...current }
            delete rest[projectId]
            return rest
        })
    }, [])

    /**
     * Marks one project busy for as long as the operation on it runs, so its own row offers nothing else
     * meanwhile and the live-change ping waits for the answer the operation is already asking for.
     *
     * @param onError handles the failure itself; true when it did, so nothing is reported
     */
    const busyWhile = useCallback((
        projectId: string,
        actionId: RowBusyId,
        run: () => Promise<unknown>,
        failKey?: string,
        onError?: (error: unknown) => boolean
    ) => {
        setBusy(projectId, actionId)
        return run()
            .catch((e: unknown) => {
                if (onError?.(e) || failKey === undefined) {
                    return
                }
                notification.error({ title: t(failKey), description: errorMessage(e) })
            })
            .finally(() => setBusy(projectId, null))
    }, [setBusy, t])

    /**
     * Runs an action on a row and shows its result: the list is read again afterwards, so the row stays
     * busy until it shows what the action did.
     */
    const runAction = useCallback((
        project: Project,
        actionId: RowBusyId,
        fn: () => Promise<unknown>,
        failKey: string,
        onError?: (error: unknown) => boolean
    ) => busyWhile(project.id, actionId, () => fn().then(() => load(true)), failKey, onError), [busyWhile, load])

    const closeProject = useCallback((project: Project, discardChanges = false) =>
        runAction(
            project,
            'close',
            () => setProjectStatus(project.id, 'CLOSED', discardChanges ? { discardChanges: true } : {}),
            'browser.status_change_failed',
            discardChanges
                ? undefined
                : (error) => {
                    if (!isProjectModifiedConflict(error)) {
                        return false
                    }
                    setDiscardCloseTarget(project)
                    return true
                }
        ), [runAction])

    // Hoisted so the branch switcher of every row hands it over instead of nesting one more callback.
    const reloadAll = useCallback(() => load(true), [load])

    /**
     * Opens one of the branch dialogs, which first reads the project's branches — a round trip the row
     * spins for, instead of dead-ending until a dialog appears unannounced.
     *
     * Nothing is refreshed behind the open dialog: it reloads the list itself when it finishes, and a
     * refresh as it opens would repaint the list behind a dialog the user is still filling in.
     */
    const openBranchDialog = useCallback((
        project: Project,
        actionId: RowBusyId,
        open: (project: Project, onSuccess: () => void) => Promise<void>
    ) => void busyWhile(
        project.id,
        actionId,
        () => open(project, () => void busyWhile(project.id, actionId, reloadAll)),
        'browser.branch.load_failed'
    ), [busyWhile, reloadAll])

    // A branch switch is no row button, but it occupies its project like one: the row's other actions are
    // gated for as long as the switch and the reload behind it run.
    const switchingBranch = useCallback(
        (project: Project, busy: boolean) => setBusy(project.id, busy ? 'switchBranch' : null),
        [setBusy]
    )

    /**
     * The read a finished dialog leaves behind: its own spinner is gone with it, so the project it acted on
     * stays busy until the list shows what it did — its row offers nothing else meanwhile, and the ping
     * echoing the dialog's own change waits instead of racing the read that already answers it.
     */
    const reloadAfterDialog = useCallback((project: Project | null, actionId: RowBusyId) => {
        if (project) {
            void busyWhile(project.id, actionId, reloadAll)
        }
    }, [busyWhile, reloadAll])

    // The dialog is mounted above the routes and answers back into this screen, so leaving the page takes
    // its question along instead of letting it confirm into a tree that is gone.
    useEffect(() => closeProjectDialog, [])

    const handlers: ProjectListHandlers = useMemo(() => ({
        // The list response carries the dependencies, so the dialog never reads them again.
        onOpen: project => openProjectDialog(
            { ...project, dependencies: project.dependencies ?? [] },
            options => runAction(
                project,
                'open',
                () => setProjectStatus(project.id, 'OPENED', options),
                'browser.status_change_failed'
            )
        ),
        onClose: project => {
            if (project.status === ProjectStatus.Editing) {
                setDiscardCloseTarget(project)
            } else {
                void closeProject(project)
            }
        },
        onSave: project => setSaveTarget(project),
        onCopy: project => setCopySource(project),
        onDeleteBranch: project => openBranchDialog(project, 'deleteBranch', openDeleteBranchDialog),
        onOpenRevision: project => setOpenRevisionFor(project),
        onSync: project => openBranchDialog(project, 'sync', openMergeDialog),
        onCompare: project => openCompareWindow(project),
        onExport: project => setExportSource(project),
        onDeploy: project => window.dispatchEvent(new CustomEvent('openDeployModal', {
            detail: project,
        })),
        onDelete: project => window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
            detail: {
                projectId: project.id,
                projectName: project.name,
                repositoryInfo: project.repositoryInfo,
                localOnly: project.status === ProjectStatus.Local,
                onSuccess: () => load(true),
            },
        })),
    }), [closeProject, load, openBranchDialog, runAction])

    const deleteParams = useCallback((...keys: string[]) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            keys.forEach(key => next.delete(key))
            return next
        }, { replace: true })
    }, [setParams])

    // Clearing the picks brings both views of the rail back to the default view; the search keeps its text.
    const resetFilters = useCallback(() => {
        setGroupPicks({})
        deleteParams(...FILTER_PARAMS, 'page')
    }, [deleteParams])

    // The no-match state clears the search too, not just the facets.
    const clearAll = useCallback(() => {
        setGroupPicks({})
        deleteParams('q', ...FILTER_PARAMS, 'page')
    }, [deleteParams])

    const content = () => {
        if (loading && projects.length === 0 && !error) {
            return (
                <div className={shared.loading} data-testid="projects-home-loading">
                    <Skeleton active paragraph={{ rows: 6 }} />
                </div>
            )
        }
        if (totalProjects === 0) {
            // Nothing matched. When the workspace still holds projects, filters (or search) hid them all, so
            // offer a prominent one-click reset — say so even while a repository is still being indexed, or the
            // screen would look like it is still loading. With no projects at all there is nothing to reveal.
            if (allProjects.length > 0) {
                return (
                    <div className={shared.stateBox}>
                        <Empty data-testid="projects-no-match" description={t('home.no_match')} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                            <Button data-testid="projects-clear-filters" icon={<ClearOutlined />} onClick={clearAll} type="primary">
                                {t('home.clear_all_filters')}
                            </Button>
                        </Empty>
                    </div>
                )
            }
            if (indexingRepositories.length > 0) {
                return (
                    <div className={shared.stateBox} data-testid="projects-indexing">
                        <Spin />
                        <span>{t('home.indexing')}</span>
                    </div>
                )
            }
            return (
                <div className={shared.stateBox}>
                    <Empty data-testid="projects-empty" description={t('home.empty')} />
                </div>
            )
        }
        return (
            <ProjectsTable
                compileStatusByProject={compileStatusByProject}
                direction={direction}
                handlers={handlers}
                onBranchSwitching={switchingBranch}
                onChanged={reloadAll}
                onOpen={openProject}
                onSort={sortBy}
                pending={pending}
                projects={projects}
                repoInfoOf={repoInfoOf}
                sort={sort}
            />
        )
    }

    if (error && !projectShown) {
        return (
            <Alert
                showIcon
                data-testid="projects-home-error"
                description={error}
                title={t('browser.load_error')}
                type="error"
                action={(
                    <Button data-testid="projects-home-retry" loading={loading} onClick={() => void load()}>
                        {t('home.retry')}
                    </Button>
                )}
            />
        )
    }

    // The tags of the rail, the tree and the project shown follow the order of the configured tag types.
    const page = (
        <div className={cx(shared.page, shared.listPageRoot)} data-testid="projects-home">
            <ProjectsRail
                // A link to a file of the project shown opens the tree on it.
                currentFile={projectShown ? urlParams.get('file') ?? undefined : undefined}
                currentFileChanges={shownRail.currentFileChanges}
                currentFolders={shownRail.currentFolders}
                currentProjectId={shownRail.currentProjectId ?? shownProjectId}
                // Beside a project, the rail is where a failed read of the projects shows.
                error={error}
                filters={query}
                narrowBy={treeNarrowBy}
                onClearFilters={resetFilters}
                onOpenFile={openFile}
                onOpenGroup={openGroup}
                onOpenProject={openProject}
                onRefresh={() => void load(true)}
                // Hand the tree the snapshot the list already read, so the list screen reads /projects once,
                // not once for the list and again for the tree. `null` while the first read is still running.
                projects={loading && allProjects.length === 0 ? null : railProjects}
                reloadToken={reloadToken}
                repositories={repositories}
                search={projectShown ? undefined : debouncedSearch}
                filterView={headerActions => renderFilterRail({
                    headerActions,
                    onClearFilters: resetFilters,
                    onPick: pickFilters,
                    branchCounts: facets?.branchCounts,
                    branches,
                    repos,
                    repositories,
                    repositoryCounts: facets?.repositoryCounts,
                    statusCounts: facets?.statusCounts,
                    statuses,
                    tagCounts: facets?.tagCounts,
                    tags,
                })}
            />
            {/* A page for each project: what it read for one project never shows under another. */}
            {projectShown ? <Fragment key={shownProjectId}><Outlet context={rail} /></Fragment> : (
                <div className={shared.main}>
                    <div className={cx(shared.content, styles.content)}>
                        <div className={styles.scroll}>{content()}</div>
                        {loading && projects.length > 0 && <BusyVeil data-testid="projects-loading-overlay" />}
                    </div>
                    {showIndexingNote && indexingShown && (
                        <Alert
                            showIcon
                            className={styles.indexingBanner}
                            closable={{ onClose: () => setIndexingShown(false) }}
                            data-testid="projects-indexing-banner"
                            title={t('home.indexing_banner', { repos: indexingRepositories.join(', ') })}
                            type="info"
                        />
                    )}
                    <ListFooter
                        current={currentPage}
                        onChange={(page, size) => setParams(prev => withPage(prev, page, size), { replace: true })}
                        pageSize={pageSize}
                        testIdPrefix="projects"
                        total={totalProjects}
                        summary={(
                            <span className={styles.compileStrip} data-testid="projects-compile-summary">
                                {COMPILE_SUMMARY_ORDER.filter(state => compileTally[state] > 0).map(state => (
                                    <span key={state} className={styles.compileItem}>
                                        <span className={shared.stateDot} style={{ background: compileColorsOf(token)[state] }} />
                                        <span className={styles.compileNum}>{compileTally[state]}</span>
                                        {t(`browser.compile.${state}`)}
                                    </span>
                                ))}
                                <span data-testid="projects-count">
                                    {t('home.summary_total', { total: totalProjects })}
                                </span>
                                {showIndexingNote && (
                                    <Button
                                        aria-expanded={indexingShown}
                                        className={styles.indexingToggle}
                                        data-testid="projects-indexing-toggle"
                                        icon={<LoadingOutlined spin />}
                                        onClick={() => setIndexingShown(shown => !shown)}
                                        size="small"
                                        type="link"
                                    >
                                        {t('home.indexing_repositories', { count: indexingRepositories.length })}
                                    </Button>
                                )}
                            </span>
                        )}
                    />
                </div>
            )}
            {!projectShown && (
                <HeaderActions>
                    {creatableRepos.length > 0 && (
                        <Button
                            data-testid="projects-new"
                            icon={<PlusOutlined />}
                            onClick={() => setCreateOpen(true)}
                            type="primary"
                        >
                            {t('browser.new_project')}
                        </Button>
                    )}
                </HeaderActions>
            )}
            <NewProjectModal
                onClose={() => setCreateOpen(false)}
                onCreated={openCreated}
                open={createOpen}
                projects={projects}
                repositories={creatableRepos}
            />
            <ExportProjectModal
                onClose={() => setExportSource(null)}
                open={exportSource !== null}
                project={exportSource}
            />
            <OpenRevisionModal
                onClose={() => setOpenRevisionFor(null)}
                onOpened={() => reloadAfterDialog(openRevisionFor, 'openRevision')}
                open={openRevisionFor !== null}
                project={openRevisionFor}
            />
            <CopyProjectModal
                onClose={() => setCopySource(null)}
                onCopied={() => reloadAfterDialog(copySource, 'copy')}
                open={copySource !== null}
                project={copySource}
                repositories={creatableRepos}
            />
            <SaveProjectModal
                onClose={() => setSaveTarget(null)}
                onSaved={() => { reloadAfterDialog(saveTarget, 'save'); setSaveTarget(null) }}
                open={saveTarget !== null}
                project={saveTarget}
            />
            <DiscardChangesModal
                cancelButtonTestId="discard-close-cancel"
                confirmButtonTestId="discard-close-confirm"
                confirmText={t('browser.close_discard_confirm_unsafe')}
                onCancel={() => setDiscardCloseTarget(null)}
                open={discardCloseTarget !== null}
                warning={t('browser.close_discard_warning')}
                onConfirm={() => {
                    const target = discardCloseTarget
                    setDiscardCloseTarget(null)
                    if (target) {
                        void closeProject(target, true)
                    }
                }}
            />
        </div>
    )
    return (
        <TagTypeOrder value={configuredTagTypes}>
            <PublishTagTypes value={publishTagTypes}>{page}</PublishTagTypes>
        </TagTypeOrder>
    )
}

/** The picks among the parameters of the list: its filters alone, without the search, the sort or the page. */
const picksOf = (params: URLSearchParams): string => {
    const picks = new URLSearchParams()
    FILTER_PARAMS.forEach(key => params.getAll(key).forEach(value => picks.append(key, value)))
    return picks.toString()
}

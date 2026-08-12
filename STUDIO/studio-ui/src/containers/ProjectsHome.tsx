import { useCallback, useEffect, useMemo, useRef, useState, type ComponentProps } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { creatableRepositories } from '../utils/repositoryFeatures'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, notification, Pagination, Skeleton, Spin, type InputRef } from 'antd'
import { ClearOutlined, LoadingOutlined, PlusOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    getDesignRepositories,
    isProjectModifiedConflict,
    setProjectStatus,
} from '../services/repositories'
import { ProjectStatus } from '../constants/project'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../constants/ui'
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
import { ProjectsToolbar, type ProjectView } from './projects/ProjectsToolbar'
import { ProjectsTable } from './projects/ProjectsTable'
import { ProjectsGrid } from './projects/ProjectsGrid'
import type { ProjectListHandlers, RowActionId } from './projects/ProjectRowActions'
import { countFacets, refineProjects, searchProjects, sortProjects, type BranchFacetCount, type ProjectSort, type SortDirection } from './projects/projectListing'
import { getProjectIndex, hasProjectIndex, invalidateProjectIndex, isProjectIndexStale, projectSignature } from '../services/projectIndex'
import { useLoadGeneration, useWindowFocus, useWorkspaceChanges } from '../hooks'
import { COMPILE_COLORS } from './projects/projectsTheme'
import { useSharedStyles } from './projects/sharedStyles'
import { NewProjectModal } from './projects/NewProjectModal'
import { CopyProjectModal } from './projects/CopyProjectModal'
import { ExportProjectModal } from './projects/ExportProjectModal'
import { OpenRevisionModal } from './projects/OpenRevisionModal'
import { openDeleteBranchDialog, openMergeDialog } from './projects/branchDialogs'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { openCompareWindow } from './projects/compare'
import { loadProjectFilters, saveProjectFilters } from './projects/filterStorage'
import { SaveProjectModal } from './projects/SaveProjectModal'
import { DiscardChangesModal } from './DiscardChangesModal'
import {
    isPushFresherThanRead,
    subscribeWorkspaceProjectStatuses,
    type ProjectCompileState,
    type ProjectStatusUpdate,
} from '../services/projectStatus'

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
        gap: 4px 14px;
        font-size: 12px;
    `,
    compileItem: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        color: ${token.colorTextSecondary};
    `,
    compileNum: css`
        color: ${token.colorText};
        font-weight: 600;
        font-variant-numeric: tabular-nums;
    `,
    compileTotal: css`
        color: ${token.colorTextTertiary};
    `,
    /** The indexing note sits in the summary line, so it reads as part of it rather than as a button. */
    indexingToggle: css`
        height: auto;
        padding: 0;
        font-size: 12px;
    `,
    /** Keeps the indexing notice off the search box below it, in the header's own rhythm. */
    indexingBanner: css`
        margin-bottom: 12px;
    `,
    content: css`
        position: relative;
        overflow: hidden;
    `,
    scroll: css`
        height: 100%;
        overflow: auto;
    `,
    overlay: css`
        position: absolute;
        inset: 0;
        z-index: 2;
        display: flex;
        align-items: center;
        justify-content: center;
        background: color-mix(in srgb, ${token.colorBgContainer} 60%, transparent);
    `,
    paginationBar: css`
        display: flex;
        justify-content: flex-end;
        padding: 10px 16px;
        border-top: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    gridPad: css`
        padding: 16px;
    `,
}))

/**
 * Compile states shown in the header health strip, ordered so what needs attention comes first. Clean
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
 * The Projects tab home: every project the user can see, as one flat, filterable list. A left rail carries
 * repository, status and tag-type facets; repositories are a facet, not a hierarchy. Search, facets, sort
 * and view live in the URL, so a filtered view survives reloads and can be shared. Selecting a row opens
 * the project's workspace page.
 */
export const ProjectsHome = () => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const navigate = useNavigate()
    const [params, setParams] = useSearchParams()

    // A plain visit to the screen restores the filters of the last one; a link with its own parameters
    // describes what its sender wanted to show, so it is left alone. Nothing is fetched until this is
    // settled, so the screen asks for the restored filters instead of the default ones and then again.
    const [restoring, setRestoring] = useState(true)
    useEffect(() => {
        const saved = [...params.keys()].length > 0 ? null : loadProjectFilters()
        if (saved && [...saved.keys()].length > 0) {
            setParams(saved, { replace: true })
        }
        setRestoring(false)
        // Restoring happens once, on the first visit to the screen.
    }, [])

    // Persisted after the typing settles: the search box writes to the URL on every keystroke.
    useEffect(() => {
        const timeout = window.setTimeout(() => saveProjectFilters(params), SEARCH_DEBOUNCE_MS)
        return () => window.clearTimeout(timeout)
    }, [params])
    const [repositories, setRepositories] = useState<Repository[]>([])
    const [allProjects, setAllProjects] = useState<Project[]>([])
    const [compileStatuses, setCompileStatuses] = useState<ProjectStatusUpdate[]>([])
    const [projectIndexHealth, setProjectIndexHealth] = useState<Record<string, ProjectIndexHealth>>({})
    // Bumped whenever the screen changed the workspace, so the tree beside it reads it again too.
    const [reloadToken, setReloadToken] = useState(0)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [createOpen, setCreateOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    const [openRevisionFor, setOpenRevisionFor] = useState<Project | null>(null)
    const [exportSource, setExportSource] = useState<Project | null>(null)
    // Keyed by project: an action running on one row must not un-gate the buttons of another.
    const [pending, setPending] = useState<Record<string, RowActionId>>({})
    const [saveTarget, setSaveTarget] = useState<Project | null>(null)
    const [discardCloseTarget, setDiscardCloseTarget] = useState<Project | null>(null)
    const searchRef = useRef<InputRef>(null)
    // Bumped on every load so a stale response never overwrites a fresher one.
    const loads = useLoadGeneration()
    // When each project's compile status last arrived by push — a load never overwrites a fresher push.
    const statusPushedAt = useRef(new Map<string, number>())

    const search = params.get('q') ?? ''
    // No sort in the URL means the default name order — shown without an arrow until a header is clicked.
    const sortParam = params.get('sort')
    const sort: ProjectSort | null = sortParam === 'updated' || sortParam === 'branch' || sortParam === 'name' ? sortParam : null
    const direction: SortDirection = params.get('dir') === 'desc' ? 'desc' : 'asc'
    const view: ProjectView = params.get('view') === 'grid' ? 'grid' : 'list'
    const statusParam = params.get('status') ?? ''
    const repoParam = params.get('repo') ?? ''
    const tagParam = params.get('tags') ?? ''
    const pageSize = parsePositiveInt(params.get('size'), DEFAULT_PAGE_SIZE)
    const requestedPage = parsePositiveInt(params.get('page'), 1)
    const debouncedSearch = useDebouncedValue(search, SEARCH_DEBOUNCE_MS)

    const statuses = useMemo(() => new Set(statusParam.split(',').filter(Boolean)), [statusParam])
    const repos = useMemo(() => new Set(repoParam.split(',').filter(Boolean)), [repoParam])
    const tags = useMemo(() => new Set(tagParam.split(',').filter(Boolean)), [tagParam])
    // Branch names may legally contain a comma, so branch filters ride as repeated params (branch=a&branch=b)
    // rather than one comma-joined value like the other facets, which can never collide with their values.
    const branches = useMemo(() => new Set(params.getAll('branch').filter(Boolean)), [params])
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
    // What the rail counts: the search scope, with the picked facets ignored — the way the API counted it.
    const facets = useMemo<ProjectFacets>(() => countFacets(searched, repositoryName), [searched, repositoryName])
    const matched = useMemo(
        () => sortProjects(refineProjects(searched, { statuses, repositories: repos, tags, branches }), sort ?? 'name', direction),
        [branches, direction, repos, searched, sort, statuses, tags]
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

    const toggleInParam = useCallback((key: string, current: Set<string>, value: string) => {
        const next = new Set(current)
        if (next.has(value)) {
            next.delete(value)
        } else {
            next.add(value)
        }
        setParam(key, [...next].join(','), true)
    }, [setParam])

    // The branch facet writes repeated params instead of a comma-joined value, since a branch name may hold
    // a comma; every other facet toggles through toggleInParam.
    const toggleBranch = useCallback((value: string) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            const chosen = new Set(prev.getAll('branch'))
            if (chosen.has(value)) {
                chosen.delete(value)
            } else {
                chosen.add(value)
            }
            setRepeatedListParam(next, 'branch', [...chosen])
            next.delete('page')
            return next
        }, { replace: true })
    }, [setParams])

    // The whole workspace is read once and kept in the browser: filtering, sorting and paging happen
    // here, so a facet click or a page step costs nothing and the server is not asked again. The facet
    // counts — the expensive part of the list response — are counted from the same snapshot.
    const load = useCallback((refresh = false, { silent = false } = {}) => {
        const { generation, startedAt } = loads.start(silent)
        if (!silent) {
            setLoading(true)
        }
        if (refresh) {
            invalidateProjectIndex()
            setReloadToken(token => token + 1)
        }
        return Promise.all([getDesignRepositories(LOCAL_LOAD_API_OPTIONS), getProjectIndex()])
            .then(([repos_, index]): Project[] | null => {
                if (!loads.isLatest(generation)) {
                    return null
                }
                setRepositories(repos_)
                setAllProjects(index.projects)
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
    }, [loads])

    useEffect(() => {
        if (restoring) {
            return
        }
        // A snapshot left over from an earlier visit paints instantly, but the workspace may have
        // moved on since — another user, another tab. Show it, then re-read behind it and swap the
        // fresh answer in without a skeleton.
        const revalidate = hasProjectIndex()
        void load().then(() => {
            if (revalidate) {
                void load(true, { silent: true })
            }
        })
    }, [load, restoring])

    // The backend pings when the workspace or a repository changed — another session, another
    // user, another client. The list re-reads behind the scenes and swaps the fresh answer in. The user is
    // told only when the refresh actually changed the list: a ping echoing their own action lands on
    // an already-fresh snapshot and stays quiet.
    // A ping landing while a row action runs waits for it: the refresh would otherwise supersede the
    // read the action is waiting for and unlock the row over the state before it.
    useWorkspaceChanges(() => {
        const before = listSignature(allProjects)
        void load(true, { silent: true }).then(loaded => {
            if (loaded !== null && listSignature(loaded) !== before) {
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
        if (!loading && totalProjects > 0 && requestedPage > totalPages) {
            setParam('page', totalPages === 1 ? null : String(totalPages))
        }
    }, [loading, requestedPage, setParam, totalPages, totalProjects])

    // "/" puts the caret in the search box, unless the user is already typing somewhere.
    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey) {
                return
            }
            const target = event.target as HTMLElement | null
            if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT' || target.isContentEditable)) {
                return
            }
            event.preventDefault()
            searchRef.current?.focus()
        }
        window.addEventListener('keydown', onKeyDown)
        return () => window.removeEventListener('keydown', onKeyDown)
    }, [])

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
        const tally: Record<ProjectCompileState, number> = { idle: 0, compiling: 0, ok: 0, warnings: 0, errors: 0 }
        for (const status of compileStatuses) {
            tally[status.compileState] += 1
        }
        return tally
    }, [compileStatuses])
    const localProjectNames = useMemo(
        () => projects.filter(project => project.status === ProjectStatus.Local).map(project => project.name),
        [projects]
    )

    // Each project reports its own repository, so the list stays complete for a user who was granted single
    // projects and cannot read the repositories they live in.
    const repoInfoOf = useCallback(
        (project: Project) => project.repositoryInfo ?? localRepositoryInfo,
        [localRepositoryInfo]
    )

    const openProject = useCallback((project: Project) => {
        navigate(`/projects/${encodeURIComponent(project.id)}`)
    }, [navigate])

    // After a create, land on the new project's page. Its server id is not known here (the create
    // response omits it), so a freshly read index is searched by repository and name; if the project
    // cannot be resolved the screen just refreshes its list instead.
    const openCreated = useCallback(async (created?: { repositoryId: string, name: string }) => {
        setCreateOpen(false)
        invalidateProjectIndex()
        if (created) {
            try {
                const index = await getProjectIndex()
                const match = index.projects.find(
                    project => project.repository === created.repositoryId && project.name === created.name
                )
                if (match) {
                    navigate(`/projects/${encodeURIComponent(match.id)}`)
                    return
                }
            } catch {
                // Fall back to refreshing the list below.
            }
        }
        void load(true)
    }, [load, navigate])

    // A group picked in the tree is the same thing as ticking its facets: the list shows its projects.
    const openGroup = useCallback((filters: NodeFilters) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            next.delete('page')
            setListParam(next, 'repo', filters.repositories)
            setRepeatedListParam(next, 'branch', filters.branches)
            setListParam(next, 'tags', filters.tags)
            return next
        }, { replace: true })
    }, [setParams])

    // Run an open/close status change on a row, reload the page, and surface a notification on failure.
    const runAction = useCallback((
        project: Project,
        actionId: RowActionId,
        fn: () => Promise<unknown>,
        failKey: string,
        onError?: (error: unknown) => boolean
    ) => {
        setPending(current => ({ ...current, [project.id]: actionId }))
        return fn()
            .then(() => load(true))
            .catch((e: unknown) => {
                if (onError?.(e)) {
                    return
                }
                notification.error({ title: t(failKey), description: errorMessage(e) })
            })
            .finally(() => setPending(current => {
                const rest = { ...current }
                delete rest[project.id]
                return rest
            }))
    }, [load, t])

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

    // Hoisted so the dialog-opening handlers below hand it over instead of nesting one more callback.
    const reloadAll = useCallback(() => void load(true), [load])

    // The dialog is mounted above the routes and answers back into this screen, so leaving the page takes
    // its question along instead of letting it confirm into a tree that is gone.
    useEffect(() => closeProjectDialog, [])

    const handlers: ProjectListHandlers = useMemo(() => ({
        // The list response carries the dependencies, so the dialog never reads them again.
        onOpen: project => openProjectDialog(
            { ...project, dependencies: project.dependencies ?? []},
            openDependencies => runAction(
                project,
                'open',
                () => setProjectStatus(project.id, 'OPENED', { openDependencies }),
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
        onDeleteBranch: project => void runAction(
            project,
            'deleteBranch',
            () => openDeleteBranchDialog(project, reloadAll),
            'browser.branch.load_failed'
        ),
        onOpenRevision: project => setOpenRevisionFor(project),
        onSync: project => void runAction(
            project,
            'sync',
            () => openMergeDialog(project, reloadAll),
            'browser.branch.load_failed'
        ),
        onCompare: project => openCompareWindow(project),
        onExport: project => setExportSource(project),
        onDeploy: project => window.dispatchEvent(new CustomEvent('openDeployModal', {
            detail: project,
        })),
        onDelete: project => window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
            detail: {
                projectId: project.id,
                projectName: project.name,
                onSuccess: () => load(true),
            },
        })),
    }), [closeProject, load, reloadAll, runAction])

    const deleteParams = useCallback((...keys: string[]) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            keys.forEach(key => next.delete(key))
            return next
        }, { replace: true })
    }, [setParams])

    const resetFilters = useCallback(() => deleteParams('status', 'repo', 'tags', 'branch', 'page'), [deleteParams])

    // The no-match state clears the search too, not just the facets.
    const clearAll = useCallback(() => deleteParams('q', 'status', 'repo', 'tags', 'branch', 'page'), [deleteParams])

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
                                {t('home.clear_filters')}
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
        return view === 'grid' ? (
            <div className={styles.gridPad}>
                <ProjectsGrid
                    compileStatusByProject={compileStatusByProject}
                    handlers={handlers}
                    onChanged={() => void load(true)}
                    onOpen={openProject}
                    pending={pending}
                    projects={projects}
                    repoInfoOf={repoInfoOf}
                />
            </div>
        ) : (
            <ProjectsTable
                compileStatusByProject={compileStatusByProject}
                direction={direction}
                handlers={handlers}
                onChanged={() => void load(true)}
                onOpen={openProject}
                onSort={sortBy}
                pending={pending}
                projects={projects}
                repoInfoOf={repoInfoOf}
                sort={sort}
            />
        )
    }

    if (error) {
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

    return (
        <div className={shared.page} data-testid="projects-home">
            <ProjectsRail
                onOpenGroup={openGroup}
                onOpenProject={openProject}
                onRefresh={() => void load(true)}
                onShowAll={resetFilters}
                // Hand the tree the snapshot the list already read, so the list screen reads /projects once,
                // not once for the list and again for the tree. `null` while the first read is still running.
                projects={loading && allProjects.length === 0 ? null : allProjects}
                reloadToken={reloadToken}
                repositories={repositories}
                filters={headerActions => renderFilterRail({
                    headerActions,
                    onReset: resetFilters,
                    onToggleBranch: toggleBranch,
                    onToggleRepo: value => toggleInParam('repo', repos, value),
                    onToggleStatus: value => toggleInParam('status', statuses, value),
                    onToggleTag: value => toggleInParam('tags', tags, value),
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
            <div className={shared.main}>
                <div className={shared.header}>
                    <div className={shared.headTop}>
                        <div>
                            <h1 className={shared.pageTitle}>{t('home.title')}</h1>
                            <div className={shared.subtitle} data-testid="projects-count">
                                <span className={styles.compileStrip} data-testid="projects-compile-summary">
                                    {COMPILE_SUMMARY_ORDER.filter(state => compileTally[state] > 0).map(state => (
                                        <span key={state} className={styles.compileItem}>
                                            <span className={shared.stateDot} style={{ background: COMPILE_COLORS[state] }} />
                                            <span className={styles.compileNum}>{compileTally[state]}</span>
                                            {t(`browser.compile.${state}`)}
                                        </span>
                                    ))}
                                    <span className={styles.compileTotal}>{t('home.summary_total', { total: totalProjects })}</span>
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
                            </div>
                        </div>
                        {creatableRepos.length > 0 && (
                            <div className={shared.headActions}>
                                <Button
                                    data-testid="projects-new"
                                    icon={<PlusOutlined />}
                                    onClick={() => setCreateOpen(true)}
                                    type="primary"
                                >
                                    {t('browser.new_project')}
                                </Button>
                            </div>
                        )}
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
                    <ProjectsToolbar
                        onSearch={value => setParam('q', value, true)}
                        onSort={sortBy}
                        onView={value => setParam('view', value === 'list' ? null : value)}
                        search={search}
                        searchRef={searchRef}
                        sort={sort ?? 'name'}
                        view={view}
                    />
                </div>
                <div className={cx(shared.content, styles.content)}>
                    <div className={styles.scroll}>{content()}</div>
                    {loading && projects.length > 0 && (
                        <div className={styles.overlay} data-testid="projects-loading-overlay">
                            <Spin />
                        </div>
                    )}
                </div>
                {totalProjects > pageSize && (
                    <div className={styles.paginationBar}>
                        <Pagination
                            showSizeChanger
                            current={currentPage}
                            data-testid="projects-pagination"
                            pageSize={pageSize}
                            pageSizeOptions={PAGE_SIZE_OPTIONS}
                            total={totalProjects}
                            onChange={(nextPage, nextSize) => {
                                // Page and size must change in one navigation; two setParams calls would
                                // both read the same snapshot and the second would drop the first.
                                setParams(prev => {
                                    const next = new URLSearchParams(prev)
                                    const apply = (key: string, value: string | null) =>
                                        value === null ? next.delete(key) : next.set(key, value)
                                    apply('page', nextPage === 1 ? null : String(nextPage))
                                    apply('size', nextSize === DEFAULT_PAGE_SIZE ? null : String(nextSize))
                                    return next
                                }, { replace: true })
                            }}
                        />
                    </div>
                )}
            </div>
            <NewProjectModal
                localProjects={localProjectNames}
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
                onOpened={() => void load(true)}
                open={openRevisionFor !== null}
                project={openRevisionFor}
            />
            <CopyProjectModal
                onClose={() => setCopySource(null)}
                onCopied={() => void load(true)}
                open={copySource !== null}
                project={copySource}
                repositories={creatableRepos}
            />
            <SaveProjectModal
                onClose={() => setSaveTarget(null)}
                onSaved={() => { setSaveTarget(null); void load(true) }}
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
}

/** Writes one of the list's multi-value parameters, or drops it when the group does not use it. */
const setListParam = (params: URLSearchParams, key: string, values: string[]): void => {
    if (values.length === 0) {
        params.delete(key)
    } else {
        params.set(key, values.join(','))
    }
}

/**
 * Writes a multi-value parameter as one entry per value ({@code key=a&key=b}), so a value that itself
 * contains the comma the other facets join on — a branch name may — survives the round trip.
 */
const setRepeatedListParam = (params: URLSearchParams, key: string, values: string[]): void => {
    params.delete(key)
    for (const value of values) {
        params.append(key, value)
    }
}

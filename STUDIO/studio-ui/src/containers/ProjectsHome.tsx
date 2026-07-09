import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Pagination, Skeleton, type InputRef } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    downloadProject,
    getDesignRepositories,
    getProjects,
} from '../services/repositories'
import { ProjectStatus } from '../constants/project'
import type { Repository, RepositoryInfo } from '../types/repositories'
import type { Project, ProjectsPage, ProjectStatusSummary } from '../types/projects'
import { ProjectsFilterRail } from './projects/ProjectsFilterRail'
import { ProjectsToolbar, type ProjectSort, type ProjectView } from './projects/ProjectsToolbar'
import { ProjectsTable } from './projects/ProjectsTable'
import { ProjectsGrid } from './projects/ProjectsGrid'
import type { ProjectListHandlers } from './projects/ProjectRowActions'
import { NewProjectModal } from './projects/NewProjectModal'
import { CopyProjectModal } from './projects/CopyProjectModal'
import type { ProjectStatusUpdate } from '../services/projectStatus'

const DEFAULT_PAGE_SIZE = 20
const SEARCH_DEBOUNCE_MS = 300
const LOCAL_LOAD_API_OPTIONS = { throwError: true, suppressErrorPages: true } as const

const emptyProjectsPage = (pageSize = DEFAULT_PAGE_SIZE): ProjectsPage => ({
    content: [],
    pageNumber: 0,
    pageSize,
    numberOfElements: 0,
    total: 0,
})

const useStyles = createStyles(({ css, token }) => ({
    page: css`
        display: flex;
        height: calc(100vh - 64px);
        overflow: hidden;
        background: ${token.colorBgLayout};
    `,
    main: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
    `,
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    headTop: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    title: css`
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        letter-spacing: -0.01em;
    `,
    subtitle: css`
        margin-top: 2px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    headActions: css`
        display: flex;
        align-items: center;
        gap: 8px;
        flex: none;
    `,
    content: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
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
    stateBox: css`
        margin: 24px;
        padding: 48px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
    `,
    loading: css`
        padding: 24px;
    `,
}))

/** Statuses meaning the project is open in the current user's workspace. */
const WORKSPACE_STATUSES = new Set<ProjectStatus>([
    ProjectStatus.Editing,
    ProjectStatus.Opened,
    ProjectStatus.ViewingVersion,
    ProjectStatus.Local,
])

const parsePositiveInt = (value: string | null, fallback: number): number => {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

const workspaceCountOf = (counts: ProjectStatusSummary | undefined, projects: Project[]): number => {
    if (!counts) {
        return projects.filter(project => WORKSPACE_STATUSES.has(project.status)).length
    }
    return counts.local + counts.opened + counts.viewingVersion + counts.editing
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
 * The Projects tab home: every project the user can see, as one flat, filterable list. A left rail carries
 * repository, status and tag-type facets; repositories are a facet, not a hierarchy. Search, facets, sort
 * and view live in the URL, so a filtered view survives reloads and can be shared. Selecting a row opens
 * the project's workspace page.
 */
export const ProjectsHome = () => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const [params, setParams] = useSearchParams()
    const [repositories, setRepositories] = useState<Repository[]>([])
    const [projectsPage, setProjectsPage] = useState<ProjectsPage>(() => emptyProjectsPage())
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [createOpen, setCreateOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    const searchRef = useRef<InputRef>(null)

    const search = params.get('q') ?? ''
    const sort: ProjectSort = params.get('sort') === 'status' ? 'status' : params.get('sort') === 'updated' ? 'updated' : 'name'
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
    const projects = projectsPage.content
    const totalProjects = projectsPage.total ?? projects.length
    const totalPages = Math.max(1, Math.ceil(totalProjects / pageSize))
    const currentPage = Math.min(requestedPage, totalPages)

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

    const toggleInParam = useCallback((key: string, current: Set<string>, value: string) => {
        const next = new Set(current)
        if (next.has(value)) {
            next.delete(value)
        } else {
            next.add(value)
        }
        setParam(key, [...next].join(','), true)
    }, [setParam])

    // Deleted projects are shown as an ordinary status facet, so the listing can include them on demand.
    const load = useCallback(() => {
        setLoading(true)
        return Promise.all([getDesignRepositories(LOCAL_LOAD_API_OPTIONS), getProjects({
            includes: ['DELETED', 'STATUS', 'SUMMARY'],
            name: debouncedSearch,
            page: requestedPage - 1,
            repositories: repos,
            size: pageSize,
            sort,
            statuses,
            tags,
        }, LOCAL_LOAD_API_OPTIONS)])
            .then(([repos_, page]) => {
                setRepositories(repos_)
                setProjectsPage(page)
                setError(null)
            })
            .catch((e: unknown) => {
                setError(errorMessage(e))
            })
            .finally(() => {
                setLoading(false)
            })
    }, [debouncedSearch, pageSize, repos, requestedPage, sort, statuses, tags])

    useEffect(() => {
        void load()
    }, [load])

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
    const repoInfo = useMemo(
        () => new Map(repositories.map(repo => [repo.id, { features: repo.features, name: repo.name, type: repo.type }])),
        [repositories]
    )
    const creatableRepos = useMemo(
        () => repositories.filter(repo => repo.capabilities?.canCreateProject),
        [repositories]
    )
    const compileStatusByProject = useMemo(() => {
        return new Map<string, ProjectStatusUpdate>(
            (projectsPage.statuses ?? []).map(status => [status.projectId, status])
        )
    }, [projectsPage.statuses])
    const localProjectNames = useMemo(
        () => projects.filter(project => project.status === ProjectStatus.Local).map(project => project.name),
        [projects]
    )

    const repoInfoOf = useCallback(
        (project: Project) => repoInfo.get(project.repository) ?? localRepositoryInfo,
        [localRepositoryInfo, repoInfo]
    )

    const workspaceCount = useMemo(
        () => workspaceCountOf(projectsPage.statusCounts, projects),
        [projects, projectsPage.statusCounts]
    )

    const openProject = useCallback((project: Project) => {
        navigate(`/projects/${encodeURIComponent(project.id)}`)
    }, [navigate])

    const handlers: ProjectListHandlers = useMemo(() => ({
        onOpen: openProject,
        onCopy: project => setCopySource(project),
        onExport: project => downloadProject(project.id),
        onDeploy: project => window.dispatchEvent(new CustomEvent('openDeployModal', {
            detail: { ...project, selectedBranches: project.selectedBranches ?? []},
        })),
        onDelete: project => window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
            detail: {
                projectId: project.id,
                projectName: project.name,
                onSuccess: load,
            },
        })),
    }), [load, openProject])

    const deleteParams = useCallback((...keys: string[]) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            keys.forEach(key => next.delete(key))
            return next
        }, { replace: true })
    }, [setParams])

    const resetFilters = useCallback(() => deleteParams('status', 'repo', 'tags', 'page'), [deleteParams])

    // The no-match state clears the search too, not just the facets.
    const clearAll = useCallback(() => deleteParams('q', 'status', 'repo', 'tags', 'page'), [deleteParams])

    const content = () => {
        if (loading && projects.length === 0 && !error) {
            return (
                <div className={styles.loading} data-testid="projects-home-loading">
                    <Skeleton active paragraph={{ rows: 6 }} />
                </div>
            )
        }
        const hasFilters = search.trim() !== '' || statuses.size > 0 || repos.size > 0 || tags.size > 0
        if (totalProjects === 0 && !hasFilters) {
            return (
                <div className={styles.stateBox}>
                    <Empty data-testid="projects-empty" description={t('home.empty')} />
                </div>
            )
        }
        if (totalProjects === 0) {
            return (
                <div className={styles.stateBox}>
                    <Empty data-testid="projects-no-match" description={t('home.no_match')} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                        <Button onClick={clearAll}>{t('home.clear_filters')}</Button>
                    </Empty>
                </div>
            )
        }
        return view === 'grid' ? (
            <div className={styles.gridPad}>
                <ProjectsGrid
                    compileStatusByProject={compileStatusByProject}
                    handlers={handlers}
                    onOpen={openProject}
                    projects={projects}
                    repoInfoOf={repoInfoOf}
                />
            </div>
        ) : (
            <ProjectsTable
                compileStatusByProject={compileStatusByProject}
                handlers={handlers}
                onOpen={openProject}
                projects={projects}
                repoInfoOf={repoInfoOf}
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
        <div className={styles.page} data-testid="projects-home">
            <ProjectsFilterRail
                onReset={resetFilters}
                onToggleRepo={value => toggleInParam('repo', repos, value)}
                onToggleStatus={value => toggleInParam('status', statuses, value)}
                onToggleTag={value => toggleInParam('tags', tags, value)}
                repos={repos}
                repositories={repositories}
                repositoryCounts={projectsPage.repositoryCounts}
                statusCounts={projectsPage.statusCounts}
                statuses={statuses}
                tagCounts={projectsPage.tagCounts}
                tags={tags}
            />
            <div className={styles.main}>
                <div className={styles.header}>
                    <div className={styles.headTop}>
                        <div>
                            <h1 className={styles.title}>{t('home.title')}</h1>
                            <div className={styles.subtitle} data-testid="projects-count">
                                {t('home.summary', { workspace: workspaceCount, total: totalProjects })}
                            </div>
                        </div>
                        {creatableRepos.length > 0 && (
                            <div className={styles.headActions}>
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
                    <ProjectsToolbar
                        onSearch={value => setParam('q', value, true)}
                        onSort={value => setParam('sort', value === 'name' ? null : value, true)}
                        onView={value => setParam('view', value === 'list' ? null : value)}
                        search={search}
                        searchRef={searchRef}
                        sort={sort}
                        view={view}
                    />
                </div>
                <div className={styles.content}>{content()}</div>
                {totalProjects > pageSize && (
                    <div className={styles.paginationBar}>
                        <Pagination
                            showSizeChanger
                            current={currentPage}
                            data-testid="projects-pagination"
                            pageSize={pageSize}
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
                onCreated={() => { setCreateOpen(false); void load() }}
                open={createOpen}
                projects={projects}
                repositories={creatableRepos}
            />
            <CopyProjectModal
                onClose={() => setCopySource(null)}
                onCopied={() => void load()}
                open={copySource !== null}
                project={copySource}
                repositories={creatableRepos}
            />
        </div>
    )
}

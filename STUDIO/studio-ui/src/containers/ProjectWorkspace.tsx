import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, notification, Skeleton } from 'antd'
import { createStyles } from 'antd-style'
import {
    getDesignRepositories,
    getProject,
    getProjectFiles,
    isProjectModifiedConflict,
    setProjectStatus,
    unlockProject,
} from '../services/repositories'
import { NotFoundError } from '../services'
import { invalidateProjectIndex, PROJECT_INDEX_TTL_MS, projectSignature } from '../services/projectIndex'
import { useLiveProjectChanges, useLoadGeneration, useWindowFocus } from '../hooks'
import { ProjectStatus } from '../constants/project'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import type { Repository } from '../types/repositories'
import type { Project } from '../types/projects'
import type { FsNode } from '../types/files'
import { ProjectDetail } from './projects/ProjectDetail'
import { CompileProblemsPanel } from './projects/CompileProblemsPanel'
import { creatableRepositories, supportsBranches } from '../utils/repositoryFeatures'
import { SaveProjectModal } from './projects/SaveProjectModal'
import { CopyProjectModal } from './projects/CopyProjectModal'
import { ExportProjectModal } from './projects/ExportProjectModal'
import { OpenRevisionModal } from './projects/OpenRevisionModal'
import { openDeleteBranchDialog, openMergeDialog } from './projects/branchDialogs'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { openCompareWindow } from './projects/compare'
import type { ActionId, ProjectActionHandlers } from './projects/ProjectActionBar'
import { DiscardChangesModal } from './DiscardChangesModal'
import { ProjectsRail } from './projects/ProjectsRail'
import type { NodeFilters } from './projects/projectGrouping'
import { toUrlSafeId } from '../services/projectId'


const useStyles = createStyles(({ css, token }) => ({
    page: css`
        height: calc(100vh - 64px);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    /** The workspace beside the tree rail: the rail on the left, the project filling the rest. */
    withRail: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
    `,
    crumb: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        margin-right: ${token.marginXXS}px;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSize}px;
        font-weight: 400;

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    body: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    centered: css`
        display: flex;
        align-items: center;
        justify-content: center;
        flex: 1;
        padding: ${token.paddingXL}px;
    `,
    skeleton: css`
        width: 480px;
        max-width: 100%;
    `,
}))

/**
 * A single project's workspace page, addressed by the project id in the URL. Hosts the project
 * header, actions and content tabs; the content canvas is where table editing will live when the
 * Editor's features move here.
 */
export const ProjectWorkspace = () => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const { projectId } = useParams()
    // Read only when the copy dialog first opens; `null` until then.
    const [repositories, setRepositories] = useState<Repository[] | null>(null)
    const [project, setProject] = useState<Project | null>(null)
    // When the read behind the shown project started, so the compile dot can tell a pushed status
    // that is newer than this answer from one this answer has already overtaken.
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [files, setFiles] = useState<FsNode[] | 'loading' | 'error'>()
    const [pendingId, setPendingId] = useState<ActionId | null>(null)
    const [saveOpen, setSaveOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    const [openRevisionFor, setOpenRevisionFor] = useState<Project | null>(null)
    const [exportSource, setExportSource] = useState<Project | null>(null)
    const [discardCloseOpen, setDiscardCloseOpen] = useState(false)
    // Bumped on every reload so tabs that cache their own data (history, file content) refetch and reset.
    const [reloadToken, setReloadToken] = useState(0)
    // What the reload behind the current token knows it touched; null means anything may have changed.
    const [changedFiles, setChangedFiles] = useState<string[] | null>(null)
    // Bumped on every project-detail load so stale navigation responses cannot overwrite the current page.
    const loads = useLoadGeneration()
    // Its own counter, so a stale files response never overwrites a fresh one.
    const fileLoads = useLoadGeneration()
    // When the page last read its project — the staleness policy behind the pings counts from here.
    const loadedAt = useRef(0)

    const reducedMotion = useMemo(
        () => typeof window !== 'undefined'
            && !!window.matchMedia
            && window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        []
    )

    // The whole workspace is driven by the single-project detail response, which carries the dependency
    // graph and the rules.xml-derived fields. A missing project (404) shows the not-found state.
    // A silent load swaps the fresh answer in without the skeleton — the background refreshes use it.
    // `touched` names the files the change behind this reload is known to cover; null means anything.
    // With `skipUnchangedFor` (the signature of what the page shows), a refresh that brings back
    // exactly that and names no files is not applied at all: no tab reset, no re-fetch cascade —
    // that would replay the reload the user's own action already performed.
    const load = useCallback(async (
        { silent = false, touched = null, skipUnchangedFor }: {
            silent?: boolean
            touched?: string[] | null
            skipUnchangedFor?: string
        } = {}
    ): Promise<Project | null> => {
        if (!projectId) {
            return null
        }
        const { generation, startedAt } = loads.start(silent)
        if (!silent) {
            setLoading(true)
        }
        try {
            const loaded = await getProject(projectId, { includes: ['status', 'descriptor']}, LOCAL_LOAD_API_OPTIONS)
            if (!loads.isLatest(generation)) {
                return null
            }
            if (skipUnchangedFor !== undefined && touched === null && projectSignature(loaded) === skipUnchangedFor) {
                // The tabs are left alone: nothing the signature covers changed. The compile status is
                // not part of it and is the server's latest word, so it is taken anyway - otherwise a
                // push that was lost would go on outranking every read that follows it.
                loadedAt.current = Date.now()
                setProject(current => {
                    const { compileStatus } = loaded
                    if (current === null) {
                        return loaded
                    }
                    // A read carrying no status says nothing about it, and leaves the shown one alone.
                    return compileStatus === undefined ? current : { ...current, compileStatus }
                })
                setStatusReadAt(startedAt)
                return loaded
            }
            setProject(loaded)
            setStatusReadAt(startedAt)
            setChangedFiles(touched)
            setReloadToken(token => token + 1)
            fileLoads.start(true)
            loadedAt.current = Date.now()
            setFiles(undefined)
            setError(null)
            return loaded
        } catch (e) {
            // A reload the user waits for reports its own failure even when a quiet one has overtaken
            // it: the quiet one reports nothing, so the action would look as if it had succeeded.
            if (!(e instanceof NotFoundError) && loads.ownsSpinner(generation)) {
                setError(errorMessage(e))
            } else if (loads.isLatest(generation) && e instanceof NotFoundError) {
                setProject(null)
                setError(null)
            }
        } finally {
            // The spinner is hidden by the reload it belongs to, even when a quiet re-read has started
            // behind it: a quiet one has no spinner to hide and would leave this one up.
            if (loads.ownsSpinner(generation)) {
                setLoading(false)
            }
        }
        return null
    }, [loads, projectId])

    // Only the copy dialog's target picker needs the repository list — the screen itself lives off the
    // project's own repositoryInfo. So the list is read once, when the dialog first opens, and never at
    // all for the user who does not copy (for one granted a single project it reads as empty anyway).
    useEffect(() => {
        if (copySource === null || repositories !== null) {
            return
        }
        getDesignRepositories(LOCAL_LOAD_API_OPTIONS)
            .then(setRepositories)
            .catch(() => setRepositories([]))
    }, [copySource, repositories])

    // Drop the previous project immediately on navigation so its content never flashes under the new id.
    useEffect(() => {
        setProject(null)
    }, [projectId])

    useEffect(() => {
        void load()
    }, [load])

    // The backend pings when this project changed elsewhere — another session or client, a commit
    // by someone else. The page re-reads itself quietly, and the
    // dropped snapshot makes the tree beside it (reloaded by the load's token) see the change too.
    // The files the ping names flow to the open-file pane, so it refreshes only when touched.
    // The user is told only when the refresh actually changed the project: a ping echoing their own
    // action lands on an already-fresh page and stays quiet.
    const refreshFromPing = useCallback((pingedFiles: string[]) => {
        const before = projectSignature(project)
        invalidateProjectIndex()
        void load({
            silent: true,
            touched: pingedFiles.length > 0 ? pingedFiles : null,
            skipUnchangedFor: before,
        }).then(loaded => {
            if (loaded !== null && projectSignature(loaded) !== before) {
                notification.info({ title: t('browser.live_project_synced') })
            }
        })
    }, [load, project, t])

    // A ping landing while the user's action runs answers the question the action is already asking,
    // so the hook holds it until the action's own answer is on screen.
    useLiveProjectChanges(project?.id, refreshFromPing, { holdWhile: pendingId !== null })

    // The staleness policy behind the pings: coming back to a tab whose page outlived the trust
    // window (pings can be lost while a laptop sleeps) re-reads it quietly.
    useWindowFocus(() => {
        if (project !== null && Date.now() - loadedAt.current > PROJECT_INDEX_TTL_MS) {
            void load({ silent: true })
        }
    })

    // The project reports its own repository, so the screen keeps its badge and its branch controls for a
    // user granted the project alone and not the repository it lives in. A local project that the
    // repository does not describe reads as the Local pseudo-repository.
    const repoInfo = project?.repositoryInfo ?? null
    const local = project?.status === ProjectStatus.Local
    let repoLabel = ''
    let repoType: string | undefined
    if (project) {
        repoLabel = repoInfo?.name ?? (local ? t('home.local') : project.repository)
        repoType = repoInfo?.type ?? (local ? 'repo-file' : undefined)
    }
    const creatableRepos = useMemo(() => creatableRepositories(repositories), [repositories])

    // Fetch the project's files only when the Files tab becomes visible. A failed fetch shows an error state,
    // retried on the next project reload.
    const loadFiles = useCallback(() => {
        if (!project || files !== undefined) {
            return
        }
        const { generation } = fileLoads.start(true)
        setFiles('loading')
        getProjectFiles(project.id)
            .then(loaded => {
                if (fileLoads.isLatest(generation)) {
                    setFiles(loaded)
                }
            })
            .catch(() => {
                if (fileLoads.isLatest(generation)) {
                    setFiles('error')
                }
            })
    }, [fileLoads, project, files])

    const runAction = useCallback(async (
        id: ActionId,
        fn: () => Promise<unknown>,
        failKey: string,
        onError?: (error: unknown) => boolean
    ) => {
        setPendingId(id)
        try {
            await fn()
            if (id === 'delete') {
                // The project no longer exists; its page has nothing left to show.
                navigate('/projects')
                return
            }
            await load()
        } catch (e) {
            if (onError?.(e)) {
                return
            }
            notification.error({
                title: t(failKey),
                description: errorMessage(e),
            })
        } finally {
            setPendingId(null)
        }
    }, [load, navigate, t])

    const closeProject = useCallback((discardChanges = false) => {
        if (!project) {
            return Promise.resolve()
        }
        return runAction(
            'close',
            () => setProjectStatus(project.id, 'CLOSED', discardChanges ? { discardChanges: true } : {}),
            'browser.status_change_failed',
            discardChanges
                ? undefined
                : (error) => {
                    if (!isProjectModifiedConflict(error)) {
                        return false
                    }
                    setDiscardCloseOpen(true)
                    return true
                }
        )
    }, [project, runAction])

    // The dialog is mounted above the routes and answers back with this project, so moving on takes its
    // question along. The page stays mounted when the route swaps one project for another, so the question
    // is withdrawn on the id as well as on the way out.
    useEffect(() => closeProjectDialog, [projectId])

    const handlers: ProjectActionHandlers = useMemo(() => {
        if (!project) {
            // The action bar is only rendered with a project; the handlers are never reached without one.
            return {} as ProjectActionHandlers
        }
        return {
            // The detail response is already loaded, so the dialog never reads the dependencies again.
            open: () => openProjectDialog(
                { ...project, dependencies: project.dependencies ?? []},
                openDependencies => runAction(
                    'open',
                    () => setProjectStatus(project.id, 'OPENED', { openDependencies }),
                    'browser.status_change_failed'
                )
            ),
            close: () => {
                if (project.status === ProjectStatus.Editing) {
                    setDiscardCloseOpen(true)
                } else {
                    void closeProject()
                }
            },
            save: () => setSaveOpen(true),
            copy: () => setCopySource(project),
            openRevision: () => setOpenRevisionFor(project),
            sync: () => void openMergeDialog(project, () => void load()),
            deleteBranch: () => void openDeleteBranchDialog(project, () => void load()),
            export: () => setExportSource(project),
            delete: () => window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
                detail: {
                    projectId: project.id,
                    projectName: project.name,
                    onSuccess: () => navigate('/projects'),
                },
            })),
            unlock: () => runAction('unlock', () => unlockProject(project.id), 'browser.unlock_failed'),
            deploy: () => {
                window.dispatchEvent(new CustomEvent('openDeployModal', {
                    detail: project,
                }))
            },
            compare: () => openCompareWindow(project),
        }
    }, [closeProject, navigate, project, runAction])

    if (loading && !project && !error) {
        return (
            <div className={styles.page}>
                <div className={styles.centered} data-testid="project-workspace-loading">
                    <Skeleton active className={styles.skeleton} />
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <Alert
                showIcon
                data-testid="project-workspace-error"
                description={error}
                title={t('browser.load_error')}
                type="error"
            />
        )
    }

    return (
        <div className={styles.page} data-testid="project-workspace">
            <div className={styles.withRail}>
                <ProjectsRail
                    currentProjectId={project?.id}
                    initialMode="tree"
                    // A group leads back to the list, showing exactly the projects it holds.
                    onOpenGroup={filters => navigate(`/projects?${groupQuery(filters)}`)}
                    onOpenProject={other => navigate(`/projects/${toUrlSafeId(other.id)}`)}
                    onShowAll={() => navigate('/projects')}
                    reloadToken={reloadToken}
                />
                <div className={styles.body}>
                    {project ? (
                        <ProjectDetail
                            changedFiles={changedFiles}
                            files={files}
                            handlers={handlers}
                            onChanged={() => void load()}
                            onFilesVisible={loadFiles}
                            pendingId={pendingId}
                            project={project}
                            reducedMotion={reducedMotion}
                            reloadToken={reloadToken}
                            repoFeatures={repoInfo?.features}
                            repoLabel={repoLabel}
                            repoType={repoType}
                            statusReadAt={statusReadAt}
                            headerPrefix={(
                                <span className={styles.crumb}>
                                    <Link to="/projects">{t('home.title')}</Link>
                                    <span aria-hidden>/</span>
                                </span>
                            )}
                        />
                    ) : (
                        <div className={styles.centered}>
                            <Empty data-testid="project-workspace-missing" description={t('home.not_found')}>
                                <Button onClick={() => navigate('/projects')} type="primary">
                                    {t('home.back_to_projects')}
                                </Button>
                            </Empty>
                        </div>
                    )}
                    {project && (
                        <CompileProblemsPanel
                            project={project}
                            statusReadAt={statusReadAt}
                            supportsBranches={supportsBranches({ features: repoInfo?.features })}
                        />
                    )}
                </div>
            </div>
            <SaveProjectModal
                onClose={() => setSaveOpen(false)}
                onSaved={() => void load()}
                open={saveOpen}
                project={project}
            />
            <ExportProjectModal
                onClose={() => setExportSource(null)}
                open={exportSource !== null}
                project={exportSource}
            />
            <OpenRevisionModal
                onClose={() => setOpenRevisionFor(null)}
                onOpened={() => void load()}
                open={openRevisionFor !== null}
                project={openRevisionFor}
            />
            <CopyProjectModal
                onClose={() => setCopySource(null)}
                onCopied={() => void load()}
                open={copySource !== null}
                project={copySource}
                repositories={creatableRepos}
            />
            <DiscardChangesModal
                cancelButtonTestId="discard-close-cancel"
                confirmButtonTestId="discard-close-confirm"
                confirmText={t('browser.close_discard_confirm_unsafe')}
                onCancel={() => setDiscardCloseOpen(false)}
                open={discardCloseOpen}
                warning={t('browser.close_discard_warning')}
                onConfirm={() => {
                    setDiscardCloseOpen(false)
                    void closeProject(true)
                }}
            />
        </div>
    )
}

/** The list parameters that show the projects of a tree group. */
const groupQuery = (filters: NodeFilters): string => {
    const params = new URLSearchParams()
    if (filters.repositories.length > 0) {
        params.set('repo', filters.repositories.join(','))
    }
    // Branch names may contain a comma, so each branch rides as its own param instead of a comma-joined one.
    for (const branch of filters.branches) {
        params.append('branch', branch)
    }
    if (filters.tags.length > 0) {
        params.set('tags', filters.tags.join(','))
    }
    return params.toString()
}

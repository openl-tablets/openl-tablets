import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { Link, useNavigate, useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { App, Alert, Skeleton } from 'antd'
import { createStyles } from 'antd-style'
import {
    getProject,
    getProjectFiles,
    isProjectModifiedConflict,
    setProjectStatus,
    unlockProject,
} from '../services/repositories'
import { isUnresolvedProjectLink } from '../services/projectLink'
import { invalidateProjectIndex, PROJECT_INDEX_TTL_MS, projectSignature } from '../services/projectIndex'
import { useLiveProjectChanges, useLoadGeneration, useWindowFocus } from '../hooks'
import { ProjectStatus } from '../constants/project'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import type { Project } from '../types/projects'
import type { FsNode } from '../types/files'
import { BusyVeil } from './projects/BusyVeil'
import { ProjectDetail } from './projects/ProjectDetail'
import { CompileProblemsPanel } from './projects/CompileProblemsPanel'
import { supportsBranches } from '../utils/repositoryFeatures'
import { ExportProjectModal } from './projects/ExportProjectModal'
import { OpenRevisionModal } from './projects/OpenRevisionModal'
import { openDeleteBranchDialog } from './projects/branchDialogs'
import { useProjectDialogs } from './projects/useProjectDialogs'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { openCompareWindow } from './projects/compare'
import type { ProjectActionHandlers } from './projects/ProjectActionBar'
import type { BusyId } from './projects/projectActions'
import { DiscardChangesModal } from './DiscardChangesModal'
import { UnresolvedProjectLink } from './projects/UnresolvedProjectLink'
import { useReportProjectRail } from './projects/projectRail'
import { buildFileChangeMap } from './projects/fileChanges'
import type { ProjectFileChange } from '../services/projectStatus'

/** One empty list for every project without pending changes, so the memo below it holds. */
const NO_FILE_CHANGES: ProjectFileChange[] = []


const useStyles = createStyles(({ css, token }) => ({
    /** The page beside the rail of the Projects screen: the project, and the dialogs over it. */
    page: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
        background: ${token.colorBgContainer};
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
        position: relative;
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
/**
 * What the page shows in place of a project it has not got: why the project could not be read, that the link leads
 * to no project, or a skeleton while the project is read. The tree beside it stays, so the reader can go on to another
 * project.
 */
const ProjectAbsent = ({ error, linkProblem, styles }: {
    error: string | null
    linkProblem: boolean
    styles: ReturnType<typeof useStyles>['styles']
}) => {
    const { t } = useTranslation('repository')
    if (error) {
        return (
            <div className={styles.centered}>
                <Alert
                    showIcon
                    data-testid="project-workspace-error"
                    description={error}
                    title={t('browser.project_load_error')}
                    type="error"
                />
            </div>
        )
    }
    if (linkProblem) {
        return <div className={styles.centered}><UnresolvedProjectLink /></div>
    }
    return (
        <div className={styles.centered} data-testid="project-workspace-loading">
            <Skeleton active className={styles.skeleton} />
        </div>
    )
}

export const ProjectWorkspace = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const { projectId } = useParams()
    const [project, setProject] = useState<Project | null>(null)
    // Whether the link leads to no project: shown in place of the project.
    const [linkProblem, setLinkProblem] = useState(false)
    // When the read behind the shown project started, so the compile dot can tell a pushed status
    // that is newer than this answer from one this answer has already overtaken.
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [files, setFiles] = useState<FsNode[] | 'loading' | 'error'>()
    // What the project is busy with, or null. Every operation on it runs through this, so the project shows
    // one busy state and offers no second operation until the first one is over.
    const [pendingId, setPendingId] = useState<BusyId | null>(null)
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
    // Folders added in the browser and not on the server yet. A folder stays empty, so it is not saved, until a
    // file is created inside it; until then both the tree and the folder pane show it.
    const [virtualFolders, setVirtualFolders] = useState<string[]>([])
    const addVirtualFolder = useCallback((path: string) => {
        setVirtualFolders(previous => (previous.includes(path) ? previous : [...previous, path]))
    }, [])
    const removeVirtualFolder = useCallback((path: string) => {
        setVirtualFolders(previous => previous.filter(folder => folder !== path && !folder.startsWith(`${path}/`)))
    }, [])
    // Once a file exists under an empty folder it is real: it leaves the list. The list stays the same one when no
    // folder left it, so the tree it is handed to is not drawn again for nothing.
    useEffect(() => {
        if (Array.isArray(files)) {
            setVirtualFolders(previous => {
                const empty = previous.filter(
                    folder => !files.some(node => node.path === folder || node.path.startsWith(`${folder}/`))
                )
                return empty.length === previous.length ? previous : empty
            })
        }
    }, [files])

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
            setLinkProblem(false)
            return loaded
        } catch (e) {
            const unresolved = isUnresolvedProjectLink(e)
            // A reload the user waits for reports its own failure even when a quiet one has overtaken
            // it: the quiet one reports nothing, so the action would look as if it had succeeded.
            if (!unresolved && loads.ownsSpinner(generation)) {
                setError(errorMessage(e))
            } else if (unresolved && loads.isLatest(generation)) {
                setProject(null)
                setError(null)
                setLinkProblem(true)
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

    // The Projects screen mounts a page for each project it shows, so a page reads its project once on mount and
    // never shows another one: what it read for a project the reader has left goes with it.
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

    // Fetch the project's files only when they are needed: a file or a folder is shown, or files are added. A failed
    // fetch shows an error state, retried on the next project reload.
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

    /**
     * Marks the project busy for as long as the operation runs, reporting a failure under `failKey`.
     * Everything the project waits for goes through here — the action bar, the dialogs, the branch
     * switcher — so a slow environment shows one busy project rather than a screen that looks as if the
     * click had never registered.
     *
     * @param onError handles the failure itself; true when it did, so nothing is reported
     */
    const busyWhile = useCallback((
        id: BusyId,
        run: () => Promise<unknown>,
        failKey?: string,
        onError?: (error: unknown) => boolean
    ) => {
        setPendingId(id)
        return run()
            .catch((e: unknown) => {
                if (onError?.(e) || failKey === undefined) {
                    return
                }
                notification.error({ title: t(failKey), description: errorMessage(e) })
            })
            .finally(() => setPendingId(null))
    }, [t])

    /**
     * Runs an action and shows its result: the project is read again afterwards, so the busy state only
     * ends once the page shows what the action did.
     */
    const runAction = useCallback((
        id: BusyId,
        fn: () => Promise<unknown>,
        failKey: string,
        onError?: (error: unknown) => boolean
    ) => busyWhile(id, () => fn().then(() => load()), failKey, onError), [busyWhile, load])

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
    // question along.
    useEffect(() => closeProjectDialog, [])

    // Saving, copying, syncing and deploying are the project's own; the module editor offers the same ones.
    const { actions: projectActions, dialogs: projectDialogs } = useProjectDialogs(project, {
        busy: (id, work) => void busyWhile(id, work),
        onChanged: () => void load(),
    })

    const handlers: ProjectActionHandlers = useMemo(() => {
        if (!project) {
            // The action bar is only rendered with a project; the handlers are never reached without one.
            return {} as ProjectActionHandlers
        }
        // The dialog reads the project's branches before it can be dispatched — a whole round trip the
        // button holds its spinner for, instead of dead-ending until a dialog appears unannounced. Nothing
        // is reloaded behind the open dialog: it reads the project itself when it finishes, and a reload as
        // it opens would flash the veil over a dialog still being filled in.
        const openDeleteBranch = () => void busyWhile(
            'deleteBranch',
            () => openDeleteBranchDialog(project, () => void busyWhile('deleteBranch', load)),
            'browser.branch.load_failed'
        )
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
            save: projectActions.save,
            copy: projectActions.copy,
            openRevision: () => setOpenRevisionFor(project),
            sync: projectActions.sync,
            deleteBranch: openDeleteBranch,
            export: () => setExportSource(project),
            delete: () => window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
                detail: {
                    projectId: project.id,
                    projectName: project.name,
                    repositoryInfo: project.repositoryInfo,
                    localOnly: project.status === ProjectStatus.Local,
                    onSuccess: () => navigate('/projects'),
                },
            })),
            unlock: () => runAction('unlock', () => unlockProject(project.id), 'browser.unlock_failed'),
            deploy: projectActions.deploy,
            compare: () => openCompareWindow(project),
        }
    }, [busyWhile, closeProject, load, navigate, project, runAction])

    // The files of the project changed and not saved yet, which the tree beside it marks.
    const pendingFileChanges = project?.compileStatus?.pendingChanges?.files ?? NO_FILE_CHANGES
    const fileChanges = useMemo(
        () => buildFileChangeMap(pendingFileChanges, project?.path, project?.name),
        [pendingFileChanges, project?.name, project?.path]
    )

    // The tree beside the page belongs to the Projects screen, which keeps it while the reader steps from the list to a
    // project and from one project to another. Until the project is read, the tree marks the one the address names.
    useReportProjectRail({
        address: projectId,
        // Until the project is read its changes are not known: the tree keeps the ones it read with the files, rather
        // than marks of the project before or none at all.
        currentFileChanges: project ? fileChanges : undefined,
        currentFolders: virtualFolders,
        currentProjectId: project?.id ?? projectId,
    }, reloadToken)

    return (
        <div className={styles.page} data-testid="project-workspace">
            <div className={styles.body}>
                {project && !error ? (
                    <>
                        <ProjectDetail
                            changedFiles={changedFiles}
                            files={files}
                            handlers={handlers}
                            onBranchSwitching={busy => setPendingId(busy ? 'switchBranch' : null)}
                            onChanged={() => load()}
                            onCreateFolder={addVirtualFolder}
                            onFilesNeeded={loadFiles}
                            onRemoveVirtualFolder={removeVirtualFolder}
                            pendingId={pendingId}
                            project={project}
                            reloadToken={reloadToken}
                            repoFeatures={repoInfo?.features}
                            repoLabel={repoLabel}
                            repoType={repoType}
                            statusReadAt={statusReadAt}
                            virtualFolders={virtualFolders}
                            headerPrefix={(
                                <span className={styles.crumb}>
                                    <Link to="/projects">{t('home.title')}</Link>
                                    <span aria-hidden>/</span>
                                </span>
                            )}
                        />
                        <CompileProblemsPanel
                            project={project}
                            statusReadAt={statusReadAt}
                            supportsBranches={supportsBranches({ features: repoInfo?.features })}
                        />
                        {/* Once the project is on screen, every wait for it is shown over what it replaces —
                                the action's own request, and the read that brings back what the action did. The
                                very first read shows the skeleton above instead, having nothing to cover. */}
                        {(loading || pendingId !== null) && <BusyVeil data-testid="project-workspace-overlay" />}
                    </>
                ) : <ProjectAbsent error={error} linkProblem={linkProblem} styles={styles} />}
            </div>
            {/* Each dialog hands the project back busy: its own spinner covers the request, and the busy
                state covers the read that follows, which is the slower half of the two. */}
            {projectDialogs}
            <ExportProjectModal
                onClose={() => setExportSource(null)}
                open={exportSource !== null}
                project={exportSource}
            />
            <OpenRevisionModal
                onClose={() => setOpenRevisionFor(null)}
                onOpened={() => void busyWhile('openRevision', load)}
                open={openRevisionFor !== null}
                project={openRevisionFor}
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

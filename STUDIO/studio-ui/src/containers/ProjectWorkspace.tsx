import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, notification, Skeleton } from 'antd'
import { createStyles } from 'antd-style'
import {
    downloadProject,
    getDesignRepositories,
    getProject,
    getProjectFiles,
    isProjectModifiedConflict,
    setProjectStatus,
    unlockProject,
} from '../services/repositories'
import { NotFoundError } from '../services'
import { ProjectStatus } from '../constants/project'
import type { Repository } from '../types/repositories'
import type { Project } from '../types/projects'
import type { FsNode } from '../types/files'
import { ProjectDetail } from './projects/ProjectDetail'
import { SaveProjectModal } from './projects/SaveProjectModal'
import { CopyProjectModal } from './projects/CopyProjectModal'
import type { ActionId, ProjectActionHandlers } from './projects/ProjectActionBar'
import { DiscardChangesModal } from './DiscardChangesModal'

const LOCAL_LOAD_API_OPTIONS = { throwError: true, suppressErrorPages: true } as const

const useStyles = createStyles(({ css, token }) => ({
    page: css`
        height: calc(100vh - 64px);
        display: flex;
        flex-direction: column;
        overflow: hidden;
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
        flex: 1;
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
    const [repositories, setRepositories] = useState<Repository[]>([])
    const [project, setProject] = useState<Project | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [files, setFiles] = useState<FsNode[] | 'loading' | 'error'>()
    const [pendingId, setPendingId] = useState<ActionId | null>(null)
    const [saveOpen, setSaveOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    const [discardCloseOpen, setDiscardCloseOpen] = useState(false)
    // Bumped on every reload so tabs that cache their own data (history, file content) refetch and reset.
    const [reloadToken, setReloadToken] = useState(0)
    // Bumped on every project-detail load so stale navigation responses cannot overwrite the current page.
    const loadGeneration = useRef(0)
    // Bumped on every reload so a stale files response never overwrites a fresh one.
    const filesGeneration = useRef(0)

    const reducedMotion = useMemo(
        () => typeof window !== 'undefined'
            && !!window.matchMedia
            && window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        []
    )

    // The whole workspace is driven by the single-project detail response, which carries the dependency
    // graph and the rules.xml-derived fields. A missing project (404) shows the not-found state.
    const load = useCallback(async () => {
        if (!projectId) {
            return
        }
        const generation = ++loadGeneration.current
        setLoading(true)
        try {
            const [repos, loaded] = await Promise.all([
                getDesignRepositories(LOCAL_LOAD_API_OPTIONS),
                getProject(projectId, { includes: ['status', 'modules']}, LOCAL_LOAD_API_OPTIONS),
            ])
            if (generation !== loadGeneration.current) {
                return
            }
            setRepositories(repos)
            setProject(loaded)
            setReloadToken(token => token + 1)
            filesGeneration.current += 1
            setFiles(undefined)
            setError(null)
        } catch (e) {
            if (generation !== loadGeneration.current) {
                return
            }
            if (e instanceof NotFoundError) {
                setProject(null)
                setError(null)
            } else {
                setError(errorMessage(e))
            }
        } finally {
            if (generation === loadGeneration.current) {
                setLoading(false)
            }
        }
    }, [projectId])

    // Drop the previous project immediately on navigation so its content never flashes under the new id.
    useEffect(() => {
        setProject(null)
    }, [projectId])

    useEffect(() => {
        void load()
    }, [load])

    const repoInfo = useMemo(() => {
        if (!project) {
            return null
        }
        return repositories.find(repo => repo.id === project.repository) ?? null
    }, [repositories, project])
    const repoLabel = useMemo(() => {
        if (!project) {
            return ''
        }
        return repoInfo?.name ?? project.repository
    }, [repoInfo, project])
    const repoType = useMemo(() => {
        if (!project) {
            return undefined
        }
        return repoInfo?.type ?? (project.status === ProjectStatus.Local ? 'repo-file' : undefined)
    }, [repoInfo, project])
    const creatableRepos = useMemo(
        () => repositories.filter(repo => repo.capabilities?.canCreateProject),
        [repositories]
    )

    // Fetch the project's files only when the Files tab becomes visible. A failed fetch shows an error state,
    // retried on the next project reload.
    const loadFiles = useCallback(() => {
        if (!project || files !== undefined) {
            return
        }
        const generation = filesGeneration.current
        setFiles('loading')
        getProjectFiles(project.id)
            .then(loaded => {
                if (generation === filesGeneration.current) {
                    setFiles(loaded)
                }
            })
            .catch(() => {
                if (generation === filesGeneration.current) {
                    setFiles('error')
                }
            })
    }, [project, files])

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

    const handlers: ProjectActionHandlers = useMemo(() => {
        const noOp: ProjectActionHandlers = {
            open: () => {},
            close: () => {},
            save: () => {},
            deploy: () => {},
            copy: () => {},
            export: () => {},
            delete: () => {},
            unlock: () => {},
        }
        if (!project) {
            return noOp
        }
        return {
            open: () => runAction('open', () => setProjectStatus(project.id, 'OPENED', true), 'browser.status_change_failed'),
            close: () => {
                if (project.status === ProjectStatus.Editing) {
                    setDiscardCloseOpen(true)
                } else {
                    void closeProject()
                }
            },
            save: () => setSaveOpen(true),
            copy: () => setCopySource(project),
            export: () => downloadProject(project.id),
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
                    detail: { ...project, selectedBranches: project.selectedBranches ?? []},
                }))
            },
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
            <div className={styles.body}>
                {project ? (
                    <ProjectDetail
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
            </div>
            <SaveProjectModal
                onClose={() => setSaveOpen(false)}
                onSaved={() => void load()}
                open={saveOpen}
                project={project}
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

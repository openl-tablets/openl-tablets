import {
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
    type MouseEvent as ReactMouseEvent,
    type ReactNode,
} from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { Empty, Tabs, Tooltip, Typography, type TabsProps } from 'antd'
import {
    BranchesOutlined,
    FileTextOutlined,
    HistoryOutlined,
    ProfileOutlined,
    RocketOutlined,
    SafetyOutlined,
    TeamOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { FsNode } from '../../types/files'
import type { RepositoryFeatures } from '../../types/repositories'
import { StatusPill } from './StatusIndicator'
import { CompileStatusBadge } from './CompileStatusBadge'
import { ProjectActionBar, type ActionId, type ProjectActionHandlers } from './ProjectActionBar'
import { FileTree } from './FileTree'
import { FilesToolbar } from './FilesToolbar'
import { FilePreviewPane } from './FilePreviewPane'
import { FolderActionsPane } from './FolderActionsPane'
import { TagsModal } from './TagsModal'
import { RevisionsPanel } from './RevisionsPanel'
import { OverviewPanel } from './OverviewPanel'
import { BranchesPanel } from './BranchesPanel'
import { PublishPanel } from './PublishPanel'
import { AccessPanel } from './AccessPanel'
import { MonoChip } from './MonoChip'
import { LocalChangesSummary } from './LocalChangesSummary'
import { buildFileChangeMap, normalizeProjectFileChanges } from './fileChanges'
import { SystemContext } from '../../contexts'
import { supportsBranches, supportsRevisionSearch } from '../../utils/repositoryFeatures'

const useStyles = createStyles(({ css, token }) => ({
    root: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    empty: css`
        margin: auto;
        padding: ${token.paddingXL}px;
    `,
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    crumb: css`
        display: flex;
        align-items: center;
        gap: 6px;
        color: ${token.colorTextTertiary};
        font-size: 14px;

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    shield: css`
        color: ${token.colorTextTertiary};
    `,
    titleRow: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        margin-top: 8px;
    `,
    titleLeft: css`
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
    `,
    title: css`
        margin: 0 !important;
        min-width: 0;
        font-size: 22px;
        font-weight: 600;
        letter-spacing: -0.01em;
    `,
    titleMuted: css`
        color: ${token.colorTextTertiary};
        text-decoration: line-through;
    `,
    tabs: css`
        flex: 1;
        min-height: 0;
        display: flex;
        flex-direction: column;

        .ant-tabs-nav {
            flex: none;
            padding: 0 16px;
            margin: 0;
        }

        /* Bound the tabs body to the available height as a flex column. Target only the body
           wrappers here, never the panes: each pane is itself an '.ant-tabs-content', and a rule on
           that class out-specifies AntD's '.ant-tabs-content-hidden { display: none }', which would
           un-hide inactive panes and stack them. */
        .ant-tabs-body-holder,
        .ant-tabs-body {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
        }

        /* The active pane fills the body and owns its scroll. The Files pane fills it exactly (its
           tree and editor scroll internally); taller panes (e.g. Publish) scroll here. */
        .ant-tabs-content-active {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            overflow: auto;
        }
    `,
    filesLayout: css`
        display: flex;
        flex: 1;
        min-height: 0;
        overflow: hidden;
    `,
    filesTree: css`
        display: flex;
        flex-direction: column;
        flex: none;
        min-height: 0;
        overflow: hidden;

        /* Toolbar + local-changes summary stay pinned; only the tree scroll wrapper (last child) grows. */
        > *:not(:last-child) {
            flex: none;
        }
    `,
    treeScroll: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    resizer: css`
        flex: none;
        width: 5px;
        cursor: col-resize;
        border-right: 1px solid ${token.colorBorderSecondary};
        transition: background 0.15s ease;

        &:hover {
            background: ${token.colorPrimaryBorder};
        }
    `,
}))

interface ProjectDetailProps {
    project: Project | null
    repoLabel: string
    repoFeatures?: RepositoryFeatures | undefined
    repoType?: string | undefined
    pendingId: ActionId | null
    handlers: ProjectActionHandlers
    files: FsNode[] | 'loading' | 'error' | undefined
    reducedMotion?: boolean
    /** Bumped whenever the project reloads, so cached tabs (history, file content) refetch and reset. */
    reloadToken?: number
    /** Rendered before the repository name, e.g. a breadcrumb link back to the projects list. */
    headerPrefix?: ReactNode
    /** Called after an edit (tags, file save) that may change the project, to refresh the list. */
    onChanged?: () => void
    /** Called when the Files tab becomes visible, so the file tree can be loaded lazily. */
    onFilesVisible?: () => void
}

/**
 * A single project's workspace: identity header, capability-driven action bar, and the Overview, Files,
 * History, Branches, Publish and Access tabs. Table editing is not hosted here yet.
 */
export const ProjectDetail = ({
    project,
    repoLabel,
    repoFeatures,
    repoType,
    pendingId,
    handlers,
    files,
    reducedMotion,
    reloadToken = 0,
    headerPrefix,
    onChanged,
    onFilesVisible,
}: ProjectDetailProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const { isUserManagementEnabled } = useContext(SystemContext)
    const [searchParams, setSearchParams] = useSearchParams()
    // The selected file lives in the URL (?file=…) so the exact view can be shared or reloaded.
    const selectedFile = searchParams.get('file')
    // Folders created in the UI but not yet on the server — shown as empty folders until a file lands
    // inside them, at which point the create-file request persists the whole chain.
    const [virtualFolders, setVirtualFolders] = useState<string[]>([])
    const addVirtualFolder = useCallback((path: string) => {
        setVirtualFolders(prev => (prev.includes(path) ? prev : [...prev, path]))
    }, [])
    const removeVirtualFolder = useCallback((path: string) => {
        setVirtualFolders(prev => prev.filter(folder => folder !== path && !folder.startsWith(`${path}/`)))
    }, [])
    // Once a file exists under a virtual folder it is real; drop it from the client-side list.
    useEffect(() => {
        if (Array.isArray(files)) {
            setVirtualFolders(prev => prev.filter(
                folder => !files.some(node => node.path === folder || node.path.startsWith(`${folder}/`))
            ))
        }
    }, [files])
    // A folder selection shows folder actions; a file selection shows its content. A virtual folder has
    // no backend node yet, but it is still a folder selection — never fetch its (missing) content.
    const selectedNode = useMemo(
        () => (Array.isArray(files) ? files.find(node => node.path === selectedFile) : undefined),
        [files, selectedFile]
    )
    // A virtual folder counts whether the selection is its full path or an intermediate ancestor segment
    // (creating "reports/2026" makes "reports" a folder too), so ancestors never render as a file preview.
    const selectedIsVirtualFolder = !!selectedFile
        && virtualFolders.some(folder => folder === selectedFile || folder.startsWith(`${selectedFile}/`))
    const selectedIsFolder = selectedNode?.type === 'folder' || selectedIsVirtualFolder
    const selectedTargetFolder = selectedIsFolder ? selectedNode?.path ?? selectedFile ?? '' : selectedNode?.basePath ?? ''
    const setSelectedFile = useCallback((path: string | null) => {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev)
            if (path) {
                next.set('file', path)
                next.set('tab', 'files')
            } else {
                next.delete('file')
            }
            return next
        }, { replace: true })
    }, [setSearchParams])
    const [editingTags, setEditingTags] = useState(false)
    const [fileFilter, setFileFilter] = useState('')
    const [treeWidth, setTreeWidth] = useState(288)
    const resizeCleanup = useRef<(() => void) | null>(null)
    const pendingFileChanges = project?.compileStatus?.pendingChanges?.files ?? []
    const localChanges = useMemo(
        () => normalizeProjectFileChanges(pendingFileChanges, project?.path, project?.name),
        [pendingFileChanges, project?.name, project?.path]
    )
    const fileChangeByPath = useMemo(
        () => buildFileChangeMap(pendingFileChanges, project?.path, project?.name),
        [pendingFileChanges, project?.name, project?.path]
    )

    const canViewHistory = project?.capabilities?.canViewHistory ?? false
    // A local project has no committed history, branches or shared access, so those tabs are hidden.
    const isLocal = project?.status === ProjectStatus.Local
    const canManageAccess = project?.capabilities?.canManage ?? false
    const canShowAccess = !isLocal && isUserManagementEnabled && canManageAccess
    const repositorySupportsBranches = supportsBranches({ features: repoFeatures })
    const repositorySupportsRevisionSearch = supportsRevisionSearch({ features: repoFeatures })
    const hasBranches = repositorySupportsBranches && !!project?.branch && !isLocal
    const tabKeys = [
        'overview',
        'files',
        ...(canViewHistory ? ['history'] : []),
        ...(hasBranches ? ['branches'] : []),
        'publish',
        ...(canShowAccess ? ['access'] : []),
    ]
    // The active tab is URL-driven too, so a shared ?file link lands on the Files tab.
    const requestedTab = searchParams.get('tab') ?? (selectedFile ? 'files' : 'overview')
    const activeTab = tabKeys.includes(requestedTab) ? requestedTab : 'overview'

    useEffect(() => {
        if (project && activeTab === 'files') {
            onFilesVisible?.()
        }
    }, [activeTab, onFilesVisible, project])

    const initialTags = useMemo(() => project?.tags ?? {}, [project?.tags])
    const stopResize = useCallback(() => {
        const cleanup = resizeCleanup.current
        resizeCleanup.current = null
        cleanup?.()
    }, [])

    useEffect(() => stopResize, [stopResize])

    // Drag the divider between the file tree and the preview to resize the tree column.
    const startResize = (event: ReactMouseEvent) => {
        event.preventDefault()
        stopResize()
        const startX = event.clientX
        const startWidth = treeWidth
        const onMove = (moveEvent: MouseEvent) => {
            setTreeWidth(Math.min(560, Math.max(200, startWidth + moveEvent.clientX - startX)))
        }
        const onUp = () => {
            stopResize()
        }
        resizeCleanup.current = () => {
            document.removeEventListener('mousemove', onMove)
            document.removeEventListener('mouseup', onUp)
        }
        document.addEventListener('mousemove', onMove)
        document.addEventListener('mouseup', onUp)
    }

    if (!project) {
        return (
            <div className={styles.root} data-testid="project-detail">
                <Empty className={styles.empty} data-testid="project-detail-empty" description={t('browser.select_project_hint')} />
            </div>
        )
    }

    const muted = project.status === ProjectStatus.Deleted
    const canWriteFiles = project.capabilities?.canWrite ?? false

    const items: TabsProps['items'] = [
        {
            key: 'overview',
            label: <><ProfileOutlined /> {t('browser.tab_overview')}</>,
            children: (
                <OverviewPanel
                    onEditTags={() => setEditingTags(true)}
                    onUnlock={handlers.unlock}
                    project={project}
                    repoLabel={repoLabel}
                    repoType={repoType}
                    supportsBranches={repositorySupportsBranches}
                />
            ),
        },
        {
            key: 'files',
            label: <><FileTextOutlined /> {t('browser.tab_files')}</>,
            children: activeTab === 'files' && (
                <div className={styles.filesLayout}>
                    <div className={styles.filesTree} style={{ width: treeWidth }}>
                        <FilesToolbar
                            canWrite={canWriteFiles}
                            filter={fileFilter}
                            onChanged={() => onChanged?.()}
                            onCreateFolder={addVirtualFolder}
                            onFilterChange={setFileFilter}
                            projectId={project.id}
                            targetFolder={selectedTargetFolder}
                        />
                        <LocalChangesSummary changes={localChanges} />
                        <div className={styles.treeScroll}>
                            <FileTree
                                changes={fileChangeByPath}
                                files={files}
                                filter={fileFilter}
                                onSelectFile={setSelectedFile}
                                projectId={project.id}
                                reducedMotion={reducedMotion ?? false}
                                selectedPath={selectedFile}
                                virtualFolders={virtualFolders}
                            />
                        </div>
                    </div>
                    <div aria-hidden className={styles.resizer} data-testid="file-tree-resizer" onMouseDown={startResize} />
                    {selectedIsFolder && selectedFile ? (
                        <FolderActionsPane
                            canDelete={canWriteFiles}
                            canWrite={canWriteFiles}
                            onChanged={() => onChanged?.()}
                            onDeleted={() => { setSelectedFile(null); onChanged?.() }}
                            onRemoveVirtual={() => { removeVirtualFolder(selectedFile); setSelectedFile(null) }}
                            path={selectedFile}
                            projectId={project.id}
                            virtual={selectedIsVirtualFolder}
                        />
                    ) : (
                        <FilePreviewPane
                            branch={repositorySupportsBranches ? project.branch : null}
                            canDelete={canWriteFiles}
                            canWrite={canWriteFiles}
                            onChanged={() => onChanged?.()}
                            onDeleted={() => { setSelectedFile(null); onChanged?.() }}
                            path={selectedFile}
                            projectId={project.id}
                            projectName={project.name}
                            reloadToken={reloadToken}
                            repositoryId={project.repository}
                        />
                    )}
                </div>
            ),
        },
        ...(canViewHistory ? [{
            key: 'history',
            label: <><HistoryOutlined /> {t('browser.tab_history')}</>,
            children: activeTab === 'history' && (
                <RevisionsPanel
                    branch={repositorySupportsBranches ? project.branch : null}
                    canCompare={project.capabilities?.canCompare ?? false}
                    currentRevision={project.revision}
                    onOpened={() => onChanged?.()}
                    projectId={project.id}
                    projectName={project.name}
                    reloadToken={reloadToken}
                    repositoryId={project.repository}
                    searchable={repositorySupportsRevisionSearch}
                />
            ),
        }] : []),
        ...(hasBranches ? [{
            key: 'branches',
            label: <><BranchesOutlined /> {t('browser.tab_branches')}</>,
            children: (
                <BranchesPanel
                    canWrite={canWriteFiles}
                    currentBranch={project.branch}
                    onChanged={() => onChanged?.()}
                    projectId={project.id}
                    projectName={project.name}
                    repositoryId={project.repository}
                    selectedBranches={project.selectedBranches ?? []}
                />
            ),
        }] : []),
        {
            key: 'publish',
            label: <><RocketOutlined /> {t('browser.tab_publish')}</>,
            children: (
                <PublishPanel
                    canDeploy={project.capabilities?.canDeploy ?? false}
                    canWrite={canWriteFiles}
                    onChanged={() => onChanged?.()}
                    onDeploy={handlers.deploy}
                    projectId={project.id}
                    projectName={project.name}
                    reloadToken={reloadToken}
                />
            ),
        },
        ...(canShowAccess ? [{
            key: 'access',
            label: <><TeamOutlined /> {t('browser.tab_management')}</>,
            children: (
                <AccessPanel
                    canManage={project.capabilities?.canManage ?? false}
                    projectId={project.id}
                    projectName={project.name}
                />
            ),
        }] : []),
    ]

    const onTabChange = (key: string) => {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev)
            next.set('tab', key)
            return next
        }, { replace: true })
    }

    return (
        <div className={styles.root} data-testid="project-detail">
            <div className={styles.header}>
                <div className={styles.crumb}>
                    {headerPrefix}
                    <MonoChip>{repoLabel}</MonoChip>
                    {hasBranches && (
                        <>
                            <span aria-hidden>/</span>
                            <MonoChip>{project.branch}</MonoChip>
                            {project.branchProtected && (
                                <Tooltip title={t('browser.branch.protected_tag')}>
                                    <SafetyOutlined className={styles.shield} data-testid="crumb-branch-protected" />
                                </Tooltip>
                            )}
                        </>
                    )}
                </div>
                <div className={styles.titleRow}>
                    <div className={styles.titleLeft}>
                        <Typography.Title
                            className={cx(styles.title, muted && styles.titleMuted)}
                            ellipsis={{ tooltip: project.name }}
                            level={3}
                        >
                            {project.name}
                        </Typography.Title>
                        <StatusPill status={project.status} testId={`status-${project.id}`} />
                        <CompileStatusBadge
                            branch={repositorySupportsBranches ? project.branch || null : null}
                            initialStatus={project.compileStatus}
                            projectId={project.id}
                        />
                    </div>
                    <ProjectActionBar handlers={handlers} pendingId={pendingId} project={project} />
                </div>
            </div>
            <Tabs activeKey={activeTab} className={styles.tabs} data-testid="project-tabs" items={items} onChange={onTabChange} />
            <TagsModal
                initialTags={initialTags}
                onClose={() => setEditingTags(false)}
                open={editingTags}
                projectId={project.id}
                onSaved={() => {
                    setEditingTags(false)
                    onChanged?.()
                }}
            />
        </div>
    )
}

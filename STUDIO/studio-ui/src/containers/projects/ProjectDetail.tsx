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
import { Empty, Skeleton, Tabs, Typography, type TabsProps } from 'antd'
import {
    FileTextOutlined,
    HistoryOutlined,
    ProfileOutlined,
    RocketOutlined,
    TeamOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { FsNode } from '../../types/files'
import type { RepositoryFeatures } from '../../types/repositories'
import { StatusMark } from './StatusIndicator'
import { LiveCompileDot } from './CompileIndicator'
import { ProjectActionBar, type ActionId, type ProjectActionHandlers } from './ProjectActionBar'
import { FileTree } from './FileTree'
import { FilesToolbar } from './FilesToolbar'
import { projectFolders } from './ProjectFolderInput'
import { FilePreviewPane } from './FilePreviewPane'
import { FolderActionsPane } from './FolderActionsPane'
import { RevisionsPanel } from './RevisionsPanel'
import { OverviewPanel } from './OverviewPanel'
import { PublishPanel } from './PublishPanel'
import { AccessPanel } from './AccessPanel'
import { ValueText } from './ValueText'
import { BranchSwitcher } from './BranchSwitcher'
import { LocalChangesSummary } from './LocalChangesSummary'
import { buildFileChangeMap, normalizeProjectFileChanges } from './fileChanges'
import { SystemContext } from '../../contexts'
import { supportsBranches, supportsRevisionSearch } from '../../utils/repositoryFeatures'

const useStyles = createStyles(({ css, token }) => ({
    root: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    empty: css`
        margin: auto;
        padding: ${token.paddingXL}px;
    `,
    header: css`
        min-width: 0;
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
    titleRow: css`
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        min-width: 0;
        margin-top: 8px;
    `,
    titleLeft: css`
        display: flex;
        flex: 1 1 auto;
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
        min-width: 0;
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
            min-width: 0;
            min-height: 0;
        }

        /* The active pane fills the body and owns its scroll. The Files pane fills it exactly (its
           tree and editor scroll internally); taller panes (e.g. Deploy Configuration) scroll here. */
        .ant-tabs-content-active {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-width: 0;
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
        /* The divider belongs to the tree column, so every border inside it ends exactly on the line. */
        border-right: 1px solid ${token.colorBorderSecondary};

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
    filePlaceholder: css`
        flex: 1;
        min-width: 0;
        padding: 16px;
    `,
    resizer: css`
        /* A grab strip laid over the divider: it takes no width, so the two panes stay flush with it. */
        position: relative;
        flex: none;
        width: 0;

        &::after {
            content: '';
            position: absolute;
            inset: 0 -4px;
            cursor: col-resize;
            transition: background 0.15s ease;
        }

        &:hover::after {
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
    /** The files the reload behind the current token is known to cover; null means anything. */
    changedFiles?: string[] | null
    /** Rendered before the repository name, e.g. a breadcrumb link back to the projects list. */
    headerPrefix?: ReactNode
    /** Called after an edit (tags, file save) that may change the project, to refresh the list. */
    onChanged?: () => void
    /** Called when the Files tab becomes visible, so the file tree can be loaded lazily. */
    onFilesVisible?: () => void
}

/**
 * A single project's workspace: identity header, capability-driven action bar, and the Overview,
 * Revisions, Files, Branches, Deploy Configuration and Access tabs. Table editing is not hosted here yet.
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
    changedFiles = null,
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
    // Whether the tree is loaded well enough to tell a file from a folder. Until it is, a selection is of
    // an unknown kind — an extension-less folder name (say "__MACOSX") would otherwise be read as a text
    // file and its content fetched, which fails. A virtual folder is known without the tree.
    const selectionClassified = Array.isArray(files) || selectedIsVirtualFolder
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
    const [fileFilter, setFileFilter] = useState('')
    const [treeWidth, setTreeWidth] = useState(288)
    const resizeCleanup = useRef<(() => void) | null>(null)
    const pendingFileChanges = project?.compileStatus?.pendingChanges?.files ?? []
    const localChanges = useMemo(
        () => normalizeProjectFileChanges(pendingFileChanges, project?.path, project?.name),
        [pendingFileChanges, project?.name, project?.path]
    )
    const folders = useMemo(() => projectFolders(Array.isArray(files) ? files : undefined), [files])
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

    // What the right side of the Files tab shows for the selection: a placeholder while the freshly
    // selected path is still being classified, the folder actions for a folder, the preview for a file.
    const filePane = () => {
        if (selectedFile && !selectionClassified) {
            return (
                <div className={styles.filePlaceholder} data-testid="file-pane-loading">
                    <Skeleton active paragraph={{ rows: 6 }} title={false} />
                </div>
            )
        }
        if (selectedIsFolder && selectedFile) {
            return (
                <FolderActionsPane
                    canDelete={canWriteFiles}
                    canWrite={canWriteFiles}
                    folders={folders}
                    onChanged={() => onChanged?.()}
                    onDeleted={() => { setSelectedFile(null); onChanged?.() }}
                    onRemoveVirtual={() => { removeVirtualFolder(selectedFile); setSelectedFile(null) }}
                    path={selectedFile}
                    projectId={project.id}
                    virtual={selectedIsVirtualFolder}
                />
            )
        }
        return (
            <FilePreviewPane
                branch={repositorySupportsBranches ? project.branch : null}
                canDelete={canWriteFiles}
                canWrite={canWriteFiles}
                changedFiles={changedFiles}
                folders={folders}
                onChanged={() => onChanged?.()}
                onDeleted={() => { setSelectedFile(null); onChanged?.() }}
                onMoved={newPath => { setSelectedFile(newPath); onChanged?.() }}
                path={selectedFile}
                projectId={project.id}
                projectName={project.name}
                reloadToken={reloadToken}
                repositoryId={project.repository}
            />
        )
    }

    const items: TabsProps['items'] = [
        {
            key: 'overview',
            label: <><ProfileOutlined /> {t('browser.tab_overview')}</>,
            children: (
                <OverviewPanel
                    onChanged={() => onChanged?.()}
                    onUnlock={handlers.unlock}
                    project={project}
                    reloadToken={reloadToken}
                    repoLabel={repoLabel}
                    repoType={repoType}
                    supportsBranches={repositorySupportsBranches}
                />
            ),
        },
        ...(canViewHistory ? [{
            key: 'history',
            label: <><HistoryOutlined /> {t('browser.tab_history')}</>,
            children: activeTab === 'history' && (
                <RevisionsPanel
                    branch={repositorySupportsBranches ? project.branch : null}
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
        {
            key: 'files',
            label: <><FileTextOutlined /> {t('browser.tab_files')}</>,
            children: activeTab === 'files' && (
                <div className={styles.filesLayout}>
                    <div className={styles.filesTree} style={{ width: treeWidth }}>
                        <FilesToolbar
                            canWrite={canWriteFiles}
                            filter={fileFilter}
                            folders={folders}
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
                    {filePane()}
                </div>
            ),
        },
        {
            key: 'publish',
            label: <><RocketOutlined /> {t('browser.tab_publish')}</>,
            children: (
                <PublishPanel
                    canWrite={canWriteFiles}
                    onChanged={() => onChanged?.()}
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
                    <ValueText>{repoLabel}</ValueText>
                    {hasBranches && (
                        <>
                            <span aria-hidden>/</span>
                            <BranchSwitcher
                                currentBranch={project.branch}
                                currentBranchDefault={project.branchDefault}
                                currentBranchProtected={project.branchProtected}
                                data-testid="crumb-branch"
                                onSwitched={() => onChanged?.()}
                                projectId={project.id}
                                selectedBranches={project.selectedBranches ?? []}
                            />
                        </>
                    )}
                </div>
                <div className={styles.titleRow}>
                    <div className={styles.titleLeft}>
                        <StatusMark status={project.status} testId={`status-${project.id}`} />
                        <Typography.Title
                            className={cx(styles.title, muted && styles.titleMuted)}
                            ellipsis={{ tooltip: project.name }}
                            level={3}
                        >
                            {project.name}
                        </Typography.Title>
                        <LiveCompileDot branch={project.branch ?? null} compileStatus={project.compileStatus} projectId={project.id} status={project.status} />
                    </div>
                    <ProjectActionBar handlers={handlers} pendingId={pendingId} project={project} />
                </div>
            </div>
            <Tabs activeKey={activeTab} className={styles.tabs} data-testid="project-tabs" items={items} onChange={onTabChange} />
        </div>
    )
}

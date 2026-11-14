import {
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Empty, Skeleton, Tabs, type TabsProps } from 'antd'
import {
    HistoryOutlined,
    ProfileOutlined,
    RocketOutlined,
    TeamOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { COMPILE_RELEVANT_STATUSES } from '../../constants/projectStatusMeta'
import { useLiveProjectStatus } from '../../hooks/useLiveProjectStatus'
import type { Project, ProjectModule } from '../../types/projects'
import type { FsNode } from '../../types/files'
import { localizationRoute, moduleRoute } from '../../services/projectId'
import type { RepositoryFeatures } from '../../types/repositories'
import { StatusMark } from './StatusIndicator'
import { LiveCompileDot } from './CompileIndicator'
import { WorkspaceHeader } from '../../components/WorkspaceHeader'
import { ProjectActionBar, type ProjectActionHandlers } from './ProjectActionBar'
import type { BusyId } from './projectActions'
import { AddFilesDialogs, type AddFilesKind } from './AddFilesMenu'
import { projectFolders } from './ProjectFolderInput'
import { parentFolder } from './projectPaths'
import { FilePreviewPane } from './FilePreviewPane'
import { FolderActionsPane } from './FolderActionsPane'
import { RevisionsPanel } from './RevisionsPanel'
import { OverviewPanel } from './OverviewPanel'
import { PublishPanel } from './PublishPanel'
import { AccessPanel } from './AccessPanel'
import { ValueText } from './ValueText'
import { BranchSwitcher } from './BranchSwitcher'
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
    /** A breadcrumb value (repository, branch): reads like the "Projects" link — secondary colour, crumb size. */
    crumbValue: css`
        color: ${token.colorTextSecondary};
    `,
    /** The row of the tabs, whether their content or a file is shown under it. */
    tabsNav: css`
        .ant-tabs-nav {
            flex: none;
            padding: 0 16px;
            margin: 0;
        }
    `,
    tabs: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;

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

        /* The active pane fills the body and owns its scroll: taller panes (e.g. Deploy Configuration)
           scroll here. */
        .ant-tabs-content-active {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-width: 0;
            min-height: 0;
            overflow: auto;
        }
    `,
    /** The tabs above a file or a folder: only their row is shown, none of them picked. */
    tabsRow: css`
        flex: none;
    `,
    /** A file or a folder picked in the projects tree, in place of the content of a tab. */
    filePane: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: hidden;
    `,
    filePlaceholder: css`
        flex: 1;
        min-width: 0;
        padding: 16px;
    `,
}))

interface ProjectDetailProps {
    project: Project | null
    /** When the read behind the shown project started; the compile dot weighs pushed statuses against it. */
    statusReadAt?: number | undefined
    repoLabel: string
    repoFeatures?: RepositoryFeatures | undefined
    repoType?: string | undefined
    /** What the project is busy with, or null: the action bar spins on it and blocks everything else. */
    pendingId: BusyId | null
    handlers: ProjectActionHandlers
    /** The files of the project, read once a file or a folder of it is shown, or files are added to it. */
    files: FsNode[] | 'loading' | 'error' | undefined
    /**
     * Folders added in the browser and not on the server yet: a folder stays empty, and so is not saved, until a
     * file is created inside it.
     */
    virtualFolders: string[]
    onCreateFolder: (path: string) => void
    onRemoveVirtualFolder: (path: string) => void
    /** Bumped whenever the project reloads, so cached tabs (history, file content) refetch and reset. */
    reloadToken?: number
    /** The files the reload behind the current token is known to cover; null means anything. */
    changedFiles?: string[] | null
    /** Rendered before the repository name, e.g. a breadcrumb link back to the projects list. */
    headerPrefix?: ReactNode
    /**
     * Called after an edit (tags, file save) that may change the project, to refresh the list. The promise
     * it returns tells the caller when the refreshed project is on screen.
     */
    onChanged?: (() => void | Promise<unknown>) | undefined
    /** Whether a branch switch — request and reload — is running, so the project can be marked busy. */
    onBranchSwitching?: ((busy: boolean) => void) | undefined
    /** Called when the files of the project are needed: a file or a folder is shown, or files are added. */
    onFilesNeeded?: (() => void) | undefined
}

/**
 * The name of the module each workbook of the project is, indexed by the file it is written in.
 *
 * <p>A declaration whose path is a pattern is no file of its own: the modules it matched are, and those
 * are the ones a reader opens.
 */
const moduleNamesByFile = (modules: ProjectModule[] | undefined): Record<string, string> => {
    const named: Record<string, string> = {}
    for (const module of modules ?? []) {
        for (const resolved of module.modules ?? [module]) {
            if (resolved.name && resolved.path) {
                named[resolved.path] = resolved.name
            }
        }
    }
    return named
}

/**
 * A single project's workspace: identity header, capability-driven action bar, and the Overview, Revisions,
 * Deploy Configuration and Management tabs. A file or a folder picked in the projects tree (`?file=`) is shown
 * in place of the content of a tab, with its actions.
 */
export const ProjectDetail = ({
    project,
    statusReadAt,
    repoLabel,
    repoFeatures,
    repoType,
    pendingId,
    handlers,
    files,
    virtualFolders,
    onCreateFolder,
    onRemoveVirtualFolder,
    reloadToken = 0,
    changedFiles = null,
    headerPrefix,
    onChanged,
    onBranchSwitching,
    onFilesNeeded,
}: ProjectDetailProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const { isUserManagementEnabled } = useContext(SystemContext)
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    // The selected file lives in the URL (?file=…) so the exact view can be shared or reloaded, and so does the
    // line of it a compilation message sent the reader to (?line=…).
    const selectedFile = searchParams.get('file')
    const selectedLine = Number(searchParams.get('line')) || undefined
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
    // A workbook the project declares as a module opens in the editor, so the file pane offers that
    // rather than an export of a file nothing on this screen can show.
    const moduleFiles = useMemo(() => moduleNamesByFile(project?.descriptor?.modules), [project?.descriptor?.modules])
    const selectedIsFolder = selectedNode?.type === 'folder' || selectedIsVirtualFolder
    // The Add menu of the action bar adds where the reader is: into the folder shown, beside the file shown, and at
    // the root of the project while a tab is shown.
    const addTarget = !selectedFile || selectedIsFolder
        ? selectedFile ?? ''
        : parentFolder(selectedFile)
    // Whether the tree is loaded well enough to tell a file from a folder. Until it is, a selection is of
    // an unknown kind — an extension-less folder name (say "__MACOSX") would otherwise be read as a text
    // file and its content fetched, which fails. A virtual folder is known without the tree.
    const selectionClassified = Array.isArray(files) || selectedIsVirtualFolder
    // The selection and the tab replace the current URL rather than grow the history: they live in the
    // query so a view can be shared and reloaded, not so the back button can step through every click.
    const updateSearch = useCallback((edit: (params: URLSearchParams) => void) => {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev)
            edit(next)
            return next
        }, { replace: true })
    }, [setSearchParams])
    // A file or a folder is shown in place of the content of a tab, so picking one leaves the tab.
    const setSelectedFile = useCallback((path: string | null) => updateSearch(params => {
        if (path) {
            params.set('file', path)
            params.delete('tab')
        } else {
            params.delete('file')
        }
        params.delete('line')
    }), [updateSearch])
    // A tree that comes back without the selected path no longer holds it — the project was closed and
    // dropped its local file, the file was deleted, a revision without it was opened. The selection is
    // stale and is dropped, so the pane falls back to its empty state and stops asking the server for a
    // file that is not there any more.
    //
    // Only a newly loaded tree judges the selection — hence the single dependency, and the condition
    // stays inside the effect rather than becoming a render-time value: a file just moved is missing
    // from the tree still on screen, and clearing it there would blank the pane for the length of the
    // reload that follows the move, which brings a tree holding it again. Judging a tree alone is
    // enough because the selection itself only ever changes to a path of the rendered tree (the tree
    // is what the user clicks) — the just-moved path above being the one exception.
    useEffect(() => {
        if (Array.isArray(files) && selectedFile && !selectedNode && !selectedIsVirtualFolder) {
            setSelectedFile(null)
        }
    }, [files])
    const folders = useMemo(() => projectFolders(Array.isArray(files) ? files : undefined), [files])
    // The dialog of the Add menu of the action bar.
    const [adding, setAdding] = useState<AddFilesKind | null>(null)
    // An empty folder added from a dialog is shown at once: it is where its first file is added from.
    const createFolder = useCallback((path: string) => {
        onCreateFolder(path)
        setSelectedFile(path)
    }, [onCreateFolder, setSelectedFile])

    const canViewHistory = project?.capabilities?.canViewHistory ?? false
    // A local project has no committed history, branches or shared access, so those tabs are hidden.
    const isLocal = project?.status === ProjectStatus.Local
    const canManageAccess = project?.capabilities?.canManage ?? false
    const canShowAccess = !isLocal && isUserManagementEnabled && canManageAccess
    const repositorySupportsBranches = supportsBranches({ features: repoFeatures })
    // The compilation of the project, whose messages mark where a Markdown file of a module breaks the format.
    const compiled = !!project && COMPILE_RELEVANT_STATUSES.has(project.status)
    const compileStatus = useLiveProjectStatus(project?.id ?? '', project?.branch ?? null, compiled,
        compiled ? project.compileStatus ?? null : null, statusReadAt)
    const repositorySupportsRevisionSearch = supportsRevisionSearch({ features: repoFeatures })
    const hasBranches = repositorySupportsBranches && !!project?.branch && !isLocal
    const tabKeys = [
        'overview',
        ...(canViewHistory ? ['history'] : []),
        'publish',
        ...(canShowAccess ? ['access'] : []),
    ]
    // The active tab is URL-driven too. A shared ?file link shows the file, with no tab picked.
    const requestedTab = searchParams.get('tab') ?? 'overview'
    const activeTab = tabKeys.includes(requestedTab) ? requestedTab : 'overview'

    // A file or a folder is told apart, and the folders offered as destinations, by the files of the project.
    useEffect(() => {
        if (project && selectedFile) {
            onFilesNeeded?.()
        }
    }, [onFilesNeeded, project, selectedFile])

    if (!project) {
        return (
            <div className={styles.root} data-testid="project-detail">
                <Empty className={styles.empty} data-testid="project-detail-empty" description={t('browser.select_project_hint')} />
            </div>
        )
    }

    const muted = project.status === ProjectStatus.Deleted
    const canWriteFiles = project.capabilities?.canWrite ?? false

    // What the page shows for a file or a folder picked in the tree: a placeholder while the freshly
    // selected path is still being classified, the folder actions for a folder, the preview for a file.
    const filePane = (path: string) => {
        if (files === 'error' && !selectedIsVirtualFolder) {
            return <Empty className={styles.empty} data-testid="file-pane-error" description={t('browser.files_error')} />
        }
        if (!selectionClassified) {
            return (
                <div className={styles.filePlaceholder} data-testid="file-pane-loading">
                    <Skeleton active paragraph={{ rows: 6 }} title={false} />
                </div>
            )
        }
        if (selectedIsFolder) {
            return (
                <FolderActionsPane
                    canDelete={canWriteFiles}
                    canWrite={canWriteFiles}
                    folders={folders}
                    onChanged={() => onChanged?.()}
                    onCreateFolder={createFolder}
                    onDeleted={() => { setSelectedFile(null); void onChanged?.() }}
                    onOpenLocalization={() => navigate(localizationRoute(project.id))}
                    onRemoveVirtual={() => { onRemoveVirtualFolder(path); setSelectedFile(null) }}
                    path={path}
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
                compileStatus={compileStatus}
                folders={folders}
                line={selectedLine}
                modules={moduleFiles}
                onChanged={() => onChanged?.()}
                onDeleted={() => { setSelectedFile(null); void onChanged?.() }}
                onMoved={newPath => { setSelectedFile(newPath); void onChanged?.() }}
                onOpenLocalization={() => navigate(localizationRoute(project.id))}
                onOpenModule={moduleName => navigate(moduleRoute(project.id, moduleName))}
                onOpenTable={(moduleName, tableId) => navigate(moduleRoute(project.id, moduleName, tableId))}
                path={path}
                projectId={project.id}
                projectName={project.name}
                projectOpened={project.status !== ProjectStatus.Closed}
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
                    busy={pendingId !== null}
                    onBranchSwitching={onBranchSwitching}
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
                    branch={project.branch}
                    headerSearch
                    currentRevision={project.revision}
                    onOpened={() => onChanged?.()}
                    projectId={project.id}
                    reloadToken={reloadToken}
                    searchable={repositorySupportsRevisionSearch}
                />
            ),
        }] : []),
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

    // A tab takes the place of the file or the folder shown, so the link the reader copies from it carries no file.
    const onTabChange = (key: string) => updateSearch(params => {
        params.set('tab', key)
        params.delete('file')
        params.delete('line')
    })

    return (
        <div className={styles.root} data-testid="project-detail">
            <WorkspaceHeader
                muted={muted}
                title={project.name}
                titleBefore={<StatusMark status={project.status} testId={`status-${project.id}`} />}
                actions={(
                    <ProjectActionBar
                        handlers={handlers}
                        pendingId={pendingId}
                        project={project}
                        onAddFiles={kind => {
                            onFilesNeeded?.()
                            setAdding(kind)
                        }}
                    />
                )}
                crumbs={(
                    <>
                        {headerPrefix}
                        <ValueText className={styles.crumbValue}>{repoLabel}</ValueText>
                        {hasBranches && (
                            <>
                                <span aria-hidden>/</span>
                                <BranchSwitcher
                                    currentBranch={project.branch}
                                    currentBranchDefault={project.branchDefault}
                                    currentBranchProtected={project.branchProtected}
                                    data-testid="crumb-branch"
                                    disabled={pendingId !== null}
                                    onBusyChange={onBranchSwitching}
                                    onSwitched={() => onChanged?.()}
                                    projectId={project.id}
                                    tone="secondary"
                                />
                            </>
                        )}
                    </>
                )}
                titleAfter={(
                    <LiveCompileDot
                        branch={project.branch ?? null}
                        compileStatus={project.compileStatus}
                        projectId={project.id}
                        status={project.status}
                        statusReadAt={statusReadAt}
                    />
                )}
            />
            {selectedFile ? (
                <>
                    {/* The tabs stay, none of them picked: any of them leads back from the file. */}
                    <Tabs
                        activeKey=""
                        className={cx(styles.tabsNav, styles.tabsRow)}
                        data-testid="project-tabs"
                        items={items.map(({ key, label }) => ({ key, label }))}
                        onChange={onTabChange}
                    />
                    <div className={styles.filePane} data-testid="project-file-pane">{filePane(selectedFile)}</div>
                </>
            ) : (
                <Tabs
                    activeKey={activeTab}
                    className={cx(styles.tabsNav, styles.tabs)}
                    data-testid="project-tabs"
                    items={items}
                    onChange={onTabChange}
                />
            )}
            <AddFilesDialogs
                folders={folders}
                kind={adding}
                onChanged={() => onChanged?.()}
                onClose={() => setAdding(null)}
                onCreateFolder={createFolder}
                projectId={project.id}
                targetFolder={addTarget}
            />
        </div>
    )
}

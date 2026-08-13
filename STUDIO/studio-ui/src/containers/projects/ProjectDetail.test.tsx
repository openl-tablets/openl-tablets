import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { FsNode } from '../../types/files'
import type { Project } from '../../types/projects'
import type { RepositoryFeatures } from '../../types/repositories'
import { SystemContext } from '../../contexts'
import { ProjectDetail } from './ProjectDetail'

const {
    branchSwitcherMock,
    filePreviewMock,
    filesToolbarMock,
    localChangesSummaryMock,
    revisionsPanelMock,
    searchParamsMock,
    setSearchParamsMock,
} = vi.hoisted(() => ({
    branchSwitcherMock: vi.fn(),
    filePreviewMock: vi.fn(),
    filesToolbarMock: vi.fn(),
    localChangesSummaryMock: vi.fn(),
    revisionsPanelMock: vi.fn(),
    searchParamsMock: new URLSearchParams('tab=files'),
    setSearchParamsMock: vi.fn(),
}))

vi.mock('react-router-dom', () => ({
    useSearchParams: () => [searchParamsMock, setSearchParamsMock],
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

// The title dot subscribes to the status channel; it has its own tests.
vi.mock('./CompileIndicator', () => ({
    LiveCompileDot: () => <span data-testid="live-compile-dot" />,
}))

vi.mock('@ant-design/icons', () => ({
    BranchesOutlined: () => null,
    CloudUploadOutlined: () => null,
    EditOutlined: () => null,
    FileTextOutlined: () => null,
    FolderOpenOutlined: () => null,
    HistoryOutlined: () => null,
    InboxOutlined: () => null,
    MinusCircleOutlined: () => null,
    ProfileOutlined: () => null,
    RocketOutlined: () => null,
    SafetyOutlined: () => null,
    TeamOutlined: () => null,
}))

interface Item {
    key: string
    label?: unknown
    children?: unknown
}

vi.mock('antd', () => {
    const Empty = ({ description, ...rest }: Record<string, unknown>) => <div {...rest}>{description as never}</div>
    const Tabs = ({ activeKey, items, onChange }: { activeKey?: string, items?: Item[], onChange?: (key: string) => void }) => (
        <div data-active-key={activeKey}>
            {items?.map(item => (
                <div key={item.key}>
                    <button data-testid={`tab-${item.key}`} onClick={() => onChange?.(item.key as string)} type="button">
                        {item.key as string}
                    </button>
                    {item.children as never}
                </div>
            ))}
        </div>
    )
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Typography = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    Typography.Title = ({ children }: Record<string, unknown>) => <h3>{children as never}</h3>
    const Skeleton = () => <div data-testid="skeleton" />
    return { Empty, Skeleton, Tabs, Tooltip, Typography }
})

vi.mock('./StatusIndicator', () => ({
    StatusMark: ({ status }: { status: string }) => <span data-testid="status-mark">{status}</span>,
}))
vi.mock('./ProjectActionBar', () => ({ ProjectActionBar: () => null }))
vi.mock('./FileTree', () => ({
    FileTree: ({ onSelectFile }: { onSelectFile: (path: string | null) => void }) => (
        <div data-testid="file-tree">
            <button data-testid="file-tree-clear" onClick={() => onSelectFile(null)} type="button">clear</button>
            <button data-testid="file-tree-select" onClick={() => onSelectFile('rules/new.txt')} type="button">select</button>
            <button data-testid="file-tree-select-virtual" onClick={() => onSelectFile('drafts/wip')} type="button">
                select virtual
            </button>
        </div>
    ),
}))
vi.mock('./FilesToolbar', () => ({
    FilesToolbar: (props: { onChanged?: () => void, onFilterChange?: (value: string) => void, onCreateFolder?: (path: string) => void }) => {
        filesToolbarMock(props)
        return (
            <div data-testid="files-toolbar">
                <button data-testid="files-filter" onClick={() => props.onFilterChange?.('xlsx')} type="button">filter</button>
                <button data-testid="files-changed" onClick={() => props.onChanged?.()} type="button">changed</button>
                <button data-testid="files-create-folder" onClick={() => props.onCreateFolder?.('drafts/wip')} type="button">new folder</button>
            </div>
        )
    },
}))
vi.mock('./FilePreviewPane', () => ({
    // The pane is handed the selected path, so the props of its last render tell what the selection
    // settled on — read them through `latestFilePreviewProps`, never an earlier call.
    FilePreviewPane: (props: {
        onChanged?: () => void
        onDeleted?: () => void
        onMoved?: (path: string) => void
        path: string | null
    }) => {
        const { onChanged, onDeleted, onMoved } = props
        filePreviewMock(props)
        return (
            <div data-testid="file-preview">
                <button data-testid="preview-changed" onClick={() => onChanged?.()} type="button">changed</button>
                <button data-testid="preview-deleted" onClick={() => onDeleted?.()} type="button">deleted</button>
                <button data-testid="preview-moved" onClick={() => onMoved?.('rules/Moved.xlsx')} type="button">moved</button>
            </div>
        )
    },
}))
vi.mock('./FolderActionsPane', () => ({
    FolderActionsPane: ({ onChanged, onDeleted }: { onChanged?: () => void, onDeleted?: () => void }) => (
        <div data-testid="folder-actions">
            <button data-testid="folder-changed" onClick={() => onChanged?.()} type="button">changed</button>
            <button data-testid="folder-deleted" onClick={() => onDeleted?.()} type="button">deleted</button>
        </div>
    ),
}))
vi.mock('./LocalChangesSummary', () => ({
    LocalChangesSummary: (props: unknown) => {
        localChangesSummaryMock(props)
        return <div data-testid="local-changes-summary" />
    },
}))
vi.mock('./RevisionsPanel', () => ({
    RevisionsPanel: (props: { onOpened?: () => void }) => {
        revisionsPanelMock(props)
        return (
            <div data-testid="history-panel">
                <button data-testid="history-opened" onClick={() => props.onOpened?.()} type="button">opened</button>
            </div>
        )
    },
}))
vi.mock('./OverviewPanel', () => ({
    OverviewPanel: () => <div data-testid="overview-panel" />,
}))
vi.mock('./PublishPanel', () => ({
    PublishPanel: ({ onChanged }: { onChanged?: () => void }) => (
        <div data-testid="publish-panel">
            <button data-testid="publish-changed" onClick={() => onChanged?.()} type="button">changed</button>
        </div>
    ),
}))
vi.mock('./AccessPanel', () => ({ AccessPanel: () => <div data-testid="access-panel" /> }))
vi.mock('./BranchSwitcher', () => ({
    BranchSwitcher: (props: Record<string, unknown>) => {
        branchSwitcherMock(props)
        return <div data-testid="crumb-branch-switcher" />
    },
}))
vi.mock('./ValueText', () => ({ ValueText: ({ children }: Record<string, unknown>) => <span>{children as never}</span> }))

const PROJECT: Project = {
    branch: 'main',
    comment: '',
    id: 'p1',
    modifiedAt: '2024-01-02T00:00:00Z',
    modifiedBy: 'jane',
    name: 'Alpha',
    repository: 'design',
    revision: 'rev1',
    status: ProjectStatus.Closed,
    capabilities: { canWrite: true },
}

const FILES: FsNode[] = [{
    basePath: '',
    name: 'rules.xlsx',
    path: 'rules.xlsx',
    type: 'file',
}, {
    basePath: '',
    name: 'rules',
    path: 'rules',
    type: 'folder',
}, {
    basePath: 'rules',
    name: 'Nested.xlsx',
    path: 'rules/Nested.xlsx',
    type: 'file',
}]

const BRANCH_REPOSITORY_FEATURES: RepositoryFeatures = {
    branches: true,
    searchable: true,
    mappedFolders: true,
}

const setParams = (query: string) => {
    for (const key of [...searchParamsMock.keys()]) {
        searchParamsMock.delete(key)
    }
    new URLSearchParams(query).forEach((value, key) => searchParamsMock.set(key, value))
}

interface DetailOptions {
    files?: FsNode[] | 'loading' | 'error' | undefined
    project?: Project | null
    repoFeatures?: RepositoryFeatures | undefined
    userManagementEnabled?: boolean
    onBranchSwitching?: (busy: boolean) => void
}

const projectDetailElement = ({
    files = FILES,
    project = PROJECT,
    repoFeatures = BRANCH_REPOSITORY_FEATURES,
    userManagementEnabled = false,
    onBranchSwitching,
}: DetailOptions = {}) => (
    <SystemContext.Provider
        value={{
            isExternalAuthSystem: false,
            isGroupsManagementEnabled: true,
            isPersonalAccessTokenEnabled: false,
            isUserManagementEnabled: userManagementEnabled,
        }}
    >
        <ProjectDetail
            files={files}
            handlers={{} as never}
            onBranchSwitching={onBranchSwitching}
            pendingId={null}
            project={project}
            repoFeatures={repoFeatures}
            repoLabel="Design"
        />
    </SystemContext.Provider>
)

const renderProjectDetail = (options: DetailOptions = {}) => render(projectDetailElement(options))

/** The props the file pane was last rendered with — earlier calls hold the state before a re-render. */
const latestFilePreviewProps = () => filePreviewMock.mock.lastCall?.[0] as { path: string | null }

describe('ProjectDetail', () => {
    beforeEach(() => {
        setParams('tab=files')
        filePreviewMock.mockClear()
        filesToolbarMock.mockClear()
        localChangesSummaryMock.mockClear()
        revisionsPanelMock.mockClear()
        setSearchParamsMock.mockReset()
        setSearchParamsMock.mockImplementation((
            updater: URLSearchParams | ((prev: URLSearchParams) => URLSearchParams)
        ) => {
            const next = typeof updater === 'function'
                ? updater(new URLSearchParams(searchParamsMock))
                : updater
            for (const key of [...searchParamsMock.keys()]) {
                searchParamsMock.delete(key)
            }
            next.forEach((value, key) => searchParamsMock.set(key, value))
        })
    })

    it('removes resize listeners when unmounted mid-drag', () => {
        const addListener = vi.spyOn(document, 'addEventListener')
        const removeListener = vi.spyOn(document, 'removeEventListener')

        const { unmount } = renderProjectDetail()

        fireEvent.mouseDown(screen.getByTestId('file-tree-resizer'), { clientX: 300 })
        const moveListener = addListener.mock.calls.find(([type]) => type === 'mousemove')?.[1]
        const upListener = addListener.mock.calls.find(([type]) => type === 'mouseup')?.[1]

        expect(moveListener).toBeTypeOf('function')
        expect(upListener).toBeTypeOf('function')

        unmount()

        expect(removeListener).toHaveBeenCalledWith('mousemove', moveListener)
        expect(removeListener).toHaveBeenCalledWith('mouseup', upListener)
    })

    it('hides the Management tab content when user management is disabled', () => {
        setParams('tab=access')

        renderProjectDetail()

        expect(screen.getByTestId('overview-panel')).toBeTruthy()
        expect(screen.queryByTestId('access-panel')).toBeNull()
    })

    it('shows the Management tab content when user management is enabled and the user can manage access', () => {
        setParams('tab=access')

        renderProjectDetail({
            userManagementEnabled: true,
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canManage: true },
            },
        })

        expect(screen.getByTestId('access-panel')).toBeTruthy()
    })

    it('hides the Management tab when the user cannot manage project access', () => {
        setParams('tab=access')

        renderProjectDetail({
            userManagementEnabled: true,
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canManage: false },
            },
        })

        expect(screen.queryByTestId('access-panel')).toBeNull()
    })

    it('uses the selected folder as the file action target', () => {
        setParams('tab=files&file=rules')

        renderProjectDetail()

        expect(filesToolbarMock).toHaveBeenCalledWith(expect.objectContaining({
            targetFolder: 'rules',
        }))
    })

    it('waits for the tree before it treats an extension-less selection as a file', () => {
        // A folder like "__MACOSX" is read as a file until the tree says otherwise: previewing it would
        // fetch a folder's content and fail. While the tree loads, neither pane is shown.
        setParams('tab=files&file=__MACOSX')

        renderProjectDetail({ files: 'loading' })

        expect(screen.getByTestId('file-pane-loading')).toBeInTheDocument()
        expect(screen.queryByTestId('file-preview')).toBeNull()
        expect(screen.queryByTestId('folder-actions')).toBeNull()
    })

    it('shows folder actions once the tree reveals the selection is a folder', () => {
        setParams('tab=files&file=rules')

        renderProjectDetail({ files: FILES })

        expect(screen.getByTestId('folder-actions')).toBeInTheDocument()
        expect(screen.queryByTestId('file-preview')).toBeNull()
        expect(screen.queryByTestId('file-pane-loading')).toBeNull()
    })

    it('uses the selected file parent folder as the file action target', () => {
        setParams('tab=files&file=rules/Nested.xlsx')

        renderProjectDetail()

        expect(filesToolbarMock).toHaveBeenCalledWith(expect.objectContaining({
            targetFolder: 'rules',
        }))
    })

    it('normalizes pending file changes for the Files tab', () => {
        renderProjectDetail({
            project: {
                ...PROJECT,
                path: 'folder/Alpha',
                compileStatus: {
                    projectId: PROJECT.id,
                    compileState: 'ok',
                    pendingChanges: {
                        total: 2,
                        files: [
                            { path: 'folder/Alpha/rules.xlsx', type: 'modified' },
                            { path: 'folder/Alpha/old.xlsx', type: 'deleted' },
                        ],
                    },
                },
            },
        })

        expect(localChangesSummaryMock).toHaveBeenCalledWith({
            changes: [
                { path: 'rules.xlsx', type: 'modified' },
                { path: 'old.xlsx', type: 'deleted' },
            ],
        })
    })

    it('does not mount the History tab content while another tab is active', () => {
        setParams('tab=overview')
        renderProjectDetail({
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canCompare: true, canViewHistory: true },
            },
        })

        expect(revisionsPanelMock).not.toHaveBeenCalled()
        expect(screen.queryByTestId('history-panel')).toBeNull()
    })

    it('mounts the History tab content when History is active', () => {
        setParams('tab=history')
        renderProjectDetail({
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canCompare: true, canViewHistory: true },
            },
        })

        expect(screen.getByTestId('history-panel')).toBeTruthy()
        expect(revisionsPanelMock).toHaveBeenCalledWith(expect.objectContaining({
            projectId: PROJECT.id,
        }))
    })

    it('turns off the history search for a repository that cannot search', () => {
        setParams('tab=history')
        renderProjectDetail({
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canViewHistory: true },
            },
            repoFeatures: { branches: false, searchable: false, mappedFolders: false },
        })

        expect(revisionsPanelMock).toHaveBeenCalledWith(expect.objectContaining({
            searchable: false,
        }))
    })

    it('renders the publish panel when the publish tab is active', () => {
        setParams('tab=publish')
        renderProjectDetail()
        expect(screen.getByTestId('publish-panel')).toBeTruthy()
    })

    it('offers no branches tab: branches are handled from the header and the Overview tab', () => {
        setParams('tab=branches')
        renderProjectDetail()
        expect(screen.queryByTestId('tab-branches')).toBeNull()
        // An unknown tab falls back to the overview one.
        expect(screen.getByTestId('overview-panel')).toBeTruthy()
    })

    it('updates the active tab through the tab control', async () => {
        setParams('tab=overview')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('tab-publish'))
        expect(setSearchParamsMock).toHaveBeenCalled()
    })

    it('falls back to overview when the requested tab is unavailable', () => {
        setParams('tab=access')
        renderProjectDetail({ userManagementEnabled: false })
        expect(screen.getByTestId('overview-panel')).toBeTruthy()
    })

    it('shows the management panel when the user can manage access', () => {
        setParams('tab=access')
        renderProjectDetail({
            userManagementEnabled: true,
            project: {
                ...PROJECT,
                capabilities: { ...PROJECT.capabilities, canManage: true },
            },
        })
        expect(screen.getByTestId('access-panel')).toBeTruthy()
    })

    it('clears the selected file from the URL', async () => {
        setParams('tab=files&file=rules.xlsx')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('file-tree-clear'))
        expect(setSearchParamsMock).toHaveBeenCalled()
    })

    it('stops resizing on mouse up', () => {
        const removeListener = vi.spyOn(document, 'removeEventListener')
        renderProjectDetail()
        fireEvent.mouseDown(screen.getByTestId('file-tree-resizer'), { clientX: 300 })
        fireEvent.mouseUp(document)
        expect(removeListener).toHaveBeenCalledWith('mousemove', expect.any(Function))
        expect(removeListener).toHaveBeenCalledWith('mouseup', expect.any(Function))
    })

    it('shows an empty state when no project is selected', () => {
        renderProjectDetail({ project: null })
        expect(screen.getByTestId('project-detail-empty')).toBeTruthy()
    })

    it('updates the file tree width while dragging the resizer', () => {
        renderProjectDetail()
        fireEvent.mouseDown(screen.getByTestId('file-tree-resizer'), { clientX: 300 })
        fireEvent.mouseMove(document, { clientX: 350 })
        fireEvent.mouseUp(document)
        expect(screen.getByTestId('file-tree-resizer')).toBeTruthy()
    })

    it('writes the selected file into the URL', async () => {
        setParams('tab=files')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('file-tree-select'))
        expect(searchParamsMock.get('file')).toBe('rules/new.txt')
        expect(searchParamsMock.get('tab')).toBe('files')
    })

    it('removes the file parameter when the selection is cleared', async () => {
        setParams('tab=files&file=rules.xlsx')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('file-tree-clear'))
        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('shows folder actions when a folder is selected', () => {
        setParams('tab=files&file=rules')
        renderProjectDetail()
        expect(screen.getByTestId('folder-actions')).toBeTruthy()
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('shows folder actions (not the content pane) for a selected virtual folder', async () => {
        // 'drafts/wip' has no backend node yet, so a selection of it would look like a file (content
        // pane). A folder created in the UI shows folder actions instead, so nothing is fetched for a
        // folder that does not exist server-side (which would 404).
        setParams('tab=files')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('file-tree-select-virtual'))

        await userEvent.click(screen.getByTestId('files-create-folder'))
        expect(screen.getByTestId('folder-actions')).toBeTruthy()
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('drops a selection the loaded tree does not hold', () => {
        // The URL outlived the file: the project was closed and dropped its local copy. Previewing the
        // path would only fetch a 404 and paint an error over an empty editor.
        setParams('tab=files&file=rules/local.txt')

        renderProjectDetail({ files: FILES })

        expect(searchParamsMock.has('file')).toBe(false)
        expect(searchParamsMock.get('tab')).toBe('files')
        // The pane ends up unselected, so it stops asking the server for the file that is gone.
        expect(latestFilePreviewProps().path).toBeNull()
    })

    it('drops the selection when a reloaded tree no longer holds the file', () => {
        setParams('tab=files&file=rules/Nested.xlsx')
        const { rerender } = renderProjectDetail({ files: FILES })

        rerender(projectDetailElement({ files: FILES.filter(node => node.path !== 'rules/Nested.xlsx') }))

        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('keeps a selection the loaded tree holds', () => {
        setParams('tab=files&file=rules/Nested.xlsx')

        renderProjectDetail({ files: FILES })

        expect(setSearchParamsMock).not.toHaveBeenCalled()
    })

    it('keeps the selection while the tree is not loaded', () => {
        setParams('tab=files&file=rules/local.txt')

        renderProjectDetail({ files: 'loading' })

        expect(searchParamsMock.get('file')).toBe('rules/local.txt')
    })

    it('keeps the path a file was just moved to, which the tree on screen cannot hold yet', async () => {
        // The move reloads the project; until its tree arrives the file is still listed under the old
        // path, and the selection must survive that window.
        setParams('tab=files&file=rules/Nested.xlsx')
        const { rerender } = renderProjectDetail({ files: FILES })

        await userEvent.click(screen.getByTestId('preview-moved'))
        rerender(projectDetailElement({ files: FILES }))

        expect(searchParamsMock.get('file')).toBe('rules/Moved.xlsx')

        // The reload behind the move brings a tree that holds the new path, and it stays selected.
        const moved: FsNode = { basePath: 'rules', name: 'Moved.xlsx', path: 'rules/Moved.xlsx', type: 'file' }
        rerender(projectDetailElement({ files: [...FILES, moved]}))

        expect(searchParamsMock.get('file')).toBe('rules/Moved.xlsx')
    })

    it('keeps a virtual folder selection when the tree reloads without it', async () => {
        setParams('tab=files')
        const { rerender } = renderProjectDetail()
        await userEvent.click(screen.getByTestId('file-tree-select-virtual'))
        await userEvent.click(screen.getByTestId('files-create-folder'))

        rerender(projectDetailElement({ files: FILES.slice() }))

        expect(searchParamsMock.get('file')).toBe('drafts/wip')
        expect(screen.getByTestId('folder-actions')).toBeTruthy()
    })

    it('refreshes the project after folder actions change', async () => {
        const onChanged = vi.fn()
        setParams('tab=files&file=rules')
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: false,
                }}
            >
                <ProjectDetail
                    files={FILES}
                    handlers={{} as never}
                    onChanged={onChanged}
                    pendingId={null}
                    project={PROJECT}
                    repoFeatures={BRANCH_REPOSITORY_FEATURES}
                    repoLabel="Design"
                />
            </SystemContext.Provider>
        )
        await userEvent.click(screen.getByTestId('folder-changed'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('clears the file selection after a folder is deleted', async () => {
        setParams('tab=files&file=rules')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('folder-deleted'))
        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('refreshes the project after a file preview change', async () => {
        const onChanged = vi.fn()
        setParams('tab=files&file=rules.xlsx')
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: false,
                }}
            >
                <ProjectDetail
                    files={FILES}
                    handlers={{} as never}
                    onChanged={onChanged}
                    pendingId={null}
                    project={PROJECT}
                    repoFeatures={BRANCH_REPOSITORY_FEATURES}
                    repoLabel="Design"
                />
            </SystemContext.Provider>
        )
        await userEvent.click(screen.getByTestId('preview-changed'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('clears the file selection after a file is deleted from the preview', async () => {
        setParams('tab=files&file=rules.xlsx')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('preview-deleted'))
        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('updates the file filter from the toolbar', async () => {
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('files-filter'))
        expect(filesToolbarMock).toHaveBeenLastCalledWith(expect.objectContaining({
            filter: 'xlsx',
        }))
    })

    it('loads files when the files tab becomes visible', () => {
        const onFilesVisible = vi.fn()
        setParams('tab=files')
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: false,
                }}
            >
                <ProjectDetail
                    files={FILES}
                    handlers={{} as never}
                    onFilesVisible={onFilesVisible}
                    pendingId={null}
                    project={PROJECT}
                    repoFeatures={BRANCH_REPOSITORY_FEATURES}
                    repoLabel="Design"
                />
            </SystemContext.Provider>
        )
        expect(onFilesVisible).toHaveBeenCalled()
    })

    it('refreshes the project when a revision is opened from history', async () => {
        const onChanged = vi.fn()
        setParams('tab=history')
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: false,
                }}
            >
                <ProjectDetail
                    files={FILES}
                    handlers={{} as never}
                    onChanged={onChanged}
                    pendingId={null}
                    repoFeatures={BRANCH_REPOSITORY_FEATURES}
                    repoLabel="Design"
                    project={{
                        ...PROJECT,
                        capabilities: { ...PROJECT.capabilities, canViewHistory: true },
                    }}
                />
            </SystemContext.Provider>
        )
        await userEvent.click(screen.getByTestId('history-opened'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('refreshes the project from the branches and publish panels', async () => {
        const onChanged = vi.fn()
        setParams('tab=branches')
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: false,
                }}
            >
                <ProjectDetail
                    files={FILES}
                    handlers={{} as never}
                    onChanged={onChanged}
                    pendingId={null}
                    project={PROJECT}
                    repoFeatures={BRANCH_REPOSITORY_FEATURES}
                    repoLabel="Design"
                />
            </SystemContext.Provider>
        )
        await userEvent.click(screen.getByTestId('publish-changed'))
        expect(onChanged).toHaveBeenCalledTimes(1)
    })

    it('updates the tab query parameter when switching tabs', async () => {
        setParams('tab=overview')
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('tab-publish'))
        expect(searchParamsMock.get('tab')).toBe('publish')
    })

    it('drops the file selection when another tab is opened', async () => {
        // The selection belongs to the Files tab: a link copied from the Overview tab carries no file.
        setParams('tab=files&file=rules/Nested.xlsx')
        renderProjectDetail()

        await userEvent.click(screen.getByTestId('tab-overview'))

        expect(searchParamsMock.get('tab')).toBe('overview')
        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('renders the branch switcher without an eager branch list', () => {
        setParams('tab=overview')
        renderProjectDetail({
            project: { ...PROJECT, branchDefault: true },
        })

        expect(screen.getByTestId('crumb-branch-switcher')).toBeTruthy()
        expect(branchSwitcherMock).toHaveBeenLastCalledWith(expect.objectContaining({
            currentBranch: 'main',
            currentBranchDefault: true,
            projectId: 'p1',
        }))
    })

    it('reports the breadcrumb branch switch, so the page can mark the project busy', () => {
        setParams('tab=overview')
        const onBranchSwitching = vi.fn()
        renderProjectDetail({ onBranchSwitching })

        const props = branchSwitcherMock.mock.calls.at(-1)?.[0] as { onBusyChange?: (busy: boolean) => void }
        props.onBusyChange?.(true)

        expect(onBranchSwitching).toHaveBeenCalledWith(true)
    })

    it('shows no breadcrumb branch for a repository without branches', () => {
        setParams('tab=overview')
        renderProjectDetail({ repoFeatures: { branches: false, searchable: false, mappedFolders: false } })

        expect(screen.queryByTestId('crumb-branch-switcher')).toBeNull()
    })
})

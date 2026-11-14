import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { FsNode } from '../../types/files'
import type { Project } from '../../types/projects'
import type { RepositoryFeatures } from '../../types/repositories'
import { SystemContext } from '../../contexts'
import { ProjectDetail } from './ProjectDetail'
import { hasSearchScope } from '../../testing/search'

const {
    addFilesDialogsMock,
    branchSwitcherMock,
    filePreviewMock,
    revisionsPanelMock,
    searchParamsMock,
    setSearchParamsMock,
} = vi.hoisted(() => ({
    addFilesDialogsMock: vi.fn(),
    branchSwitcherMock: vi.fn(),
    filePreviewMock: vi.fn(),
    revisionsPanelMock: vi.fn(),
    searchParamsMock: new URLSearchParams('tab=overview'),
    setSearchParamsMock: vi.fn(),
}))

vi.mock('react-router', () => ({
    useBlocker: () => ({ state: 'unblocked' }),
    useNavigate: () => navigateMock,
    useSearchParams: () => [searchParamsMock, setSearchParamsMock],
}))

const navigateMock = vi.fn()

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
// The live compilation status comes over the status channel, which no test opens.
vi.mock('../../hooks/useLiveProjectStatus', () => ({
    useLiveProjectStatus: () => null,
}))

vi.mock('@ant-design/icons', () => ({
    BranchesOutlined: () => null,
    CloudUploadOutlined: () => null,
    DeleteOutlined: () => null,
    EditOutlined: () => null,
    FileTextOutlined: () => null,
    FolderOpenOutlined: () => null,
    FolderOutlined: () => null,
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
        <div data-active-key={activeKey} data-testid="tabs">
            {items?.map(item => (
                <div key={item.key}>
                    <button data-testid={`tab-${item.key}`} onClick={() => onChange?.(item.key)} type="button">
                        {item.key}
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
// The bar has its own tests; here it only offers the Add menu it is handed.
vi.mock('./ProjectActionBar', () => ({
    ProjectActionBar: ({ onAddFiles }: { onAddFiles?: (kind: string) => void }) => (
        <button data-testid="bar-add-upload" onClick={() => onAddFiles?.('upload')} type="button">add</button>
    ),
}))
// The dialogs of the Add menu have their own tests; here they show which one is open, and create a folder.
vi.mock('./AddFilesMenu', () => ({
    AddFilesDialogs: (props: { kind: string | null, onCreateFolder: (path: string) => void, targetFolder: string }) => {
        addFilesDialogsMock(props)
        return (
            <div data-kind={props.kind ?? ''} data-testid="add-files-dialogs">
                <button data-testid="dialog-create-folder" onClick={() => props.onCreateFolder('drafts/wip')} type="button">
                    create
                </button>
            </div>
        )
    },
}))
vi.mock('./FilePreviewPane', () => ({
    // The pane is handed the selected path, so the props of its last render tell what the selection
    // settled on — read them through `latestFilePreviewProps`, never an earlier call.
    FilePreviewPane: (props: {
        modules?: Record<string, string>
        onChanged?: () => void
        onDeleted?: () => void
        onMoved?: (path: string) => void
        onOpenModule?: (moduleName: string) => void
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
    FolderActionsPane: (props: {
        onChanged?: () => void
        onCreateFolder: (path: string) => void
        onDeleted?: () => void
        onRemoveVirtual?: () => void
        path: string
        virtual?: boolean
    }) => (
        <div data-path={props.path} data-testid="folder-actions" data-virtual={String(!!props.virtual)}>
            <button data-testid="folder-changed" onClick={() => props.onChanged?.()} type="button">changed</button>
            <button data-testid="folder-deleted" onClick={() => props.onDeleted?.()} type="button">deleted</button>
            <button data-testid="folder-add-folder" onClick={() => props.onCreateFolder(`${props.path}/sub`)} type="button">
                add folder
            </button>
            <button data-testid="folder-remove-virtual" onClick={() => props.onRemoveVirtual?.()} type="button">remove</button>
        </div>
    ),
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
    onChanged?: () => void
    onFilesNeeded?: () => void
    virtualFolders?: string[]
    onCreateFolder?: (path: string) => void
    onRemoveVirtualFolder?: (path: string) => void
}

const projectDetailElement = ({
    files = FILES,
    project = PROJECT,
    repoFeatures = BRANCH_REPOSITORY_FEATURES,
    userManagementEnabled = false,
    onBranchSwitching,
    onChanged,
    onFilesNeeded,
    virtualFolders = [],
    onCreateFolder = vi.fn(),
    onRemoveVirtualFolder = vi.fn(),
}: DetailOptions = {}) => (
    <SystemContext.Provider
        value={{
            isExternalAuthSystem: false,
            isGroupsManagementEnabled: true,
            isPersonalAccessTokenEnabled: false,
            isAuditLogEnabled: false,
            isUserManagementEnabled: userManagementEnabled,
        }}
    >
        <ProjectDetail
            files={files}
            handlers={{} as never}
            onBranchSwitching={onBranchSwitching}
            onChanged={onChanged}
            onCreateFolder={onCreateFolder}
            onFilesNeeded={onFilesNeeded}
            onRemoveVirtualFolder={onRemoveVirtualFolder}
            pendingId={null}
            project={project}
            repoFeatures={repoFeatures}
            repoLabel="Design"
            virtualFolders={virtualFolders}
        />
    </SystemContext.Provider>
)

const renderProjectDetail = (options: DetailOptions = {}) => render(projectDetailElement(options))

/** The props the file pane was last rendered with — earlier calls hold the state before a re-render. */
const latestFilePreviewProps = () => filePreviewMock.mock.lastCall?.[0] as {
    path: string | null
    line?: number
    modules?: Record<string, string>
    onOpenModule?: (moduleName: string) => void
}

describe('ProjectDetail', () => {
    beforeEach(() => {
        setParams('tab=overview')
        addFilesDialogsMock.mockClear()
        filePreviewMock.mockClear()
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

    it('offers no Files tab: the files are reached from the projects tree', () => {
        renderProjectDetail()

        expect(screen.queryByTestId('tab-files')).toBeNull()
        expect(screen.getByTestId('tab-overview')).toBeTruthy()
    })

    it('shows a file in place of the content of a tab, the tabs standing with none picked', () => {
        setParams('file=rules.xlsx')

        renderProjectDetail()

        expect(screen.getByTestId('project-file-pane')).toBeTruthy()
        expect(screen.getByTestId('file-preview')).toBeTruthy()
        expect(screen.getByTestId('tabs').getAttribute('data-active-key')).toBe('')
        expect(screen.getByTestId('tab-overview')).toBeTruthy()
        expect(screen.queryByTestId('overview-panel')).toBeNull()
    })

    it('shows a file the URL names over the tab it also names', () => {
        setParams('tab=publish&file=rules.xlsx')

        renderProjectDetail()

        expect(screen.getByTestId('file-preview')).toBeTruthy()
        expect(screen.queryByTestId('publish-panel')).toBeNull()
    })

    it('leaves the file for the tab picked, and the line of the file with it', async () => {
        setParams('file=rules.xlsx&line=3')
        renderProjectDetail()

        await userEvent.click(screen.getByTestId('tab-publish'))

        expect(searchParamsMock.get('tab')).toBe('publish')
        expect(searchParamsMock.has('file')).toBe(false)
        expect(searchParamsMock.has('line')).toBe(false)
    })

    it('reads the files of the project once a file or a folder is shown, and not for a tab', () => {
        const onFilesNeeded = vi.fn()
        const { rerender } = renderProjectDetail({ onFilesNeeded })
        expect(onFilesNeeded).not.toHaveBeenCalled()

        setParams('file=rules')
        rerender(projectDetailElement({ onFilesNeeded }))

        expect(onFilesNeeded).toHaveBeenCalled()
    })

    it('says so when the files of the project cannot be read', () => {
        setParams('file=rules.xlsx')

        renderProjectDetail({ files: 'error' })

        expect(screen.getByTestId('file-pane-error')).toHaveTextContent('browser.files_error')
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('offers the files to no search of the header, whatever is shown', () => {
        setParams('file=rules.xlsx')

        renderProjectDetail()

        expect(hasSearchScope('files')).toBe(false)
    })

    it.each([
        ['into the folder shown', 'file=rules', 'rules'],
        ['beside the file shown', 'file=rules/Nested.xlsx', 'rules'],
        ['at the root beside a file at the root', 'file=rules.xlsx', ''],
    ])('adds files from the Add menu of the action bar %s', async (_where, params, folder) => {
        setParams(params)
        renderProjectDetail()

        await userEvent.click(screen.getByTestId('bar-add-upload'))

        expect(addFilesDialogsMock).toHaveBeenLastCalledWith(expect.objectContaining({ kind: 'upload', targetFolder: folder }))
    })

    it('adds files at the root of the project from the Add menu of the action bar while a tab is shown', async () => {
        const onFilesNeeded = vi.fn()
        renderProjectDetail({ onFilesNeeded })
        expect(screen.getByTestId('add-files-dialogs').getAttribute('data-kind')).toBe('')

        await userEvent.click(screen.getByTestId('bar-add-upload'))

        expect(screen.getByTestId('add-files-dialogs').getAttribute('data-kind')).toBe('upload')
        expect(addFilesDialogsMock).toHaveBeenLastCalledWith(expect.objectContaining({ targetFolder: '' }))
        // The dialogs offer the folders of the project as paths.
        expect(onFilesNeeded).toHaveBeenCalled()
    })

    it('shows an empty folder as soon as it is added, to add its first file from', async () => {
        const onCreateFolder = vi.fn()
        renderProjectDetail({ onCreateFolder })

        await userEvent.click(screen.getByTestId('dialog-create-folder'))

        expect(onCreateFolder).toHaveBeenCalledWith('drafts/wip')
        expect(searchParamsMock.get('file')).toBe('drafts/wip')
        expect(searchParamsMock.has('tab')).toBe(false)
    })

    it('adds a folder inside the folder shown, and shows the new one', async () => {
        const onCreateFolder = vi.fn()
        setParams('file=rules')
        renderProjectDetail({ onCreateFolder })

        await userEvent.click(screen.getByTestId('folder-add-folder'))

        expect(onCreateFolder).toHaveBeenCalledWith('rules/sub')
        expect(searchParamsMock.get('file')).toBe('rules/sub')
    })

    it('drops an empty folder the reader removes, and the file shown with it', async () => {
        const onRemoveVirtualFolder = vi.fn()
        setParams('file=drafts/wip')
        renderProjectDetail({ virtualFolders: ['drafts/wip'], onRemoveVirtualFolder })

        await userEvent.click(screen.getByTestId('folder-remove-virtual'))

        expect(onRemoveVirtualFolder).toHaveBeenCalledWith('drafts/wip')
        expect(searchParamsMock.has('file')).toBe(false)
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

    it('waits for the files before it treats an extension-less selection as a file', () => {
        // A folder like "__MACOSX" is read as a file until the files say otherwise: previewing it would
        // fetch a folder's content and fail. While the files load, neither pane is shown.
        setParams('file=__MACOSX')

        renderProjectDetail({ files: 'loading' })

        expect(screen.getByTestId('file-pane-loading')).toBeInTheDocument()
        expect(screen.queryByTestId('file-preview')).toBeNull()
        expect(screen.queryByTestId('folder-actions')).toBeNull()
    })

    it('shows folder actions once the files reveal the selection is a folder', () => {
        setParams('file=rules')

        renderProjectDetail({ files: FILES })

        expect(screen.getByTestId('folder-actions')).toBeInTheDocument()
        expect(screen.queryByTestId('file-preview')).toBeNull()
        expect(screen.queryByTestId('file-pane-loading')).toBeNull()
    })

    it('does not mount the History tab content while another tab is active', () => {
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

    it('hides project history for a file repository without versions', () => {
        setParams('tab=history')
        renderProjectDetail({
            project: { ...PROJECT, capabilities: { ...PROJECT.capabilities } },
            repoFeatures: { branches: false, searchable: false, mappedFolders: true, comments: false },
        })

        expect(screen.queryByTestId('tab-history')).toBeNull()
        expect(screen.queryByTestId('history-panel')).toBeNull()
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

    it.each(['branches', 'files'])('falls back to the overview for the tab %s, which it does not offer', tab => {
        setParams(`tab=${tab}`)
        renderProjectDetail()
        expect(screen.queryByTestId(`tab-${tab}`)).toBeNull()
        expect(screen.getByTestId('overview-panel')).toBeTruthy()
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

    it('shows an empty state when no project is selected', () => {
        renderProjectDetail({ project: null })
        expect(screen.getByTestId('project-detail-empty')).toBeTruthy()
    })

    it('opens the file on the line the URL names, and forgets the line once the file moves', async () => {
        setParams('file=rules.xlsx&line=3')
        renderProjectDetail()

        expect(latestFilePreviewProps().line).toBe(3)

        await userEvent.click(screen.getByTestId('preview-moved'))
        expect(searchParamsMock.get('file')).toBe('rules/Moved.xlsx')
        expect(searchParamsMock.has('line')).toBe(false)
    })

    it.each([
        ['clears the file selection after a folder is deleted', 'file=rules', 'folder-deleted'],
        ['clears the file selection after a file is deleted from the preview', 'file=rules.xlsx', 'preview-deleted'],
    ])('%s', async (_behaviour, params, trigger) => {
        setParams(params)
        renderProjectDetail()
        await userEvent.click(screen.getByTestId(trigger))
        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('shows folder actions when a folder is selected', () => {
        setParams('file=rules')
        renderProjectDetail()
        expect(screen.getByTestId('folder-actions').getAttribute('data-path')).toBe('rules')
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('shows folder actions (not the content pane) for an empty folder added in the browser', () => {
        // 'drafts/wip' has no backend node yet, so a selection of it would look like a file (content
        // pane). A folder created in the UI shows folder actions instead, so nothing is fetched for a
        // folder that does not exist server-side (which would 404).
        setParams('file=drafts/wip')
        renderProjectDetail({ virtualFolders: ['drafts/wip']})

        expect(screen.getByTestId('folder-actions').getAttribute('data-virtual')).toBe('true')
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('hands the pane the module each workbook is, and opens the one it is asked for', () => {
        setParams('file=rules/Nested.xlsx')
        renderProjectDetail({
            files: FILES,
            project: {
                ...PROJECT,
                descriptor: { modules: [
                    { name: 'Rules', path: 'rules/**/*.xlsx', modules: [{ name: 'Nested', path: 'rules/Nested.xlsx' }]},
                    { name: 'Standalone', path: 'rules.xlsx' },
                ]},
            },
        })

        // A pattern is no file of its own; the modules it matched are the ones a reader opens.
        expect(latestFilePreviewProps().modules).toEqual({
            'rules/Nested.xlsx': 'Nested',
            'rules.xlsx': 'Standalone',
        })

        latestFilePreviewProps().onOpenModule?.('Nested')

        expect(navigateMock).toHaveBeenCalledWith('/projects/p1/modules/Nested')
    })

    it('drops a selection the loaded files do not hold, and shows the tabs again', () => {
        // The URL outlived the file: the project was closed and dropped its local copy. Previewing the
        // path would only fetch a 404 and paint an error over an empty editor.
        setParams('file=rules/local.txt')

        const { rerender } = renderProjectDetail({ files: FILES })
        rerender(projectDetailElement({ files: FILES }))

        expect(searchParamsMock.has('file')).toBe(false)
        expect(screen.getByTestId('overview-panel')).toBeTruthy()
        expect(screen.queryByTestId('file-preview')).toBeNull()
    })

    it('drops the selection when reloaded files no longer hold the file', () => {
        setParams('file=rules/Nested.xlsx')
        const { rerender } = renderProjectDetail({ files: FILES })

        rerender(projectDetailElement({ files: FILES.filter(node => node.path !== 'rules/Nested.xlsx') }))

        expect(searchParamsMock.has('file')).toBe(false)
    })

    it('keeps a selection the loaded files hold', () => {
        setParams('file=rules/Nested.xlsx')

        renderProjectDetail({ files: FILES })

        expect(setSearchParamsMock).not.toHaveBeenCalled()
    })

    it('keeps the selection while the files are not loaded', () => {
        setParams('file=rules/local.txt')

        renderProjectDetail({ files: 'loading' })

        expect(searchParamsMock.get('file')).toBe('rules/local.txt')
    })

    it('keeps the path a file was just moved to, which the files on screen cannot hold yet', async () => {
        // The move reloads the project; until its files arrive the file is still listed under the old
        // path, and the selection must survive that window.
        setParams('file=rules/Nested.xlsx')
        const { rerender } = renderProjectDetail({ files: FILES })

        await userEvent.click(screen.getByTestId('preview-moved'))
        rerender(projectDetailElement({ files: FILES }))

        expect(searchParamsMock.get('file')).toBe('rules/Moved.xlsx')

        // The reload behind the move brings files that hold the new path, and it stays selected.
        const moved: FsNode = { basePath: 'rules', name: 'Moved.xlsx', path: 'rules/Moved.xlsx', type: 'file' }
        rerender(projectDetailElement({ files: [...FILES, moved]}))

        expect(searchParamsMock.get('file')).toBe('rules/Moved.xlsx')
    })

    it('keeps an empty folder selection when the files reload without it', () => {
        setParams('file=drafts/wip')
        const { rerender } = renderProjectDetail({ virtualFolders: ['drafts/wip']})

        rerender(projectDetailElement({ files: FILES.slice(), virtualFolders: ['drafts/wip']}))

        expect(searchParamsMock.get('file')).toBe('drafts/wip')
        expect(screen.getByTestId('folder-actions')).toBeTruthy()
    })

    it('refreshes the project after folder actions change', async () => {
        const onChanged = vi.fn()
        setParams('file=rules')
        renderProjectDetail({ onChanged })
        await userEvent.click(screen.getByTestId('folder-changed'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('refreshes the project after a file preview change', async () => {
        const onChanged = vi.fn()
        setParams('file=rules.xlsx')
        renderProjectDetail({ onChanged })
        await userEvent.click(screen.getByTestId('preview-changed'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('refreshes the project when a revision is opened from history', async () => {
        const onChanged = vi.fn()
        setParams('tab=history')
        renderProjectDetail({
            onChanged,
            project: { ...PROJECT, capabilities: { ...PROJECT.capabilities, canViewHistory: true } },
        })
        await userEvent.click(screen.getByTestId('history-opened'))
        expect(onChanged).toHaveBeenCalled()
    })

    it('refreshes the project from the publish panel', async () => {
        const onChanged = vi.fn()
        setParams('tab=publish')
        renderProjectDetail({ onChanged })
        await userEvent.click(screen.getByTestId('publish-changed'))
        expect(onChanged).toHaveBeenCalledTimes(1)
    })

    it('updates the tab query parameter when switching tabs', async () => {
        renderProjectDetail()
        await userEvent.click(screen.getByTestId('tab-publish'))
        expect(searchParamsMock.get('tab')).toBe('publish')
    })

    it('renders the branch switcher without an eager branch list', () => {
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
        const onBranchSwitching = vi.fn()
        renderProjectDetail({ onBranchSwitching })

        const props = branchSwitcherMock.mock.calls.at(-1)?.[0] as { onBusyChange?: (busy: boolean) => void }
        props.onBusyChange?.(true)

        expect(onBranchSwitching).toHaveBeenCalledWith(true)
    })

    it('shows no breadcrumb branch for a repository without branches', () => {
        renderProjectDetail({ repoFeatures: { branches: false, searchable: false, mappedFolders: false } })

        expect(screen.queryByTestId('crumb-branch-switcher')).toBeNull()
    })
})

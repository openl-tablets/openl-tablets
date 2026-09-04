import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectWorkspace } from './ProjectWorkspace'
import {
    getDesignRepositories,
    getProjectBranches,
    getProjectFiles,
    getProject,
    setProjectStatus,
    switchProjectBranch,
    unlockProject,
} from '../services/repositories'
import { ApiHttpError, NotFoundError } from '../services'
import { notification } from 'antd'

const { copyModalMock, navigateMock, routeParams, searchParamsMock, setSearchParamsMock, liveHandlers } = vi.hoisted(() => ({
    copyModalMock: vi.fn(),
    navigateMock: vi.fn(),
    routeParams: { projectId: 'p1' } as { projectId: string },
    searchParamsMock: new URLSearchParams(),
    setSearchParamsMock: vi.fn(),
    // The change ping captured from the screen, plus whether the screen asks the hook to hold it.
    liveHandlers: {
        projectChange: undefined as ((files: string[]) => void) | undefined,
        holdWhile: false,
    },
}))

vi.mock('../hooks', async () => ({
    ...(await vi.importActual<typeof import('../hooks/useLoadGeneration')>('../hooks/useLoadGeneration')),
    useLiveProjectChanges: (
        _projectId: string | undefined,
        onChange: (files: string[]) => void,
        options?: { holdWhile?: boolean }
    ) => {
        liveHandlers.projectChange = onChange
        liveHandlers.holdWhile = options?.holdWhile ?? false
    },
    useWindowFocus: () => {},
}))

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
    useParams: () => routeParams,
    useSearchParams: () => [searchParamsMock, setSearchParamsMock],
    Link: ({ children, to }: { children?: unknown, to?: string }) => <a href={to}>{children as never}</a>,
}))

vi.mock('../services/repositories', () => ({
    getDesignRepositories: vi.fn(),
    getProject: vi.fn(),
    getProjectBranches: vi.fn(),
    getProjectFiles: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => Boolean(
        error
            && typeof error === 'object'
            && 'payload' in error
            && (error as { payload?: { code?: string } }).payload?.code === 'openl.error.409.project.close.modified.message'
    )),
    setProjectStatus: vi.fn(),
    switchProjectBranch: vi.fn(),
    unlockProject: vi.fn(),
    downloadProject: vi.fn(),
}))

vi.mock('./projects/SaveProjectModal', () => ({
    SaveProjectModal: ({ open, onSaved }: { open: boolean, onSaved?: () => void }) => (open ? (
        <div data-testid="save-modal-open">
            <button data-testid="save-modal-saved" onClick={() => onSaved?.()} type="button" />
        </div>
    ) : null),
}))

vi.mock('./projects/ExportProjectModal', () => ({
    ExportProjectModal: ({ open }: { open: boolean }) => open ? <div data-testid="export-modal" /> : null,
}))
vi.mock('./projects/OpenRevisionModal', () => ({
    OpenRevisionModal: ({ open }: { open: boolean }) => open ? <div data-testid="open-revision-modal" /> : null,
}))
vi.mock('./projects/CopyProjectModal', () => ({
    CopyProjectModal: (props: { open: boolean, repositories?: { id: string }[] }) => {
        copyModalMock(props)
        return props.open
            ? <div data-testid="copy-modal-open">{props.repositories?.map(repo => repo.id).join(',')}</div>
            : null
    },
}))

vi.mock('./projects/TagsModal', () => ({ TagsModal: () => null }))

vi.mock('./projects/BranchSelector', () => ({ BranchSelector: () => null }))


vi.mock('./projects/FilesToolbar', () => ({ FilesToolbar: () => null }))

vi.mock('./projects/RevisionsPanel', () => ({ RevisionsPanel: () => null }))

vi.mock('./projects/CompileProblemsPanel', () => ({
    CompileProblemsPanel: ({ project }: { project: { compileStatus?: { compileState: string } } }) => (
        <span data-testid="compile-state">{project.compileStatus?.compileState ?? 'none'}</span>
    ),
}))

vi.mock('./projects/DeployConfigPanel', () => ({ DeployConfigPanel: () => null }))



// The tab panels load their own data (status, branches, deployments, ACL); stub them so the workspace
// test stays focused on the header, action bar and Files tab.
vi.mock('./projects/OverviewPanel', () => ({ OverviewPanel: () => null }))
vi.mock('./projects/PublishPanel', () => ({ PublishPanel: () => null }))
vi.mock('./projects/AccessPanel', () => ({ AccessPanel: () => null }))

vi.mock('../services/files', () => ({
    isEditableTextFile: () => true,
    getFileContent: vi.fn(),
    updateFileContent: vi.fn(),
    deleteFile: vi.fn(),
    uploadFiles: vi.fn(),
    copyFile: vi.fn(),
    moveFile: vi.fn(),
    downloadFile: vi.fn(),
}))

// The rail beside the project has its own tests; here it only has to be out of the way.
vi.mock('./projects/ProjectsRail', () => ({
    ProjectsRail: ({ reloadToken }: { reloadToken?: number }) =>
        <aside data-reload-token={reloadToken} data-testid="projects-rail" />,
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        i18n: { language: 'en', resolvedLanguage: 'en' },
        t: (key: string) => key,
    }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
    keyframes: () => '',
}))

interface Node { key: string, title?: unknown, children?: Node[] }
interface Item { key: string, label?: unknown, children?: unknown, title?: unknown }

vi.mock('antd', () => {
    const drop = <T extends Record<string, unknown>>(props: T) => props
    const domProps = (props: unknown): Record<string, unknown> => {
        if (!props || typeof props !== 'object') {
            return {}
        }
        const { danger, ...dom } = props as Record<string, unknown>
        drop({ danger })
        return dom
    }

    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { danger, loading, type, size, shape, ghost, block, ...dom } = rest
        drop({ danger, loading, size, shape, ghost, block })
        // The primary flag is surfaced so tests can assert which action carries the main button.
        return (
            <button data-primary={type === 'primary' ? 'true' : undefined} onClick={onClick as never} {...dom}>
                {icon as never}{children as never}
            </button>
        )
    }

    const Input = ({ allowClear, prefix, suffix, ...rest }: Record<string, unknown>) => {
        drop({ allowClear, prefix, suffix })
        return <input {...rest} />
    }
    Input.Search = ({ value, onChange, placeholder, allowClear, ...rest }: Record<string, unknown>) => {
        drop({ allowClear })
        return <input onChange={onChange as never} placeholder={placeholder as never} value={(value as string) ?? ''} {...rest} />
    }

    interface DropdownMenu { items?: Array<{ key: string, label: unknown }>, onClick?: (info: { key: string }) => void }
    const menuButtons = (menu: DropdownMenu | undefined) => menu?.items?.map(item => (
        <button key={item.key} onClick={() => menu.onClick?.({ key: item.key })}>{item.label as never}</button>
    ))
    const Dropdown = ({ children, menu, popupRender, onOpenChange }: Record<string, unknown>) => (
        <div>
            {/* Opening is what makes a dropdown fetch what it offers, so a click on the trigger says so. */}
            <span onClick={() => (onOpenChange as (open: boolean) => void)?.(true)} role="presentation">
                {children as never}
            </span>
            {popupRender ? ((popupRender as (n: unknown) => unknown)(null) as never) : null}
            {menuButtons(menu as DropdownMenu)}
        </div>
    )




    const Popconfirm = ({ children, onConfirm }: { children?: unknown, onConfirm?: () => void }) =>
        <span onClick={onConfirm}>{children as never}</span>

    const Tag = ({ children, icon }: { children?: unknown, icon?: unknown }) => <span>{icon as never}{children as never}</span>
    const Tooltip = ({ children }: { children?: unknown }) => <>{children as never}</>

    const Empty = ({ description, image, children, ...rest }: Record<string, unknown>) => {
        drop({ image })
        return <div {...rest}>{description as never}{children as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'

    const Skeleton = () => <div>skeleton</div>
    const Spin = () => <div>spin</div>
    const Modal = ({
        cancelButtonProps,
        children,
        okButtonProps,
        okText,
        onCancel,
        onOk,
        open,
        title,
    }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <h4>{title as never}</h4>
            <div>{children as never}</div>
            <button {...domProps(cancelButtonProps)} onClick={onCancel as never}>cancel</button>
            <button {...domProps(okButtonProps)} onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null

    const Tabs = ({ activeKey, items, onChange }: { activeKey?: string, items?: Item[], onChange?: (key: string) => void }) => (
        <div>
            {items?.map(item => (
                <button key={item.key} onClick={() => onChange?.(item.key)}>{item.label as never}</button>
            ))}
            <div>{items?.find(item => item.key === activeKey)?.children as never}</div>
        </div>
    )

    const renderTree = (nodes?: Node[]): unknown => (
        <ul>{nodes?.map(node => <li key={node.key}>{node.title as never}{node.children ? (renderTree(node.children) as never) : null}</li>)}</ul>
    )
    const Tree = ({ treeData }: { treeData?: Node[] }) => renderTree(treeData) as never

    const Descriptions = ({ items }: { items?: Item[] }) => (
        <dl>{items?.map(item => <div key={item.key}><dt>{item.label as never}</dt><dd>{item.children as never}</dd></div>)}</dl>
    )

    const Alert = ({ title, message, description, action, showIcon, type, banner, ...rest }: Record<string, unknown>) => {
        drop({ showIcon, type, banner })
        return <div {...rest}>{title as never}{message as never}{description as never}{action as never}</div>
    }

    const Typography = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Typography.Text = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Typography.Title = ({ children }: { children?: unknown }) => <h4>{children as never}</h4>
    Typography.Link = ({ children, onClick, ...rest }: Record<string, unknown>) =>
        <a onClick={onClick as never} {...rest}>{children as never}</a>

    const Space = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Space.Compact = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Divider = () => <span />
    const notification = { error: vi.fn(), info: vi.fn() }

    return { Button, Input, Dropdown, Popconfirm, Tag, Tooltip, Empty, Skeleton, Spin, Modal, Tabs, Tree, Descriptions, Alert, Typography, Space, Divider, notification }
})

const repositories = [
    { id: 'design', name: 'Design', aclId: 'a', capabilities: { canCreateProject: true } },
]

const project = (overrides: Record<string, unknown> = {}) => ({
    id: 'p1',
    name: 'Alpha',
    repository: 'design',
    status: 'CLOSED',
    branch: 'main',
    modifiedBy: 'jane',
    capabilities: {},
    ...overrides,
})

function deferred<T>() {
    let resolve!: (value: T) => void
    let reject!: (reason?: unknown) => void
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise
        reject = rejectPromise
    })
    return { promise, resolve, reject }
}

async function renderWorkspace() {
    await act(async () => {
        render(<ProjectWorkspace />)
        await new Promise(resolve => setTimeout(resolve, 50))
    })
}

/** The action rendered as the single primary (blue) button, if any. */
const primaryActionId = () => screen.getByTestId('project-actions')
    .querySelector('[data-primary="true"]')
    ?.getAttribute('data-testid')

describe('ProjectWorkspace', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        routeParams.projectId = 'p1'
        for (const key of Array.from(searchParamsMock.keys())) {
            searchParamsMock.delete(key)
        }
        vi.mocked(getDesignRepositories).mockResolvedValue(repositories as never)
        vi.mocked(getProject).mockResolvedValue(project() as never)
        vi.mocked(getProjectFiles).mockResolvedValue([])
        vi.mocked(setProjectStatus).mockResolvedValue()
        vi.mocked(unlockProject).mockResolvedValue()
    })

    it('shows the project addressed by the URL, including deleted listings', async () => {
        await renderWorkspace()

        // The name appears in both the breadcrumb and the project header.
        expect(screen.getAllByText('Alpha').length).toBeGreaterThan(0)
        // The screen lives off the project's own repositoryInfo — the repository list is not read
        // until the copy dialog first asks for its targets.
        expect(getDesignRepositories).not.toHaveBeenCalled()
        expect(getProject).toHaveBeenCalledWith(
            'p1',
            { includes: ['status', 'descriptor']},
            { throwError: true, suppressErrorPages: true }
        )
        // The breadcrumb links back to the Projects home.
        expect(screen.getByText('home.title').closest('a')?.getAttribute('href')).toBe('/projects')
    })

    it('re-reads the project when the backend pings that it changed elsewhere', async () => {
        await renderWorkspace()
        expect(getProject).toHaveBeenCalledTimes(1)
        // The project moved on in another session — another client saved a table.
        vi.mocked(getProject).mockResolvedValue(project({ status: 'EDITING' }) as never)

        await act(async () => {
            liveHandlers.projectChange?.([])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(getProject).toHaveBeenCalledTimes(2)
        // The refresh changed the page, so the user learns why it moved under them.
        expect(notification.info).toHaveBeenCalledWith({ title: 'browser.live_project_synced' })
        // The real change resets the tabs and the tree beside them.
        expect(screen.getByTestId('projects-rail').getAttribute('data-reload-token')).toBe('2')
    })

    it('stays quiet when a ping merely echoes what the page already shows', async () => {
        await renderWorkspace()

        await act(async () => {
            liveHandlers.projectChange?.([])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(getProject).toHaveBeenCalledTimes(2)
        expect(notification.info).not.toHaveBeenCalled()
        // Nothing changed, so nothing is applied: no tab reset, no re-fetch cascade — the user's
        // own action already reloaded the page once.
        expect(screen.getByTestId('projects-rail').getAttribute('data-reload-token')).toBe('1')
    })

    it('takes the compile status of a ping that changed nothing else', async () => {
        // A compile that finished while its completion push was lost: the project itself is untouched,
        // so the re-read applies nothing - except the status, which is the server's latest word.
        const compiling = { projectId: 'p1', branch: 'main', compileState: 'compiling' }
        vi.mocked(getProject).mockResolvedValue(project({ status: 'OPENED', compileStatus: compiling }) as never)
        await renderWorkspace()
        expect(screen.getByTestId('compile-state').textContent).toBe('compiling')

        vi.mocked(getProject).mockResolvedValue(project({
            status: 'OPENED',
            compileStatus: {
                projectId: 'p1',
                branch: 'main',
                compileState: 'errors',
                compilation: {
                    messages: {
                        total: 1,
                        errors: 1,
                        warnings: 0,
                        items: [{ id: 1, summary: 'Cannot find type Foo', severity: 'ERROR', stacktrace: false }],
                    },
                },
            },
        }) as never)

        await act(async () => {
            liveHandlers.projectChange?.([])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.getByTestId('compile-state').textContent).toBe('errors')
        // The page itself did not change, so the tabs and the tree beside them are left alone.
        expect(screen.getByTestId('projects-rail').getAttribute('data-reload-token')).toBe('1')
    })

    it('names the repository from the project when the repository list is unreadable', async () => {
        // Granted the project alone, the user reads no repositories — the breadcrumb still names one.
        vi.mocked(getDesignRepositories).mockRejectedValue(new Error('forbidden'))
        vi.mocked(getProject).mockResolvedValue(project({
            repositoryInfo: { id: 'design', name: 'Design', type: 'repo-git', features: { branches: false, searchable: false, mappedFolders: false } },
        }) as never)

        await renderWorkspace()

        expect(screen.getAllByText('Design').length).toBeGreaterThan(0)
    })

    it('hides the history tab when history cannot be viewed', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: {} }) as never)
        await renderWorkspace()

        expect(screen.queryByText('browser.tab_history')).toBeNull()
    })

    it('shows the history tab when history can be viewed', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canViewHistory: true } }) as never)
        await renderWorkspace()

        expect(screen.getByText('browser.tab_history')).toBeTruthy()
    })

    it('ignores a stale project load after navigating to another project', async () => {
        const firstLoad = deferred<ReturnType<typeof project>>()
        const secondLoad = deferred<ReturnType<typeof project>>()
        vi.mocked(getProject).mockImplementation((id) => {
            return (id === 'p1' ? firstLoad.promise : secondLoad.promise) as never
        })

        let view!: ReturnType<typeof render>
        await act(async () => {
            view = render(<ProjectWorkspace />)
        })

        routeParams.projectId = 'p2'
        await act(async () => {
            view.rerender(<ProjectWorkspace />)
        })

        await act(async () => {
            secondLoad.resolve(project({ id: 'p2', name: 'Beta' }))
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        expect(screen.getAllByText('Beta').length).toBeGreaterThan(0)

        await act(async () => {
            firstLoad.resolve(project({ id: 'p1', name: 'Alpha' }))
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByText('Alpha')).toBeNull()
        expect(screen.getAllByText('Beta').length).toBeGreaterThan(0)
    })

    it('holds the live pings while its own action runs, and applies the answer that action brings', async () => {
        const closed = project({ capabilities: { canOpen: true } })
        const opened = project({ status: 'OPENED', capabilities: { canOpen: true } })
        const actionRead = deferred<ReturnType<typeof project>>()
        let reads = 0
        vi.mocked(getProject).mockImplementation(() => {
            reads += 1
            if (reads === 1) {
                return Promise.resolve(closed) as never
            }
            return (reads === 2 ? actionRead.promise : Promise.resolve(opened)) as never
        })
        await renderWorkspace()
        expect(liveHandlers.holdWhile).toBe(false)

        await userEvent.click(screen.getByTestId('open-p1'))
        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'OPENED', { openDependencies: true }))
        // The action's own read is on the wire, and the environment is slow. A refresh started
        // beside it would supersede it and unlock the buttons over the state before the action, so
        // the page asks the hook to hold what arrives (the holding itself is the hook's own test).
        await waitFor(() => expect(reads).toBe(2))
        expect(liveHandlers.holdWhile).toBe(true)

        await act(async () => {
            actionRead.resolve(opened)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        // The action's own answer reached the page — the tabs and the tree beside them followed it.
        expect(screen.getByTestId('projects-rail').getAttribute('data-reload-token')).toBe('2')
        // And with the action finished, the pings flow again.
        expect(liveHandlers.holdWhile).toBe(false)
    })

    it('shows the project busy until the read behind its action lands, not just until the request returns', async () => {
        const closed = project({ capabilities: { canOpen: true, canCopy: true } })
        const actionRead = deferred<ReturnType<typeof project>>()
        let reads = 0
        vi.mocked(getProject).mockImplementation(() => {
            reads += 1
            return (reads === 1 ? Promise.resolve(closed) : actionRead.promise) as never
        })
        await renderWorkspace()
        expect(screen.queryByTestId('project-workspace-overlay')).toBeNull()

        await userEvent.click(screen.getByTestId('open-p1'))

        // The status request is over and the reload is on the wire. Without a busy state here the page
        // shows exactly what it showed before the click, for as long as the environment is slow.
        await waitFor(() => expect(reads).toBe(2))
        expect(screen.getByTestId('project-workspace-overlay')).toBeInTheDocument()
        expect(screen.getByTestId('copy-p1')).toBeDisabled()

        await act(async () => {
            actionRead.resolve(project({ status: 'OPENED', capabilities: { canOpen: true, canCopy: true } }))
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByTestId('project-workspace-overlay')).toBeNull()
        expect(screen.getByTestId('copy-p1')).not.toBeDisabled()
    })

    it('holds the Sync action through the branch-list request and leaves its dialog unrefreshed', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canManageBranches: true } }) as never)
        const branches = deferred<unknown[]>()
        vi.mocked(getProjectBranches).mockReturnValue(branches.promise as never)
        const dispatchSpy = vi.spyOn(window, 'dispatchEvent')
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('sync-p1'))

        // The dialog cannot be dispatched before the branches are known — a whole round trip during which
        // the click would otherwise dead-end.
        expect(screen.getByTestId('project-workspace-overlay')).toBeInTheDocument()
        expect(dispatchSpy.mock.calls.some(([event]) => event.type === 'openMergeModal')).toBe(false)

        await act(async () => {
            branches.resolve([{ name: 'main', base: true }])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(dispatchSpy.mock.calls.some(([event]) => event.type === 'openMergeModal')).toBe(true)
        // The dialog is the user's now: the project is free again and no reload flashes behind it.
        expect(screen.queryByTestId('project-workspace-overlay')).toBeNull()
        expect(getProject).toHaveBeenCalledTimes(1)
        dispatchSpy.mockRestore()
    })

    it('keeps the project busy while a dialog it just finished is being read back', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canSave: true } }) as never)
        await renderWorkspace()
        await userEvent.click(screen.getByTestId('save-p1'))

        const reload = deferred<ReturnType<typeof project>>()
        vi.mocked(getProject).mockReturnValueOnce(reload.promise as never)
        await userEvent.click(screen.getByTestId('save-modal-saved'))

        // The dialog's own spinner is gone with the dialog; without this the page would sit showing the
        // state from before the save for as long as reading it back takes.
        await waitFor(() => expect(screen.getByTestId('project-workspace-overlay')).toBeInTheDocument())

        await act(async () => {
            reload.resolve(project({ capabilities: { canSave: true } }))
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByTestId('project-workspace-overlay')).toBeNull()
    })

    it('shows the branch switch over the project, from the click until the new branch lands', async () => {
        vi.mocked(getDesignRepositories).mockResolvedValue([
            { ...repositories[0]!, features: { branches: true, searchable: false, mappedFolders: false } },
        ] as never)
        vi.mocked(getProject).mockResolvedValue(project({
            capabilities: { canSave: true },
            repositoryInfo: { id: 'design', name: 'Design', type: 'repo-git', features: { branches: true } },
        }) as never)
        vi.mocked(getProjectBranches).mockResolvedValue([{ name: 'main', base: true }, { name: 'dev' }] as never)
        const switched = deferred<void>()
        vi.mocked(switchProjectBranch).mockReturnValue(switched.promise as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('crumb-branch-trigger'))
        await screen.findByText('dev')
        await userEvent.click(screen.getByText('dev'))

        // Two round trips — the switch and the read of the project on its new branch — during which the
        // page would otherwise be indistinguishable from one where the click never registered.
        await waitFor(() => expect(screen.getByTestId('project-workspace-overlay')).toBeInTheDocument())
        expect(screen.getByTestId('save-p1')).toBeDisabled()

        await act(async () => {
            switched.resolve()
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.queryByTestId('project-workspace-overlay')).toBeNull())
    })

    it('blocks the branch switch while the project is busy with an action of its own', async () => {
        vi.mocked(getDesignRepositories).mockResolvedValue([
            { ...repositories[0]!, features: { branches: true, searchable: false, mappedFolders: false } },
        ] as never)
        vi.mocked(getProject).mockResolvedValue(project({
            capabilities: { canOpen: true },
            repositoryInfo: { id: 'design', name: 'Design', type: 'repo-git', features: { branches: true } },
        }) as never)
        const opened = deferred<void>()
        vi.mocked(setProjectStatus).mockReturnValue(opened.promise as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('open-p1'))

        // The veil over the project takes the clicks, but not the keyboard, which reaches the switcher.
        // A switch started here would run against the state the action is about to replace.
        await waitFor(() => expect(screen.getByTestId('crumb-branch-trigger')).toBeDisabled())

        await act(async () => {
            opened.resolve()
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.getByTestId('crumb-branch-trigger')).not.toBeDisabled())
    })

    it('shows a not-found state for an unknown project id', async () => {
        routeParams.projectId = 'missing'
        vi.mocked(getProject).mockRejectedValue(new NotFoundError())
        await renderWorkspace()

        expect(screen.getByTestId('project-workspace-missing')).toBeTruthy()
        await userEvent.click(screen.getByText('home.back_to_projects'))
        expect(navigateMock).toHaveBeenCalledWith('/projects')
    })

    it('opens the project with dependencies via the gated Open action', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canOpen: true } }) as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('open-p1'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'OPENED', { openDependencies: true }))
    })

    it('withdraws the open question when the route swaps in another project', async () => {
        // The page stays mounted across /projects/p1 -> /projects/p2, and the dialog lives above the routes,
        // so without withdrawing it a confirmation would open the project the user has already left.
        vi.mocked(getProject).mockResolvedValue(project({
            capabilities: { canOpen: true },
            dependencies: [{ name: 'Provider', missing: true }],
        }) as never)
        const events: CustomEvent[] = []
        const listener = (event: Event) => events.push(event as CustomEvent)
        window.addEventListener('openProjectModal', listener)

        try {
            let view!: ReturnType<typeof render>
            await act(async () => {
                view = render(<ProjectWorkspace />)
                await new Promise(resolve => setTimeout(resolve, 50))
            })

            await userEvent.click(screen.getByTestId('open-p1'))
            expect(events.at(-1)?.detail?.projectName).toBe('Alpha')

            routeParams.projectId = 'p2'
            await act(async () => {
                view.rerender(<ProjectWorkspace />)
                await new Promise(resolve => setTimeout(resolve, 50))
            })

            expect(events.at(-1)?.detail).toBeNull()
            // The question was never answered, so nothing was opened.
            expect(setProjectStatus).not.toHaveBeenCalled()
        } finally {
            window.removeEventListener('openProjectModal', listener)
        }
    })

    it('closes an unmodified project immediately', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ status: 'OPENED', capabilities: { canClose: true } }) as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('close-p1'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'CLOSED', {}))
    })

    it('offers discarding changes when the backend rejects a regular close because of unsaved changes', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ status: 'OPENED', capabilities: { canClose: true } }) as never)
        vi.mocked(setProjectStatus)
            .mockRejectedValueOnce(new ApiHttpError(409, 'Cannot close a project with unsaved changes.', {
                code: 'openl.error.409.project.close.modified.message',
            }))
            .mockResolvedValueOnce(undefined)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('close-p1'))

        await screen.findByText('browser.close_discard_title')
        expect(screen.getByText('browser.close_discard_warning')).toBeTruthy()
        expect(screen.getByText('browser.close_discard_confirm_unsafe')).toBeTruthy()
        expect(setProjectStatus).toHaveBeenCalledWith('p1', 'CLOSED', {})

        await userEvent.click(screen.getByTestId('discard-close-confirm'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenLastCalledWith('p1', 'CLOSED', { discardChanges: true }))
    })

    it('requires confirmation before closing a project with unsaved changes', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ status: 'EDITING', capabilities: { canClose: true } }) as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('close-p1'))

        expect(setProjectStatus).not.toHaveBeenCalled()
        expect(screen.getByText('browser.close_discard_title')).toBeTruthy()
        expect(screen.getByText('browser.close_discard_warning')).toBeTruthy()
        expect(screen.getByText('browser.close_discard_confirm_unsafe')).toBeTruthy()
        await userEvent.click(screen.getByTestId('discard-close-confirm'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'CLOSED', { discardChanges: true }))
    })

    it('deletes the project and returns to the Projects home', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canDelete: true } }) as never)
        const listener = vi.fn()
        window.addEventListener('openDeleteProjectModal', listener as EventListener)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('delete-p1'))

        expect(listener).toHaveBeenCalled()
        const event = listener.mock.calls.at(-1)![0] as CustomEvent
        expect(event.detail).toMatchObject({
            projectId: 'p1',
            projectName: 'Alpha',
        })

        event.detail.onSuccess()

        expect(navigateMock).toHaveBeenCalledWith('/projects')
        window.removeEventListener('openDeleteProjectModal', listener as EventListener)
    })

    it('opens the save dialog via the gated Save action', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ status: 'EDITING', capabilities: { canSave: true } }) as never)
        await renderWorkspace()

        expect(screen.queryByTestId('save-modal-open')).toBeNull()
        await userEvent.click(screen.getByTestId('save-p1'))

        await screen.findByTestId('save-modal-open')
    })

    it('opens the copy dialog via the gated Copy action', async () => {
        vi.mocked(getDesignRepositories).mockResolvedValue([
            ...repositories,
            { id: 'readonly', name: 'Read Only', aclId: 'b', capabilities: {} },
        ] as never)
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canCopy: true } }) as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('copy-p1'))

        await screen.findByTestId('copy-modal-open')
        expect(screen.getByTestId('copy-modal-open').textContent).toBe('design')
    })

    it('leads with Open Revision once the project is already open, leaving Close the primary action', async () => {
        vi.mocked(getProject).mockResolvedValue(project({
            capabilities: { canViewHistory: true, canClose: true },
        }) as never)
        await renderWorkspace()

        // It takes the Open slot, so it comes before every other action — but it never takes the blue.
        expect(screen.getByTestId('project-actions').firstElementChild)
            .toHaveAttribute('data-testid', 'openRevision-p1')
        expect(primaryActionId()).toBe('close-p1')

        await userEvent.click(screen.getByTestId('openRevision-p1'))

        await screen.findByTestId('open-revision-modal')
    })

    it('hangs Open Revision off the Open button while the project can still be opened', async () => {
        vi.mocked(getProject)
            .mockResolvedValue(project({ capabilities: { canOpen: true, canViewHistory: true } }) as never)
        await renderWorkspace()

        // Open stays the action; opening a revision is its menu item.
        expect(screen.getByTestId('open-p1')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('openRevision-p1'))

        await screen.findByTestId('open-revision-modal')
    })

    it('opens the merge dialog via the Sync action', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canManageBranches: true } }) as never)
        const dispatchSpy = vi.spyOn(window, 'dispatchEvent')
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('sync-p1'))

        await waitFor(() => expect(dispatchSpy.mock.calls
            .some(([event]) => event instanceof CustomEvent && event.type === 'openMergeModal')).toBe(true))
        dispatchSpy.mockRestore()
    })

    it('opens the delete-branch dialog via the Delete Branch action', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canDeleteBranch: true } }) as never)
        const dispatchSpy = vi.spyOn(window, 'dispatchEvent')
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('deleteBranch-p1'))

        await waitFor(() => expect(dispatchSpy.mock.calls
            .some(([event]) => event instanceof CustomEvent && event.type === 'openDeleteBranchModal')).toBe(true))
        dispatchSpy.mockRestore()
    })

    it('offers no Delete Branch action on the repository main branch', async () => {
        // The main branch cannot be deleted, so the server withholds the capability while still allowing
        // the other branch operations.
        vi.mocked(getProject)
            .mockResolvedValue(project({ branchDefault: true, capabilities: { canManageBranches: true } }) as never)
        await renderWorkspace()

        expect(screen.getByTestId('sync-p1')).toBeInTheDocument()
        expect(screen.queryByTestId('deleteBranch-p1')).toBeNull()
    })

    it('opens the deploy modal via a CustomEvent', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ comment: '', revision: 'r1', capabilities: { canDeploy: true } }) as never)
        const dispatchSpy = vi.spyOn(window, 'dispatchEvent')
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('deploy-p1'))

        expect(dispatchSpy.mock.calls.some(([event]) => event instanceof CustomEvent && event.type === 'openDeployModal')).toBe(true)
        dispatchSpy.mockRestore()
    })

    it('offers the revision to export instead of downloading straight away', async () => {
        vi.mocked(getProject).mockResolvedValue(project({ capabilities: { canExport: true } }) as never)
        await renderWorkspace()

        await userEvent.click(screen.getByTestId('export-p1'))

        expect(await screen.findByTestId('export-modal')).toBeInTheDocument()
    })

    it('does not load project files before the Files tab is active', async () => {
        await renderWorkspace()

        expect(screen.queryByTestId('files-p1')).toBeNull()
        expect(getProjectFiles).not.toHaveBeenCalled()
    })

    it('loads the project files into the Files tab', async () => {
        searchParamsMock.set('tab', 'files')
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'rules/Main.xlsx', name: 'Main.xlsx', basePath: 'rules', type: 'file' },
        ] as never)
        await renderWorkspace()

        await screen.findByTestId('files-p1')
        expect(screen.getByText('Main.xlsx')).toBeTruthy()
        expect(getProjectFiles).toHaveBeenCalledWith('p1')
    })

    it('shows an error state and does not loop when file loading fails', async () => {
        searchParamsMock.set('tab', 'files')
        vi.mocked(getProjectFiles).mockRejectedValue(new Error('boom'))
        await renderWorkspace()

        await screen.findByText('browser.files_error')
        // A failed load must be requested exactly once, never retried in a loop.
        expect(getProjectFiles).toHaveBeenCalledTimes(1)
    })

    it('shows an error state when the listing fails to load', async () => {
        vi.mocked(getProject).mockRejectedValue(new Error('boom'))
        await renderWorkspace()

        expect(screen.getByTestId('project-workspace-error')).toBeTruthy()
    })
})

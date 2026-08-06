import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectsHome } from './ProjectsHome'
import { getDesignRepositories, getProjects, setProjectStatus } from '../services/repositories'
import type { Project, ProjectsPage } from '../types/projects'
import { ProjectStatus } from '../constants/project'
import { getProjectIndex, invalidateProjectIndex } from '../services/projectIndex'
import { notification } from 'antd'
import { openDeleteBranchDialog, openMergeDialog } from './projects/branchDialogs'
import { openCompareWindow } from './projects/compare'

const { copyModalMock, navigateMock, liveHandlers } = vi.hoisted(() => ({
    copyModalMock: vi.fn(),
    navigateMock: vi.fn(),
    // The pings, the focus revalidation and the status stream captured from the screen, to fire by hand.
    liveHandlers: {
        workspaceChange: undefined as (() => void) | undefined,
        focus: undefined as (() => void) | undefined,
        statusUpdate: undefined as ((update: unknown) => void) | undefined,
    },
}))

vi.mock('../hooks', () => ({
    useWorkspaceChanges: (onChange: () => void) => {
        liveHandlers.workspaceChange = onChange
    },
    useWindowFocus: (onFocus: () => void) => {
        liveHandlers.focus = onFocus
    },
}))

vi.mock('../services/projectStatus', () => ({
    subscribeWorkspaceProjectStatuses: (onUpdate: (update: unknown) => void) => {
        liveHandlers.statusUpdate = onUpdate
        return { unsubscribe: vi.fn() }
    },
}))

vi.mock('react-router-dom', async () => {
    const { useState } = await import('react')
    return {
        useNavigate: () => navigateMock,
        // URL params modeled as plain state: functional updates work, options are ignored.
        useSearchParams: () => {
            const [params, setParams] = useState(new URLSearchParams())
            const set = (next: URLSearchParams | ((prev: URLSearchParams) => URLSearchParams)) => {
                setParams(prev => (typeof next === 'function' ? next(prev) : next))
            }
            return [params, set] as const
        },
    }
})

vi.mock('./projects/filterStorage', () => ({
    // The screen under test starts from a clean slate; restoring is covered by the storage's own tests.
    loadProjectFilters: vi.fn(() => null),
    saveProjectFilters: vi.fn(),
}))

vi.mock('../services/repositories', () => ({
    getDesignRepositories: vi.fn(),
    getProjects: vi.fn(),
    downloadProject: vi.fn(),
    deleteProject: vi.fn(),
    setProjectStatus: vi.fn(),
}))

vi.mock('./projects/NewProjectModal', () => ({
    NewProjectModal: ({ open, repositories, onCreated }: {
        open: boolean
        repositories?: { id: string }[]
        onCreated?: (created?: { repositoryId: string, name: string }) => void
    }) => (open ? (
        <div data-testid="new-project-modal">
            {repositories?.map(repo => repo.id).join(',')}
            <button
                aria-label="created"
                data-testid="new-project-fire-created"
                onClick={() => onCreated?.({ repositoryId: 'design', name: 'Created' })}
                type="button"
            />
        </div>
    ) : null),
}))

vi.mock('./projects/CopyProjectModal', () => ({
    CopyProjectModal: (props: { open: boolean, repositories?: { id: string }[] }) => {
        copyModalMock(props)
        return props.open
            ? <div data-testid="copy-project-modal">{props.repositories?.map(repo => repo.id).join(',')}</div>
            : null
    },
}))

vi.mock('./projects/ExportProjectModal', () => ({
    ExportProjectModal: ({ open }: { open: boolean }) => (open ? <div data-testid="export-modal" /> : null),
}))

vi.mock('./projects/OpenRevisionModal', () => ({
    OpenRevisionModal: ({ open }: { open: boolean }) => (open ? <div data-testid="open-revision-modal" /> : null),
}))

vi.mock('./projects/SaveProjectModal', () => ({
    SaveProjectModal: ({ open }: { open: boolean }) => (open ? <div data-testid="save-project-modal" /> : null),
}))

vi.mock('./projects/branchDialogs', () => ({
    openDeleteBranchDialog: vi.fn().mockResolvedValue(undefined),
    openMergeDialog: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('./projects/compare', () => ({ openCompareWindow: vi.fn() }))

// The compile dot fetches project status; stub it out so the list test stays offline.
vi.mock('./projects/CompileIndicator', () => ({
    RowCompileDot: () => null,
    CompileDot: () => null,
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
}))

interface Option { value: string, label?: unknown }
interface MenuItem { key: string, label?: unknown, type?: string }

vi.mock('antd', async () => {
    const { forwardRef } = await import('react')
    const drop = <T extends Record<string, unknown>>(props: T) => props

    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { danger, loading, type, size, shape, ghost, block, ...dom } = rest
        drop({ danger, loading, type, size, shape, ghost, block })
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }

    const Input = Object.assign(
        forwardRef<HTMLInputElement, Record<string, unknown>>(({ value, onChange, placeholder, allowClear, className, prefix, suffix, ...rest }, ref) => {
            drop({ allowClear, className, prefix, suffix })
            return <input ref={ref} onChange={onChange as never} placeholder={placeholder as never} value={(value as string) ?? ''} {...rest} />
        }),
        {
            TextArea: ({ onChange, ...rest }: Record<string, unknown>) => <textarea onChange={onChange as never} {...rest} />,
            Search: ({ onChange, ...rest }: Record<string, unknown>) => <input onChange={onChange as never} {...rest} />,
        }
    )

    const Select = ({ options, onChange, value, placeholder, allowClear, className, prefix, ...rest }: Record<string, unknown>) => {
        drop({ allowClear, className, prefix })
        return (
            <select
                onChange={event => (onChange as (v: string | undefined) => void)?.(event.target.value || undefined)}
                value={(value as string) ?? ''}
                {...rest}
            >
                <option value="">{placeholder as never}</option>
                {(options as Option[])?.map(option => (
                    <option key={option.value} value={option.value}>{option.label as never}</option>
                ))}
            </select>
        )
    }

    const Segmented = ({ options, onChange, value, block, size, className, ...rest }: Record<string, unknown>) => {
        drop({ value, block, size, className })
        return (
            <div {...rest}>
                {(options as Option[])?.map(option => (
                    <button key={option.value} onClick={() => (onChange as (v: string) => void)?.(option.value)} type="button">
                        {String(option.value)}
                    </button>
                ))}
            </div>
        )
    }

    const Dropdown = ({ children, menu }: { children?: unknown, menu?: { items?: MenuItem[], onClick?: (info: { key: string }) => void } }) => (
        <div>
            {children as never}
            {menu?.items?.filter(item => item.type !== 'divider').map(item => (
                <button
                    key={item.key}
                    onClick={() => menu.onClick?.({ key: item.key, domEvent: { stopPropagation: vi.fn() } } as never)}
                    type="button"
                >
                    {item.label as never}
                </button>
            ))}
        </div>
    )

    const Checkbox = ({ children, checked, onChange, ...rest }: Record<string, unknown>) => (
        <label {...rest}>
            <input
                checked={checked as boolean}
                onChange={event => (onChange as (e: { target: { checked: boolean } }) => void)?.({ target: { checked: event.target.checked } })}
                type="checkbox"
            />
            {children as never}
        </label>
    )

    const Empty = ({ description, image, children, ...rest }: Record<string, unknown>) => {
        drop({ image })
        return <div {...rest}>{description as never}{children as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'

    const Tag = ({ children, icon, ...rest }: Record<string, unknown>) =>
        <span data-testid={rest['data-testid'] as string}>{icon as never}{children as never}</span>
    const Tooltip = ({ children }: { children?: unknown }) => <>{children as never}</>
    const Skeleton = () => <div>skeleton</div>
    const Spin = () => <div>spin</div>
    const Alert = ({ action, title, description, showIcon, type, closable, ...rest }: Record<string, unknown>) => {
        drop({ showIcon, type })
        const onClose = (closable as { onClose?: () => void } | undefined)?.onClose
        return (
            <div {...rest}>
                {title as never}{description as never}{action as never}
                {closable ? <button aria-label="close" onClick={onClose} type="button" /> : null}
            </div>
        )
    }

    const Typography = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Typography.Text = ({ children, ...rest }: Record<string, unknown>) => {
        const { ellipsis, className, ...dom } = rest
        drop({ ellipsis, className })
        return <span {...dom}>{children as never}</span>
    }
    Typography.Title = ({ children }: { children?: unknown }) => <h3>{children as never}</h3>

    const Modal = Object.assign(({ open, children, title, onOk, onCancel, okText, okButtonProps, cancelButtonProps }: Record<string, unknown>) => (open
        ? (
            <div role="dialog">
                {title as never}
                {children as never}
                <button
                    data-testid={(okButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                    onClick={onOk as never}
                    type="button"
                >
                    {(okText as string | undefined) ?? 'ok'}
                </button>
                <button
                    data-testid={(cancelButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                    onClick={onCancel as never}
                    type="button"
                >
                    cancel
                </button>
            </div>
        )
        : null), { confirm: vi.fn() })
    const notification = { error: vi.fn(), info: vi.fn(), success: vi.fn() }

    return { Button, Input, Select, Segmented, Dropdown, Checkbox, Empty, Tag, Tooltip, Skeleton, Spin, Alert, Typography, Modal, notification }
})

const repositories = [
    { id: 'design', name: 'Design', aclId: 'a', capabilities: { canCreateProject: true } },
    { id: 'ro', name: 'ReadOnly', aclId: 'b', capabilities: {} },
]

const projects = [
    {
        id: 'p1',
        name: 'Alpha',
        repository: 'design',
        repositoryInfo: { id: 'design', name: 'Design', type: 'repo-git', features: { branches: true, searchable: true, mappedFolders: false } },
        status: ProjectStatus.Closed,
        branch: 'main',
        modifiedBy: 'jane',
        modifiedAt: '2026-01-01T00:00:00Z',
        comment: '',
        revision: '1',
        path: 'rules/Alpha',
        tags: { Category: 'Payroll' },
    },
    {
        id: 'p2',
        name: 'Beta',
        repository: 'ro',
        repositoryInfo: { id: 'ro', name: 'ReadOnly', type: 'repo-jdbc', features: { branches: false, searchable: false, mappedFolders: false } },
        status: ProjectStatus.Closed,
        branch: 'main',
        modifiedBy: 'john',
        modifiedAt: '2026-06-01T00:00:00Z',
        comment: '',
        path: 'rules/Beta',
        revision: '2',
    },
] satisfies Project[]

const rowOrder = () => screen.getAllByTestId(/^project-row-/).map(el => el.getAttribute('data-testid'))

async function renderHome() {
    await act(async () => {
        render(<ProjectsHome />)
        await new Promise(resolve => setTimeout(resolve, 50))
    })
    // The screen restores the saved filters before it asks for anything, so wait for the answer itself
    // rather than for a fixed delay — on a busy machine the fetch lands after it.
    await waitFor(() => expect(screen.queryByText('skeleton')).toBeNull())
}

async function flushSearch() {
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 350))
    })
}

function projectsPage(content: Project[], total = content.length, withCounts = true): ProjectsPage {
    const page: ProjectsPage = {
        content,
        numberOfElements: content.length,
        pageNumber: 0,
        pageSize: 20,
        total,
    }
    if (!withCounts) {
        return page
    }
    return {
        ...page,
        statusCounts: {
            local: content.filter(project => project.status === ProjectStatus.Local).length,
            opened: content.filter(project => project.status === ProjectStatus.Opened).length,
            viewingVersion: content.filter(project => project.status === ProjectStatus.ViewingVersion).length,
            editing: content.filter(project => project.status === ProjectStatus.Editing).length,
            closed: content.filter(project => project.status === ProjectStatus.Closed).length,
            deleted: content.filter(project => project.status === ProjectStatus.Deleted).length,
        },
        repositoryCounts: [
            { id: 'design', name: 'Design', count: content.filter(project => project.repository === 'design').length },
            { id: 'ro', name: 'ReadOnly', count: content.filter(project => project.repository === 'ro').length },
            { id: '__local__', name: 'Local', count: content.filter(project => project.status === ProjectStatus.Local).length },
        ].filter(count => count.count > 0),
        tagCounts: [
            {
                type: 'Category',
                values: [{ id: 'Payroll', name: 'Payroll', count: content.filter(project => project.tags?.['Category'] === 'Payroll').length }]
                    .filter(count => count.count > 0),
            },
        ].filter(facet => facet.values.length > 0),
    }
}

/**
 * The screen reads the whole workspace once and filters, sorts and pages it in the browser, so the
 * server answers one request with everything.
 */
function mockProjectSearch(source: Project[] = projects) {
    vi.mocked(getProjects).mockImplementation(async () =>
        projectsPage([...source].sort((left, right) => left.name.localeCompare(right.name)), source.length, false))
}

describe('ProjectsHome', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        // The snapshot of the workspace is shared by the screens, so each test starts without one.
        invalidateProjectIndex()
        vi.mocked(getDesignRepositories).mockResolvedValue(repositories as never)
        mockProjectSearch()
    })

    it('lists every project as one flat row with its repository as a facet', async () => {
        await renderHome()

        expect(screen.getByTestId('project-row-p1')).toBeTruthy()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
        // Repositories are a filter facet, shown in the rail rather than as a list-row column.
        expect(screen.getAllByText('Design').length).toBeGreaterThanOrEqual(1)
        expect(screen.getAllByText('ReadOnly').length).toBeGreaterThanOrEqual(1)
        // The tag value shows up both as a row tag and as a tag facet in the rail.
        expect(screen.getAllByText('Payroll').length).toBeGreaterThan(1)
        expect(screen.queryByText('Design/rules/Alpha')).toBeNull()

        await userEvent.click(screen.getByText('grid'))

        expect(screen.getByTestId('project-card-p1')).toBeTruthy()
        // The grid card still carries the repository badge, so it now appears in the rail and the card.
        expect(screen.getAllByText('Design').length).toBeGreaterThan(1)
        expect(screen.queryByText('Design/rules/Alpha')).toBeNull()
    })

    it('keeps the compile health strip live from the one workspace status stream', async () => {
        await renderHome()
        expect(screen.getByTestId('projects-compile-summary').textContent).not.toContain('browser.compile.errors')

        // A compile finished with errors somewhere in the workspace — no per-row subscription involved.
        await act(async () => {
            liveHandlers.statusUpdate?.({ projectId: 'p1', compileState: 'errors' })
        })

        expect(screen.getByTestId('projects-compile-summary').textContent).toContain('browser.compile.errors')
    })

    it('re-reads the list when the backend pings that the workspace changed', async () => {
        await renderHome()
        expect(getProjects).toHaveBeenCalledTimes(1)
        // The workspace moved on elsewhere — another session created a project.
        mockProjectSearch([...projects, {
            ...projects[1]!,
            id: 'p3',
            name: 'Gamma',
            repository: 'design',
        }] as Project[])

        await act(async () => {
            liveHandlers.workspaceChange?.()
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        // The fresh answer swaps in behind the scenes, without a skeleton — and the user is told.
        expect(await screen.findByTestId('project-row-p3')).toBeTruthy()
        expect(getProjects).toHaveBeenCalledTimes(2)
        expect(notification.info).toHaveBeenCalledWith({ title: 'home.live_synced' })
    })

    it('stays quiet when a ping merely echoes what the list already shows', async () => {
        await renderHome()

        // The ping echoes the user's own action: the re-read returns the same list.
        await act(async () => {
            liveHandlers.workspaceChange?.()
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(getProjects).toHaveBeenCalledTimes(2)
        expect(notification.info).not.toHaveBeenCalled()
    })

    it('re-reads a snapshot left by an earlier visit and swaps the fresh answer in', async () => {
        // An earlier visit left a snapshot holding only Alpha…
        mockProjectSearch(projects.slice(0, 1))
        await getProjectIndex()
        // …and the workspace moved on meanwhile: Beta appeared.
        mockProjectSearch()

        await renderHome()

        // The background re-read brought Beta in without the user asking.
        await waitFor(() => expect(screen.getByTestId('project-row-p2')).toBeTruthy())
        // One read from the earlier visit, one re-read; the first paint itself came from memory.
        expect(getProjects).toHaveBeenCalledTimes(2)
    })

    it('names the repository of a project the user may read without reading its repository', async () => {
        // A user granted a single project sees no repositories at all: the badge has to come from the project.
        vi.mocked(getDesignRepositories).mockResolvedValue([])

        await renderHome()

        await userEvent.click(screen.getByText('grid'))

        expect(screen.getAllByText('Design').length).toBeGreaterThanOrEqual(1)
    })

    it('keeps initial load failures local to the Projects page', async () => {
        await renderHome()

        expect(getDesignRepositories).toHaveBeenCalledWith({ throwError: true, suppressErrorPages: true })
        expect(getProjects).toHaveBeenCalledWith(expect.any(Object), { throwError: true, suppressErrorPages: true })
    })

    it('sorts alphabetically by default, by the Modified header on demand, newest first', async () => {
        await renderHome()

        expect(rowOrder()).toEqual(['project-row-p1', 'project-row-p2'])

        await userEvent.click(screen.getByTestId('projects-sort-updated'))

        await screen.findByTestId('project-row-p2')
        expect(rowOrder()).toEqual(['project-row-p2', 'project-row-p1'])

        // A second click on the same header flips the direction.
        await userEvent.click(screen.getByTestId('projects-sort-updated'))
        expect(rowOrder()).toEqual(['project-row-p1', 'project-row-p2'])
    })

    it('navigates to the project workspace when a row is clicked', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('project-row-p1'))

        expect(navigateMock).toHaveBeenCalledWith('/projects/p1')
    })

    it('filters rows by the search text', async () => {
        await renderHome()

        await userEvent.type(screen.getByTestId('projects-search'), 'alp')
        await flushSearch()

        expect(screen.getByTestId('project-row-p1')).toBeTruthy()
        expect(screen.queryByTestId('project-row-p2')).toBeNull()
    })

    it('focuses the search box on the / shortcut', async () => {
        await renderHome()

        fireEvent.keyDown(window, { key: '/' })

        expect(document.activeElement).toBe(screen.getByTestId('projects-search'))
    })

    it('filters rows by repository from the rail', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('filter-repo-ro'))
        await screen.findByTestId('project-row-p2')

        expect(screen.queryByTestId('project-row-p1')).toBeNull()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
    })

    it('offers only the statuses some project is actually in', async () => {
        // Both fixture projects are CLOSED: the other states are noise and are not offered.
        await renderHome()

        expect(screen.getByTestId('filter-status-CLOSED')).toBeTruthy()
        expect(screen.queryByTestId('filter-status-OPENED')).toBeNull()
        expect(screen.queryByTestId('filter-status-LOCAL')).toBeNull()
        expect(screen.queryByTestId('filter-status-EDITING')).toBeNull()
    })

    it('keeps a ticked status visible even when the search leaves it with no matches', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('filter-status-CLOSED'))
        await userEvent.type(screen.getByTestId('projects-search'), 'nothing-matches-this')
        await flushSearch()

        // Zero matches everywhere, but the ticked state must stay so it can be unticked.
        expect(screen.getByTestId('filter-status-CLOSED')).toBeTruthy()
    })

    it('filters rows by status from the rail', async () => {
        const mixed: Project[] = [{ ...projects[0]! }, { ...projects[1]!, status: ProjectStatus.Opened }]
        mockProjectSearch(mixed)
        await renderHome()

        await userEvent.click(screen.getByTestId('filter-status-OPENED'))
        await screen.findByTestId('project-row-p2')

        expect(screen.queryByTestId('project-row-p1')).toBeNull()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
    })

    it('reads the workspace once and filters it without asking the server again', async () => {
        await renderHome()

        expect(getProjects).toHaveBeenCalledTimes(1)
        // The counts of the rail are counted in the browser, not asked for.
        expect(screen.getByTestId('filter-tag-Category:Payroll')).toBeTruthy()

        await userEvent.click(screen.getByTestId('filter-repo-ro'))
        await waitFor(() => expect(screen.queryByTestId('project-row-p1')).toBeNull())

        await userEvent.type(screen.getByTestId('projects-search'), 'p')
        await flushSearch()

        // Filtering and searching are answered from the snapshot: no second read.
        expect(getProjects).toHaveBeenCalledTimes(1)
        // The counts still stand for the search scope, with the picked facets ignored.
        expect(screen.getByTestId('filter-tag-Category:Payroll')).toBeTruthy()
    })

    it('gives every tag type a group of its own, which folds on its own', async () => {
        await renderHome()

        const tagGroup = within(screen.getByTestId('filter-group-tag:Category'))
        expect(tagGroup.getByText('Category')).toBeTruthy()
        expect(tagGroup.getByTestId('filter-tag-Category:Payroll')).toBeTruthy()

        await userEvent.click(screen.getByTestId('filter-toggle-tag:Category'))

        expect(screen.getByTestId('filter-toggle-tag:Category').getAttribute('aria-expanded')).toBe('false')
        expect(screen.queryByTestId('filter-tag-Category:Payroll')).toBeNull()
    })

    it('reads the repositories first, then branches, the tags and the states last', async () => {
        await renderHome()

        const groups = [...document.querySelectorAll('[data-testid^="filter-group-"]')]
            .map(group => group.getAttribute('data-testid'))

        expect(groups).toEqual(['filter-group-repository', 'filter-group-branch', 'filter-group-tag:Category', 'filter-group-status'])
    })

    it('marks the default and protected branches in the branch facet', async () => {
        mockProjectSearch([{ ...projects[0]!, branch: 'main', branchDefault: true, branchProtected: true }])
        await renderHome()

        expect(screen.getByTestId('filter-branch-label-main-default')).toBeTruthy()
        expect(screen.getByTestId('filter-branch-label-main-protected')).toBeTruthy()
    })

    it('puts a filter away and brings it back, while the rail is being arranged', async () => {
        await renderHome()

        // Off that mode the rail carries no controls of its own — only the filters themselves.
        expect(screen.queryByTestId('filter-hide-status')).toBeNull()
        expect(screen.queryByTestId('filter-drag-status')).toBeNull()

        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        await userEvent.click(screen.getByTestId('filter-hide-status'))

        expect(screen.queryByTestId('filter-group-status')).toBeNull()

        await userEvent.click(screen.getByTestId('filter-show-status'))

        expect(screen.getByTestId('filter-group-status')).toBeTruthy()
    })

    it('keeps what was put away out of sight once the arranging is done', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        await userEvent.click(screen.getByTestId('filter-hide-status'))

        expect(screen.getByTestId('filter-hidden')).toBeTruthy()

        await userEvent.click(screen.getByTestId('projects-filter-arrange-done'))

        expect(screen.queryByTestId('filter-hidden')).toBeNull()
        expect(screen.queryByTestId('filter-group-status')).toBeNull()
    })

    it('offers a Local facet and matches local-only projects with it', async () => {
        const withLocal = [
            ...projects,
            {
                id: 'loc1',
                name: 'Draft',
                repository: 'local',
                status: ProjectStatus.Local,
                branch: '',
                modifiedBy: 'jane',
                modifiedAt: '',
                comment: '',
                revision: '',
            },
        ] satisfies Project[]
        mockProjectSearch(withLocal)
        await renderHome()

        expect(screen.getByTestId('project-row-loc1')).toBeTruthy()
        await userEvent.click(screen.getByTestId('filter-repo-__local__'))
        await screen.findByTestId('project-row-loc1')

        expect(screen.getByTestId('project-row-loc1')).toBeTruthy()
        expect(screen.queryByTestId('project-row-p1')).toBeNull()
    })

    it('offers a reset button that clears all filters when they hide every project', async () => {
        await renderHome()

        await userEvent.type(screen.getByTestId('projects-search'), 'nothing-matches-this')
        await flushSearch()
        expect(screen.getByTestId('projects-no-match')).toBeTruthy()

        await userEvent.click(screen.getByTestId('projects-clear-filters'))
        await screen.findByTestId('project-row-p1')

        expect(screen.getByTestId('project-row-p1')).toBeTruthy()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
    })

    it('shows the plain empty state, not a reset button, when the workspace has no projects at all', async () => {
        vi.mocked(getProjects).mockResolvedValue(projectsPage([]))
        await renderHome()

        // A lingering filter cannot reveal anything when there is nothing to reveal.
        await userEvent.type(screen.getByTestId('projects-search'), 'anything')
        await flushSearch()

        expect(screen.getByTestId('projects-empty')).toBeTruthy()
        expect(screen.queryByTestId('projects-clear-filters')).toBeNull()
    })

    it('opens the create wizard with the creatable repositories', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('projects-new'))

        expect(screen.getByTestId('new-project-modal').textContent).toBe('design')
    })

    it('opens the created project page after a successful create', async () => {
        mockProjectSearch([{ ...projects[0]!, id: 'created-id', name: 'Created', repository: 'design' }])
        await renderHome()

        await userEvent.click(screen.getByTestId('projects-new'))
        await userEvent.click(screen.getByTestId('new-project-fire-created'))

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/projects/created-id'))
    })

    it('opens the copy dialog with only creatable repositories', async () => {
        mockProjectSearch([{ ...projects[0]!, capabilities: { canCopy: true } }])
        await renderHome()

        await userEvent.click(screen.getByTestId('project-action-copy-p1'))

        expect(screen.getByTestId('copy-project-modal').textContent).toBe('design')
    })

    it('hides the New project button when no repository allows creating', async () => {
        vi.mocked(getDesignRepositories).mockResolvedValue([repositories[1]] as never)
        await renderHome()

        expect(screen.queryByTestId('projects-new')).toBeNull()
    })

    it('shows the empty state when there are no projects at all', async () => {
        vi.mocked(getProjects).mockResolvedValue(projectsPage([]))
        await renderHome()

        expect(screen.getByTestId('projects-empty')).toBeTruthy()
    })

    it('distinguishes an empty branch index that is still being built', async () => {
        vi.mocked(getProjects).mockResolvedValue({
            ...projectsPage([]),
            projectIndexHealth: {
                design: { state: 'indexing', failedBranches: []},
            },
        })
        await renderHome()

        expect(screen.getByTestId('projects-indexing')).toHaveTextContent('home.indexing')
        expect(screen.queryByTestId('projects-empty')).toBeNull()
    })

    it('notes in the summary line that a repository is still indexing, and names them on demand', async () => {
        vi.mocked(getProjects).mockResolvedValue({
            ...projectsPage(projects, projects.length, false),
            projectIndexHealth: {
                design: { state: 'indexing', failedBranches: []},
            },
        })
        await renderHome()

        // The note stays in the summary line; the names only appear when the user asks for them.
        expect(screen.getByTestId('projects-indexing-toggle')).toHaveTextContent('home.indexing_repositories')
        expect(screen.queryByTestId('projects-indexing-banner')).toBeNull()

        await userEvent.click(screen.getByTestId('projects-indexing-toggle'))
        expect(screen.getByTestId('projects-indexing-banner')).toHaveTextContent('home.indexing_banner')

        await userEvent.click(screen.getByTestId('projects-indexing-toggle'))
        expect(screen.queryByTestId('projects-indexing-banner')).toBeNull()
    })

    it('still reports that filters matched nothing while a repository is indexing', async () => {
        vi.mocked(getProjects).mockResolvedValue({
            ...projectsPage(projects, projects.length, false),
            projectIndexHealth: {
                design: { state: 'indexing', failedBranches: []},
            },
        })
        await renderHome()

        await userEvent.type(screen.getByPlaceholderText('home.search_placeholder'), 'nothing-matches-this')

        // The indexing state must not stand in for "your filter hid everything", or the screen reads as a hang.
        expect(await screen.findByTestId('projects-no-match')).toBeTruthy()
        expect(screen.getByTestId('projects-clear-filters')).toBeTruthy()
        expect(screen.queryByTestId('projects-indexing')).toBeNull()
    })

    it('shows no indexing note once every repository is ready', async () => {
        vi.mocked(getProjects).mockResolvedValue({
            ...projectsPage(projects, projects.length, false),
            projectIndexHealth: {
                design: { state: 'ready', failedBranches: []},
            },
        })
        await renderHome()

        expect(screen.queryByTestId('projects-indexing-toggle')).toBeNull()
        expect(screen.queryByTestId('projects-indexing-banner')).toBeNull()
    })

    it('shows an error state when loading fails', async () => {
        vi.mocked(getProjects).mockRejectedValue(new Error('boom'))
        await renderHome()

        expect(screen.getByTestId('projects-home-error')).toBeTruthy()
    })

    it('retries loading from the error state', async () => {
        vi.mocked(getProjects).mockRejectedValueOnce(new Error('boom'))
        await renderHome()

        expect(screen.getByTestId('projects-home-error')).toBeTruthy()

        await userEvent.click(screen.getByTestId('projects-home-retry'))

        await screen.findByTestId('project-row-p1')
        expect(screen.queryByTestId('projects-home-error')).toBeNull()
        expect(getProjects).toHaveBeenCalledTimes(2)
    })
})

/** Row actions run against one project, so each test lists exactly the one it drives. */
describe('ProjectsHome row actions', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        // The snapshot of the workspace is shared by the screens, so each test starts without one.
        invalidateProjectIndex()
        vi.mocked(getDesignRepositories).mockResolvedValue(repositories as never)
    })

    const single = (capabilities: NonNullable<Project['capabilities']>, over: Partial<Project> = {}) =>
        [{ ...projects[0]!, capabilities, ...over }] as Project[]

    it('opens a project and reloads the list', async () => {
        mockProjectSearch(single({ canOpen: true }))
        vi.mocked(setProjectStatus).mockResolvedValue(undefined as never)
        await renderHome()

        await userEvent.click(screen.getByTestId('project-action-open-p1'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'OPENED', { openDependencies: true }))
    })

    it('hides the loading overlay when a ping echoes the action the user is still waiting for', async () => {
        mockProjectSearch(single({ canOpen: true }))
        vi.mocked(setProjectStatus).mockResolvedValue(undefined as never)
        await renderHome()

        // The re-read the action starts is held, so the backend's echo of that same action lands while
        // the user is still waiting behind the overlay.
        let answer: (() => void) | undefined
        const held = new Promise<void>(resolve => { answer = resolve })
        vi.mocked(getProjects).mockImplementationOnce(async () => {
            await held
            return projectsPage(single({ canOpen: true }), 1, false)
        })

        await userEvent.click(screen.getByTestId('project-action-open-p1'))
        await waitFor(() => expect(screen.getByTestId('projects-loading-overlay')).toBeTruthy())

        await act(async () => {
            liveHandlers.workspaceChange?.()
            answer?.()
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.queryByTestId('projects-loading-overlay')).toBeNull())
    })

    it('says a status change failed instead of failing silently', async () => {
        mockProjectSearch(single({ canOpen: true }))
        vi.mocked(setProjectStatus).mockRejectedValue(new Error('boom'))
        await renderHome()

        await userEvent.click(screen.getByTestId('project-action-open-p1'))

        await waitFor(() => expect(notification.error).toHaveBeenCalled())
    })

    it('asks before closing a project with unsaved changes and closes discarding them', async () => {
        mockProjectSearch(single({ canClose: true }, { status: ProjectStatus.Editing }))
        vi.mocked(setProjectStatus).mockResolvedValue(undefined as never)
        await renderHome()

        await userEvent.click(screen.getByTestId('project-action-close-p1'))
        // Nothing is sent until the user accepts losing the changes.
        expect(setProjectStatus).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('discard-close-confirm'))

        await waitFor(() => expect(setProjectStatus).toHaveBeenCalledWith('p1', 'CLOSED', { discardChanges: true }))
    })

    it('hands branch deletion to the shared branch dialog', async () => {
        mockProjectSearch(single({ canDeleteBranch: true }))
        await renderHome()

        await userEvent.click(screen.getByTestId('project-action-deleteBranch-p1'))

        await waitFor(() => expect(openDeleteBranchDialog).toHaveBeenCalled())
    })

    it('hands branch sync to the shared merge dialog', async () => {
        mockProjectSearch(single({ canManageBranches: true }))
        await renderHome()

        await userEvent.click(screen.getByText('browser.sync'))

        await waitFor(() => expect(openMergeDialog).toHaveBeenCalled())
    })

    it('opens the comparison window for a comparable project', async () => {
        mockProjectSearch(single({ canCompare: true }))
        await renderHome()

        await userEvent.click(screen.getByText('browser.compare'))

        expect(openCompareWindow).toHaveBeenCalled()
    })

    it('opens the export, revision and save dialogs from the row menu', async () => {
        mockProjectSearch(single({ canExport: true, canViewHistory: true, canSave: true }))
        await renderHome()

        await userEvent.click(screen.getByText('browser.export'))
        expect(screen.getByTestId('export-modal')).toBeInTheDocument()

        await userEvent.click(screen.getByText('browser.open_revision'))
        expect(screen.getByTestId('open-revision-modal')).toBeInTheDocument()

        await userEvent.click(screen.getByText('browser.save'))
        expect(screen.getByTestId('save-project-modal')).toBeInTheDocument()
    })

    it('raises the global deploy and delete dialogs through window events', async () => {
        mockProjectSearch(single({ canDeploy: true, canDelete: true }))
        const deploy = vi.fn()
        const remove = vi.fn()
        window.addEventListener('openDeployModal', deploy)
        window.addEventListener('openDeleteProjectModal', remove)
        try {
            await renderHome()

            await userEvent.click(screen.getByText('browser.deploy'))
            expect(deploy).toHaveBeenCalled()

            await userEvent.click(screen.getByText('browser.delete'))
            expect(remove).toHaveBeenCalled()
            // The delete dialog reports back through the event, and the list reloads.
            const detail = (remove.mock.calls[0]![0] as CustomEvent<{ onSuccess: () => void }>).detail
            const before = vi.mocked(getProjects).mock.calls.length
            await act(async () => {
                detail.onSuccess()
                await new Promise(resolve => setTimeout(resolve, 0))
            })
            expect(vi.mocked(getProjects).mock.calls.length).toBeGreaterThan(before)
        } finally {
            window.removeEventListener('openDeployModal', deploy)
            window.removeEventListener('openDeleteProjectModal', remove)
        }
    })
})

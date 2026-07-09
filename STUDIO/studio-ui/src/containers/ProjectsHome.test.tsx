import { act, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectsHome } from './ProjectsHome'
import { getDesignRepositories, getProjects, type GetProjectsQuery } from '../services/repositories'
import type { Project, ProjectsPage } from '../types/projects'
import { ProjectStatus } from '../constants/project'

const { copyModalMock, navigateMock } = vi.hoisted(() => ({ copyModalMock: vi.fn(), navigateMock: vi.fn() }))

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

vi.mock('../services/repositories', () => ({
    getDesignRepositories: vi.fn(),
    getProjects: vi.fn(),
    downloadProject: vi.fn(),
    deleteProject: vi.fn(),
}))

vi.mock('./projects/NewProjectModal', () => ({
    NewProjectModal: ({ open, repositories }: { open: boolean, repositories?: { id: string }[] }) =>
        (open ? <div data-testid="new-project-modal">{repositories?.map(repo => repo.id).join(',')}</div> : null),
}))

vi.mock('./projects/CopyProjectModal', () => ({
    CopyProjectModal: (props: { open: boolean, repositories?: { id: string }[] }) => {
        copyModalMock(props)
        return props.open
            ? <div data-testid="copy-project-modal">{props.repositories?.map(repo => repo.id).join(',')}</div>
            : null
    },
}))

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

    const Segmented = ({ options, onChange, value, ...rest }: Record<string, unknown>) => {
        drop({ value })
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

    const Tag = ({ children, icon }: { children?: unknown, icon?: unknown }) => <span>{icon as never}{children as never}</span>
    const Tooltip = ({ children }: { children?: unknown }) => <>{children as never}</>
    const Skeleton = () => <div>skeleton</div>
    const Alert = ({ action, title, description, showIcon, type, ...rest }: Record<string, unknown>) => {
        drop({ showIcon, type })
        return <div {...rest}>{title as never}{description as never}{action as never}</div>
    }

    const Typography = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Typography.Text = ({ children, ...rest }: Record<string, unknown>) => {
        const { ellipsis, className, ...dom } = rest
        drop({ ellipsis, className })
        return <span {...dom}>{children as never}</span>
    }
    Typography.Title = ({ children }: { children?: unknown }) => <h3>{children as never}</h3>

    const Modal = Object.assign(() => null, { confirm: vi.fn() })
    const notification = { error: vi.fn(), success: vi.fn() }

    return { Button, Input, Select, Segmented, Dropdown, Checkbox, Empty, Tag, Tooltip, Skeleton, Alert, Typography, Modal, notification }
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
}

async function flushSearch() {
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 350))
    })
}

function projectsPage(content: Project[], total = content.length): ProjectsPage {
    return {
        content,
        numberOfElements: content.length,
        pageNumber: 0,
        pageSize: 20,
        total,
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

function mockProjectSearch(source: Project[] = projects) {
    vi.mocked(getProjects).mockImplementation(async (query: GetProjectsQuery = {}) => {
        let result = [...source]
        const name = query.name?.toLowerCase()
        if (name) {
            result = result.filter(project => project.name.toLowerCase().includes(name))
        }
        const statuses = new Set([...(query.statuses ?? [])].map(String))
        if (statuses.size > 0) {
            result = result.filter(project => statuses.has(project.status))
        }
        const repositories = new Set([...(query.repositories ?? [])])
        if (repositories.size > 0) {
            result = result.filter(project => repositories.has(project.repository)
                || repositories.has('__local__') && project.status === ProjectStatus.Local)
        }
        if (query.sort === 'updated') {
            result.sort((a, b) => new Date(b.modifiedAt ?? 0).getTime() - new Date(a.modifiedAt ?? 0).getTime())
        } else {
            result.sort((a, b) => a.name.localeCompare(b.name))
        }
        return projectsPage(result)
    })
}

describe('ProjectsHome', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getDesignRepositories).mockResolvedValue(repositories as never)
        mockProjectSearch()
    })

    it('lists every project as one flat row with its repository as a facet', async () => {
        await renderHome()

        expect(screen.getByTestId('project-row-p1')).toBeTruthy()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
        // Repository names show up both in the filter rail and as row metadata.
        expect(screen.getAllByText('Design').length).toBeGreaterThan(1)
        expect(screen.getAllByText('ReadOnly').length).toBeGreaterThan(1)
        // The tag value shows up both as a row tag and as a tag facet in the rail.
        expect(screen.getAllByText('Payroll').length).toBeGreaterThan(1)
        expect(screen.queryByText('Design/rules/Alpha')).toBeNull()

        await userEvent.click(screen.getByText('grid'))

        expect(screen.getByTestId('project-card-p1')).toBeTruthy()
        expect(screen.queryByText('Design/rules/Alpha')).toBeNull()
    })

    it('keeps initial load failures local to the Projects page', async () => {
        await renderHome()

        expect(getDesignRepositories).toHaveBeenCalledWith({ throwError: true, suppressErrorPages: true })
        expect(getProjects).toHaveBeenCalledWith(expect.any(Object), { throwError: true, suppressErrorPages: true })
    })

    it('sorts alphabetically by default, by most recent update on demand', async () => {
        await renderHome()

        expect(rowOrder()).toEqual(['project-row-p1', 'project-row-p2'])

        await userEvent.selectOptions(screen.getByTestId('projects-sort'), 'updated')

        await screen.findByTestId('project-row-p2')
        expect(rowOrder()).toEqual(['project-row-p2', 'project-row-p1'])
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

    it('filters rows by status from the rail', async () => {
        const mixed: Project[] = [{ ...projects[0]! }, { ...projects[1]!, status: ProjectStatus.Opened }]
        mockProjectSearch(mixed)
        await renderHome()

        await userEvent.click(screen.getByTestId('filter-status-OPENED'))
        await screen.findByTestId('project-row-p2')

        expect(screen.queryByTestId('project-row-p1')).toBeNull()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
    })

    it('groups tag filters under a collapsible Tags section', async () => {
        await renderHome()

        const tagGroup = within(screen.getByTestId('filter-tags'))
        expect(tagGroup.getByText('home.facet_tags')).toBeTruthy()
        expect(tagGroup.getByText('Category')).toBeTruthy()
        expect(tagGroup.getByTestId('filter-tag-Category:Payroll')).toBeTruthy()

        await userEvent.click(screen.getByTestId('filter-tags-toggle'))

        expect(screen.getByTestId('filter-tags-toggle').getAttribute('aria-expanded')).toBe('false')
        expect(screen.queryByTestId('filter-tag-Category:Payroll')).toBeNull()
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

    it('clears all filters from the no-match state', async () => {
        await renderHome()

        await userEvent.type(screen.getByTestId('projects-search'), 'nothing-matches-this')
        await flushSearch()
        expect(screen.getByTestId('projects-no-match')).toBeTruthy()

        await userEvent.click(screen.getByText('home.clear_filters'))
        await screen.findByTestId('project-row-p1')

        expect(screen.getByTestId('project-row-p1')).toBeTruthy()
        expect(screen.getByTestId('project-row-p2')).toBeTruthy()
    })

    it('opens the create wizard with the creatable repositories', async () => {
        await renderHome()

        await userEvent.click(screen.getByTestId('projects-new'))

        expect(screen.getByTestId('new-project-modal').textContent).toBe('design')
    })

    it('opens the copy dialog with only creatable repositories', async () => {
        mockProjectSearch([{ ...projects[0]!, capabilities: { canCopy: true } }])
        await renderHome()

        await userEvent.click(screen.getByTestId('project-actions-p1'))
        await userEvent.click(screen.getByText('browser.copy'))

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

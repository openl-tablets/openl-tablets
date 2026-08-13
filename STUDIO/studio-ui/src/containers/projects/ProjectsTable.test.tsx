import type { ComponentProps } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ProjectsTable } from './ProjectsTable'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('antd', () => ({
    Tooltip: ({ children }: Record<string, unknown>) => <>{children as never}</>,
    Typography: { Text: ({ children }: Record<string, unknown>) => <span>{children as never}</span> },
}))

vi.mock('@ant-design/icons', () => ({
    CaretDownFilled: () => <i data-testid="arrow-desc" />,
    CaretUpFilled: () => <i data-testid="arrow-asc" />,
    LockOutlined: () => null,
}))
vi.mock('./CompileIndicator', () => ({ RowCompileDot: () => null }))
vi.mock('./StatusIndicator', () => ({ StatusMark: () => null }))
vi.mock('./ProjectRowActions', () => ({ ProjectRowActions: () => null }))
vi.mock('./projectRow', () => ({
    activateOnKey: () => () => {},
    // The shared branch cell decides for itself whether a project shows a branch; that rule is tested in
    // projectRow.test.tsx, against the real switcher. Here it only has to say which props it was handed.
    ProjectBranchSwitch: (props: Record<string, unknown>) => {
        const project = props['project'] as Project
        return props['supportsBranches'] && !!project.branch && project.status !== ProjectStatus.Local
            ? <span data-busy={String(props['busy'])} data-testid={`row-branch-${project.id}`}>{project.branch}</span>
            : null
    },
    deriveProjectRow: (project: Project) => ({
        muted: false,
        supportsBranches: project.repository !== 'flat',
        lockLabel: null,
        tags: [],
        date: 'Jan 2, 2024',
    }),
    ProjectTags: () => null,
    hasBranch: (project: Project) => project.repository !== 'flat'
        && !!project.branch
        && project.status !== ProjectStatus.Local,
}))

const project = (overrides: Partial<Project> = {}): Project => ({
    branch: 'main',
    comment: '',
    id: 'p1',
    modifiedAt: '2024-01-02T00:00:00Z',
    modifiedBy: 'jane',
    name: 'Alpha',
    repository: 'design',
    revision: 'rev1',
    status: ProjectStatus.Opened,
    ...overrides,
})

const renderTable = (projects: Project[], sorting: Partial<ComponentProps<typeof ProjectsTable>> = {}) => {
    const onSort = vi.fn()
    render(
        <ProjectsTable
            compileStatusByProject={new Map()}
            direction="asc"
            handlers={{} as never}
            onChanged={vi.fn()}
            onOpen={vi.fn()}
            onSort={onSort}
            pending={{}}
            projects={projects}
            repoInfoOf={() => ({ name: 'Design', type: 'repo-git' } as RepositoryInfo)}
            sort={null}
            {...sorting}
        />
    )
    return onSort
}

describe('ProjectsTable', () => {
    it('offers the branch of every row for switching', () => {
        renderTable([project()])

        expect(screen.getByTestId('row-branch-p1')).toHaveTextContent('main')
    })

    it('gates the branch of a row that is busy with an action', () => {
        renderTable([project()], { pending: { p1: 'open' } })

        expect(screen.getByTestId('row-branch-p1')).toHaveAttribute('data-busy', 'true')
    })

    it('shows no switcher where a project has no branch', () => {
        renderTable([project(), project({ id: 'p2', repository: 'flat', branch: '' })])

        expect(screen.queryByTestId('row-branch-p2')).toBeNull()
        expect(screen.getByTestId('row-branch-p1')).toBeInTheDocument()
    })

    it('shows no switcher for a project that lives only in the workspace', () => {
        renderTable([project(), project({ id: 'p3', status: ProjectStatus.Local })])

        expect(screen.queryByTestId('row-branch-p3')).toBeNull()
    })

    it('drops the branch column when no project on the page has a branch', () => {
        renderTable([project({ id: 'p3', status: ProjectStatus.Local })])

        expect(screen.queryByText('home.col_branch')).toBeNull()
    })

    it('sorts by a clicked header, showing no arrow until then', () => {
        const onSort = renderTable([project()])

        expect(screen.queryByTestId('arrow-asc')).toBeNull()
        expect(screen.queryByTestId('arrow-desc')).toBeNull()

        fireEvent.click(screen.getByTestId('projects-sort-updated'))
        expect(onSort).toHaveBeenCalledWith('updated')
    })

    it('marks the sorted column with the arrow of its direction', () => {
        renderTable([project()], { sort: 'name', direction: 'desc' })

        const header = screen.getByTestId('projects-sort-name')
        expect(header).toContainElement(screen.getByTestId('arrow-desc'))
        expect(screen.queryByTestId('arrow-asc')).toBeNull()
    })
})

import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import { ProjectsGrid } from './ProjectsGrid'

const project: Project = {
    branch: 'main',
    comment: '',
    id: 'p1',
    modifiedAt: '2024-01-02T00:00:00Z',
    modifiedBy: 'jane',
    name: 'Alpha',
    repository: 'design',
    revision: 'rev1',
    status: ProjectStatus.Closed,
    tags: { Team: 'Payroll' },
}

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('antd', () => {
    const Typography = { Text: ({ children }: Record<string, unknown>) => <span>{children as never}</span> }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    return { Tooltip, Typography }
})

vi.mock('@ant-design/icons', () => ({
    LockOutlined: () => null,
    RightOutlined: () => null,
}))

vi.mock('./StatusIndicator', () => ({
    StatusMark: ({ status }: { status: string }) => <span data-testid="status-mark">{status}</span>,
}))
vi.mock('./RepoBadge', () => ({ RepoBadge: () => <span data-testid="repo-badge" /> }))
vi.mock('./CompileIndicator', () => ({ RowCompileDot: () => null }))
vi.mock('./ProjectRowActions', () => ({ ProjectRowActions: () => null }))

const { branchSwitcherMock } = vi.hoisted(() => ({ branchSwitcherMock: vi.fn() }))
vi.mock('./projectRow', () => ({
    activateOnKey: (action: () => void) => (event: { key: string, preventDefault: () => void }) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            action()
        }
    },
    // The branch cell is shared with the table view and tested there, against the real switcher.
    ProjectBranchSwitch: (props: Record<string, unknown>) => {
        branchSwitcherMock(props)
        return <div data-testid={`${props['testIdPrefix'] as string}-branch-p1`} />
    },
    deriveProjectRow: () => ({
        muted: false,
        repoLabel: 'Design',
        repoType: 'git',
        supportsBranches: true,
        lockLabel: null,
        tags: { Team: 'Payroll' },
        date: 'Jan 2',
    }),
    ProjectTags: () => <span data-testid="project-tags" />,
}))

describe('ProjectsGrid', () => {
    it('opens a project from click and keyboard activation', async () => {
        const onOpen = vi.fn()
        const handlers = {
            onOpen: vi.fn(), onClose: vi.fn(), onSave: vi.fn(),
            onCopy: vi.fn(), onDelete: vi.fn(), onDeleteBranch: vi.fn(), onDeploy: vi.fn(),
            onExport: vi.fn(), onOpenRevision: vi.fn(), onSync: vi.fn(), onCompare: vi.fn(),
        }

        render(
            <ProjectsGrid
                compileStatusByProject={new Map()}
                handlers={handlers}
                onChanged={vi.fn()}
                onOpen={onOpen}
                pending={{}}
                projects={[project]}
                repoInfoOf={() => ({ id: 'design', name: 'Design', type: 'git' })}
            />
        )

        await userEvent.click(screen.getByTestId('project-card-p1'))
        expect(onOpen).toHaveBeenCalledWith(project)

        fireEvent.keyDown(screen.getByTestId('project-card-p1'), { key: 'Enter' })
        expect(onOpen).toHaveBeenCalledTimes(2)
    })

    it('offers the branch switcher on a card without opening the project', async () => {
        const onOpen = vi.fn()
        const handlers = {
            onOpen: vi.fn(), onClose: vi.fn(), onSave: vi.fn(),
            onCopy: vi.fn(), onDelete: vi.fn(), onDeleteBranch: vi.fn(), onDeploy: vi.fn(),
            onExport: vi.fn(), onOpenRevision: vi.fn(), onSync: vi.fn(), onCompare: vi.fn(),
        }

        render(
            <ProjectsGrid
                compileStatusByProject={new Map()}
                handlers={handlers}
                onChanged={vi.fn()}
                onOpen={onOpen}
                pending={{}}
                projects={[project]}
                repoInfoOf={() => ({ id: 'design', name: 'Design', type: 'git' })}
            />
        )

        const branch = screen.getByTestId('card-branch-p1')
        expect(branch).toBeInTheDocument()
        // Interacting with the branch is a card action of its own: it must not open the project.
        await userEvent.click(branch)
        expect(onOpen).not.toHaveBeenCalled()
    })

    it('gates the card branch switch while the card is busy', () => {
        render(
            <ProjectsGrid
                compileStatusByProject={new Map()}
                handlers={{} as never}
                onChanged={vi.fn()}
                onOpen={vi.fn()}
                pending={{ p1: 'open' }}
                projects={[project]}
                repoInfoOf={() => ({ id: 'design', name: 'Design', type: 'git' })}
            />
        )

        expect(branchSwitcherMock).toHaveBeenLastCalledWith(expect.objectContaining({ busy: true, testIdPrefix: 'card' }))
    })
})

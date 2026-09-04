import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import { ProjectRowActions, type ProjectListHandlers } from './ProjectRowActions'

const project: Project = {
    branch: 'main',
    comment: '',
    id: 'p1',
    modifiedAt: '2024-01-02T00:00:00Z',
    modifiedBy: 'jane',
    name: 'Alpha',
    repository: 'design',
    revision: 'rev1',
    status: ProjectStatus.Opened,
    capabilities: {
        canClose: true,
        canSave: true,
        canCopy: true,
        canDeploy: true,
        canExport: true,
        canDelete: true,
    },
}

const makeHandlers = (): ProjectListHandlers => ({
    onOpen: vi.fn(),
    onClose: vi.fn(),
    onSave: vi.fn(),
    onCopy: vi.fn(),
    onDeleteBranch: vi.fn(),
    onOpenRevision: vi.fn(),
    onSync: vi.fn(),
    onDeploy: vi.fn(),
    onCompare: vi.fn(),
    onExport: vi.fn(),
    onDelete: vi.fn(),
})

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('@ant-design/icons', () => ({
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DiffOutlined: () => null,
    DownloadOutlined: () => null,
    FolderOpenOutlined: () => null,
    FolderOutlined: () => null,
    HistoryOutlined: () => null,
    MergeOutlined: () => null,
    LoadingOutlined: () => <i data-testid="menu-running" />,
    MoreOutlined: () => null,
    RocketOutlined: () => null,
    SaveOutlined: () => null,
}))

vi.mock('antd', () => {
    interface Menu {
        items?: Array<{ key?: string, label?: unknown, icon?: unknown, disabled?: boolean }>
        onClick?: (info: { key: string, domEvent: { stopPropagation: () => void } }) => void
    }
    return {
        Tooltip: ({ children }: { children: React.ReactNode }) => children,
        Dropdown: ({ children, menu }: Record<string, unknown>) => (
            <div>
                {children as never}
                {(menu as Menu).items?.filter(item => item.key).map(item => (
                    <button
                        key={item.key}
                        data-testid={`project-menu-${item.key}-p1`}
                        disabled={item.disabled ?? false}
                        onClick={() => (menu as Menu).onClick?.({ key: item.key!, domEvent: { stopPropagation: () => {} } })}
                    >
                        {item.icon as never}{item.label as never}
                    </button>
                ))}
            </div>
        ),
        Button: ({ children, onClick, icon, loading, danger, type, size, ...rest }: Record<string, unknown>) => (
            <button onClick={onClick as never} {...rest}>{(children ?? icon) as never}</button>
        ),
    }
})

describe('ProjectRowActions', () => {
    const everything: Project = {
        ...project,
        capabilities: {
            canClose: true,
            canSave: true,
            canCopy: true,
            canManageBranches: true,
            canDeleteBranch: true,
            canViewHistory: true,
            canDeploy: true,
            canCompare: true,
            canExport: true,
            canDelete: true,
        },
    }

    it('renders nothing when no capabilities are granted', () => {
        const { container } = render(
            <ProjectRowActions handlers={makeHandlers()} pendingActionId={null} project={{ ...project, capabilities: {} }} />
        )

        expect(container.textContent).toBe('')
    })

    it('keeps the everyday actions as buttons and hides the rest in the menu', () => {
        render(<ProjectRowActions handlers={makeHandlers()} pendingActionId={null} project={everything} />)

        expect(screen.getByTestId('project-action-copy-p1')).toBeTruthy()
        expect(screen.getByTestId('project-action-deleteBranch-p1')).toBeTruthy()
        expect(screen.getByTestId('project-action-close-p1')).toBeTruthy()
        // Everything else is one click deeper.
        expect(screen.queryByTestId('project-action-deploy-p1')).toBeNull()
        expect(screen.getByTestId('project-menu-deploy-p1')).toBeTruthy()
        expect(screen.getByTestId('project-menu-openRevision-p1')).toBeTruthy()
        expect(screen.getByTestId('project-menu-delete-p1')).toBeTruthy()
    })

    it('offers only what the capabilities allow', () => {
        render(
            <ProjectRowActions
                handlers={makeHandlers()}
                pendingActionId={null}
                project={{ ...project, status: ProjectStatus.Closed, capabilities: { canOpen: true, canDelete: true } }}
            />
        )

        expect(screen.getByTestId('project-action-open-p1')).toBeTruthy()
        expect(screen.queryByTestId('project-action-close-p1')).toBeNull()
        expect(screen.queryByTestId('project-action-copy-p1')).toBeNull()
        expect(screen.getByTestId('project-menu-delete-p1')).toBeTruthy()
    })


    it('invokes the matching handler from a button and from the menu', async () => {
        const handlers = makeHandlers()
        render(<ProjectRowActions handlers={handlers} pendingActionId={null} project={everything} />)

        await userEvent.click(screen.getByTestId('project-action-copy-p1'))
        await userEvent.click(screen.getByTestId('project-action-close-p1'))
        await userEvent.click(screen.getByTestId('project-menu-sync-p1'))
        await userEvent.click(screen.getByTestId('project-menu-export-p1'))
        await userEvent.click(screen.getByTestId('project-menu-delete-p1'))

        expect(handlers.onCopy).toHaveBeenCalledWith(everything)
        expect(handlers.onClose).toHaveBeenCalledWith(everything)
        expect(handlers.onSync).toHaveBeenCalledWith(everything)
        expect(handlers.onExport).toHaveBeenCalledWith(everything)
        expect(handlers.onDelete).toHaveBeenCalledWith(everything)
    })

    it('folds every action into the menu on a card', () => {
        render(<ProjectRowActions handlers={makeHandlers()} layout="menu" pendingActionId={null} project={everything} />)

        expect(screen.queryByTestId('project-action-copy-p1')).toBeNull()
        expect(screen.getByTestId('project-menu-copy-p1')).toBeTruthy()
        expect(screen.getByTestId('project-menu-close-p1')).toBeTruthy()
        expect(screen.getByTestId('project-menu-deploy-p1')).toBeTruthy()
    })

    it('disables the other buttons while an action is pending', () => {
        render(<ProjectRowActions handlers={makeHandlers()} pendingActionId="close" project={everything} />)

        expect(screen.getByTestId('project-action-copy-p1')).toBeDisabled()
    })

    it('names the running action in the card menu, where one trigger stands for them all', () => {
        render(<ProjectRowActions handlers={makeHandlers()} layout="menu" pendingActionId="close" project={everything} />)

        // The card's single three-dots spinner cannot say which action is running; the menu can.
        expect(within(screen.getByTestId('project-menu-close-p1')).getByTestId('menu-running')).toBeTruthy()
        expect(screen.getByTestId('project-menu-copy-p1')).toBeDisabled()
    })

    it('offers nothing on a row whose project is busy switching branch', () => {
        render(<ProjectRowActions handlers={makeHandlers()} pendingActionId="switchBranch" project={everything} />)

        // No button of its own spins for a branch switch, but the row is just as occupied.
        expect(screen.getByTestId('project-action-copy-p1')).toBeDisabled()
        expect(screen.getByTestId('project-action-close-p1')).toBeDisabled()
    })
})

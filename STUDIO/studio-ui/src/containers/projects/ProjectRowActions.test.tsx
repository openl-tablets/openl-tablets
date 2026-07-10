import { render, screen } from '@testing-library/react'
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
    onDeploy: vi.fn(),
    onExport: vi.fn(),
    onDelete: vi.fn(),
})

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('@ant-design/icons', () => ({
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DownloadOutlined: () => null,
    FolderOpenOutlined: () => null,
    FolderOutlined: () => null,
    MoreOutlined: () => null,
    RocketOutlined: () => null,
    SaveOutlined: () => null,
}))

vi.mock('antd', () => ({
    Tooltip: ({ children }: { children: React.ReactNode }) => children,
    Dropdown: ({ children }: { children: React.ReactNode }) => children,
    Button: ({ children, onClick, icon, loading, danger, type, size, ...rest }: Record<string, unknown>) => (
        <button onClick={onClick as never} {...rest}>{(children ?? icon) as never}</button>
    ),
}))

describe('ProjectRowActions', () => {
    it('renders nothing when no capabilities are granted', () => {
        const { container } = render(
            <ProjectRowActions handlers={makeHandlers()} pendingActionId={null} project={{ ...project, capabilities: {} }} />
        )

        expect(container.textContent).toBe('')
    })

    it('shows only the pictograms allowed by capabilities', () => {
        render(
            <ProjectRowActions
                handlers={makeHandlers()}
                pendingActionId={null}
                project={{ ...project, status: ProjectStatus.Closed, capabilities: { canOpen: true, canDelete: true } }}
            />
        )

        expect(screen.getByTestId('project-action-open-p1')).toBeTruthy()
        expect(screen.getByTestId('project-action-delete-p1')).toBeTruthy()
        expect(screen.queryByTestId('project-action-close-p1')).toBeNull()
        expect(screen.queryByTestId('project-action-save-p1')).toBeNull()
    })

    it('invokes the matching handler for each pictogram', async () => {
        const handlers = makeHandlers()
        render(<ProjectRowActions handlers={handlers} pendingActionId={null} project={project} />)

        await userEvent.click(screen.getByTestId('project-action-close-p1'))
        await userEvent.click(screen.getByTestId('project-action-save-p1'))
        await userEvent.click(screen.getByTestId('project-action-copy-p1'))
        await userEvent.click(screen.getByTestId('project-action-deploy-p1'))
        await userEvent.click(screen.getByTestId('project-action-export-p1'))
        await userEvent.click(screen.getByTestId('project-action-delete-p1'))

        expect(handlers.onClose).toHaveBeenCalledWith(project)
        expect(handlers.onSave).toHaveBeenCalledWith(project)
        expect(handlers.onCopy).toHaveBeenCalledWith(project)
        expect(handlers.onDeploy).toHaveBeenCalledWith(project)
        expect(handlers.onExport).toHaveBeenCalledWith(project)
        expect(handlers.onDelete).toHaveBeenCalledWith(project)
    })

    it('disables the other pictograms while an action is pending', () => {
        render(<ProjectRowActions handlers={makeHandlers()} pendingActionId="close" project={project} />)

        expect(screen.getByTestId('project-action-copy-p1')).toBeDisabled()
        expect(screen.getByTestId('project-action-close-p1')).not.toBeDisabled()
    })
})

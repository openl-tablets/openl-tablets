import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import { ProjectRowActions } from './ProjectRowActions'

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
    capabilities: {
        canOpen: true,
        canExport: true,
        canCopy: true,
        canDeploy: true,
        canDelete: true,
    },
}

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('@ant-design/icons', () => ({
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DownloadOutlined: () => null,
    FolderOpenOutlined: () => null,
    MoreOutlined: () => null,
    RocketOutlined: () => null,
}))

vi.mock('antd', () => {
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => (
        <button onClick={onClick as never} {...rest}>{children as never}</button>
    )
    const Dropdown = ({ children, menu }: Record<string, unknown>) => (
        <div>
            {children as never}
            <div data-testid="dropdown-menu">
                {(menu as { items: { key: string, label: string }[], onClick: (info: { key: string, domEvent: { stopPropagation: () => void } }) => void }).items
                    .filter(item => item.key)
                    .map(item => (
                        <button
                            key={item.key}
                            data-testid={`menu-${item.key}`}
                            type="button"
                            onClick={() => (menu as { onClick: (info: { key: string, domEvent: { stopPropagation: () => void } }) => void }).onClick({
                                key: item.key,
                                domEvent: { stopPropagation: vi.fn() },
                            })}
                        >
                            {item.label}
                        </button>
                    ))}
            </div>
        </div>
    )
    return { Button, Dropdown }
})

describe('ProjectRowActions', () => {
    it('renders nothing when no capabilities are granted', () => {
        const { container } = render(
            <ProjectRowActions
                handlers={{ onCopy: vi.fn(), onDelete: vi.fn(), onDeploy: vi.fn(), onExport: vi.fn(), onOpen: vi.fn() }}
                project={{ ...project, capabilities: {} }}
            />
        )

        expect(container.textContent).toBe('')
    })

    it('invokes the matching handler for each menu item', async () => {
        const handlers = {
            onOpen: vi.fn(),
            onCopy: vi.fn(),
            onDeploy: vi.fn(),
            onExport: vi.fn(),
            onDelete: vi.fn(),
        }

        render(<ProjectRowActions handlers={handlers} project={project} />)

        await userEvent.click(screen.getByTestId('menu-open'))
        await userEvent.click(screen.getByTestId('menu-export'))
        await userEvent.click(screen.getByTestId('menu-copy'))
        await userEvent.click(screen.getByTestId('menu-deploy'))
        await userEvent.click(screen.getByTestId('menu-delete'))

        expect(handlers.onOpen).toHaveBeenCalledWith(project)
        expect(handlers.onExport).toHaveBeenCalledWith(project)
        expect(handlers.onCopy).toHaveBeenCalledWith(project)
        expect(handlers.onDeploy).toHaveBeenCalledWith(project)
        expect(handlers.onDelete).toHaveBeenCalledWith(project)
    })
})

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import { ProjectActionBar, type ProjectActionHandlers } from './ProjectActionBar'

vi.mock('react-i18next', () => {
    // One t for every render: a fresh function each call would destabilize the memoized action set.
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }), cx: (...a: unknown[]) => a.join(' ') }),
}))

vi.mock('../../components/SplitButton', () => ({
    SplitButton: ({ children, ...rest }: Record<string, unknown>) =>
        <button data-testid={rest['data-testid'] as string}>{children as never}</button>,
}))

// The Add menu opens its entries in a dropdown; here they are plain buttons beside it.
vi.mock('../../components/MenuButton', () => {
    interface Menu { items?: Array<{ key?: string }>, onClick?: (info: { key: string }) => void }
    return {
        MenuButton: ({ children, menu, ...rest }: Record<string, unknown>) => (
            <span>
                <button data-testid={rest['data-testid'] as string}>{children as never}</button>
                {(menu as Menu).items?.filter(item => item.key).map(item => (
                    <button
                        key={item.key}
                        data-testid={`add-${item.key}`}
                        onClick={() => (menu as Menu).onClick?.({ key: item.key! })}
                    />
                ))}
            </span>
        ),
    }
})

vi.mock('antd', () => {
    const Button = ({ children, type, ...rest }: Record<string, unknown>) => (
        <button
            data-primary={type === 'primary' ? 'true' : undefined}
            data-testid={rest['data-testid'] as string}
            onClick={rest['onClick'] as never}
        >
            {children as never}
        </button>
    )
    // Says whether it stands open; a click on its trigger opens it.
    const Dropdown = ({ children, open, onOpenChange, popupRender }: Record<string, unknown>) => (
        <div data-open={String(open)} data-testid="dropdown">
            <span onClick={() => (onOpenChange as (open: boolean) => void)(true)} role="presentation">
                {children as never}
            </span>
            {popupRender ? ((popupRender as (n: unknown) => unknown)(null) as never) : null}
        </div>
    )
    const Popconfirm = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Space = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    Space.Compact = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    return { Button, Dropdown, Popconfirm, Space }
})

const project = (capabilities: Project['capabilities'], status = ProjectStatus.Opened): Project => ({
    id: 'p1',
    name: 'Alpha',
    status,
    capabilities,
} as Project)

const noHandlers = {} as ProjectActionHandlers

/** Force the bar and its measured buttons to a fixed size so the fit maths runs under jsdom. */
const mockSizes = (parentWidth: number, itemWidth: number) => {
    vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(parentWidth)
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(itemWidth)
}

afterEach(() => vi.restoreAllMocks())

describe('ProjectActionBar', () => {
    it('adds files at the root of a project the reader may write to, after Close and Export', () => {
        const onAddFiles = vi.fn()
        render(<ProjectActionBar
            handlers={noHandlers}
            onAddFiles={onAddFiles}
            pendingId={null}
            project={project({ canClose: true, canWrite: true, canExport: true })}
        />)

        const bar = screen.getByTestId('project-actions')
        const order = [...bar.querySelectorAll(':scope > * [data-testid], :scope > [data-testid]')]
            .map(node => node.getAttribute('data-testid'))
        expect(order.indexOf('addFiles-p1')).toBeGreaterThan(order.indexOf('close-p1'))
        expect(order.indexOf('addFiles-p1')).toBeGreaterThan(order.indexOf('export-p1'))

        screen.getByTestId('add-upload').click()
        expect(onAddFiles).toHaveBeenCalledWith('upload')
    })

    it('lines the actions of an open project up as Close, Copy, Sync, Deploy, Export, Add, Compare, then the rest', () => {
        render(<ProjectActionBar
            handlers={noHandlers}
            onAddFiles={vi.fn()}
            pendingId={null}
            project={project({
                canClose: true,
                canWrite: true,
                canCopy: true,
                canMerge: true,
                canManageBranches: true,
                canDeploy: true,
                canExport: true,
                canCompare: true,
                canViewHistory: true,
                canDelete: true,
            })}
        />)

        const bar = screen.getByTestId('project-actions')
        const order = [...bar.querySelectorAll(':scope > * [data-testid], :scope > [data-testid]')]
            .map(node => node.getAttribute('data-testid'))
            .filter(id => id?.endsWith('-p1'))
        const shown = ['close', 'copy', 'sync', 'deploy', 'export', 'addFiles', 'compare', 'openRevision', 'delete']
            .map(id => `${id}-p1`)
            .filter(id => order.includes(id))
        expect(shown.slice(0, 7)).toEqual(['close-p1', 'copy-p1', 'sync-p1', 'deploy-p1', 'export-p1', 'addFiles-p1', 'compare-p1'])
        expect(shown.map(id => order.indexOf(id))).toEqual([...shown.map(id => order.indexOf(id))].sort((a, b) => a - b))
    })

    it('offers no Add menu to a reader who may not write, nor without a way to add', () => {
        const { rerender } = render(<ProjectActionBar
            handlers={noHandlers}
            onAddFiles={vi.fn()}
            pendingId={null}
            project={project({ canClose: true })}
        />)
        expect(screen.queryByTestId('addFiles-p1')).toBeNull()

        rerender(<ProjectActionBar
            handlers={noHandlers}
            pendingId={null}
            project={project({ canClose: true, canWrite: true })}
        />)
        expect(screen.queryByTestId('addFiles-p1')).toBeNull()
    })

    it('shows every available action when the width is unknown (jsdom)', () => {
        render(<ProjectActionBar
            handlers={noHandlers}
            pendingId={null}
            project={project({
                canClose: true,
                canSync: true,
                canCopy: true,
                canExport: true,
            } as Project['capabilities'])}
        />)

        expect(screen.getByTestId('close-p1')).toBeInTheDocument()
        expect(screen.getByTestId('export-p1')).toBeInTheDocument()
        expect(screen.queryByTestId('project-actions-more')).toBeNull()
    })

    it('marks the first ladder action as primary', () => {
        render(<ProjectActionBar
            handlers={noHandlers}
            pendingId={null}
            project={project({
                canClose: true,
                canExport: true,
            })}
        />)

        expect(screen.getByTestId('close-p1')).toHaveAttribute('data-primary', 'true')
        expect(screen.getByTestId('export-p1')).not.toHaveAttribute('data-primary')
    })

    it('collapses the actions that do not fit behind a three-dots menu, keeping the primary visible', () => {
        mockSizes(150, 100)
        render(<ProjectActionBar
            handlers={noHandlers}
            pendingId={null}
            project={project({
                canClose: true,
                canSync: true,
                canCopy: true,
                canExport: true,
            } as Project['capabilities'])}
        />)

        // Only the primary stays in the bar; the rest move into the overflow menu.
        expect(screen.getByTestId('close-p1')).toHaveAttribute('data-primary', 'true')
        expect(screen.getByTestId('project-actions-more')).toBeInTheDocument()
        const overflow = screen.getByTestId('project-actions-overflow')
        expect(overflow).toContainElement(screen.getByTestId('export-p1'))
    })

    it('closes the menu once an action in it runs', async () => {
        mockSizes(150, 100)
        const exported = vi.fn()
        render(<ProjectActionBar
            handlers={{ ...noHandlers, export: exported }}
            pendingId={null}
            project={project({ canClose: true, canExport: true })}
        />)

        await userEvent.click(screen.getByTestId('project-actions-more'))
        expect(screen.getByTestId('dropdown')).toHaveAttribute('data-open', 'true')
        await userEvent.click(screen.getByTestId('export-p1'))

        expect(exported).toHaveBeenCalledTimes(1)
        expect(screen.getByTestId('dropdown')).toHaveAttribute('data-open', 'false')
    })

    it('renders nothing when no action is available', () => {
        const { container } = render(
            <ProjectActionBar handlers={noHandlers} pendingId={null} project={project({})} />
        )
        expect(container).toBeEmptyDOMElement()
    })
})

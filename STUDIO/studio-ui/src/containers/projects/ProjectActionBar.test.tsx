import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import { fitActionCount, ProjectActionBar, type ProjectActionHandlers } from './ProjectActionBar'

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

vi.mock('antd', () => {
    const Button = ({ children, type, ...rest }: Record<string, unknown>) => (
        <button
            data-primary={type === 'primary' ? 'true' : undefined}
            data-testid={rest['data-testid'] as string}
        >
            {children as never}
        </button>
    )
    const Dropdown = ({ children, popupRender }: Record<string, unknown>) => (
        <div>{children as never}{popupRender ? ((popupRender as (n: unknown) => unknown)(null) as never) : null}</div>
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

describe('fitActionCount', () => {
    it('shows every action when they all fit', () => {
        expect(fitActionCount([50, 50, 50], 40, 500, 8)).toBe(3)
    })

    it('reserves room for the overflow trigger once something spills', () => {
        // 50 + 8 + 8 + 40 = 106 <= 110 fits one; a second (50 + 8) would need 164.
        expect(fitActionCount([50, 50, 50], 40, 110, 8)).toBe(1)
    })

    it('always keeps at least the first action, however tight the space', () => {
        expect(fitActionCount([200, 50], 40, 30, 8)).toBe(1)
    })

    it('returns zero only when there are no actions', () => {
        expect(fitActionCount([], 40, 500, 8)).toBe(0)
    })
})

describe('ProjectActionBar', () => {
    it('shows every available action when the width is unknown (jsdom)', () => {
        render(<ProjectActionBar
            handlers={noHandlers}
            pendingId={null}
            project={project({
                canClose: true, canSync: true, canCopy: true, canExport: true,
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
                canClose: true, canExport: true,
            } as Project['capabilities'])}
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
                canClose: true, canSync: true, canCopy: true, canExport: true,
            } as Project['capabilities'])}
        />)

        // Only the primary stays in the bar; the rest move into the overflow menu.
        expect(screen.getByTestId('close-p1')).toHaveAttribute('data-primary', 'true')
        expect(screen.getByTestId('project-actions-more')).toBeInTheDocument()
        const overflow = screen.getByTestId('project-actions-overflow')
        expect(overflow).toContainElement(screen.getByTestId('export-p1'))
    })

    it('renders nothing when no action is available', () => {
        const { container } = render(
            <ProjectActionBar handlers={noHandlers} pendingId={null} project={project({} as Project['capabilities'])} />
        )
        expect(container).toBeEmptyDOMElement()
    })
})

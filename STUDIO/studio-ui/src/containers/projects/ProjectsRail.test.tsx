import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectsRail } from './ProjectsRail'
import { getTagTypes } from '../../services/repositories'
import type { Repository } from '../../types/repositories'

vi.mock('../../services/repositories', () => ({ getTagTypes: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: (_target, name) => String(name) }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('./ProjectsTree', () => ({ ProjectsTree: () => <div data-testid="tree" /> }))

vi.mock('antd', () => {
    interface Option { label: string, value: string }
    const Segmented = ({ options, onChange, ...rest }: Record<string, unknown>) => {
        const { block, size, className, value, ...dom } = rest
        void block; void size; void className; void value
        return (
            <div {...dom}>
                {(options as Option[]).map(option => (
                    <button key={option.value} onClick={() => (onChange as (v: string) => void)(option.value)} type="button">
                        {option.label}
                    </button>
                ))}
            </div>
        )
    }
    const Button = ({ icon, onClick, ...rest }: Record<string, unknown>) => {
        const { size, type, ...dom } = rest
        void size; void type
        return <button onClick={onClick as never} {...dom}>{icon as never}</button>
    }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    return { Button, Segmented, Tooltip }
})

vi.mock('@ant-design/icons', () => ({
    LeftOutlined: () => <span>left</span>,
    RightOutlined: () => <span>right</span>,
}))

/** The rail remembers its mode and its width in the browser; each test starts from an empty memory. */
const stubStorage = () => {
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
        getItem: (key: string) => store[key] ?? null,
        setItem: (key: string, value: string) => { store[key] = value },
        removeItem: (key: string) => { delete store[key] },
        clear: () => Object.keys(store).forEach(key => delete store[key]),
    })
}

const repositories = [{ id: 'design', name: 'Design' }] as unknown as Repository[]

const renderRail = () => render(
    <ProjectsRail
        filters={headerActions => <div data-testid="filters">{headerActions}</div>}
        onOpenGroup={vi.fn()}
        onOpenProject={vi.fn()}
        onShowAll={vi.fn()}
        repositories={repositories}
    />
)

describe('ProjectsRail', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        stubStorage()
        vi.mocked(getTagTypes).mockResolvedValue([])
    })

    it('shows the filters until the user asks for the tree, and remembers the choice', async () => {
        renderRail()

        expect(screen.getByTestId('filters')).toBeInTheDocument()

        await userEvent.click(screen.getByText('home.tree.mode_tree'))

        expect(await screen.findByTestId('tree')).toBeInTheDocument()
        expect(localStorage.getItem('openl.projects.rail')).toBe('tree')
    })

    it('is put away and brought back by the handle of the panel itself', async () => {
        renderRail()

        await userEvent.click(screen.getByTestId('projects-rail-collapse'))

        // Only the strip with its handle is left; the panel is not in the way.
        expect(screen.getByTestId('projects-rail-collapsed')).toBeInTheDocument()
        expect(screen.queryByTestId('filters')).not.toBeInTheDocument()
        expect(localStorage.getItem('openl.projects.rail.collapsed')).toBe('yes')

        await userEvent.click(screen.getByTestId('projects-rail-expand'))

        expect(screen.getByTestId('filters')).toBeInTheDocument()
        expect(localStorage.getItem('openl.projects.rail.collapsed')).toBe('no')
    })

    it('shows the tree alone when the screen has no filters to offer', () => {
        render(
            <ProjectsRail
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onShowAll={vi.fn()}
                repositories={repositories}
            />
        )

        expect(screen.getByTestId('tree')).toBeInTheDocument()
        expect(screen.queryByTestId('projects-rail-mode')).not.toBeInTheDocument()
    })

    it('is resized by dragging its edge, and reopens at that width', () => {
        renderRail()
        const rail = screen.getByTestId('projects-rail')

        fireEvent.pointerDown(screen.getByTestId('projects-rail-resizer'), { clientX: 256 })
        fireEvent(window, new MouseEvent('pointermove', { clientX: 420 }))
        fireEvent(window, new MouseEvent('pointerup', { clientX: 420 }))

        expect(rail).toHaveStyle({ width: '420px' })
        expect(localStorage.getItem('openl.projects.rail.width')).toBe('420')
    })

    it('keeps the rail within the width it can be read at', () => {
        renderRail()
        const rail = screen.getByTestId('projects-rail')

        fireEvent.pointerDown(screen.getByTestId('projects-rail-resizer'), { clientX: 256 })
        fireEvent(window, new MouseEvent('pointermove', { clientX: 40 }))
        expect(rail).toHaveStyle({ width: '200px' })

        fireEvent(window, new MouseEvent('pointermove', { clientX: 2000 }))
        fireEvent(window, new MouseEvent('pointerup', { clientX: 2000 }))
        expect(rail).toHaveStyle({ width: '720px' })
    })
})

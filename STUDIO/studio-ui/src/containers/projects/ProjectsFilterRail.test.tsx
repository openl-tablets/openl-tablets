import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProjectStatusSummary, TagFacetSummary } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { loadFilterLayout } from './filterLayout'
import type { BranchFacetCount } from './projectListing'
import { ProjectsFilterRail } from './ProjectsFilterRail'

vi.mock('react-i18next', () => {
    // The key and the values put into it, so a test reads which group and which place a message names.
    const t = (key: string, values?: Record<string, unknown>) => (values ? [key, ...Object.values(values)].join(' ') : key)
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: (_target, name) => String(name) }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

/** The height of a group head, of one value under it, and of the part of the rail the reader sees. */
const HEAD = 32
const VALUE = 24
const RAIL = 400

const repositories = [{ id: 'design', name: 'Design', type: 'repo-git' }] as unknown as Repository[]

/** The longest group of a real rail: a branch for every piece of work under way. */
const branchCounts: BranchFacetCount[] = Array.from({ length: 40 }, (_, index) => {
    const id = index === 0 ? 'main' : `feature-${index}`
    return { id, name: id, count: 1, isDefault: index === 0, isProtected: false }
})

const tagCounts: TagFacetSummary[] = [{ type: 'Domain', values: [{ id: 'Policy', name: 'Policy', count: 2 }]}]

const statusCounts: ProjectStatusSummary = { local: 0, opened: 1, viewingVersion: 0, editing: 0, closed: 3, deleted: 0 }

const renderRail = (tags = tagCounts) => render(
    <ProjectsFilterRail
        branchCounts={branchCounts}
        branches={new Set()}
        onClearFilters={vi.fn()}
        onToggleBranch={vi.fn()}
        onToggleRepo={vi.fn()}
        onToggleStatus={vi.fn()}
        onToggleTag={vi.fn()}
        repos={new Set()}
        repositories={repositories}
        repositoryCounts={[{ id: 'design', name: 'Design', count: 4 }]}
        statusCounts={statusCounts}
        statuses={new Set()}
        tagCounts={tags}
        tags={new Set()}
    />
)

const groupNodes = () => [...document.querySelectorAll<HTMLElement>('[data-testid^="filter-group-"]')]

/** The groups of the rail, top to bottom. */
const order = () => groupNodes().map(group => group.dataset['testid']?.replace('filter-group-', ''))

/**
 * Lays the rail out the way a browser would, which jsdom does not: the groups one under another, each as
 * tall as the values it lists, in a rail that shows the first {@link RAIL} pixels of them.
 */
const layOutRail = () => {
    vi.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function (this: Element) {
        if (this.matches('[data-testid="projects-filter-scroll"]')) {
            return new DOMRect(0, 0, 240, RAIL)
        }
        const groups = groupNodes()
        let top = 0
        for (const group of groups) {
            const height = HEAD + VALUE * group.querySelectorAll('input[type="checkbox"]').length
            if (group === this) {
                return new DOMRect(0, top, 240, height)
            }
            top += height
        }
        // The list of the groups is as tall as all of them together, however little of it the rail shows.
        return this === groups[0]?.parentElement ? new DOMRect(0, 0, 240, top) : new DOMRect()
    })
}

/** Presses the given part of a group, moves the pointer down the rail by the given distance and lets go. */
const drag = async (target: Element, id: string, distance: number) => {
    const clientX = 120
    const clientY = screen.getByTestId(`filter-group-${id}`).getBoundingClientRect().top + HEAD / 2
    fireEvent.pointerDown(target, { button: 0, clientX, clientY, isPrimary: true })
    // The pointer travels a few pixels before a press becomes a drag.
    fireEvent.pointerMove(document, { clientX, clientY: clientY + Math.sign(distance) * 8 })
    fireEvent.pointerMove(document, { clientX, clientY: clientY + distance })
    fireEvent.pointerUp(document, { clientX, clientY: clientY + distance })
    // A drop swallows the clicks of the next 50 ms, so that it clicks nothing it lands on; the next click of
    // the test, or of the next test, comes after that.
    await new Promise(resolve => setTimeout(resolve, 60))
}

/** The title in the head of a group. */
const headTitle = (id: string, title: string) => within(screen.getByTestId(`filter-drag-${id}`)).getByText(title)

/** The groups once the Branch group is moved to the last place. */
const BRANCH_LAST = ['repository', 'tag:Domain', 'status', 'branch']

describe('ProjectsFilterRail', () => {
    beforeEach(() => {
        localStorage.clear()
        layOutRail()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('lists no values while the filters are arranged, and shows them again folded as before', async () => {
        renderRail()
        await userEvent.click(screen.getByTestId('filter-toggle-tag:Domain'))

        await userEvent.click(screen.getByTestId('projects-filter-arrange'))

        // Every group is down to its head, and a head that folds nothing offers no fold.
        expect(screen.queryAllByRole('checkbox')).toHaveLength(0)
        expect(screen.queryByTestId('filter-toggle-branch')).toBeNull()
        expect(headTitle('branch', 'home.facet_branch')).toBeTruthy()

        await userEvent.click(screen.getByTestId('projects-filter-arrange-done'))

        expect(screen.getByTestId('filter-branch-feature-39')).toBeTruthy()
        expect(screen.getByTestId('filter-status-CLOSED')).toBeTruthy()
        expect(screen.getByTestId('filter-toggle-tag:Domain').getAttribute('aria-expanded')).toBe('false')
        expect(screen.queryByTestId('filter-tag-Domain:Policy')).toBeNull()
    })

    it('moves the longest group to the last place and back, dragged by its title', async () => {
        renderRail()
        await userEvent.click(screen.getByTestId('projects-filter-arrange'))

        await drag(headTitle('branch', 'home.facet_branch'), 'branch', 2 * HEAD)

        expect(order()).toEqual(BRANCH_LAST)
        expect(loadFilterLayout().order).toEqual(BRANCH_LAST)

        await drag(headTitle('branch', 'home.facet_branch'), 'branch', -3 * HEAD)

        expect(order()).toEqual(['branch', 'repository', 'tag:Domain', 'status'])
    })

    it('moves a group to the last place of a rail that its heads alone overflow', async () => {
        const types = Array.from({ length: 14 }, (_, index) => `Type${index + 1}`)
        renderRail(types.map(type => ({ type, values: [{ id: 'Any', name: 'Any', count: 1 }]})))
        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        // Seventeen heads are taller than the part of the rail the reader sees.
        expect(groupNodes().length * HEAD).toBeGreaterThan(RAIL)

        await drag(headTitle('repository', 'home.facet_repository'), 'repository', 16 * HEAD)

        expect(order()).toEqual(['branch', ...types.map(type => `tag:${type}`), 'status', 'repository'])
    })

    it('moves a group with the keyboard from its head, telling a screen reader where it goes', async () => {
        renderRail()
        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        const head = screen.getByTestId('filter-drag-branch')
        head.focus()

        await userEvent.keyboard(' {ArrowDown}{ArrowDown} ')

        expect(order()).toEqual(BRANCH_LAST)
        expect(document.getElementById(head.getAttribute('aria-describedby') ?? ''))
            .toHaveTextContent('home.filter_group.instructions')
        expect(screen.getByRole('status')).toHaveTextContent('home.filter_group.dropped home.facet_branch 4 4')
    })

    it('leaves a group where it was when the move is cancelled', async () => {
        renderRail()
        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        screen.getByTestId('filter-drag-branch').focus()

        await userEvent.keyboard(' {ArrowDown}{Escape}')

        expect(order()).toEqual(['repository', 'branch', 'tag:Domain', 'status'])
        expect(screen.getByRole('status')).toHaveTextContent('home.filter_group.cancelled home.facet_branch')
    })

    it('puts a group away by its button without picking the group up', async () => {
        renderRail()
        await userEvent.click(screen.getByTestId('projects-filter-arrange'))
        const hide = screen.getByTestId('filter-hide-branch')

        // A press on the button that slips down the rail moves nothing.
        await drag(hide, 'branch', 2 * HEAD)
        expect(order()).toEqual(['repository', 'branch', 'tag:Domain', 'status'])

        await userEvent.click(hide)

        expect(order()).toEqual(['repository', 'tag:Domain', 'status'])
        expect(screen.getByTestId('filter-show-branch')).toBeTruthy()
    })
})

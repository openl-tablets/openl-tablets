import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { deriveProjectRow, fittingTags, ProjectBranch, ProjectBranchSwitch, ProjectTags } from './projectRow'
import { tagColorsOf } from '../tags/tagColor'
import { TagTypeOrder } from '../tags/tagOrder'
import { formatDateTime } from '../../utils/dateFormat'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const { density } = vi.hoisted(() => ({ density: { compact: false } }))

vi.mock('../../providers/AppThemeProvider', async importOriginal => ({
    ...await importOriginal<typeof import('../../providers/AppThemeProvider')>(),
    useAppTheme: () => ({ compact: density.compact }),
}))

const { branchSwitcherMock } = vi.hoisted(() => ({ branchSwitcherMock: vi.fn() }))

vi.mock('./BranchSwitcher', () => ({
    BranchSwitcher: (props: Record<string, unknown>) => {
        branchSwitcherMock(props)
        return <span data-testid={props['data-testid'] as string}>{props['currentBranch'] as string}</span>
    },
}))

const project: Project = {
    branch: 'main',
    comment: '',
    id: 'p1',
    modifiedAt: '',
    modifiedBy: '',
    name: 'Alpha',
    repository: 'design',
    revision: '',
    status: ProjectStatus.Closed,
}

describe('ProjectBranch', () => {
    it('carries the same marks as the rest of the workspace', () => {
        render(<ProjectBranch supportsBranches project={{ ...project, branchDefault: true, branchProtected: true }} />)

        expect(screen.getByText('main')).toBeInTheDocument()
        expect(screen.getByTestId('row-branch-default')).toBeInTheDocument()
        expect(screen.getByTestId('row-branch-protected')).toBeInTheDocument()
    })

    it('leaves an ordinary branch unmarked', () => {
        render(<ProjectBranch supportsBranches project={{ ...project, branch: 'feature/rates' }} />)

        expect(screen.queryByTestId('row-branch-default')).toBeNull()
        expect(screen.queryByTestId('row-branch-protected')).toBeNull()
    })

    it('shows nothing for a project listed without a branch', () => {
        const { container } = render(
            <ProjectBranch supportsBranches project={{ ...project, status: ProjectStatus.Local }} />
        )

        expect(container.textContent).toBe('')
    })
})

describe('deriveProjectRow', () => {
    // The row builds every label the table and the grid show, so the dates are formatted here.
    const t = ((key: string, values?: Record<string, unknown>) =>
        values ? `${key}:${JSON.stringify(values)}` : key) as never
    const repoInfoOf = () => ({ name: 'Design', type: 'repo-git' }) as never

    it('formats the modification date and the date a lock was taken', () => {
        const locked = {
            ...project,
            modifiedAt: '2026-07-09T10:30:00Z',
            lockInfo: { lockedBy: 'jane', lockedAt: '2026-07-09T11:45:00Z' },
        }

        const row = deriveProjectRow(locked, repoInfoOf, t)

        expect(row.date).toBe(formatDateTime('2026-07-09T10:30:00Z'))
        // A lock used to carry the raw timestamp while the row beside it was formatted.
        expect(row.lockLabel).toContain(formatDateTime('2026-07-09T11:45:00Z') as string)
        expect(row.lockLabel).not.toContain('2026-07-09T11:45:00Z')
    })

    it('keeps each tag value by its type, and no tags for a project without any', () => {
        expect(deriveProjectRow({ ...project, tags: { Domain: 'Policy' } }, repoInfoOf, t).tags).toEqual({ Domain: 'Policy' })
        expect(deriveProjectRow(project, repoInfoOf, t).tags).toEqual({})
    })
})

describe('fittingTags', () => {
    it('takes every tag that fits, keeping no room for a "+N" chip', () => {
        expect(fittingTags([50, 50], 104, 4, 30)).toBe(2)
        expect(fittingTags([], 0, 4, 30)).toBe(0)
    })

    it('keeps the room of the "+N" chip when not every tag fits', () => {
        // 30 + 4 + 50 = 84 fits, a second tag would need 138.
        expect(fittingTags([50, 50, 50], 120, 4, 30)).toBe(1)
    })

    it('stops at the first tag that does not fit, even if a later one would', () => {
        expect(fittingTags([50, 200, 10], 120, 4, 30)).toBe(1)
    })

    it('counts fractional widths as they are', () => {
        expect(fittingTags(Array.from({ length: 10 }, () => 60.25), 636, 4, 30)).toBe(9)
    })

    it('shows the "+N" chip alone when not even the first tag fits beside it', () => {
        expect(fittingTags([100, 10], 100, 4, 30)).toBe(0)
    })
})

describe('ProjectTags', () => {
    it('shows each value in the color of its tag type, naming the type in the tooltip', () => {
        render(
            <TagTypeOrder value={['Domain', 'LOB']}>
                <ProjectTags tags={{ Domain: 'Policy', LOB: 'Auto' }} />
            </TagTypeOrder>
        )

        const chip = screen.getByTestId('project-tag-Domain')
        expect(chip).toHaveTextContent('Policy')
        expect(chip).toHaveAttribute('title', 'Domain: Policy')
        expect(chip.className).toContain(`ant-tag-${tagColorsOf(['Domain', 'LOB'])('Domain')}`)
        expect(screen.getByTestId('project-tag-LOB').className).toContain(`ant-tag-${tagColorsOf(['Domain', 'LOB'])('LOB')}`)
    })

    it('lists the tags in the order an administrator arranged the tag types in, then the others by name', () => {
        render(
            <TagTypeOrder value={['LOB', 'Domain']}>
                <ProjectTags tags={{ Zone: 'North', Domain: 'Policy', area: 'West', LOB: 'Auto' }} />
            </TagTypeOrder>
        )

        expect(screen.getAllByTestId(/^project-tag-/).map(chip => chip.textContent)).toEqual(['Auto', 'Policy', 'West', 'North'])
    })

    it('shows a tag of a type that is not configured in gray', () => {
        render(
            <TagTypeOrder value={['Domain']}>
                <ProjectTags tags={{ Domain: 'Policy', Region: 'EU' }} />
            </TagTypeOrder>
        )

        expect(screen.getByTestId('project-tag-Region').className).toContain('ant-tag-default')
        expect(screen.getByTestId('project-tag-Domain').className).not.toContain('ant-tag-default')
    })

    describe('on a line too narrow for all of them', () => {
        /** Lays the line out as a browser would, which jsdom does not: chips, the "+N" chip and the line, in px. */
        const layOut = (chip: number, more: number, line: number) => {
            const measure = HTMLElement.prototype.getBoundingClientRect
            const widthOf = (element: HTMLElement): number | undefined => {
                if (element.hasAttribute('data-tag-more-measure')) {
                    return more
                }
                if (element.hasAttribute('data-tag-chip')) {
                    return chip
                }
                return element.querySelector(':scope > [data-tag-chip]') ? line : undefined
            }
            // Anything else, such as what a tooltip is placed by, keeps the measure jsdom gives it.
            vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
                const width = widthOf(this)
                return width === undefined ? measure.call(this) : new DOMRect(0, 0, width, 20)
            })
        }

        // Every chip is 60 px wide, the "+N" chip 30 px, and the line 200 px.
        beforeEach(() => {
            layOut(60, 30, 200)
        })

        afterEach(() => {
            vi.restoreAllMocks()
            density.compact = false
        })

        it('shows the tags that fit and collapses the rest into a "+N" chip, on one line', () => {
            render(<ProjectTags tags={{ A: '1', B: '2', C: '3', D: '4', E: '5' }} />)

            // 30 for "+N", then 4 + 60 per chip: two chips take 158 px, a third would take 222.
            expect(screen.getByTestId('project-tag-A')).not.toHaveAttribute('aria-hidden')
            expect(screen.getByTestId('project-tag-B')).not.toHaveAttribute('aria-hidden')
            expect(screen.getByTestId('project-tag-C')).toHaveAttribute('aria-hidden', 'true')
            expect(screen.getByTestId('project-tag-E')).toHaveAttribute('aria-hidden', 'true')
            expect(screen.getByTestId('project-tags-more')).toHaveTextContent('+3')
        })

        it('fits the line again when the density changes the size of the chips', () => {
            const { rerender } = render(<ProjectTags tags={{ A: '1', B: '2', C: '3', D: '4', E: '5' }} />)
            expect(screen.getByTestId('project-tags-more')).toHaveTextContent('+3')

            // The compact density draws smaller chips: 30 + 4 times (4 + 40) = 206 is too wide, three take 162.
            layOut(40, 30, 200)
            density.compact = true
            rerender(<ProjectTags tags={{ A: '1', B: '2', C: '3', D: '4', E: '5' }} />)

            expect(screen.getByTestId('project-tags-more')).toHaveTextContent('+2')
            expect(screen.getByTestId('project-tag-C')).not.toHaveAttribute('aria-hidden')
        })

        it('keeps the fractions of the widths, so the last chip that would be cut collapses instead', () => {
            // Ten chips of 60.25 px need 638.5 px with their gaps; rounded down, they would seem to fit in 636.
            layOut(60.25, 30, 636)
            render(<ProjectTags tags={Object.fromEntries([...'ABCDEFGHIJ'].map(type => [type, type.toLowerCase()]))} />)

            expect(screen.getByTestId('project-tags-more')).toHaveTextContent('+1')
            expect(screen.getByTestId('project-tag-J')).toHaveAttribute('aria-hidden', 'true')
        })

        it('lists the collapsed tags in the tooltip of the "+N" chip', async () => {
            render(<ProjectTags tags={{ A: 'first', B: 'second', C: 'third', D: 'fourth' }} />)

            await userEvent.hover(screen.getByTestId('project-tags-more'))

            const tooltip = await screen.findByRole('tooltip')
            expect(tooltip).toHaveTextContent('thirdfourth')
            expect(tooltip).not.toHaveTextContent('first')
        })
    })

    it('shows every tag without a "+N" chip when they all fit', () => {
        render(<ProjectTags tags={{ A: '1', B: '2', C: '3', D: '4', E: '5', F: '6' }} />)

        expect(screen.getByTestId('project-tag-F')).not.toHaveAttribute('aria-hidden')
        expect(screen.queryByTestId('project-tags-more')).toBeNull()
    })

    it('shows nothing for a project without tags', () => {
        expect(render(<ProjectTags tags={{}} />).container.textContent).toBe('')
    })
})

describe('ProjectBranchSwitch', () => {
    const render_ = (over: Partial<Project> = {}, props: Partial<Parameters<typeof ProjectBranchSwitch>[0]> = {}) =>
        render(
            <ProjectBranchSwitch
                supportsBranches
                busy={false}
                onSwitched={vi.fn()}
                project={{ ...project, ...over }}
                {...props}
            />
        )

    it('offers the branch of the project, named after the view it sits in', () => {
        render_()

        expect(screen.getByTestId('row-branch-p1')).toHaveTextContent('main')
    })

    it('shows nothing where a project has no branch to switch', () => {
        expect(render_({ branch: '' }).container.textContent).toBe('')
        expect(render_({ status: ProjectStatus.Local }).container.textContent).toBe('')
        expect(render_({}, { supportsBranches: false }).container.textContent).toBe('')
    })

    it('blocks the switch while the project is busy with something else', () => {
        render_({}, { busy: true })

        expect(branchSwitcherMock).toHaveBeenLastCalledWith(expect.objectContaining({ disabled: true }))
    })

    it('reports its switch against its own project, so one busy row leaves the others alone', () => {
        const onSwitching = vi.fn()
        render_({}, { onSwitching })

        const props = branchSwitcherMock.mock.calls.at(-1)?.[0] as { onBusyChange: (busy: boolean) => void }
        props.onBusyChange(true)

        expect(onSwitching).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), true)
    })
})

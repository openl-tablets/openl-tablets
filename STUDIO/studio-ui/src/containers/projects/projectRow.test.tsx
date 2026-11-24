import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { deriveProjectRow, ProjectBranch, ProjectBranchSwitch, ProjectTags } from './projectRow'
import { tagColorsOf } from '../tags/tagColor'
import { TagTypeOrder } from '../tags/tagOrder'
import { formatDateTime } from '../../utils/dateFormat'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

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

    it('collapses the tags past the fourth into a count', () => {
        render(<ProjectTags tags={{ A: '1', B: '2', C: '3', D: '4', E: '5', F: '6' }} />)

        expect(screen.queryByTestId('project-tag-E')).toBeNull()
        expect(screen.getByText('+2')).toBeInTheDocument()
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
                testIdPrefix="row"
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

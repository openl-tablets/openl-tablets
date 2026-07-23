import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { deriveProjectRow, ProjectBranch } from './projectRow'
import { formatDateTime } from '../../utils/dateFormat'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

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
})

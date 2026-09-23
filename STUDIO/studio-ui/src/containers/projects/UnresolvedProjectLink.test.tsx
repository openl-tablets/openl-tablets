import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UnresolvedProjectLink } from './UnresolvedProjectLink'

const { navigateMock } = vi.hoisted(() => ({ navigateMock: vi.fn() }))

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: { name?: string }) => (options?.name ? `${key}:${options.name}` : key)
    return { useTranslation: () => ({ t }) }
})

const routeOf = (id: string) => `/projects/${id}/modules/Main?table=t1`

describe('UnresolvedProjectLink', () => {
    beforeEach(() => {
        navigateMock.mockReset()
    })

    it('says a link leads to no project, and leads back to the projects', async () => {
        render(<UnresolvedProjectLink addressed="nonexistent" problem={{ kind: 'missing' }} routeOf={routeOf} />)

        expect(screen.getByTestId('project-workspace-missing')).toHaveTextContent('home.not_found')
        await userEvent.click(screen.getByText('home.back_to_projects'))
        expect(navigateMock).toHaveBeenCalledWith('/projects')
    })

    it('lists the projects a name leads to, and opens the one picked by its id', async () => {
        const problem = {
            kind: 'ambiguous' as const,
            candidates: [
                { id: 'ZGVzaWduOkhlbGxv', name: 'Hello', repository: 'design', repositoryName: 'Design', path: 'Hello' },
                { id: 'ZGVz+aWdu/MTpoZWxsbw==', name: 'hello', repository: 'design1', repositoryName: 'Design1', path: 'hello' },
            ],
        }
        render(<UnresolvedProjectLink addressed="hello" problem={problem} routeOf={routeOf} />)

        expect(screen.getByTestId('project-link-ambiguous')).toHaveTextContent('home.ambiguous_title:hello')
        const candidates = screen.getAllByTestId('project-link-candidate')
        expect(candidates.map(candidate => candidate.textContent)).toEqual(['HelloDesign · Hello', 'helloDesign1 · hello'])

        // The id the server issued may carry the older alphabet: the address spells it the way the screen does.
        await userEvent.click(candidates[1]!)
        expect(navigateMock).toHaveBeenCalledWith('/projects/ZGVz-aWdu_MTpoZWxsbw==/modules/Main?table=t1')
    })

    it('tells apart projects of one name in one repository by the folders they occupy', () => {
        const problem = {
            kind: 'ambiguous' as const,
            candidates: [
                { id: 'ZGVzaWduOlRFU1Q6YQ==', name: 'TEST', repository: 'design', repositoryName: 'Design', path: 'f1/TEST' },
                { id: 'ZGVzaWduOlRFU1Q6Yg==', name: 'TEST', repository: 'design', repositoryName: 'Design', path: 'f2/TEST' },
            ],
        }
        render(<UnresolvedProjectLink addressed="TEST" problem={problem} routeOf={routeOf} />)

        expect(screen.getAllByTestId('project-link-candidate').map(candidate => candidate.textContent))
            .toEqual(['TESTDesign · f1/TEST', 'TESTDesign · f2/TEST'])
    })

    it('lists a candidate stored in no repository by its name alone', () => {
        const problem = {
            kind: 'ambiguous' as const,
            candidates: [
                { id: 'ZGVzaWduOkhlbGxv', name: 'Hello', repository: 'design', repositoryName: 'Design' },
                { id: 'bG9jYWw6SGVsbG8=', name: 'Hello' },
            ],
        }
        render(<UnresolvedProjectLink addressed="Hello" problem={problem} routeOf={routeOf} />)

        expect(screen.getAllByTestId('project-link-candidate').map(candidate => candidate.textContent))
            .toEqual(['HelloDesign', 'Hello'])
    })
})

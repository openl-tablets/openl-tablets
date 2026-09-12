import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Project } from '../../types/projects'
import { ModuleActionBar } from './ModuleActionBar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

vi.mock('../projects/RevisionsPanel', () => ({
    RevisionsPanel: ({ projectId }: { projectId: string }) => <div data-testid="revisions-panel">{projectId}</div>,
}))

vi.mock('../projects/LocalChangesView', () => ({ LocalChangesView: () => <div data-testid="local-changes-view" /> }))

vi.mock('../execution/TestsResultModal', () => ({ TestsResultModal: () => <div data-testid="tests-result" /> }))

const project = (capabilities: Project['capabilities']): Project => ({
    id: 'p1',
    name: 'Rating',
    revision: 'abc123',
    capabilities,
} as Project)

const bar = async (capabilities: Project['capabilities'] = { canViewHistory: true } as Project['capabilities']) => {
    render(<ModuleActionBar moduleName="Claims" project={project(capabilities)} />)
    await act(async () => {
        await Promise.resolve()
    })
}

describe('ModuleActionBar', () => {
    it('reads the project history in a window rather than on the project screen', async () => {
        await bar()

        await userEvent.click(screen.getByTestId('module-more'))
        await userEvent.click(await screen.findByText('browser.module.revisions'))

        expect(await screen.findByTestId('revisions-panel')).toHaveTextContent('p1')
        // Leaving the editor for the project screen would take the reader away from the module they are reading.
        expect(navigate).not.toHaveBeenCalled()
    })

    it('offers no history to a reader who may not read it', async () => {
        await bar({ canViewHistory: false } as Project['capabilities'])

        await userEvent.click(screen.getByTestId('module-more'))

        expect(await screen.findByText('browser.module.local_changes')).toBeInTheDocument()
        expect(screen.queryByText('browser.module.revisions')).toBeNull()
    })
})

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UnresolvedProjectLink } from './UnresolvedProjectLink'

const { navigateMock } = vi.hoisted(() => ({ navigateMock: vi.fn() }))

vi.mock('react-router', () => ({
    useNavigate: () => navigateMock,
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

describe('UnresolvedProjectLink', () => {
    beforeEach(() => {
        navigateMock.mockReset()
    })

    it('says a link leads to no project, and leads back to the projects', async () => {
        render(<UnresolvedProjectLink />)

        expect(screen.getByTestId('project-workspace-missing')).toHaveTextContent('home.not_found')
        await userEvent.click(screen.getByText('home.back_to_projects'))
        expect(navigateMock).toHaveBeenCalledWith('/projects')
    })
})

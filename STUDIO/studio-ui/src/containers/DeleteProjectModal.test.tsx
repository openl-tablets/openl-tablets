import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DeleteProjectModal } from './DeleteProjectModal'
import { deleteProject } from 'services/projects'
import { getRepositoryConfig } from 'services/repositories'
import type { RepositoryConfig } from 'types/repositories'

vi.mock('services/projects', () => ({
    deleteProject: vi.fn(),
}))

vi.mock('services/repositories', async () => ({
    ...await vi.importActual<typeof import('services/repositories')>('services/repositories'),
    getRepositoryConfig: vi.fn(),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({
        open,
        title,
        children,
        footer,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        footer?: React.ReactNode
    }) =>
        open ? (
            <div role="dialog">
                {title && <div data-testid="modal-title">{title}</div>}
                {children}
                {footer && <div data-testid="modal-footer">{footer}</div>}
            </div>
        ) : null
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const mockDeleteProject = vi.mocked(deleteProject)
const mockGetRepositoryConfig = vi.mocked(getRepositoryConfig)

const TEST_PATTERN: RepositoryConfig = {
    comment: { userMessagePattern: String.raw`Test\.`, invalidUserMessageHint: 'Error.' },
}

const urlSafeId = (raw: string) => btoa(raw).replaceAll('+', '-').replaceAll('/', '_')

const openModal = async (onSuccess = vi.fn(), comments = true, localOnly = false) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
            detail: {
                projectId: urlSafeId('design:MyProject'),
                projectName: 'MyProject',
                repositoryInfo: { name: 'Design', features: { comments } },
                localOnly,
                onSuccess,
            },
        }))
    })
    return onSuccess
}

const getDeleteButton = () => screen.getByRole('button', { name: 'repository:delete_project_modal.confirm_button' })

/**
 * Opens the dialog while the settings of the repository are loading.
 *
 * @returns the answer of the load: it hands the dialog the settings and waits until they are drawn
 */
const openLoadingSettings = async () => {
    const config = Promise.withResolvers<RepositoryConfig>()
    mockGetRepositoryConfig.mockReturnValue(config.promise)
    render(<DeleteProjectModal />)
    await openModal()
    return (settings: RepositoryConfig) => act(async () => config.resolve(settings))
}

/** Opens the dialog on a repository with the given settings, drawn before the test goes on. */
const openWithSettings = async (settings: RepositoryConfig) => {
    const answer = await openLoadingSettings()
    await answer(settings)
}

describe('DeleteProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockGetRepositoryConfig.mockResolvedValue({ comment: {} })
    })

    it('does not render when no event is dispatched', () => {
        render(<DeleteProjectModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('requires acknowledgement and a comment before deleting a project', async () => {
        mockDeleteProject.mockResolvedValueOnce(true)
        const user = userEvent.setup()
        const onSuccess = vi.fn()

        render(<DeleteProjectModal />)
        await openModal(onSuccess)

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())
        const deleteButton = getDeleteButton()

        expect(deleteButton).toBeDisabled()

        await user.type(screen.getByRole('textbox'), 'No longer needed')
        expect(deleteButton).toBeDisabled()

        await user.click(screen.getByRole('checkbox'))
        expect(deleteButton).toBeEnabled()

        await user.click(deleteButton)

        await waitFor(() => {
            expect(mockDeleteProject).toHaveBeenCalledWith(urlSafeId('design:MyProject'), 'MyProject', 'No longer needed')
            expect(onSuccess).toHaveBeenCalled()
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        })
    })

    it('flags the comment as it is after each keystroke', async () => {
        const user = userEvent.setup()
        await openWithSettings(TEST_PATTERN)
        const comment = screen.getByRole('textbox')
        const deleteButton = getDeleteButton()
        await user.click(screen.getByRole('checkbox'))

        await user.type(comment, 'Test')
        expect(await screen.findByText('Error.')).toBeInTheDocument()
        expect(comment).toHaveAttribute('aria-invalid', 'true')
        expect(deleteButton).toBeDisabled()

        await user.type(comment, '.')
        await waitFor(() => expect(comment).not.toHaveAttribute('aria-invalid'))
        expect(deleteButton).toBeEnabled()

        await user.type(comment, '{Backspace}')
        await waitFor(() => expect(comment).toHaveAttribute('aria-invalid', 'true'))
        expect(screen.getByText('Error.')).toBeInTheDocument()
        expect(deleteButton).toBeDisabled()
    })

    it('checks a comment typed before the settings arrive once they do', async () => {
        const user = userEvent.setup()
        const answer = await openLoadingSettings()
        const comment = screen.getByRole('textbox')
        await user.type(comment, 'Test')
        expect(comment).not.toHaveAttribute('aria-invalid')

        await answer(TEST_PATTERN)

        expect(await screen.findByText('Error.')).toBeInTheDocument()
        expect(comment).toHaveAttribute('aria-invalid', 'true')
    })

    it('asks for a comment, without the pattern hint, when it is blank', async () => {
        const user = userEvent.setup()
        await openWithSettings(TEST_PATTERN)
        await user.type(screen.getByRole('textbox'), '  ')

        expect(await screen.findByText('common:validation.required')).toBeInTheDocument()
        expect(screen.queryByText('Error.')).not.toBeInTheDocument()
    })

    it('accepts a comment that matches the pattern once the spaces around it are left out', async () => {
        mockDeleteProject.mockResolvedValueOnce(true)
        const user = userEvent.setup()
        await openWithSettings(TEST_PATTERN)
        await user.click(screen.getByRole('checkbox'))
        await user.type(screen.getByRole('textbox'), ' Test. ')
        await user.click(getDeleteButton())

        await waitFor(() => expect(mockDeleteProject).toHaveBeenCalledWith(
            urlSafeId('design:MyProject'), 'MyProject', 'Test.'))
    })

    it('deletes from a file repository without asking for a comment', async () => {
        mockDeleteProject.mockResolvedValueOnce(true)
        render(<DeleteProjectModal />)
        await openModal(vi.fn(), false)

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
        await userEvent.click(screen.getByRole('checkbox'))
        const deleteButton = getDeleteButton()
        expect(deleteButton).toBeEnabled()
        await userEvent.click(deleteButton)

        await waitFor(() => expect(mockDeleteProject).toHaveBeenCalledWith(
            urlSafeId('design:MyProject'), 'MyProject', undefined))
    })

    it('does not ask for a comment when deleting a local project', async () => {
        render(<DeleteProjectModal />)
        await openModal(vi.fn(), true, true)

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })
})

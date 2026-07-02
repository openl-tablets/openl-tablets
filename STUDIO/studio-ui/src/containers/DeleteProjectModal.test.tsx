import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DeleteProjectModal } from './DeleteProjectModal'
import { deleteProject } from 'services/projects'
import type { MockedFunction } from 'vitest'

vi.mock('services/projects', () => ({
    deleteProject: vi.fn(),
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

const mockDeleteProject = deleteProject as MockedFunction<typeof deleteProject>

const urlSafeId = (raw: string) => btoa(raw).replaceAll('+', '-').replaceAll('/', '_')

const openModal = async (onSuccess = vi.fn()) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openDeleteProjectModal', {
            detail: {
                projectId: urlSafeId('design:MyProject'),
                projectName: 'MyProject',
                onSuccess,
            },
        }))
    })
    return onSuccess
}

describe('DeleteProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
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
        const deleteButton = screen.getByRole('button', { name: 'repository:delete_project_modal.confirm_button' })

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
})

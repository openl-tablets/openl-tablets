import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DeleteFileModal } from './DeleteFileModal'
import { deleteProjectFile } from 'services/projects'
import type { MockedFunction } from 'vitest'

vi.mock('services/projects', () => ({
    deleteProjectFile: vi.fn(),
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

const mockDeleteProjectFile = deleteProjectFile as MockedFunction<typeof deleteProjectFile>

const openModal = async (isFolder = false, onSuccess = vi.fn()) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openDeleteFileModal', {
            detail: {
                projectId: 'proj-id',
                path: 'rules/Main.xlsx',
                name: 'Main.xlsx',
                isFolder,
                onSuccess,
            },
        }))
    })
    return onSuccess
}

describe('DeleteFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('does not render when no event is dispatched', () => {
        render(<DeleteFileModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('deletes the file after confirmation and closes on success', async () => {
        mockDeleteProjectFile.mockResolvedValueOnce(true)
        const user = userEvent.setup()
        const onSuccess = vi.fn()

        render(<DeleteFileModal />)
        await openModal(false, onSuccess)

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())
        expect(screen.getByText('repository:delete_file_modal.confirm_file')).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: 'repository:delete_file_modal.confirm_button' }))

        await waitFor(() => {
            expect(mockDeleteProjectFile).toHaveBeenCalledWith('proj-id', 'rules/Main.xlsx', 'Main.xlsx', false)
            expect(onSuccess).toHaveBeenCalled()
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        })
    })

    it('shows the folder confirmation and stays open when deletion fails', async () => {
        mockDeleteProjectFile.mockResolvedValueOnce(false)
        const user = userEvent.setup()
        const onSuccess = vi.fn()

        render(<DeleteFileModal />)
        await openModal(true, onSuccess)

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())
        expect(screen.getByText('repository:delete_file_modal.confirm_folder')).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: 'repository:delete_file_modal.confirm_button' }))

        await waitFor(() => {
            expect(mockDeleteProjectFile).toHaveBeenCalledWith('proj-id', 'rules/Main.xlsx', 'Main.xlsx', true)
        })
        expect(onSuccess).not.toHaveBeenCalled()
        expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
})

import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeleteFileModal } from './DeleteFileModal'
import { deleteFile } from '../../services/files'
import { ApiHttpError } from '../../services/apiCall'

vi.mock('../../services/files', () => ({ deleteFile: vi.fn() }))

const notification = vi.hoisted(() => ({ error: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', async () => {
    const Modal = ({ open, children, onOk, okButtonProps }: Record<string, unknown>) => {
        // `danger` is an Ant Design prop, not a DOM attribute.
        const { danger, ...dom } = (okButtonProps ?? {}) as Record<string, unknown>
        void danger
        return open ? (
            <div role="dialog">
                {children as never}
                <button {...dom} onClick={onOk as never}>ok</button>
            </div>
        ) : null
    }
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({ Modal, notification })
})

const props = {
    open: true,
    projectId: 'p1',
    path: 'rules/Main.xlsx',
    onClose: vi.fn(),
    onDeleted: vi.fn(),
}

describe('DeleteFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(deleteFile).mockResolvedValue()
    })

    it('deletes the file once confirmed in the dialog', async () => {
        render(<DeleteFileModal {...props} />)

        await userEvent.click(screen.getByTestId('file-delete-submit'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx'))
        expect(props.onDeleted).toHaveBeenCalled()
        expect(props.onClose).toHaveBeenCalled()
    })

    it('names the entry being deleted without spelling out its whole path', () => {
        render(<DeleteFileModal {...props} />)

        expect(screen.getByRole('dialog')).toHaveTextContent('delete_file_modal.confirm_file')
    })

    it('closes and reports a deletion the server refuses, which another click would not change (EPBDS-16638)',
        async () => {
            vi.mocked(deleteFile).mockRejectedValue(
                new ApiHttpError(409, 'The project descriptor rules.xml cannot be deleted or moved'))
            render(<DeleteFileModal {...props} path="rules.xml" />)

            await userEvent.click(screen.getByTestId('file-delete-submit'))

            await waitFor(() => expect(props.onClose).toHaveBeenCalled())
            expect(notification.error).toHaveBeenCalledWith(expect.objectContaining({
                description: 'The project descriptor rules.xml cannot be deleted or moved',
            }))
            expect(props.onDeleted).not.toHaveBeenCalled()
        })

    it('stays open and reports a deletion that failed on the way, so it can be asked for again', async () => {
        vi.mocked(deleteFile).mockRejectedValue(new Error('Failed to fetch'))
        render(<DeleteFileModal {...props} />)

        await userEvent.click(screen.getByTestId('file-delete-submit'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalled())
        expect(props.onDeleted).not.toHaveBeenCalled()
        expect(props.onClose).not.toHaveBeenCalled()
    })
})

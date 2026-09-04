import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeleteFileModal } from './DeleteFileModal'
import { deleteFile } from '../../services/files'

vi.mock('../../services/files', () => ({ deleteFile: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
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
    const notification = { error: vi.fn() }
    return { Modal, notification }
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

    it('stays open and reports a failed deletion', async () => {
        vi.mocked(deleteFile).mockRejectedValue(new Error('locked'))
        render(<DeleteFileModal {...props} />)

        await userEvent.click(screen.getByTestId('file-delete-submit'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalled())
        expect(props.onDeleted).not.toHaveBeenCalled()
        expect(props.onClose).not.toHaveBeenCalled()
    })
})

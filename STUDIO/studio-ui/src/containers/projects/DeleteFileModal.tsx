import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { App, Modal } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { deleteFile } from '../../services/files'
import { isApiHttpError } from '../../services/apiCall'
import { basename } from './projectPaths'

interface DeleteFileModalProps {
    open: boolean
    projectId: string
    /** Project-relative path of the file or folder being deleted. */
    path: string
    /** Deleting a folder rather than a file — it only changes what the dialog says. */
    folder?: boolean
    onClose: () => void
    onDeleted: () => void
}

/**
 * Whether the server refused the deletion for a reason of its own, such as a file the project cannot do without:
 * asking again would be refused the same way.
 */
const isRefusal = (error: unknown) => isApiHttpError(error) && error.status >= 400 && error.status < 500

/**
 * Asks before deleting a file or folder of the project, in a dialog rather than over the button.
 *
 * <p>A deletion the server refuses closes the dialog and says why. One that failed on the way, such as a lost
 * connection, keeps the dialog open, so the deletion can be asked for again.
 */
export const DeleteFileModal = ({ open, projectId, path, folder = false, onClose, onDeleted }: DeleteFileModalProps) => {
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const [busy, setBusy] = useState(false)

    const submit = async () => {
        setBusy(true)
        try {
            await deleteFile(projectId, path)
            onDeleted()
            onClose()
        } catch (e) {
            notification.error({ title: t('browser.files.delete_failed'), description: errorMessage(e) })
            if (isRefusal(e)) {
                onClose()
            }
        } finally {
            setBusy(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={busy}
            okButtonProps={{ 'data-testid': 'file-delete-submit', danger: true }}
            okText={t('delete_file_modal.confirm_button')}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('delete_file_modal.title')}
        >
            {t(folder ? 'delete_file_modal.confirm_folder' : 'delete_file_modal.confirm_file', {
                name: basename(path),
            })}
        </Modal>
    )
}

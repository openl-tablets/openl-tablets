import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Modal, notification } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { deleteFile } from '../../services/files'
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

/** Asks before deleting a file or folder of the project, in a dialog rather than over the button. */
export const DeleteFileModal = ({ open, projectId, path, folder = false, onClose, onDeleted }: DeleteFileModalProps) => {
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

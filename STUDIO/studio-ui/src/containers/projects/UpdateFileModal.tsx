import { useEffect, useState } from 'react'
import { Alert, Modal, Space, notification } from 'antd'
import type { UploadFile } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { FileDropzone } from '../../components/FileDropzone'
import { replaceFile } from '../../services/files'

interface UpdateFileModalProps {
    open: boolean
    projectId: string
    /** Project-relative path of the file being replaced, e.g. "rules/Main.xlsx". */
    path: string
    /** Dialog title: what is being replaced reads differently for a module and for a plain file. */
    title: string
    /** File extensions the dialog accepts; any file is taken when omitted. */
    extensions?: string[]
    onClose: () => void
    onUpdated: () => void
}

/**
 * Replaces a file of the project with one uploaded from the computer, keeping its path.
 *
 * A warning appears when the picked file is named differently from the one being replaced, so grabbing the
 * wrong file is noticed before the update rather than after it.
 */
export const UpdateFileModal = ({
    open,
    projectId,
    path,
    title,
    extensions,
    onClose,
    onUpdated,
}: UpdateFileModalProps) => {
    const { t } = useTranslation()
    const [uploading, setUploading] = useState(false)
    const [fileList, setFileList] = useState<UploadFile[]>([])

    useEffect(() => {
        if (open) {
            setUploading(false)
            setFileList([])
        }
    }, [open])

    const accepts = (file: File) => {
        if (extensions && !extensions.some(extension => file.name.toLowerCase().endsWith(extension))) {
            notification.info({ title: t('project:update_module_modal.only_excel') })
            return false
        }
        return true
    }

    const file = fileList[0]?.originFileObj
    const currentName = path.split('/').pop()
    const nameDiffers = !!file && !!currentName && file.name !== currentName

    const update = async () => {
        if (!file) {
            return
        }
        setUploading(true)
        try {
            await replaceFile(projectId, path, file)
            notification.success({ title: t('repository:browser.files.update_succeeded', { name: currentName }) })
            onUpdated()
            onClose()
        } catch (e) {
            notification.error({
                title: t('repository:browser.files.update_failed'),
                description: errorMessage(e),
            })
        } finally {
            setUploading(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            cancelButtonProps={{ disabled: uploading }}
            cancelText={t('common:btn.cancel')}
            okButtonProps={{ 'data-testid': 'update-file-submit', disabled: !file, loading: uploading }}
            okText={t('project:update_module_modal.confirm_button')}
            onCancel={onClose}
            onOk={update}
            open={open}
            title={<Space><UploadOutlined />{title}</Space>}
        >
            <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                <FileDropzone
                    accept={extensions}
                    data-testid="update-file-dragger"
                    fileList={fileList}
                    hint={t('project:update_module_modal.hint')}
                    onChange={setFileList}
                    validate={accepts}
                />
                {nameDiffers && (
                    <Alert
                        data-testid="update-file-name-warning"
                        title={t('project:update_module_modal.name_differs', { file: currentName })}
                        type="warning"
                    />
                )}
            </Space>
        </Modal>
    )
}

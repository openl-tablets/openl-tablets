import React, { useCallback, useEffect, useState } from 'react'
import { Button, Modal, Space, Typography } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { deleteProjectFile } from 'services/projects'

/**
 * Detail passed from the legacy JSF repository page via the {@code openDeleteFileModal} event.
 *
 * The projectId is the REST id computed by the backend; the path is the file or folder
 * path relative to the project root. The deletion is staged in the project working copy.
 */
interface DeleteFileModalDetail {
    projectId: string
    path: string
    name: string
    isFolder: boolean
    onSuccess?: () => void
}

export const DeleteFileModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<DeleteFileModalDetail>('openDeleteFileModal')
    const [visible, setVisible] = useState(false)
    const [deleting, setDeleting] = useState(false)

    useEffect(() => {
        setVisible(!!(detail && Object.keys(detail).length > 0))
        setDeleting(false)
    }, [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openDeleteFileModal', { detail: null }))
    }, [])

    const handleDelete = useCallback(async () => {
        if (!detail) {
            return
        }
        setDeleting(true)
        try {
            const deleted = await deleteProjectFile(detail.projectId, detail.path, detail.name, detail.isFolder)
            if (deleted) {
                handleClose()
                detail.onSuccess?.()
            }
        } finally {
            setDeleting(false)
        }
    }, [detail, handleClose])

    const confirmKey = detail?.isFolder ? 'confirm_folder' : 'confirm_file'

    return (
        <Modal
            destroyOnHidden
            onCancel={handleClose}
            open={visible}
            footer={[
                <Button key="cancel" disabled={deleting} onClick={handleClose}>
                    {t('common:btn.cancel')}
                </Button>,
                <Button
                    key="delete"
                    danger
                    loading={deleting}
                    onClick={handleDelete}
                    type="primary"
                >
                    {t('repository:delete_file_modal.confirm_button')}
                </Button>,
            ]}
            title={(
                <Space>
                    <DeleteOutlined />
                    {t('repository:delete_file_modal.title')}
                </Space>
            )}
        >
            <Typography.Paragraph>
                {t(`repository:delete_file_modal.${confirmKey}`, { name: detail?.name })}
            </Typography.Paragraph>
        </Modal>
    )
}

import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Modal, notification, Space, Upload } from 'antd'
import type { UploadFile, UploadProps } from 'antd'
import { InboxOutlined, UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { updateModuleFile } from 'services/projects'

/**
 * Detail passed from the legacy JSF editor shell via the {@code openUpdateModuleModal} event.
 */
export interface UpdateModuleModalDetail {
    projectId: string
    /** Project-relative path of the module's rules file, e.g. "rules/Main.xlsx". */
    modulePath: string
    /** Runs after a successful upload, e.g. to reload the editor page. */
    onSuccess?: () => void
}

const MODULE_EXTENSIONS = ['.xls', '.xlsx', '.xlsm']

/**
 * Dialog replacing the open module's rules file through the project files REST API. It replaces
 * the RichFaces "Update module" popup with an Ant Design modal mounted once in
 * {@link DefaultLayout}.
 *
 * The user uploads a single Excel file which overwrites the module file at its current path.
 * A warning is shown when the selected file name differs from the module file name, so an
 * accidental pick of another module is noticed before the update.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openUpdateModuleModal', {detail: {projectId, modulePath}}))
 */
export const UpdateModuleModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<UpdateModuleModalDetail>('openUpdateModuleModal')
    const [visible, setVisible] = useState(false)
    const [uploading, setUploading] = useState(false)
    const [fileList, setFileList] = useState<UploadFile[]>([])

    useEffect(() => {
        setVisible(!!(detail && Object.keys(detail).length > 0))
        setUploading(false)
        setFileList([])
    }, [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openUpdateModuleModal', { detail: null }))
    }, [])

    // Uploads are sent manually on OK: a valid pick stays in the controlled list (false),
    // a non-Excel file is reported and dropped.
    const beforeUpload = useCallback<NonNullable<UploadProps['beforeUpload']>>(file => {
        if (!MODULE_EXTENSIONS.some(extension => file.name.toLowerCase().endsWith(extension))) {
            notification.info({ title: t('project:update_module_modal.only_excel') })
            return Upload.LIST_IGNORE
        }
        return false
    }, [t])

    const file = fileList[0]?.originFileObj
    const moduleFileName = detail?.modulePath.split('/').pop()
    const nameDiffers = !!file && !!moduleFileName && file.name !== moduleFileName

    const handleUpdate = async () => {
        if (!detail || !file) {
            return
        }
        setUploading(true)
        try {
            if (await updateModuleFile(detail.projectId, detail.modulePath, file)) {
                handleClose()
                detail.onSuccess?.()
            }
        } finally {
            setUploading(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            cancelButtonProps={{ disabled: uploading }}
            cancelText={t('common:btn.cancel')}
            okButtonProps={{ disabled: !file, loading: uploading }}
            okText={t('project:update_module_modal.confirm_button')}
            onCancel={handleClose}
            onOk={handleUpdate}
            open={visible}
            title={(
                <Space>
                    <UploadOutlined />
                    {t('project:update_module_modal.title')}
                </Space>
            )}
        >
            <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                <Upload.Dragger
                    accept={MODULE_EXTENSIONS.join(',')}
                    beforeUpload={beforeUpload}
                    fileList={fileList}
                    maxCount={1}
                    onChange={info => setFileList(info.fileList)}
                >
                    <p className="ant-upload-drag-icon">
                        <InboxOutlined />
                    </p>
                    <p className="ant-upload-text">
                        {t('project:update_module_modal.hint')}
                    </p>
                </Upload.Dragger>
                {nameDiffers && (
                    <Alert
                        data-testid="update-module-name-warning"
                        title={t('project:update_module_modal.name_differs', { file: moduleFileName })}
                        type="warning"
                    />
                )}
            </Space>
        </Modal>
    )
}

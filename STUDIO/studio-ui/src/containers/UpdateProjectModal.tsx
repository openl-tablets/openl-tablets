import React, { useCallback, useEffect, useState } from 'react'
import { Modal, notification, Segmented, Space, Typography, Upload } from 'antd'
import type { UploadFile, UploadProps } from 'antd'
import { InboxOutlined, UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { type ProjectUploadEntry, updateProjectFromFiles, updateProjectFromZip } from 'services/projects'

/**
 * Detail passed from the legacy JSF editor shell via the {@code openUpdateProjectModal} event.
 */
export interface UpdateProjectModalDetail {
    projectId: string
    projectName: string
    /** Runs after a successful upload, e.g. to reload the editor page. */
    onSuccess?: () => void
}

type UploadSource = 'zip' | 'folder'

/**
 * Maps the picked files to project-relative upload entries.
 *
 * Folder pickers report each file path starting with the picked folder itself. When every file
 * sits under one common root folder, that root is stripped so the folder's content lands at the
 * project root. Loose files (without a folder path) keep their plain names.
 */
export const toUploadEntries = (files: File[]): ProjectUploadEntry[] => {
    const relativePath = (file: File): string => (file.webkitRelativePath || file.name).replaceAll('\\', '/')
    const roots = new Set(files.map(file => {
        const path = relativePath(file)
        return path.includes('/') ? path.slice(0, path.indexOf('/')) : null
    }))
    const stripRoot = roots.size === 1 && !roots.has(null)
    return files.map(file => {
        const path = relativePath(file)
        return { path: stripRoot ? path.slice(path.indexOf('/') + 1) : path, file }
    })
}

/**
 * Dialog updating the open project's content through the project files REST API. It replaces
 * the RichFaces "Update project" popup with an Ant Design modal mounted once in
 * {@link DefaultLayout}.
 *
 * The user uploads either a zip archive expanded at the project root, or a folder whose files
 * are sent with their relative paths. The upload replaces the project content: files with the
 * same path are overwritten and project files absent from the upload are deleted.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openUpdateProjectModal', {detail: {projectId, projectName}}))
 */
export const UpdateProjectModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<UpdateProjectModalDetail>('openUpdateProjectModal')
    const [visible, setVisible] = useState(false)
    const [uploading, setUploading] = useState(false)
    const [source, setSource] = useState<UploadSource>('zip')
    const [fileList, setFileList] = useState<UploadFile[]>([])

    useEffect(() => {
        setVisible(!!(detail && Object.keys(detail).length > 0))
        setUploading(false)
        setSource('zip')
        setFileList([])
    }, [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openUpdateProjectModal', { detail: null }))
    }, [])

    const handleSourceChange = useCallback((value: UploadSource) => {
        setSource(value)
        setFileList([])
    }, [])

    // Uploads are sent manually on OK: a valid pick stays in the controlled list (false),
    // a wrong archive type is reported and dropped.
    const beforeUpload = useCallback<NonNullable<UploadProps['beforeUpload']>>(file => {
        if (source === 'zip' && !file.name.toLowerCase().endsWith('.zip')) {
            notification.info({ title: t('project:update_project_modal.only_zip') })
            return Upload.LIST_IGNORE
        }
        return false
    }, [source, t])

    const files = fileList
        .map(item => item.originFileObj)
        .filter((file): file is NonNullable<UploadFile['originFileObj']> => !!file)

    const handleUpdate = async () => {
        const archive = files[0]
        if (!detail || !archive) {
            return
        }
        setUploading(true)
        try {
            const updated = source === 'zip'
                ? await updateProjectFromZip(detail.projectId, detail.projectName, archive)
                : await updateProjectFromFiles(detail.projectId, detail.projectName, toUploadEntries(files))
            if (updated) {
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
            okButtonProps={{ disabled: files.length === 0, loading: uploading }}
            okText={t('project:update_project_modal.confirm_button')}
            onCancel={handleClose}
            onOk={handleUpdate}
            open={visible}
            title={(
                <Space>
                    <UploadOutlined />
                    {t('project:update_project_modal.title')}
                </Space>
            )}
        >
            <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                <Segmented<UploadSource>
                    data-testid="update-project-source"
                    onChange={handleSourceChange}
                    value={source}
                    options={[
                        { label: t('project:update_project_modal.source_zip'), value: 'zip' },
                        { label: t('project:update_project_modal.source_folder'), value: 'folder' },
                    ]}
                />
                <Upload.Dragger
                    beforeUpload={beforeUpload}
                    fileList={fileList}
                    onChange={info => setFileList(info.fileList)}
                    showUploadList={source === 'zip'}
                    {...(source === 'zip'
                        ? { accept: '.zip', maxCount: 1 }
                        : { directory: true, multiple: true })}
                >
                    <p className="ant-upload-drag-icon">
                        <InboxOutlined />
                    </p>
                    <p className="ant-upload-text">
                        {t(`project:update_project_modal.${source === 'zip' ? 'zip_hint' : 'folder_hint'}`)}
                    </p>
                </Upload.Dragger>
                {source === 'folder' && files.length > 0 && (
                    <Typography.Text data-testid="update-project-folder-summary">
                        {t('project:update_project_modal.folder_selected', { count: files.length })}
                    </Typography.Text>
                )}
                <Typography.Text type="secondary">
                    {t('project:update_project_modal.replace_note')}
                </Typography.Text>
            </Space>
        </Modal>
    )
}

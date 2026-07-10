import { useRef, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, Modal, notification, Space, Tooltip } from 'antd'
import { FileAddOutlined, FolderAddOutlined, UploadOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { createTextFile, uploadFiles } from '../../services/files'

const useStyles = createStyles(({ css, token }) => ({
    toolbar: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingSM}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorSplit};
    `,
    search: css`
        flex: 1;
        min-width: 0;
    `,
    actions: css`
        flex: none;
        margin-left: auto;
    `,
    createTarget: css`
        margin-bottom: ${token.marginSM}px;
    `,
}))

interface FilesToolbarProps {
    projectId: string
    canWrite: boolean
    filter: string
    /** Folder path used as a base for create and upload actions. Empty means project root. */
    targetFolder?: string
    onFilterChange: (value: string) => void
    onChanged: () => void
    /**
     * Adds a client-side (virtual) folder to the tree. An empty folder is never persisted on its own;
     * it becomes real once a file is created inside it, which creates the whole folder chain.
     */
    onCreateFolder: (path: string) => void
}

type CreateKind = 'folder' | 'text-file'

const normalizePath = (path: string): string => path.trim().replaceAll('\\', '/').replace(/^\/+|\/+$/g, '')

const joinPath = (basePath: string, path: string): string => {
    const base = normalizePath(basePath)
    const relative = normalizePath(path)
    return base && relative ? `${base}/${relative}` : base || relative
}

const uploadTargetPath = (basePath: string): string => {
    const normalized = normalizePath(basePath)
    return normalized ? `${normalized}/` : ''
}

/**
 * Actions above the project file tree: filter files, plus a right-aligned group of icon buttons to create
 * folders or text files and upload files. Write actions use the selected node's folder as their base; the list
 * refreshes on success.
 */
export const FilesToolbar = ({
    projectId,
    canWrite,
    filter,
    targetFolder = '',
    onFilterChange,
    onChanged,
    onCreateFolder,
}: FilesToolbarProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const fileInputRef = useRef<HTMLInputElement>(null)
    const [uploading, setUploading] = useState(false)
    const [createKind, setCreateKind] = useState<CreateKind | null>(null)
    const [createPath, setCreatePath] = useState('')
    const [createBasePath, setCreateBasePath] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const normalizedTargetFolder = normalizePath(targetFolder)

    const upload = async (files: File[]) => {
        setUploading(true)
        try {
            await uploadFiles(projectId, uploadTargetPath(normalizedTargetFolder), files)
            onChanged()
        } catch (e) {
            notification.error({ title: t('browser.files.upload_failed'), description: errorMessage(e) })
        } finally {
            setUploading(false)
        }
    }

    const onFilesPicked = (event: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(event.target.files ?? [])
        if (files.length) {
            void upload(files)
        }
        event.target.value = ''
    }

    const openCreate = (kind: CreateKind) => {
        setCreateKind(kind)
        setCreateBasePath(normalizedTargetFolder)
        setCreatePath('')
    }

    const closeCreate = () => {
        setCreateKind(null)
        setCreatePath('')
        setCreateBasePath('')
    }

    const create = async () => {
        if (!createKind) {
            return
        }
        const path = joinPath(createBasePath, createPath)
        if (!path) {
            return
        }
        // An empty folder is only a UI convenience — add it virtually and let the file creation persist
        // the folder chain later. Only a text file hits the server here.
        if (createKind === 'folder') {
            onCreateFolder(path)
            closeCreate()
            return
        }
        setSubmitting(true)
        try {
            await createTextFile(projectId, path)
            closeCreate()
            onChanged()
        } catch (e) {
            notification.error({ title: t('browser.files.text_file_failed'), description: errorMessage(e) })
        } finally {
            setSubmitting(false)
        }
    }

    const createTitle = createKind === 'folder' ? t('browser.files.new_folder') : t('browser.files.new_text_file')
    const createInputTestId = createKind === 'folder' ? 'files-folder-path' : 'files-text-file-path'
    const createSubmitTestId = createKind === 'folder' ? 'files-folder-submit' : 'files-text-file-submit'
    const activeCreateKind = createKind ?? 'folder'

    return (
        <div className={styles.toolbar}>
            <Input.Search
                allowClear
                className={styles.search}
                data-testid="files-search"
                onChange={event => onFilterChange(event.target.value)}
                placeholder={t('browser.files.search')}
                value={filter}
            />
            {canWrite && (
                <Space.Compact className={styles.actions}>
                    <Tooltip title={t('browser.files.new_folder')}>
                        <Button
                            data-testid="files-new-folder"
                            icon={<FolderAddOutlined />}
                            onClick={() => openCreate('folder')}
                            size="small"
                        />
                    </Tooltip>
                    <Tooltip title={t('browser.files.new_text_file')}>
                        <Button
                            data-testid="files-new-text-file"
                            icon={<FileAddOutlined />}
                            onClick={() => openCreate('text-file')}
                            size="small"
                        />
                    </Tooltip>
                    <Tooltip title={t('browser.files.upload')}>
                        <Button
                            data-testid="files-upload"
                            icon={<UploadOutlined />}
                            loading={uploading}
                            onClick={() => fileInputRef.current?.click()}
                            size="small"
                        />
                    </Tooltip>
                </Space.Compact>
            )}
            {canWrite && (
                <input
                    ref={fileInputRef}
                    hidden
                    multiple
                    data-testid="files-upload-input"
                    onChange={onFilesPicked}
                    type="file"
                />
            )}
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okButtonProps={{ 'data-testid': createSubmitTestId }}
                onCancel={closeCreate}
                onOk={create}
                open={createKind !== null}
                title={createTitle}
            >
                {createBasePath && (
                    <Alert
                        showIcon
                        className={styles.createTarget}
                        message={t(`browser.files.create_target.${activeCreateKind}`, { path: createBasePath })}
                        type="info"
                        action={(
                            <Button onClick={() => setCreateBasePath('')} size="small">
                                {t('browser.files.create_in_root')}
                            </Button>
                        )}
                    />
                )}
                <Input
                    data-testid={createInputTestId}
                    onChange={event => setCreatePath(event.target.value)}
                    onPressEnter={create}
                    placeholder={t(`browser.files.create_placeholder.${activeCreateKind}`)}
                    value={createPath}
                />
            </Modal>
        </div>
    )
}

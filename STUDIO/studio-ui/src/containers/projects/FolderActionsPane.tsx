import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Space } from 'antd'
import { CopyOutlined, DeleteOutlined, DownloadOutlined, FolderOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { downloadFolder } from '../../services/files'
import { useSharedStyles } from './sharedStyles'
import { CopyFileModal } from './CopyFileModal'
import { DeleteFileModal } from './DeleteFileModal'

const useStyles = createStyles(({ css, token }) => ({
    path: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;

        .anticon {
            flex: none;
            color: ${token.colorTextTertiary};
        }
    `,
    actions: css`
        margin-left: auto;
        flex: none;
    `,
}))

interface FolderActionsPaneProps {
    projectId: string
    /** Folders of the project, offered as the destination when copying. */
    folders: string[]
    path: string
    canWrite: boolean
    canDelete: boolean
    onChanged: () => void
    onDeleted: () => void
    /** A client-only folder not yet on the server: it has no content to download, copy or delete. */
    virtual?: boolean
    /** Drops a virtual folder from the tree (client-side only, no server call). */
    onRemoveVirtual?: () => void
}

/**
 * The right pane of the Files tab when a folder is selected. A folder has no content to show, so it
 * offers folder-level actions instead: download the folder as a zip archive, copy it, or delete it.
 *
 * A virtual folder exists only in the browser until a file is added inside it, so it offers no server
 * actions — only a hint to add a file and a way to drop the empty folder from the tree.
 */
export const FolderActionsPane = ({ projectId, folders, path, canWrite, canDelete, onChanged, onDeleted, virtual, onRemoveVirtual }: FolderActionsPaneProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [copying, setCopying] = useState(false)
    const [deleting, setDeleting] = useState(false)

    return (
        <div className={shared.paneColumn} data-testid="folder-actions">
            <div className={shared.paneHeader}>
                <span className={cx(shared.valueText, shared.ellipsis, styles.path)}><FolderOutlined />{path}</span>
                <Space className={styles.actions}>
                    {virtual ? (
                        <Button danger data-testid="folder-remove-virtual" icon={<DeleteOutlined />} onClick={onRemoveVirtual} size="small">
                            {t('browser.files.remove_empty_folder')}
                        </Button>
                    ) : (
                        <>
                            <Button
                                data-testid="folder-download"
                                icon={<DownloadOutlined />}
                                onClick={() => downloadFolder(projectId, path)}
                                size="small"
                            >
                                {t('browser.files.download_archive')}
                            </Button>
                            {canWrite && (
                                <Button
                                    data-testid="folder-copy"
                                    icon={<CopyOutlined />}
                                    onClick={() => setCopying(true)}
                                    size="small"
                                >
                                    {t('browser.files.copy')}
                                </Button>
                            )}
                            {canDelete && (
                                <Button danger data-testid="folder-delete" icon={<DeleteOutlined />} onClick={() => setDeleting(true)} size="small">
                                    {t('browser.files.delete')}
                                </Button>
                            )}
                        </>
                    )}
                </Space>
            </div>
            <div className={shared.panePlaceholder} data-testid="folder-actions-body">
                <FolderOutlined />
                <span>{virtual ? t('browser.files.empty_folder_hint') : t('browser.files.folder_hint')}</span>
            </div>
            <CopyFileModal
                folder
                folders={folders}
                onClose={() => setCopying(false)}
                onCopied={onChanged}
                open={copying}
                path={path}
                projectId={projectId}
            />
            <DeleteFileModal
                folder
                onClose={() => setDeleting(false)}
                onDeleted={onDeleted}
                open={deleting}
                path={path}
                projectId={projectId}
            />
        </div>
    )
}

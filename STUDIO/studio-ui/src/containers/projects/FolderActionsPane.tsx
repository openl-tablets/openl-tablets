import { useState } from 'react'
import { errorMessage as message } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Button, Input, Modal, notification, Popconfirm, Space, Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined, DownloadOutlined, FolderOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { copyFile, deleteFile, downloadFolder } from '../../services/files'
import { ELLIPSIS, MOCKUP } from './projectsTheme'

const useStyles = createStyles(({ css, token }) => ({
    pane: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    toolbar: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    path: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        font-family: ${MOCKUP.fontMono};
        font-size: 12px;
        ${ELLIPSIS}

        .anticon {
            flex: none;
            color: ${token.colorTextTertiary};
        }
    `,
    actions: css`
        margin-left: auto;
        flex: none;
    `,
    body: css`
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: ${token.colorTextTertiary};

        .anticon {
            font-size: 32px;
            color: ${token.colorTextQuaternary};
        }
    `,
}))

/** Proposes a sibling path for a copy, appending "-copy" to the folder name. */
const suggestCopyPath = (path: string): string => `${path}-copy`

interface FolderActionsPaneProps {
    projectId: string
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
export const FolderActionsPane = ({ projectId, path, canWrite, canDelete, onChanged, onDeleted, virtual, onRemoveVirtual }: FolderActionsPaneProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const [copying, setCopying] = useState<string | null>(null)
    const [copyBusy, setCopyBusy] = useState(false)

    const remove = async () => {
        try {
            await deleteFile(projectId, path)
            onDeleted()
        } catch (e) {
            notification.error({ title: t('browser.files.delete_failed'), description: message(e) })
        }
    }

    const doCopy = async () => {
        if (!copying?.trim() || copying.trim() === path) {
            setCopying(null)
            return
        }
        setCopyBusy(true)
        try {
            await copyFile(projectId, path, copying.trim())
            setCopying(null)
            onChanged()
        } catch (e) {
            notification.error({ title: t('browser.files.copy_failed'), description: message(e) })
        } finally {
            setCopyBusy(false)
        }
    }

    return (
        <div className={styles.pane} data-testid="folder-actions">
            <div className={styles.toolbar}>
                <span className={styles.path}><FolderOutlined />{path}</span>
                <Space.Compact className={styles.actions}>
                    {virtual ? (
                        <Tooltip title={t('browser.files.remove_empty_folder')}>
                            <Button danger data-testid="folder-remove-virtual" icon={<DeleteOutlined />} onClick={onRemoveVirtual} size="small" />
                        </Tooltip>
                    ) : (
                        <>
                            <Tooltip title={t('browser.files.download_archive')}>
                                <Button data-testid="folder-download" icon={<DownloadOutlined />} onClick={() => downloadFolder(projectId, path)} size="small" />
                            </Tooltip>
                            {canWrite && (
                                <Tooltip title={t('browser.files.copy')}>
                                    <Button data-testid="folder-copy" icon={<CopyOutlined />} onClick={() => setCopying(suggestCopyPath(path))} size="small" />
                                </Tooltip>
                            )}
                            {canDelete && (
                                <Popconfirm onConfirm={remove} title={t('browser.files.delete_confirm', { path })}>
                                    <Button danger data-testid="folder-delete" icon={<DeleteOutlined />} size="small" />
                                </Popconfirm>
                            )}
                        </>
                    )}
                </Space.Compact>
            </div>
            <div className={styles.body} data-testid="folder-actions-body">
                <FolderOutlined />
                <span>{virtual ? t('browser.files.empty_folder_hint') : t('browser.files.folder_hint')}</span>
            </div>
            <Modal
                destroyOnHidden
                confirmLoading={copyBusy}
                okButtonProps={{ 'data-testid': 'folder-copy-submit' }}
                onCancel={() => setCopying(null)}
                onOk={doCopy}
                open={copying !== null}
                title={t('browser.files.copy_folder_title')}
            >
                <Input
                    data-testid="folder-copy-input"
                    onChange={event => setCopying(event.target.value)}
                    onPressEnter={doCopy}
                    value={copying ?? ''}
                />
            </Modal>
        </div>
    )
}

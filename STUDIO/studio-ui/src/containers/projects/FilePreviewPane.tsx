import { lazy, Suspense, useEffect, useState } from 'react'
import { errorMessage as message } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Dropdown, Modal, Skeleton, Space, Tag, type MenuProps } from 'antd'
import {
    CloseOutlined,
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    DragOutlined,
    EditOutlined,
    FolderOpenOutlined,
    FontColorsOutlined,
    MoreOutlined,
    SaveOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'
import { useSharedStyles } from './sharedStyles'
import { CopyFileModal } from './CopyFileModal'
import { DeleteFileModal } from './DeleteFileModal'
import { MoveFileModal } from './MoveFileModal'
import { UpdateFileModal } from './UpdateFileModal'

// The code editor pulls in CodeMirror; load it only when a text file is actually opened.
const CodeEditor = lazy(() => import('./CodeEditor').then(module => ({ default: module.CodeEditor })))

const useStyles = createStyles(({ css, token }) => ({
    badge: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 10px;
    `,
    actions: css`
        margin-left: auto;
        flex: none;
    `,
    /** The file path fills the header and clips, so the actions beside it never leave the pane. */
    path: css`
        flex: 1 1 auto;
        min-width: 0;
    `,
    body: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: auto;
    `,
    error: css`
        margin: 12px;
    `,
}))

interface FilePreviewPaneProps {
    projectId: string
    /** Folders of the project, offered as the destination when copying. */
    folders: string[]
    repositoryId: string
    projectName: string
    branch?: string | null
    path: string | null
    canWrite: boolean
    canDelete: boolean
    onChanged: () => void
    onDeleted: () => void
    /** The open file was renamed or moved: follow it to its new path so the pane does not 404 on the old one. */
    onMoved: (newPath: string) => void
    /** Bumped when the project reloads (save, close, …), forcing content to refetch and edit mode to reset. */
    reloadToken?: number
}

interface FileSelection {
    projectId: string
    repositoryId: string
    projectName: string
    branch: string | null
    path: string | null
    reloadToken: number | undefined
}

/**
 * The right pane of the Files tab: shows the selected file. Text files open read-only in a
 * syntax-highlighted viewer; the pencil switches to an editable view with save and cancel. Binary files
 * (e.g. .xlsx) offer a download. File-level actions live here, grouped in one button bar, rather than on
 * each tree row.
 */
export const FilePreviewPane = ({ projectId, repositoryId, projectName, branch, path, folders, canWrite, canDelete, onChanged, onDeleted, onMoved, reloadToken }: FilePreviewPaneProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [activeSelection, setActiveSelection] = useState<FileSelection>({
        branch: branch ?? null,
        path,
        projectId,
        projectName,
        reloadToken,
        repositoryId,
    })
    const [pendingSelection, setPendingSelection] = useState<FileSelection | null>(null)
    const [content, setContent] = useState('')
    const [original, setOriginal] = useState('')
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [updating, setUpdating] = useState(false)
    const [copying, setCopying] = useState(false)
    const [moving, setMoving] = useState<'move' | 'rename' | null>(null)
    const [deleting, setDeleting] = useState(false)
    const [editing, setEditing] = useState(false)

    const activePath = activeSelection.path
    const editable = !!activePath && isEditableTextFile(activePath)
    const dirty = content !== original
    const incomingSelection: FileSelection = {
        branch: branch ?? null,
        path,
        projectId,
        projectName,
        reloadToken,
        repositoryId,
    }

    useEffect(() => {
        const sameSelection = activeSelection.branch === incomingSelection.branch &&
            activeSelection.path === incomingSelection.path &&
            activeSelection.projectId === incomingSelection.projectId &&
            activeSelection.projectName === incomingSelection.projectName &&
            activeSelection.reloadToken === incomingSelection.reloadToken &&
            activeSelection.repositoryId === incomingSelection.repositoryId
        if (sameSelection) {
            return
        }
        if (dirty) {
            setPendingSelection(incomingSelection)
            return
        }
        setPendingSelection(null)
        setActiveSelection(incomingSelection)
    }, [
        activeSelection.branch,
        activeSelection.path,
        activeSelection.projectId,
        activeSelection.projectName,
        activeSelection.reloadToken,
        activeSelection.repositoryId,
        dirty,
        incomingSelection.branch,
        incomingSelection.path,
        incomingSelection.projectId,
        incomingSelection.projectName,
        incomingSelection.reloadToken,
        incomingSelection.repositoryId,
    ])

    // A newly accepted file — or a project reload (save/close) — returns the pane to read-only view.
    useEffect(() => {
        setEditing(false)
        setError(null)
    }, [activePath, activeSelection.reloadToken])

    useEffect(() => {
        if (!activePath || !editable) {
            setContent('')
            setOriginal('')
            setLoading(false)
            setError(null)
            return
        }
        let cancelled = false
        setLoading(true)
        setError(null)
        getFileContent(activeSelection.projectId, activePath)
            .then(text => {
                if (!cancelled) {
                    setContent(text)
                    setOriginal(text)
                }
            })
            .catch(e => { if (!cancelled) setError(message(e)) })
            .finally(() => { if (!cancelled) setLoading(false) })
        return () => { cancelled = true }
    }, [activeSelection.projectId, activePath, editable, activeSelection.reloadToken])

    const save = async () => {
        if (!activePath) {
            return
        }
        setSaving(true)
        setError(null)
        try {
            await updateFileContent(activeSelection.projectId, activePath, content)
            setOriginal(content)
            setEditing(false)
            onChanged()
        } catch (e) {
            setError(message(e))
        } finally {
            setSaving(false)
        }
    }

    const cancelEdit = () => {
        setContent(original)
        setEditing(false)
        setError(null)
    }

    const keepCurrentFile = () => {
        setPendingSelection(null)
    }

    const discardCurrentFile = () => {
        if (pendingSelection) {
            setPendingSelection(null)
            setActiveSelection(pendingSelection)
        }
    }

    if (!activePath) {
        return (
            <div className={shared.paneColumn}>
                <div className={shared.panePlaceholder} data-testid="file-preview-empty">
                    <FolderOpenOutlined />
                    <span>{t('browser.files.select_hint')}</span>
                </div>
            </div>
        )
    }

    // Edit and Export stay to hand; the rarer file actions gather under one menu, as they do elsewhere.
    const menuItems: MenuProps['items'] = [
        ...(canWrite ? [{ key: 'rename', icon: <FontColorsOutlined />, label: t('browser.files.rename') }] : []),
        ...(canWrite ? [{ key: 'move', icon: <DragOutlined />, label: t('browser.files.move') }] : []),
        ...(canWrite ? [{ key: 'update', icon: <UploadOutlined />, label: t('browser.files.update') }] : []),
        ...(canWrite ? [{ key: 'copy', icon: <CopyOutlined />, label: t('browser.files.copy') }] : []),
        ...(canDelete ? [{ key: 'delete', danger: true, icon: <DeleteOutlined />, label: t('browser.files.delete') }] : []),
    ]
    const runFromMenu: MenuProps['onClick'] = ({ key }) => {
        if (key === 'rename') {
            setMoving('rename')
        } else if (key === 'move') {
            setMoving('move')
        } else if (key === 'update') {
            setUpdating(true)
        } else if (key === 'copy') {
            setCopying(true)
        } else if (key === 'delete') {
            setDeleting(true)
        }
    }

    return (
        <div className={shared.paneColumn} data-testid="file-preview">
            <div className={shared.paneHeader}>
                <span className={cx(shared.mono, shared.ellipsis, styles.path)}>{activePath}</span>
                <Tag className={styles.badge}>
                    {!editable ? t('browser.files.binary') : editing ? t('browser.files.text_editor') : t('browser.editor.read_only')}
                </Tag>
                <Space className={styles.actions}>
                    {editing ? (
                        <>
                            <Button
                                data-testid="file-save"
                                disabled={!dirty}
                                icon={<SaveOutlined />}
                                loading={saving}
                                onClick={save}
                                size="small"
                                type="primary"
                            >
                                {t('browser.editor.save')}
                            </Button>
                            <Button data-testid="file-cancel" icon={<CloseOutlined />} onClick={cancelEdit} size="small">
                                {t('browser.editor.cancel')}
                            </Button>
                        </>
                    ) : (
                        <>
                            {editable && canWrite && (
                                <Button data-testid="file-edit" icon={<EditOutlined />} onClick={() => setEditing(true)} size="small">
                                    {t('browser.editor.edit')}
                                </Button>
                            )}
                            <Button
                                data-testid="file-download"
                                icon={<DownloadOutlined />}
                                onClick={() => downloadFile(activeSelection.projectId, activePath)}
                                size="small"
                            >
                                {t('browser.files.download')}
                            </Button>
                            {menuItems.length > 0 && (
                                <Dropdown menu={{ items: menuItems, onClick: runFromMenu }} trigger={['click']}>
                                    <Button
                                        aria-label={t('browser.files.more_actions')}
                                        data-testid="file-actions"
                                        icon={<MoreOutlined />}
                                        size="small"
                                    />
                                </Dropdown>
                            )}
                        </>
                    )}
                </Space>
            </div>
            {error && <Alert showIcon className={styles.error} data-testid="file-preview-error" title={error} type="error" />}
            {editable ? (
                loading
                    ? <Skeleton active paragraph={{ rows: 10 }} style={{ padding: 16 }} title={false} />
                    : (
                        <div className={styles.body}>
                            <Suspense fallback={<Skeleton active paragraph={{ rows: 10 }} style={{ padding: 16 }} title={false} />}>
                                <CodeEditor onChange={setContent} path={activePath} readOnly={!editing} value={content} />
                            </Suspense>
                        </div>
                    )
            ) : (
                <div className={shared.panePlaceholder} data-testid="file-preview-binary">
                    <span>{t('browser.files.binary_hint')}</span>
                    <Button icon={<DownloadOutlined />} onClick={() => downloadFile(activeSelection.projectId, activePath)}>
                        {t('browser.files.download')}
                    </Button>
                </div>
            )}
            <Modal
                destroyOnHidden
                cancelButtonProps={{ 'data-testid': 'file-discard-cancel' }}
                okButtonProps={{ 'data-testid': 'file-discard-confirm' }}
                okText={t('browser.files.discard_changes')}
                onCancel={keepCurrentFile}
                onOk={discardCurrentFile}
                open={pendingSelection !== null}
                title={t('browser.files.discard_changes_title')}
            >
                {t('browser.files.discard_changes_desc')}
            </Modal>
            {activePath && (
                <CopyFileModal
                    folders={folders}
                    onClose={() => setCopying(false)}
                    onCopied={onChanged}
                    open={copying}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && moving && (
                <MoveFileModal
                    open
                    folders={folders}
                    mode={moving}
                    onClose={() => setMoving(null)}
                    onMoved={onMoved}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && (
                <DeleteFileModal
                    onClose={() => setDeleting(false)}
                    onDeleted={onDeleted}
                    open={deleting}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && (
                <UpdateFileModal
                    onClose={() => setUpdating(false)}
                    onUpdated={onChanged}
                    open={updating}
                    path={activePath}
                    projectId={activeSelection.projectId}
                    title={t('browser.files.update_title', { name: activePath.split('/').pop() })}
                />
            )}
        </div>
    )
}

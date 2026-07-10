import { lazy, Suspense, useEffect, useState } from 'react'
import { errorMessage as message } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, Modal, notification, Popconfirm, Skeleton, Space, Tag, Tooltip } from 'antd'
import {
    CloseOutlined,
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    EditOutlined,
    FolderOpenOutlined,
    SaveOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    copyFile,
    deleteFile,
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'
import { ELLIPSIS, MOCKUP } from './projectsTheme'

// The code editor pulls in CodeMirror; load it only when a text file is actually opened.
const CodeEditor = lazy(() => import('./CodeEditor').then(module => ({ default: module.CodeEditor })))

const useStyles = createStyles(({ css, token }) => ({
    pane: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    empty: css`
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
    toolbar: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    path: css`
        font-family: ${MOCKUP.fontMono};
        font-size: 12px;
        ${ELLIPSIS}
    `,
    badge: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 10px;
    `,
    actions: css`
        margin-left: auto;
        flex: none;
    `,
    body: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    binary: css`
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        color: ${token.colorTextTertiary};
    `,
    error: css`
        margin: 12px;
    `,
}))


/** Proposes a sibling path for a copy, inserting "-copy" before the file extension. */
const suggestCopyPath = (path: string): string => {
    const slash = path.lastIndexOf('/')
    const dir = slash >= 0 ? path.slice(0, slash + 1) : ''
    const name = path.slice(slash + 1)
    const dot = name.lastIndexOf('.')
    return dot > 0 ? `${dir}${name.slice(0, dot)}-copy${name.slice(dot)}` : `${dir}${name}-copy`
}

interface FilePreviewPaneProps {
    projectId: string
    repositoryId: string
    projectName: string
    branch?: string | null
    path: string | null
    canWrite: boolean
    canDelete: boolean
    onChanged: () => void
    onDeleted: () => void
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
export const FilePreviewPane = ({ projectId, repositoryId, projectName, branch, path, canWrite, canDelete, onChanged, onDeleted, reloadToken }: FilePreviewPaneProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
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
    const [copying, setCopying] = useState<string | null>(null)
    const [copyBusy, setCopyBusy] = useState(false)
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

    const remove = async () => {
        if (!activePath) {
            return
        }
        try {
            await deleteFile(activeSelection.projectId, activePath)
            onDeleted()
        } catch (e) {
            notification.error({ title: t('browser.files.delete_failed'), description: message(e) })
        }
    }

    const doCopy = async () => {
        if (!activePath || !copying?.trim() || copying.trim() === activePath) {
            setCopying(null)
            return
        }
        setCopyBusy(true)
        try {
            await copyFile(activeSelection.projectId, activePath, copying.trim())
            setCopying(null)
            onChanged()
        } catch (e) {
            notification.error({ title: t('browser.files.copy_failed'), description: message(e) })
        } finally {
            setCopyBusy(false)
        }
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
            <div className={styles.pane}>
                <div className={styles.empty} data-testid="file-preview-empty">
                    <FolderOpenOutlined />
                    <span>{t('browser.files.select_hint')}</span>
                </div>
            </div>
        )
    }

    return (
        <div className={styles.pane} data-testid="file-preview">
            <div className={styles.toolbar}>
                <span className={styles.path}>{activePath}</span>
                <Tag className={styles.badge}>
                    {!editable ? t('browser.files.binary') : editing ? t('browser.files.text_editor') : t('browser.editor.read_only')}
                </Tag>
                <Space.Compact className={styles.actions}>
                    {editing ? (
                        <>
                            <Button data-testid="file-save" disabled={!dirty} icon={<SaveOutlined />} loading={saving} onClick={save} size="small" type="primary">
                                {t('browser.editor.save')}
                            </Button>
                            <Tooltip title={t('browser.editor.cancel')}>
                                <Button data-testid="file-cancel" icon={<CloseOutlined />} onClick={cancelEdit} size="small" />
                            </Tooltip>
                        </>
                    ) : (
                        <>
                            {editable && canWrite && (
                                <Tooltip title={t('browser.editor.edit')}>
                                    <Button data-testid="file-edit" icon={<EditOutlined />} onClick={() => setEditing(true)} size="small" />
                                </Tooltip>
                            )}
                            <Tooltip title={t('browser.files.download')}>
                                <Button
                                    data-testid="file-download"
                                    icon={<DownloadOutlined />}
                                    onClick={() => downloadFile(activeSelection.projectId, activePath)}
                                    size="small"
                                />
                            </Tooltip>
                            {canWrite && (
                                <Tooltip title={t('browser.files.copy')}>
                                    <Button data-testid="file-copy" icon={<CopyOutlined />} onClick={() => setCopying(suggestCopyPath(activePath))} size="small" />
                                </Tooltip>
                            )}
                            {canDelete && (
                                <Popconfirm onConfirm={remove} title={t('browser.files.delete_confirm', { path: activePath })}>
                                    <Button danger data-testid="file-delete" icon={<DeleteOutlined />} size="small" />
                                </Popconfirm>
                            )}
                        </>
                    )}
                </Space.Compact>
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
                <div className={styles.binary} data-testid="file-preview-binary">
                    <span>{t('browser.files.binary_hint')}</span>
                    <Button icon={<DownloadOutlined />} onClick={() => downloadFile(activeSelection.projectId, activePath)}>
                        {t('browser.files.download')}
                    </Button>
                </div>
            )}
            <Modal
                destroyOnHidden
                confirmLoading={copyBusy}
                okButtonProps={{ 'data-testid': 'file-copy-submit' }}
                onCancel={() => setCopying(null)}
                onOk={doCopy}
                open={copying !== null}
                title={t('browser.files.copy_title')}
            >
                <Input
                    data-testid="file-copy-input"
                    onChange={event => setCopying(event.target.value)}
                    onPressEnter={doCopy}
                    value={copying ?? ''}
                />
            </Modal>
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
        </div>
    )
}

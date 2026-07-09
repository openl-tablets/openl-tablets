import { useEffect, useState } from 'react'
import { errorMessage as message } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, Modal, Popconfirm, Skeleton, Space, Tag } from 'antd'
import { deleteFile, getFileContent, updateFileContent } from '../../services/files'

interface FileEditorModalProps {
    open: boolean
    projectId: string | null
    path: string | null
    canWrite: boolean
    canDelete: boolean
    onClose: () => void
    onSaved: () => void
    onDeleted: () => void
}


/**
 * Edit any text file in a project (Groovy scripts, .properties, XML/JSON/YAML, plain text) over the
 * files API. Loads the raw content on open, tracks unsaved changes, and saves the whole file back. When
 * the caller has no write access the content is shown read-only.
 */
export const FileEditorModal = ({ open, projectId, path, canWrite, canDelete, onClose, onSaved, onDeleted }: FileEditorModalProps) => {
    const { t } = useTranslation('repository')
    const [content, setContent] = useState('')
    const [original, setOriginal] = useState('')
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [removing, setRemoving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const dirty = content !== original

    useEffect(() => {
        if (!open || !projectId || !path) {
            return
        }
        let cancelled = false
        setLoading(true)
        setError(null)
        getFileContent(projectId, path)
            .then(text => {
                if (!cancelled) {
                    setContent(text)
                    setOriginal(text)
                }
            })
            .catch(e => {
                if (!cancelled) {
                    setError(message(e))
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false)
                }
            })
        return () => {
            cancelled = true
        }
    }, [open, projectId, path])

    const save = async () => {
        if (!projectId || !path) {
            return
        }
        setSaving(true)
        setError(null)
        try {
            await updateFileContent(projectId, path, content)
            setOriginal(content)
            onSaved()
        } catch (e) {
            setError(message(e))
        } finally {
            setSaving(false)
        }
    }

    const remove = async () => {
        if (!projectId || !path) {
            return
        }
        setRemoving(true)
        setError(null)
        try {
            await deleteFile(projectId, path)
            onDeleted()
        } catch (e) {
            setError(message(e))
        } finally {
            setRemoving(false)
        }
    }

    const busy = saving || removing

    const footer = [
        canDelete && (
            <Popconfirm key="delete" onConfirm={remove} title={t('browser.editor.delete_confirm')}>
                <Button danger data-testid="file-editor-delete" disabled={busy || loading} loading={removing}>
                    {t('browser.editor.delete')}
                </Button>
            </Popconfirm>
        ),
        <Button key="close" data-testid="file-editor-close" disabled={busy} onClick={onClose}>
            {t('browser.editor.close')}
        </Button>,
        canWrite && (
            <Button
                key="save"
                data-testid="file-editor-save"
                disabled={!dirty || loading || removing}
                loading={saving}
                onClick={save}
                type="primary"
            >
                {t('browser.editor.save')}
            </Button>
        ),
    ].filter(Boolean)

    return (
        <Modal
            destroyOnHidden
            footer={footer}
            onCancel={onClose}
            open={open}
            width={820}
            title={
                <Space>
                    <span data-testid="file-editor-title">{path}</span>
                    {!canWrite && <Tag>{t('browser.editor.read_only')}</Tag>}
                    {dirty && <Tag color="orange" data-testid="file-editor-dirty">{t('browser.editor.unsaved')}</Tag>}
                </Space>
            }
        >
            {loading
                ? <Skeleton active paragraph={{ rows: 8 }} title={false} />
                : (
                    <>
                        {error && <Alert showIcon data-testid="file-editor-error" style={{ marginBottom: 8 }} title={error} type="error" />}
                        <Input.TextArea
                            data-testid="file-editor-content"
                            onChange={event => setContent(event.target.value)}
                            readOnly={!canWrite}
                            rows={20}
                            spellCheck={false}
                            style={{ fontFamily: 'monospace', fontSize: 13 }}
                            value={content}
                        />
                    </>
                )}
        </Modal>
    )
}

import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, Modal, notification } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { copyFile } from '../../services/files'
import { FieldRow } from '../../components/FieldRow'
import { ProjectFolderInput } from './ProjectFolderInput'
import { basename, joinProjectPath, normalizeProjectPath, parentFolder } from './projectPaths'

const LABEL_WIDTH = 130

interface CopyFileModalProps {
    open: boolean
    projectId: string
    /** Project-relative path of the file or folder being copied. */
    path: string
    /** Copying a folder rather than a file — it only changes what the dialog is called. */
    folder?: boolean
    /** Folders of the project, offered as the destination. */
    folders: string[]
    onClose: () => void
    onCopied: () => void
}

/** The name a copy starts with: the original with a "-copy" suffix, before any extension. */
export const suggestCopyName = (path: string): string => {
    const name = basename(path)
    const dot = name.lastIndexOf('.')
    return dot > 0 ? `${name.slice(0, dot)}-copy${name.slice(dot)}` : `${name}-copy`
}

// Re-exported so callers (and the test) that reach for it here still resolve it.
export { parentFolder }

/** Copies a file or folder of the project under a new name, into a folder the user picks. */
export const CopyFileModal = ({
    open,
    projectId,
    path,
    folder = false,
    folders,
    onClose,
    onCopied,
}: CopyFileModalProps) => {
    const { t } = useTranslation('repository')
    const [name, setName] = useState('')
    const [target, setTarget] = useState('')
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        if (open) {
            setName(suggestCopyName(path))
            setTarget(parentFolder(path))
        }
    }, [open, path])

    const destination = joinProjectPath(target, name)

    const submit = async () => {
        if (busy) {
            return
        }
        if (!normalizeProjectPath(name) || destination === path) {
            onClose()
            return
        }
        setBusy(true)
        try {
            await copyFile(projectId, path, destination)
            onCopied()
            onClose()
        } catch (e) {
            notification.error({ title: t('browser.files.copy_failed'), description: errorMessage(e) })
        } finally {
            setBusy(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={busy}
            okButtonProps={{ 'data-testid': 'file-copy-submit', disabled: !normalizeProjectPath(name) }}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t(folder ? 'browser.files.copy_folder_title' : 'browser.files.copy_title')}
        >
            <FieldRow required label={t(folder ? 'browser.files.new_folder_name' : 'browser.files.new_file_name')} labelWidth={LABEL_WIDTH}>
                <Input
                    data-testid="file-copy-name"
                    onChange={event => setName(event.target.value)}
                    onPressEnter={submit}
                    value={name}
                />
            </FieldRow>
            <FieldRow required label={t('browser.files.path')} labelWidth={LABEL_WIDTH}>
                <ProjectFolderInput
                    data-testid="file-copy-path"
                    folders={folders}
                    onChange={setTarget}
                    value={target}
                />
            </FieldRow>
        </Modal>
    )
}

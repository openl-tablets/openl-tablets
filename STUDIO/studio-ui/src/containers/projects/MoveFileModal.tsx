import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, Modal, notification } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { moveFile } from '../../services/files'
import { FieldRow } from '../../components/FieldRow'
import { ProjectFolderInput } from './ProjectFolderInput'
import { basename, joinProjectPath, normalizeProjectPath, parentFolder } from './projectPaths'

const LABEL_WIDTH = 130

interface MoveFileModalProps {
    open: boolean
    projectId: string
    /** Project-relative path of the file being moved or renamed. */
    path: string
    /** Folders of the project, offered as the destination when moving. */
    folders: string[]
    /** Rename keeps the file where it is and changes its name; move keeps its name and changes its folder. */
    mode: 'move' | 'rename'
    onClose: () => void
    /** Called with the file's new project-relative path, so the caller can follow it to where it landed. */
    onMoved: (destination: string) => void
}

/**
 * Moves a file to a folder the user picks, or renames it in place. Both are the one move the Files API
 * offers — a rename is a move within the same folder — so this dialog does whichever the mode asks for.
 */
export const MoveFileModal = ({ open, projectId, path, folders, mode, onClose, onMoved }: MoveFileModalProps) => {
    const { t } = useTranslation('repository')
    const [name, setName] = useState('')
    const [target, setTarget] = useState('')
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        if (open) {
            setName(basename(path))
            setTarget(parentFolder(path))
        }
    }, [open, path])

    const renaming = mode === 'rename'
    const destination = joinProjectPath(target, name)

    const submit = async () => {
        if (busy) {
            return
        }
        // Nothing to do when the name is blank or the file would land back where it already is.
        if (!normalizeProjectPath(name) || destination === path) {
            onClose()
            return
        }
        setBusy(true)
        try {
            await moveFile(projectId, path, destination)
            onMoved(destination)
            onClose()
        } catch (e) {
            notification.error({
                title: t(renaming ? 'browser.files.rename_failed' : 'browser.files.move_failed'),
                description: errorMessage(e),
            })
        } finally {
            setBusy(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={busy}
            okButtonProps={{ 'data-testid': 'file-move-submit', disabled: !normalizeProjectPath(name) }}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t(renaming ? 'browser.files.rename_title' : 'browser.files.move_title')}
        >
            {renaming ? (
                <FieldRow required label={t('browser.files.new_file_name')} labelWidth={LABEL_WIDTH}>
                    <Input
                        data-testid="file-move-name"
                        onChange={event => setName(event.target.value)}
                        onPressEnter={submit}
                        value={name}
                    />
                </FieldRow>
            ) : (
                <FieldRow required label={t('browser.files.path')} labelWidth={LABEL_WIDTH}>
                    <ProjectFolderInput
                        data-testid="file-move-path"
                        folders={folders}
                        onChange={setTarget}
                        value={target}
                    />
                </FieldRow>
            )}
        </Modal>
    )
}

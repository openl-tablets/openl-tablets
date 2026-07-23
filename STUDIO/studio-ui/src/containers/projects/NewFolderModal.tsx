import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Modal } from 'antd'
import { FieldRow } from '../../components/FieldRow'
import { ProjectFolderInput } from './ProjectFolderInput'
import { normalizeProjectPath } from './projectPaths'

const LABEL_WIDTH = 80

interface NewFolderModalProps {
    open: boolean
    /** Folders of the project, offered while typing the path. */
    folders: string[]
    /** Folder the tree selection points at; the new path starts from it. */
    targetFolder: string
    onClose: () => void
    /** The folder is only added to the tree: an empty folder is persisted once a file lands inside it. */
    onCreate: (path: string) => void
}

/** Creates a folder in the project, at the path shown in full. */
export const NewFolderModal = ({ open, folders, targetFolder, onClose, onCreate }: NewFolderModalProps) => {
    const { t } = useTranslation('repository')
    const [path, setPath] = useState('')

    useEffect(() => {
        if (open) {
            setPath(targetFolder ? `${targetFolder}/` : '')
        }
    }, [open, targetFolder])

    const submit = () => {
        const normalized = normalizeProjectPath(path)
        if (normalized) {
            onCreate(normalized)
            onClose()
        }
    }

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ 'data-testid': 'files-folder-submit', disabled: !normalizeProjectPath(path) }}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('browser.files.new_folder')}
        >
            <FieldRow required label={t('browser.files.path')} labelWidth={LABEL_WIDTH}>
                <ProjectFolderInput
                    data-testid="files-folder-path"
                    folders={folders}
                    onChange={setPath}
                    value={path}
                />
            </FieldRow>
        </Modal>
    )
}

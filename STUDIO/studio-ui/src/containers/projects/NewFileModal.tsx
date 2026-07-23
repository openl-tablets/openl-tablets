import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, Modal, notification } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { createTextFile } from '../../services/files'
import { FieldRow } from '../../components/FieldRow'
import { ProjectFolderInput } from './ProjectFolderInput'
import { joinProjectPath, normalizeProjectPath } from './projectPaths'

const LABEL_WIDTH = 80

interface NewFileModalProps {
    open: boolean
    projectId: string
    /** Folders of the project, offered while typing the path. */
    folders: string[]
    /** Folder the tree selection points at; the new file starts there. */
    targetFolder: string
    onClose: () => void
    onCreated: () => void
}

/** Creates an empty text file under a folder of the project. */
export const NewFileModal = ({
    open,
    projectId,
    folders,
    targetFolder,
    onClose,
    onCreated,
}: NewFileModalProps) => {
    const { t } = useTranslation('repository')
    const [name, setName] = useState('')
    const [folder, setFolder] = useState('')
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        if (open) {
            setName('')
            setFolder(targetFolder)
        }
    }, [open, targetFolder])

    const submit = async () => {
        if (submitting || !normalizeProjectPath(name)) {
            return
        }
        setSubmitting(true)
        try {
            await createTextFile(projectId, joinProjectPath(folder, name))
            onCreated()
            onClose()
        } catch (e) {
            notification.error({ title: t('browser.files.text_file_failed'), description: errorMessage(e) })
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={submitting}
            okButtonProps={{ 'data-testid': 'files-text-file-submit', disabled: !normalizeProjectPath(name) }}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('browser.files.new_text_file')}
        >
            <FieldRow required label={t('browser.files.name')} labelWidth={LABEL_WIDTH}>
                <Input
                    data-testid="files-text-file-name"
                    onChange={event => setName(event.target.value)}
                    onPressEnter={submit}
                    value={name}
                />
            </FieldRow>
            {/* An empty path puts the file in the project root, which is a perfectly ordinary choice. */}
            <FieldRow label={t('browser.files.path')} labelWidth={LABEL_WIDTH}>
                <ProjectFolderInput
                    data-testid="files-text-file-path"
                    folders={folders}
                    onChange={setFolder}
                    value={folder}
                />
            </FieldRow>
        </Modal>
    )
}

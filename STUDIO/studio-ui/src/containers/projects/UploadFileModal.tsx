import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, Modal, notification, type UploadFile } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { uploadFile, uploadFiles } from '../../services/files'
import { FieldRow } from '../../components/FieldRow'
import { FileDropzone } from '../../components/FileDropzone'
import { ProjectFolderInput } from './ProjectFolderInput'
import { normalizeProjectPath, uploadTargetPath } from './projectPaths'

const LABEL_WIDTH = 80

interface UploadFileModalProps {
    open: boolean
    projectId: string
    /** Folders of the project, offered while typing the path. */
    folders: string[]
    /** Folder the tree selection points at; the file starts there. */
    targetFolder: string
    onClose: () => void
    onUploaded: () => void
}

/**
 * Uploads files from the computer into a folder of the project.
 *
 * A single file can be renamed on the way in; several files keep their own names and go up in one request,
 * so migrating a folder of rules is one action and one commit.
 */
export const UploadFileModal = ({
    open,
    projectId,
    folders,
    targetFolder,
    onClose,
    onUploaded,
}: UploadFileModalProps) => {
    const { t } = useTranslation('repository')
    const [fileList, setFileList] = useState<UploadFile[]>([])
    const [name, setName] = useState('')
    const [folder, setFolder] = useState('')
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        if (open) {
            setFileList([])
            setName('')
            setFolder(targetFolder)
        }
    }, [open, targetFolder])

    // The staged list stays as Ant Design hands it over, so a second pick adds to it instead of
    // replacing it; the files themselves are read from it when the dialog is confirmed.
    const files = fileList.flatMap(item => (item.originFileObj ? [item.originFileObj as File] : []))
    const single = files.length === 1
    const canSubmit = files.length > 0 && (!single || !!normalizeProjectPath(name))

    const submit = async () => {
        if (!canSubmit) {
            return
        }
        setSubmitting(true)
        try {
            const target = uploadTargetPath(folder)
            await (single ? uploadFile(projectId, target, files[0]!, name.trim()) : uploadFiles(projectId, target, files))
            onUploaded()
            onClose()
        } catch (e) {
            notification.error({ title: t('browser.files.upload_failed'), description: errorMessage(e) })
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={submitting}
            okButtonProps={{ 'data-testid': 'files-upload-submit', disabled: !canSubmit }}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('browser.files.upload')}
        >
            <FileDropzone
                multiple
                data-testid="files-upload-dragger"
                fileList={fileList}
                hint={t('browser.files.upload_hint')}
                style={{ marginBottom: 16 }}
                onChange={picked => {
                    setFileList(picked)
                    // One file is named by the user; a batch keeps the names it came with.
                    setName(picked.length === 1 ? picked[0]!.name : '')
                }}
            />
            {single && (
                <FieldRow required label={t('browser.files.name')} labelWidth={LABEL_WIDTH}>
                    <Input
                        data-testid="files-upload-name"
                        onChange={event => setName(event.target.value)}
                        value={name}
                    />
                </FieldRow>
            )}
            {/* An empty path puts the file in the project root, which is a perfectly ordinary choice. */}
            <FieldRow label={t('browser.files.path')} labelWidth={LABEL_WIDTH}>
                <ProjectFolderInput
                    data-testid="files-upload-path"
                    folders={folders}
                    onChange={setFolder}
                    value={folder}
                />
            </FieldRow>
        </Modal>
    )
}

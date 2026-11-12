import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { createStyles } from 'antd-style'
import { FileAddOutlined, FolderAddOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { MenuButton } from '../../components/MenuButton'
import { useSharedStyles } from './sharedStyles'
import { NewFolderModal } from './NewFolderModal'
import { NewFileModal } from './NewFileModal'
import { UploadFileModal } from './UploadFileModal'
import { normalizeProjectPath } from './projectPaths'

const useStyles = createStyles(({ css }) => ({
    actions: css`
        flex: none;
        margin-left: auto;
    `,
}))

interface FilesToolbarProps {
    projectId: string
    /** Folder path used as a base for create and upload actions. Empty means project root. */
    targetFolder?: string
    /** Folders of the project, offered as paths in the create and upload dialogs. */
    folders: string[]
    onChanged: () => void
    /**
     * Adds a client-side (virtual) folder to the tree. An empty folder is never persisted on its own;
     * it becomes real once a file is created inside it, which creates the whole folder chain.
     */
    onCreateFolder: (path: string) => void
}

type AddAction = 'folder' | 'text-file' | 'upload'

/**
 * Actions above the project file tree, for a reader who may change the files: an Add menu that creates a folder or
 * a file and uploads one from the computer. Each entry opens a dialog carrying the full path inside the project;
 * the tree selection only decides where that path starts.
 */
export const FilesToolbar = ({
    projectId,
    targetFolder = '',
    folders,
    onChanged,
    onCreateFolder,
}: FilesToolbarProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [action, setAction] = useState<AddAction | null>(null)
    const normalizedTargetFolder = normalizeProjectPath(targetFolder)
    const close = () => setAction(null)

    return (
        <div className={shared.paneHeader}>
            <MenuButton
                className={styles.actions}
                data-testid="files-add"
                icon={<PlusOutlined />}
                size="small"
                menu={{
                    items: [
                        {
                            key: 'folder',
                            icon: <FolderAddOutlined />,
                            label: <span data-testid="files-new-folder">{t('browser.files.new_folder')}</span>,
                        },
                        {
                            key: 'text-file',
                            icon: <FileAddOutlined />,
                            label: <span data-testid="files-new-text-file">{t('browser.files.new_text_file')}</span>,
                        },
                        { type: 'divider' },
                        {
                            key: 'upload',
                            icon: <UploadOutlined />,
                            label: <span data-testid="files-upload">{t('browser.files.upload')}</span>,
                        },
                    ],
                    onClick: ({ key }) => setAction(key as AddAction),
                }}
            >
                {t('browser.files.add')}
            </MenuButton>
            <NewFolderModal
                folders={folders}
                onClose={close}
                onCreate={onCreateFolder}
                open={action === 'folder'}
                targetFolder={normalizedTargetFolder}
            />
            <NewFileModal
                folders={folders}
                onClose={close}
                onCreated={onChanged}
                open={action === 'text-file'}
                projectId={projectId}
                targetFolder={normalizedTargetFolder}
            />
            <UploadFileModal
                folders={folders}
                onClose={close}
                onUploaded={onChanged}
                open={action === 'upload'}
                projectId={projectId}
                targetFolder={normalizedTargetFolder}
            />
        </div>
    )
}

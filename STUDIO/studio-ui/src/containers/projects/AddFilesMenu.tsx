import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import type { MenuProps } from 'antd'
import { FileAddOutlined, FolderAddOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { MenuButton } from '../../components/MenuButton'
import { NewFolderModal } from './NewFolderModal'
import { NewFileModal } from './NewFileModal'
import { UploadFileModal } from './UploadFileModal'
import { normalizeProjectPath } from './projectPaths'

/** What the Add menu adds: an empty folder, a new text file, or a file uploaded from the computer. */
export type AddFilesKind = 'folder' | 'text-file' | 'upload'

/** The entries of the Add menu, the same wherever it is offered. */
export const addFilesMenuItems = (t: TFunction): NonNullable<MenuProps['items']> => [
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
]

interface AddFilesDialogsProps {
    /** The dialog to show, or null for none. */
    kind: AddFilesKind | null
    onClose: () => void
    projectId: string
    /** The folder the new path starts in. Empty means the project root. */
    targetFolder: string
    /** Folders of the project, offered as paths in the dialogs. */
    folders: string[]
    onChanged: () => void
    /**
     * Adds an empty folder. It is kept in the browser only: it becomes real once a file is created inside it,
     * which creates the whole folder chain.
     */
    onCreateFolder: (path: string) => void
}

/**
 * The dialogs the Add menu opens. Each carries the full path inside the project, starting in the target folder.
 *
 * <p>They are kept apart from the menu, so a menu that moves — into the overflow of the action bar — never takes
 * an open dialog away with it.
 */
export const AddFilesDialogs = ({ kind, onClose, projectId, targetFolder, folders, onChanged, onCreateFolder }: AddFilesDialogsProps) => {
    const folder = normalizeProjectPath(targetFolder)
    return (
        <>
            <NewFolderModal
                folders={folders}
                onClose={onClose}
                onCreate={onCreateFolder}
                open={kind === 'folder'}
                targetFolder={folder}
            />
            <NewFileModal
                folders={folders}
                onClose={onClose}
                onCreated={onChanged}
                open={kind === 'text-file'}
                projectId={projectId}
                targetFolder={folder}
            />
            <UploadFileModal
                folders={folders}
                onClose={onClose}
                onUploaded={onChanged}
                open={kind === 'upload'}
                projectId={projectId}
                targetFolder={folder}
            />
        </>
    )
}

type AddFilesMenuProps = Omit<AddFilesDialogsProps, 'kind' | 'onClose'>

/**
 * The Add button of a folder, for a reader who may change the files: it creates a folder or a file in the folder,
 * or uploads one from the computer.
 */
export const AddFilesMenu = (props: AddFilesMenuProps) => {
    const { t } = useTranslation('repository')
    const [kind, setKind] = useState<AddFilesKind | null>(null)

    return (
        <>
            <MenuButton
                data-testid="files-add"
                icon={<PlusOutlined />}
                menu={{ items: addFilesMenuItems(t), onClick: ({ key }) => setKind(key as AddFilesKind) }}
                size="small"
            >
                {t('browser.files.add')}
            </MenuButton>
            <AddFilesDialogs {...props} kind={kind} onClose={() => setKind(null)} />
        </>
    )
}

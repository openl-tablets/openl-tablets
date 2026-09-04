import { useCallback, useEffect, useState, type Key } from 'react'
import { App, type TreeDataNode } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { listRepoFolders } from '../../services/repositories'
import type { FsNode } from '../../types/files'
import { FolderPickerDialog, useFolderNode } from './FolderPickerDialog'

/** Return a copy of the tree with freshly loaded children attached under the node with the given key. */
export const attachChildren = (nodes: TreeDataNode[], key: Key, children: TreeDataNode[]): TreeDataNode[] =>
    nodes.map(node => {
        if (node.key === key) {
            return { ...node, children }
        }
        if (node.children) {
            return { ...node, children: attachChildren(node.children, key, children) }
        }
        return node
    })

interface RepoFolderPickerProps {
    /** Whether the picker dialog is open. */
    open: boolean
    /** Design repository whose folders are browsed. */
    repositoryId: string
    /** Called with the repository-relative path of the picked folder. */
    onSelect: (path: string) => void
    /** Called when the dialog is dismissed or a folder is picked. */
    onClose: () => void
}

/**
 * The folders of a design repository, loaded one level at a time as the user expands them, so the picker
 * stays responsive on very large repositories.
 */
export const RepoFolderPicker = ({ open, repositoryId, onSelect, onClose }: RepoFolderPickerProps) => {
    const { notification } = App.useApp()
    const folderNode = useFolderNode()
    // null while the root level is loading; an array (possibly empty) once loaded.
    const [treeData, setTreeData] = useState<TreeDataNode[] | null>(null)

    const toNode = useCallback((folder: FsNode) => folderNode(folder.path, folder.name), [folderNode])

    const loadRoot = useCallback(async () => {
        setTreeData(null)
        try {
            const folders = await listRepoFolders(repositoryId)
            setTreeData(folders.map(toNode))
        } catch (e) {
            setTreeData([])
            notification.error({ message: errorMessage(e) })
        }
    }, [repositoryId, notification, toNode])

    const loadChildren = useCallback(async (node: TreeDataNode) => {
        try {
            const folders = await listRepoFolders(repositoryId, String(node.key))
            setTreeData(prev => attachChildren(prev ?? [], node.key, folders.map(toNode)))
        } catch (e) {
            setTreeData(prev => attachChildren(prev ?? [], node.key, []))
            notification.error({ message: errorMessage(e) })
        }
    }, [repositoryId, notification, toNode])

    useEffect(() => {
        if (open) {
            void loadRoot()
        }
    }, [open, loadRoot])

    return (
        <FolderPickerDialog
            data-testid="repo-folder-tree"
            loadData={loadChildren}
            onClose={onClose}
            onSelect={onSelect}
            open={open}
            treeData={treeData}
        />
    )
}

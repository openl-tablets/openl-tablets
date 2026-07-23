import { useMemo } from 'react'
import type { TreeDataNode } from 'antd'
import { FolderPickerDialog, useFolderNode } from './FolderPickerDialog'

interface ProjectFolderPickerProps {
    open: boolean
    /** Folder paths of the project, as a flat list. */
    folders: string[]
    /** Called with the project-relative path of the picked folder. */
    onSelect: (path: string) => void
    onClose: () => void
}

/**
 * The folders of a project. They are already loaded with the file tree, so the whole tree opens at once —
 * no level-by-level loading, unlike the repository picker.
 */
export const ProjectFolderPicker = ({ open, folders, onSelect, onClose }: ProjectFolderPickerProps) => {
    const folderNode = useFolderNode()
    const treeData = useMemo(() => toTree(folders, folderNode), [folders, folderNode])

    return (
        <FolderPickerDialog
            data-testid="project-folder-tree"
            onClose={onClose}
            onSelect={onSelect}
            open={open}
            treeData={treeData}
        />
    )
}

/** Builds the nested tree the picker shows out of the flat folder paths. */
const toTree = (
    folders: string[],
    folderNode: (key: string, name: string) => TreeDataNode
): TreeDataNode[] => {
    const roots: TreeDataNode[] = []
    const byPath = new Map<string, TreeDataNode>()
    for (const folder of [...folders].sort((left, right) => left.localeCompare(right))) {
        const segments = folder.split('/').filter(Boolean)
        segments.forEach((segment, index) => {
            const path = segments.slice(0, index + 1).join('/')
            if (byPath.has(path)) {
                return
            }
            const node = folderNode(path, segment)
            byPath.set(path, node)
            const parent = index === 0 ? undefined : byPath.get(segments.slice(0, index).join('/'))
            if (parent) {
                parent.children = [...(parent.children ?? []), node]
            } else {
                roots.push(node)
            }
        })
    }
    return roots
}

// The shape of a project's files as a tree, built from the flat paths the server lists them by. Both the
// Files tab and the projects rail draw it, each with its own icons and titles, so only the shape lives here.

/** A file or a folder of a project, with the folders it holds when it is one. */
export interface FileNode {
    /** Project-relative path, which is also what addresses the file. */
    path: string
    /** The last segment of the path — what the row reads as. */
    name: string
    isFile: boolean
    /** The nodes a folder holds, absent on a file. */
    children?: FileNode[]
}

/** Folders first, then files, each in the order their paths read in. */
const sorted = (nodes: FileNode[]): FileNode[] => {
    for (const node of nodes) {
        if (node.children) {
            sorted(node.children)
        }
    }
    return nodes.sort((left, right) =>
        Number(left.isFile) - Number(right.isFile) || left.path.localeCompare(right.path))
}

/**
 * The tree the given paths describe, with every folder along the way derived from them.
 *
 * <p>A folder path is a folder of its own even when no file is under it yet — that is what a folder the
 * user has just created looks like until something lands in it.
 */
export const buildFileNodes = (filePaths: string[], folderPaths: string[] = []): FileNode[] => {
    const roots: FileNode[] = []
    const childrenByPath = new Map<string, FileNode[]>([['', roots]])
    const byPath = new Map<string, FileNode>()

    const add = (path: string, lastIsFile: boolean) => {
        let parent = ''
        const segments = path.split('/').filter(Boolean)
        segments.forEach((segment, index) => {
            const isFile = lastIsFile && index === segments.length - 1
            const key = parent ? `${parent}/${segment}` : segment
            if (!byPath.has(key)) {
                const node: FileNode = { path: key, name: segment, isFile }
                if (!isFile) {
                    node.children = []
                    childrenByPath.set(key, node.children)
                }
                byPath.set(key, node)
                childrenByPath.get(parent)?.push(node)
            }
            parent = key
        })
    }

    for (const path of filePaths) {
        add(path, true)
    }
    for (const path of folderPaths) {
        add(path, false)
    }
    return sorted(roots)
}

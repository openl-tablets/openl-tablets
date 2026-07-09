import { getFileBlob } from '../../services/files'
import { getProjectFiles } from '../../services/repositories'
import type { FsNode } from '../../types/files'
import {
    fileName,
    isExcelFile,
    openComparePopup,
    openLegacyExcelCompare,
    writeCompareLoading,
} from '../../utils/legacyCompare'

const isExcelNode = (node: FsNode): boolean => node.type === 'file' && isExcelFile(node.path)

export const listProjectRevisionExcelFiles = async (
    projectId: string,
    fromRevision: string,
    toRevision: string
): Promise<string[]> => {
    const [fromFiles, toFiles] = await Promise.all([
        getProjectFiles(projectId, true, fromRevision),
        getProjectFiles(projectId, true, toRevision),
    ])
    const fromPaths = new Set(fromFiles.filter(isExcelNode).map(node => node.path))
    return toFiles
        .filter(node => isExcelNode(node) && fromPaths.has(node.path))
        .map(node => node.path)
        .sort((left, right) => left.localeCompare(right))
}

export const openProjectRevisionFileCompare = async (
    projectId: string,
    path: string,
    fromRevision: string,
    toRevision: string
): Promise<void> => {
    const popup = openComparePopup()
    try {
        writeCompareLoading(popup, path)
        const [from, to] = await Promise.all([
            getFileBlob(projectId, path, fromRevision),
            getFileBlob(projectId, path, toRevision),
        ])
        const name = fileName(path)
        await openLegacyExcelCompare(popup, path, from, to, `${fromRevision}-${name}`, `${toRevision}-${name}`)
    } catch (error) {
        popup.close()
        throw error
    }
}

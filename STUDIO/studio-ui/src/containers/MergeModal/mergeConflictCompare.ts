import { apiCall } from '../../services'
import {
    fileName,
    htmlEscape,
    isExcelFile,
    openComparePopup,
    openLegacyExcelCompare,
    writeCompareLoading,
} from '../../utils/legacyCompare'
import { DiffInputTooLargeError, diffLines, type DiffLine } from '../../utils/lineDiff'
import type { FileSide } from './types'

const conflictFileUrl = (projectId: string, filePath: string, side: FileSide): string =>
    `/projects/${encodeURIComponent(projectId)}/merge/conflicts/files?file=${encodeURIComponent(filePath)}&side=${side}`

const fetchConflictFile = async (projectId: string, filePath: string, side: FileSide): Promise<Blob> => {
    return await apiCall(conflictFileUrl(projectId, filePath, side), undefined, {
        throwError: true,
        responseType: 'blob',
    }) as Blob
}

const renderDiffLine = (line: DiffLine): string => {
    const sign = line.kind === 'add' ? '+' : line.kind === 'remove' ? '-' : ' '
    return `
        <div class="line ${line.kind}">
            <span class="gutter">${line.oldNumber ?? ''}</span>
            <span class="gutter">${line.newNumber ?? ''}</span>
            <span class="code">${htmlEscape(`${sign} ${line.text}`)}</span>
        </div>
    `
}

const writeTextDiff = (popup: Window, path: string, theirs: string, ours: string) => {
    let body: string
    try {
        const lines = diffLines(theirs, ours)
        const hasChanges = lines.some(line => line.kind !== 'context')
        body = hasChanges
            ? lines.map(renderDiffLine).join('')
            : '<p class="empty">Files are identical.</p>'
    } catch (error) {
        if (!(error instanceof DiffInputTooLargeError)) {
            throw error
        }
        body = '<p class="empty">The selected files are too large to compare in the browser.</p>'
    }

    popup.document.open()
    popup.document.write(`
        <!doctype html>
        <html>
            <head>
                <title>Compare ${htmlEscape(fileName(path))}</title>
                <style>
                    body { margin: 0; font-family: Arial, sans-serif; color: #1f1f1f; }
                    header { padding: 12px 16px; border-bottom: 1px solid #d9d9d9; }
                    h1 { margin: 0 0 4px; font-size: 16px; }
                    .meta { color: #666; font-size: 12px; }
                    .diff { font-family: Menlo, Consolas, monospace; font-size: 12px; line-height: 1.45; }
                    .line { display: flex; white-space: pre-wrap; word-break: break-word; }
                    .gutter { flex: 0 0 48px; padding-right: 8px; color: #999; text-align: right; user-select: none; }
                    .code { flex: 1; padding-right: 12px; }
                    .add { background: #f6ffed; }
                    .remove { background: #fff1f0; }
                    .context { background: #fff; }
                    .empty { padding: 16px; color: #666; }
                </style>
            </head>
            <body>
                <header>
                    <h1>${htmlEscape(path)}</h1>
                    <div class="meta">Theirs to yours</div>
                </header>
                <main class="diff">${body}</main>
            </body>
        </html>
    `)
    popup.document.close()
}

const openExcelCompare = async (popup: Window, projectId: string, path: string) => {
    const [theirs, ours] = await Promise.all([
        fetchConflictFile(projectId, path, 'THEIRS'),
        fetchConflictFile(projectId, path, 'OURS'),
    ])
    const name = fileName(path)
    await openLegacyExcelCompare(popup, path, theirs, ours, `theirs-${name}`, `ours-${name}`)
}

const openTextCompare = async (popup: Window, projectId: string, path: string) => {
    const [theirs, ours] = await Promise.all([
        fetchConflictFile(projectId, path, 'THEIRS').then(blob => blob.text()),
        fetchConflictFile(projectId, path, 'OURS').then(blob => blob.text()),
    ])
    writeTextDiff(popup, path, theirs, ours)
}

export const openMergeConflictCompare = async (projectId: string, path: string): Promise<void> => {
    const popup = openComparePopup()
    try {
        writeCompareLoading(popup, path)
        if (isExcelFile(path)) {
            await openExcelCompare(popup, projectId, path)
        } else {
            await openTextCompare(popup, projectId, path)
        }
    } catch (error) {
        popup.close()
        throw error
    }
}

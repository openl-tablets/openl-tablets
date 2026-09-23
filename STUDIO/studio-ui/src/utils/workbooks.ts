/**
 * What the engine reads as a workbook.
 *
 * <p>The same three the server keeps in `org.openl.util.FileTypeHelper`. A rule spelled out a second time
 * drifts from the first, so every screen that asks "is this a workbook" asks it here.
 */
export const WORKBOOK_EXTENSIONS = ['.xlsx', '.xls', '.xlsm'] as const

/** The extensions as a file picker's `accept` asks for them. */
export const WORKBOOK_ACCEPT = WORKBOOK_EXTENSIONS.join(',')

/**
 * Whether the file at that path is read as a workbook.
 *
 * <p>A file Excel wrote while the workbook was open — its name starts with `~$` — is a lock file rather
 * than a workbook, and is answered for as one, the way the server answers for it.
 *
 * <p>A path written with either separator ends in a file name, so one written on Windows is read the same.
 */
export const isWorkbookPath = (path: string): boolean => {
    const name = path.slice(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1).toLowerCase()
    return !name.startsWith('~$') && WORKBOOK_EXTENSIONS.some(extension => name.endsWith(extension))
}

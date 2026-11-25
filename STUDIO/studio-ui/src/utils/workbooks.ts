import { fileName } from './fileFormats'

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
 * <p>The lock file Excel writes beside an open workbook, `~$Rules.xlsx`, never reaches a screen: the server
 * ignores it with the other system and temporary files, so it is not told apart here.
 *
 * <p>A path written with either separator ends in a file name, so one written on Windows is read the same.
 */
export const isWorkbookPath = (path: string): boolean => {
    const name = fileName(path).toLowerCase()
    return WORKBOOK_EXTENSIONS.some(extension => name.endsWith(extension))
}

/**
 * Whether the file at that path is a Markdown rules module.
 *
 * <p>The same rule the server keeps in `org.openl.util.FileTypeHelper`. Such a module is plain text: its tables
 * are read on the module screen and changed in the file editor.
 */
export const isMarkdownPath = (path: string | undefined): boolean => !!path && path.toLowerCase().endsWith('.md')

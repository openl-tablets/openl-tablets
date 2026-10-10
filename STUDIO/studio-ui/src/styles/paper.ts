import { theme as antdTheme } from 'antd'

/** The token Ant Design gives its own light appearance. */
type PaperToken = ReturnType<typeof antdTheme.getDesignToken>

let paper: PaperToken | undefined

/**
 * The token a table of a workbook is drawn with where the reader asks for the colours of its Excel file: Ant
 * Design's own in the light appearance, whatever the theme.
 *
 * A workbook is written in black on white, and its author coloured its cells for that paper — the default black
 * text of a cell filled in cyan reads on it, while the light text of a dark theme would not. Excel draws a
 * workbook the same whatever the window around it looks like, and so are its cells drawn then, together with
 * every mark a screen lays on them, so that a mark reads on the paper it lies on. Otherwise a table is drawn in the
 * colours of the theme ({@link tableThemeOf}).
 *
 * It is worked out the first time it is asked for rather than when the module loads, so a test that replaces
 * Ant Design with a stand-in can still import whatever refers to it.
 */
export const paperToken = (): PaperToken => (paper ??= antdTheme.getDesignToken())

/** The colours a table is laid on and written in, under the colours its cells give themselves. */
export interface TablePaper {
    /** The ground of the table, which a cell filled no other way shows. */
    background: string
    /** A text and a line that name no colour. */
    text: string
    /** The lines of the grid between the cells. */
    grid: string
    /** A piece of a cell that names a table. */
    link: string
    /** A piece of a cell that names a table, under the pointer. */
    linkHover: string
    /** The mark of a note in the corner of a cell. */
    note: string
}

let workbookPaperColours: TablePaper | undefined

/** The paper of a workbook, in the colours of {@link paperToken}, worked out the first time it is asked for. */
export const workbookPaper = (): TablePaper => (workbookPaperColours ??= {
    background: paperToken().colorBgContainer,
    text: paperToken().colorText,
    grid: paperToken().colorBorderSecondary,
    link: paperToken().colorLink,
    linkHover: paperToken().colorLinkHover,
    note: paperToken().colorError,
})

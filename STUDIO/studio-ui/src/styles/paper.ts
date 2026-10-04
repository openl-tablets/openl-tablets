import { theme as antdTheme } from 'antd'

/** The token Ant Design gives its own light appearance. */
type PaperToken = ReturnType<typeof antdTheme.getDesignToken>

let paper: PaperToken | undefined

/**
 * The token a table of a workbook is drawn with: Ant Design's own in the light appearance, whatever the theme.
 *
 * A workbook is written in black on white, and its author coloured its cells for that paper — the default black
 * text of a cell filled in cyan reads on it, while the light text of a dark theme would not. Excel draws a
 * workbook the same whatever the window around it looks like, and so are its cells drawn here, together with
 * every mark a screen lays on them, so that a mark reads on the paper it lies on.
 *
 * It is worked out the first time it is asked for rather than when the module loads, so a test that replaces
 * Ant Design with a stand-in can still import whatever refers to it.
 */
export const paperToken = (): PaperToken => (paper ??= antdTheme.getDesignToken())

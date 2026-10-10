import { type PropsWithChildren, useMemo } from 'react'
import { ConfigProvider, type ThemeConfig } from 'antd'
import { useTheme } from 'antd-style'
import { densityTheme, useAppTheme } from '../providers/AppThemeProvider'
import { tableOf } from '../styles/tableColours'

/**
 * Draws the Ant Design controls inside it on the paper the tables are laid on, such as a field written into a cell.
 *
 * The tables follow the theme of the application, and so do the controls: they are drawn as the rest of the screen.
 * Where the reader asks for the colours of the Excel files, the tables lie on the paper of a workbook
 * ({@link paperToken}), and the controls are drawn in Ant Design's own light appearance, whatever the theme: a field
 * written into a cell is then black on white, as in Excel. The theme of the application is left out rather than laid
 * under, so neither the text nor the background of a dark theme reaches the controls. The density in force is kept.
 */
export const PaperTheme = ({ children }: PropsWithChildren) => {
    const { compact } = useAppTheme()
    const onWorkbookPaper = tableOf(useTheme()).palette === undefined
    const theme = useMemo<ThemeConfig>(() => ({ inherit: false, ...densityTheme(compact) }), [compact])
    return onWorkbookPaper ? <ConfigProvider theme={theme}>{children}</ConfigProvider> : children
}

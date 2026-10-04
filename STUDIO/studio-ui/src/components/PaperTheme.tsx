import { type PropsWithChildren, useMemo } from 'react'
import { ConfigProvider, type ThemeConfig } from 'antd'
import { densityTheme, useAppTheme } from '../providers/AppThemeProvider'

/**
 * Draws the Ant Design controls inside it on the paper of a workbook ({@link paperToken}): in Ant Design's own
 * light appearance, whatever the theme. A field written into a cell is then black on white, as in Excel.
 *
 * The theme of the application is left out rather than laid under, so neither the text nor the background of a
 * dark theme reaches the controls. The density in force is kept. Styles of the application's own read the paper
 * through `paperToken()`, since `createStyles` takes its token from the antd-style provider, not from here.
 */
export const PaperTheme = ({ children }: PropsWithChildren) => {
    const { compact } = useAppTheme()
    const theme = useMemo<ThemeConfig>(() => ({ inherit: false, ...densityTheme(compact) }), [compact])
    return <ConfigProvider theme={theme}>{children}</ConfigProvider>
}

import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { FastColor } from '@ant-design/fast-color'
import { theme as antdTheme, type GlobalToken } from 'antd'
import type { ThemeMode } from 'antd-style'
import { onTestFinished } from 'vitest'
import { AppThemeProvider } from '../providers/AppThemeProvider'
import { appTheme } from '../styles/appTheme'
import { paletteOf, type Palette } from '../styles/listPageTheme'
import type { ThemeName } from '../styles/themes'
import { useUserStore } from '../store/userStore'
import type { UserProfile } from '../types/user'
import { storeCompactMode, storeThemeMode, storeThemeName } from '../utils/themeMode'

/** The look a test draws in, as a reader would have picked it. */
interface Look {
    theme?: ThemeName
    mode?: ThemeMode
    compact?: boolean
    /** Whether the reader asks, in My Settings, for the tables in the colours of their Excel files. */
    excelFormatting?: boolean
}

/**
 * Asks, as the reader does in My Settings, for the tables in the formatting and the colours of their Excel files.
 *
 * The profile of the user is put back when the test ends.
 */
export const showExcelFormatting = () => {
    const { userProfile } = useUserStore.getState()
    useUserStore.setState({ userProfile: { ...userProfile, showExcelFormatting: true } as UserProfile })
    onTestFinished(() => useUserStore.setState({ userProfile }))
}

/**
 * Renders under the application's theme provider, in the look given.
 *
 * The look is put where the provider reads it — the browser's storage and the profile of the user — and taken out
 * again when the test ends.
 */
export const renderInTheme = (ui: ReactElement,
    { theme = 'standard', mode = 'light', compact = false, excelFormatting = false }: Look = {}) => {
    storeThemeName(theme)
    storeThemeMode(mode)
    storeCompactMode(compact)
    onTestFinished(() => localStorage.clear())
    if (excelFormatting) {
        showExcelFormatting()
    }
    return render(<AppThemeProvider>{ui}</AppThemeProvider>)
}

/** The token Ant Design works out for a theme in an appearance, as the application's provider does. */
export const tokenFor = (name: ThemeName, isDarkMode: boolean): GlobalToken => antdTheme.getDesignToken({
    ...appTheme(name, isDarkMode),
    algorithm: isDarkMode ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
})

/** The palette a theme paints with in an appearance, read off the token Ant Design works out for it. */
export const paletteFor = (name: ThemeName, isDarkMode: boolean): Palette =>
    paletteOf(tokenFor(name, isDarkMode), name, isDarkMode)

/** A colour written out in full, so `#FFF` and `#ffffff` compare as the same colour. */
export const hex = (color: unknown): string | undefined =>
    (typeof color === 'string' ? new FastColor(color).toHexString() : undefined)

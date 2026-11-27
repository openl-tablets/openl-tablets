import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { FastColor } from '@ant-design/fast-color'
import { theme as antdTheme, type GlobalToken } from 'antd'
import type { ThemeMode } from 'antd-style'
import { onTestFinished } from 'vitest'
import { AppThemeProvider } from '../providers/AppThemeProvider'
import { SystemContext } from '../contexts/SystemContext'
import { appTheme } from '../styles/appTheme'
import { paletteOf, type Palette } from '../styles/listPageTheme'
import type { ThemeName } from '../styles/themes'
import { useSettingsStore } from '../store/settingsStore'
import { compactModeSetting, themeModeSetting, themeNameSetting } from '../utils/themeMode'

/** A server whose tables are not formatted on save, where the Excel Formatting of **My Settings** counts. */
export const NOT_FORMATTING_ON_SAVE = {
    isExternalAuthSystem: false,
    isUserManagementEnabled: false,
    isGroupsManagementEnabled: false,
    isPersonalAccessTokenEnabled: false,
    isAuditLogEnabled: false,
    systemSettings: { formatTablesOnSave: false },
}

/** The look a test draws in, as a reader would have picked it. */
interface Look {
    theme?: ThemeName
    mode?: ThemeMode
    compact?: boolean
    /** Whether the tables are drawn in the formatting of their workbooks rather than in the Studio look. */
    excelFormatting?: boolean
}

/**
 * Renders under the application's theme provider, in the look given.
 *
 * The look is put where the provider and **My Settings** read it — the browser's storage — and taken out again when
 * the test ends. Excel Formatting counts only while the tables are not formatted on save, so a test drawing in it
 * draws where they are not, unless the screen drawn says otherwise itself.
 */
export const renderInTheme = (ui: ReactElement,
    { theme = 'standard', mode = 'light', compact = false, excelFormatting = false }: Look = {}) => {
    themeNameSetting.write(theme)
    themeModeSetting.write(mode)
    compactModeSetting.write(compact)
    useSettingsStore.getState().setSetting('excelFormatting', excelFormatting)
    onTestFinished(() => {
        useSettingsStore.getState().setSetting('excelFormatting', false)
        localStorage.clear()
    })
    const drawn = excelFormatting
        ? <SystemContext.Provider value={NOT_FORMATTING_ON_SAVE}>{ui}</SystemContext.Provider>
        : ui
    return render(<AppThemeProvider>{drawn}</AppThemeProvider>)
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

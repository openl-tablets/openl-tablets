import type { ThemeAppearance, ThemeMode } from 'antd-style'
import { booleanText, oneOf, storedSetting } from './localStore'
import { THEME_ORDER, type ThemeName } from '../styles/themes'

/** Where the picked appearance is remembered between visits. */
export const THEME_MODE_KEY = 'openl.theme.mode'

/** The appearance follows the operating system until the user picks light or dark. */
export const DEFAULT_THEME_MODE: ThemeMode = 'auto'

/**
 * The picked appearance, remembered between visits.
 *
 * It follows the system, {@link DEFAULT_THEME_MODE}, when nothing was picked yet, when the browser refuses storage,
 * or when the stored value is not one this version knows; following the system again stores nothing.
 */
export const themeModeSetting = storedSetting<ThemeMode>(
    THEME_MODE_KEY,
    DEFAULT_THEME_MODE,
    oneOf(['auto', 'light', 'dark'])
)

/**
 * The appearance a mode stands for right now: itself, or the one the operating system asks for under `auto`.
 *
 * Answered synchronously, so the very first render is already drawn in it rather than in a light frame that
 * is repainted a moment later.
 */
export const appearanceOf = (mode: ThemeMode): ThemeAppearance => {
    if (mode !== 'auto') {
        return mode
    }
    return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Where the picked density is remembered between visits. */
export const THEME_COMPACT_KEY = 'openl.theme.compact'

/**
 * Whether the compact density was picked.
 *
 * The comfortable density is the default, so anything but a stored `true` — nothing picked yet, a browser
 * that refuses storage, a value this version does not know — leaves the application comfortable, and picking it
 * again stores nothing.
 */
export const compactModeSetting = storedSetting(THEME_COMPACT_KEY, false, booleanText)

/** Where the choice to show the tables in the formatting of their Excel files is remembered between visits. */
export const THEME_EXCEL_FORMATTING_KEY = 'openl.theme.excelFormatting'

/**
 * Whether the tables are shown in the formatting of their Excel files (**Excel Formatting** in the theme menu).
 *
 * The tables are drawn in the colours of the theme by default, so anything but a stored `true` — nothing picked
 * yet, a browser that refuses storage, a value this version does not know — draws them so, and going back to them
 * stores nothing.
 */
export const excelFormattingSetting = storedSetting(THEME_EXCEL_FORMATTING_KEY, false, booleanText)

/** Where the picked theme is remembered between visits. */
export const THEME_NAME_KEY = 'openl.theme.name'

/** The theme a user who has not picked one is given. */
export const DEFAULT_THEME_NAME: ThemeName = 'standard'

/**
 * The picked theme, remembered between visits.
 *
 * Falls back to {@link DEFAULT_THEME_NAME} when nothing was picked yet, when the browser refuses storage,
 * or when the stored theme is one this version no longer offers — only a theme of this version's own counts, not
 * a name every object answers to, such as `toString`. Picking the standard theme again stores nothing.
 */
export const themeNameSetting = storedSetting(THEME_NAME_KEY, DEFAULT_THEME_NAME, oneOf(THEME_ORDER))

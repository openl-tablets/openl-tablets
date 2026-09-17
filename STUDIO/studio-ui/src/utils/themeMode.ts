import type { ThemeAppearance, ThemeMode } from 'antd-style'
import { readStored, writeStored } from './localStore'
import { THEMES, type ThemeName } from '../styles/listPageTheme'

/** Where the picked appearance is remembered between visits. */
export const THEME_MODE_KEY = 'openl.theme.mode'

/** The appearance follows the operating system until the user picks light or dark. */
export const DEFAULT_THEME_MODE: ThemeMode = 'auto'

const THEME_MODES: readonly ThemeMode[] = ['auto', 'light', 'dark']

const isThemeMode = (value: string | null): value is ThemeMode => THEME_MODES.includes(value as ThemeMode)

/**
 * The remembered appearance.
 *
 * Falls back to {@link DEFAULT_THEME_MODE} when nothing was picked yet, when the browser refuses storage,
 * or when the stored value is not one this version knows.
 */
export const readThemeMode = (): ThemeMode => {
    const stored = readStored(THEME_MODE_KEY)
    return isThemeMode(stored) ? stored : DEFAULT_THEME_MODE
}

/** Remembers the picked appearance for the next visit. */
export const storeThemeMode = (mode: ThemeMode): void => writeStored(THEME_MODE_KEY, mode)

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
 * that refuses storage, a value this version does not know — leaves the application comfortable.
 */
export const readCompactMode = (): boolean => readStored(THEME_COMPACT_KEY) === 'true'

/** Remembers the picked density for the next visit. */
export const storeCompactMode = (compact: boolean): void => writeStored(THEME_COMPACT_KEY, String(compact))

/** Where the picked theme is remembered between visits. */
export const THEME_NAME_KEY = 'openl.theme.name'

/** The theme a user who has not picked one is given. */
export const DEFAULT_THEME_NAME: ThemeName = 'standard'

// `in` would also answer for `toString` and the rest of the prototype, and a stored name such as that
// resolves to no palette at all, so only a theme of this version's own counts.
const isThemeName = (value: string | null): value is ThemeName => value !== null && Object.hasOwn(THEMES, value)

/**
 * The remembered theme.
 *
 * Falls back to {@link DEFAULT_THEME_NAME} when nothing was picked yet, when the browser refuses storage,
 * or when the stored theme is one this version no longer offers.
 */
export const readThemeName = (): ThemeName => {
    const stored = readStored(THEME_NAME_KEY)
    return isThemeName(stored) ? stored : DEFAULT_THEME_NAME
}

/** Remembers the picked theme for the next visit. */
export const storeThemeName = (name: ThemeName): void => writeStored(THEME_NAME_KEY, name)

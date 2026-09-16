import type { ThemeMode } from 'antd-style'
import { readStored, writeStored } from './localStore'

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

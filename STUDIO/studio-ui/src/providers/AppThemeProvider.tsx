import { createContext, useCallback, useContext, useMemo, useState, type PropsWithChildren } from 'react'
import { ThemeProvider, type ThemeMode } from 'antd-style'
import { theme as antdTheme, type ThemeConfig } from 'antd'
import { readCompactMode, readThemeMode, storeCompactMode, storeThemeMode } from '../utils/themeMode'

interface AppTheme {
    /** Whether the application is laid out in the compact density. */
    compact: boolean
    setCompact: (compact: boolean) => void
}

const AppThemeContext = createContext<AppTheme>({ compact: false, setCompact: () => {} })

/** The density the user picked, and the way to change it. */
export const useAppTheme = (): AppTheme => useContext(AppThemeContext)

/**
 * The part of the theme a density decides.
 *
 * Compact is an Ant Design algorithm rather than a set of tokens: it recomputes the paddings, the control
 * heights and the font sizes, so every screen tightens at once. The comfortable density adds nothing —
 * the appearance algorithm alone already describes it.
 *
 * An area with a theme of its own merges this in, or it would lay itself out comfortably inside a compact
 * application.
 */
export const densityTheme = (compact: boolean): ThemeConfig =>
    (compact ? { algorithm: antdTheme.compactAlgorithm } : {})

/**
 * Gives the whole application the appearance the user picked — light, dark, or the one the operating
 * system asks for — and the density they picked with it.
 *
 * Both choices are restored from browser storage on start and written back whenever they change, so they
 * survive a reload. Under `auto` the provider follows the system and repaints when the system switches.
 */
export const AppThemeProvider = ({ children }: PropsWithChildren) => {
    const [themeMode, setThemeMode] = useState<ThemeMode>(readThemeMode)
    const [compact, setCompactState] = useState<boolean>(readCompactMode)

    const onThemeModeChange = useCallback((mode: ThemeMode) => {
        storeThemeMode(mode)
        setThemeMode(mode)
    }, [])

    const setCompact = useCallback((next: boolean) => {
        storeCompactMode(next)
        setCompactState(next)
    }, [])

    const appTheme = useMemo(() => ({ compact, setCompact }), [compact, setCompact])

    return (
        <AppThemeContext.Provider value={appTheme}>
            <ThemeProvider onThemeModeChange={onThemeModeChange} theme={densityTheme(compact)} themeMode={themeMode}>
                {children}
            </ThemeProvider>
        </AppThemeContext.Provider>
    )
}

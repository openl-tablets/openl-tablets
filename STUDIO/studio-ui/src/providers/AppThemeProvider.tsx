import { useCallback, useState, type PropsWithChildren } from 'react'
import { ThemeProvider, type ThemeMode } from 'antd-style'
import { readThemeMode, storeThemeMode } from '../utils/themeMode'

/**
 * Gives the whole application the appearance the user picked — light, dark, or the one the operating
 * system asks for.
 *
 * The choice is restored from browser storage on start and written back whenever it changes, so it
 * survives a reload. Under `auto` the provider follows the system and repaints when the system switches.
 */
export const AppThemeProvider = ({ children }: PropsWithChildren) => {
    const [themeMode, setThemeMode] = useState<ThemeMode>(readThemeMode)

    const onThemeModeChange = useCallback((mode: ThemeMode) => {
        storeThemeMode(mode)
        setThemeMode(mode)
    }, [])

    return (
        <ThemeProvider onThemeModeChange={onThemeModeChange} themeMode={themeMode}>
            {children}
        </ThemeProvider>
    )
}

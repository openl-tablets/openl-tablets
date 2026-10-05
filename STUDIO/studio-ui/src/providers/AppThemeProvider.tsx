import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { ThemeProvider, useTheme, type CustomTokenParams, type ThemeAppearance, type ThemeMode } from 'antd-style'
import { theme as antdTheme, type ThemeConfig } from 'antd'
import {
    appearanceOf,
    readCompactMode,
    readTablesFollowTheme,
    readThemeMode,
    readThemeName,
    storeCompactMode,
    storeTablesFollowTheme,
    storeThemeMode,
    storeThemeName,
} from '../utils/themeMode'
import { appTheme } from '../styles/appTheme'
import { paletteOf } from '../styles/listPageTheme'
import type { ThemeName } from '../styles/themes'
import '../styles/customToken'

interface AppTheme {
    /** Whether the application is laid out in the compact density. */
    compact: boolean
    setCompact: (compact: boolean) => void
    /** The theme whose colours the application is painted in. */
    themeName: ThemeName
    setThemeName: (name: ThemeName) => void
    /** Whether the tables are drawn in the look of the theme in place of the table theme, where it has one for them. */
    tablesFollowTheme: boolean
    setTablesFollowTheme: (follow: boolean) => void
}

const AppThemeContext = createContext<AppTheme>({
    compact: false,
    setCompact: () => {},
    themeName: 'standard',
    setThemeName: () => {},
    tablesFollowTheme: false,
    setTablesFollowTheme: () => {},
})

/** The theme and the density the user picked, and the way to change them. */
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
 * Tints the browser's own chrome — the address bar on a phone, the title bar of an installed app — in the
 * page's surface colour, so it follows the theme and the appearance like the page does. Renders nothing.
 */
const ThemeColorMeta = () => {
    const { colorBgContainer } = useTheme()
    useEffect(() => {
        let meta = document.head.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
        if (!meta) {
            meta = document.createElement('meta')
            meta.name = 'theme-color'
            document.head.append(meta)
        }
        meta.content = colorBgContainer
    }, [colorBgContainer])
    return null
}

/**
 * Gives the whole application the theme the user picked, the appearance they picked — light, dark, or the
 * one the operating system asks for — and the density they picked with them.
 *
 * All three choices are restored from browser storage on start and written back whenever they change, so
 * they survive a reload. The first render is already drawn in the remembered appearance: antd-style would
 * otherwise start light and switch in an effect, which a dark reader sees as a white flash on every load.
 * Under `auto` the provider follows the system and repaints when the system switches. The three are
 * independent: every theme has a variant for either appearance, worn at either density. The provider also keeps,
 * remembered the same way, whether the tables are drawn in the look of the theme in place of the table theme (My
 * Settings).
 *
 * The provider adds the algorithm of the appearance, dark or default, to the tokens the theme hands over. The
 * palette of the theme and appearance in force is read off the token it works out, and travels down as the `openl`
 * custom token, so a style that needs a real colour reads it from the theme instead of importing a palette of its
 * own.
 */
export const AppThemeProvider = ({ children }: PropsWithChildren) => {
    const [themeMode, setThemeMode] = useState<ThemeMode>(readThemeMode)
    const [compact, setCompact] = useState<boolean>(readCompactMode)
    const [themeName, setThemeName] = useState<ThemeName>(readThemeName)
    const [tablesFollowTheme, setTablesFollowTheme] = useState<boolean>(readTablesFollowTheme)

    const onThemeModeChange = useCallback((mode: ThemeMode) => {
        storeThemeMode(mode)
        setThemeMode(mode)
    }, [])

    const rememberCompact = useCallback((next: boolean) => {
        storeCompactMode(next)
        setCompact(next)
    }, [])

    const rememberThemeName = useCallback((next: ThemeName) => {
        storeThemeName(next)
        setThemeName(next)
    }, [])

    const rememberTablesFollowTheme = useCallback((next: boolean) => {
        storeTablesFollowTheme(next)
        setTablesFollowTheme(next)
    }, [])

    const value = useMemo(
        () => ({
            compact,
            setCompact: rememberCompact,
            setTablesFollowTheme: rememberTablesFollowTheme,
            setThemeName: rememberThemeName,
            tablesFollowTheme,
            themeName,
        }),
        [compact, rememberCompact, rememberTablesFollowTheme, rememberThemeName, tablesFollowTheme, themeName]
    )

    const theme = useCallback(
        (appearance: ThemeAppearance) => ({
            ...appTheme(themeName, appearance === 'dark'),
            ...densityTheme(compact),
        }),
        [compact, themeName]
    )

    const customToken = useCallback(
        ({ token, isDarkMode }: CustomTokenParams) => ({ openl: paletteOf(token, themeName, isDarkMode) }),
        [themeName]
    )

    return (
        <AppThemeContext.Provider value={value}>
            <ThemeProvider
                customToken={customToken}
                defaultAppearance={appearanceOf(themeMode)}
                onThemeModeChange={onThemeModeChange}
                theme={theme}
                themeMode={themeMode}
            >
                <ThemeColorMeta />
                {children}
            </ThemeProvider>
        </AppThemeContext.Provider>
    )
}

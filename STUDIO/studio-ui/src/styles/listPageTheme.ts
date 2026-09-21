/**
 * The colours OpenL Studio paints itself with — one set of them per theme and appearance.
 *
 * Every palette carries the same keys, so a colour has a counterpart in the other appearance and in every
 * other theme. The palette in force is published as CSS custom properties by `AppStyles`, and styles refer
 * to a colour through {@link LIST_PAGE_COLORS} — the custom property, not a fixed value — so switching the
 * theme or the appearance repaints them without re-rendering anything.
 *
 * Ant Design cannot read a custom property: it derives whole palettes from a colour, so a `ThemeConfig`
 * token takes the palette itself — see {@link paletteOf}.
 */
export interface Palette {
    primary: string
    /** The wordmark in the header — the one hue that is not an Ant Design token. */
    brand: string
    primaryFg: string
    pageBg: string
    containerBg: string
    text: string
    textSecondary: string
    textTertiary: string
    textQuaternary: string
    border: string
    borderSecondary: string
    secondaryBg: string
    accent: string
    accentFg: string
    sidebarBg: string
    success: string
    warning: string
    info: string
    error: string
    /** Compilation state — OpenL's core signal — kept distinct from the brand indigo. */
    compileOk: string
    compileWarnings: string
    compileErrors: string
    compileCompiling: string
    /** A module that was never compiled, or whose compilation was cancelled. */
    compileIdle: string
    /**
     * Fills of the solid status badges that carry white text. They are deepened on purpose: a badge is
     * read at a glance, so white on the fill clears WCAG AA (>= 4.5:1) in either appearance.
     */
    statusNeutral: string
    statusRunning: string
    statusPaused: string
    statusFinished: string
    statusFailed: string
    /** Syntax highlighting of a parameter value, in the manner of a code editor. */
    syntaxName: string
    syntaxString: string
    syntaxNumber: string
    syntaxBoolean: string
}

/** Colours of the standard theme in the light appearance — the Figma mockup palette (oklch → sRGB). */
export const LIGHT_PALETTE: Palette = {
    primary: '#2757b6',
    brand: '#384f81',
    primaryFg: '#f9fcff',
    pageBg: '#f6f8fc',
    containerBg: '#ffffff',
    text: '#131922',
    textSecondary: '#4b525c',
    textTertiary: '#646972',
    textQuaternary: '#8b9199',
    border: '#dbdee2',
    borderSecondary: '#e7e9ee',
    secondaryBg: '#eef0f3',
    accent: '#e4ecf9',
    accentFg: '#223251',
    sidebarBg: '#f4f5f7',
    success: '#249057',
    warning: '#df911a',
    info: '#2a75ba',
    error: '#de2024',
    compileOk: '#1f8a63',
    compileWarnings: '#c0851b',
    compileErrors: '#cf4436',
    compileCompiling: '#2a75ba',
    compileIdle: '#8b9199',
    statusNeutral: '#595959',
    statusRunning: '#0958d9',
    statusPaused: '#8c5a00',
    statusFinished: '#237804',
    statusFailed: '#cf1322',
    syntaxName: '#871094',
    syntaxString: '#067d17',
    syntaxNumber: '#1750eb',
    syntaxBoolean: '#0033b3',
}

/**
 * Colours of the standard theme in the dark appearance. Each one plays the role its light counterpart
 * plays: the surfaces run from the page through the container to the sidebar, and the text and accent
 * hues are lifted until they read against those surfaces.
 */
export const DARK_PALETTE: Palette = {
    primary: '#6f9bec',
    brand: '#9db4e8',
    primaryFg: '#0b1220',
    pageBg: '#14181f',
    containerBg: '#1b202a',
    text: '#e6e9ef',
    textSecondary: '#b3bac5',
    textTertiary: '#939ba7',
    textQuaternary: '#727a86',
    border: '#39404c',
    borderSecondary: '#2a303a',
    secondaryBg: '#232935',
    accent: '#24314a',
    accentFg: '#c6d6f2',
    sidebarBg: '#171b23',
    success: '#43b581',
    warning: '#e8b339',
    info: '#5aa9e6',
    error: '#f2695f',
    compileOk: '#3fba8a',
    compileWarnings: '#dcae4a',
    compileErrors: '#ef6f62',
    compileCompiling: '#5aa9e6',
    compileIdle: '#8a929e',
    statusNeutral: '#64686f',
    statusRunning: '#1554c9',
    statusPaused: '#96631a',
    statusFinished: '#2a8a10',
    statusFailed: '#c62230',
    syntaxName: '#c77dbb',
    syntaxString: '#6a8759',
    syntaxNumber: '#6897bb',
    syntaxBoolean: '#cc7832',
}

/**
 * Colours of the Evergreen theme in the light appearance.
 *
 * It answers the standard theme's indigo with a deep teal, and warms the neutrals towards green, so the
 * two themes are told apart at a glance rather than by looking for the one control that differs.
 */
export const EVERGREEN_LIGHT_PALETTE: Palette = {
    primary: '#0f766e',
    brand: '#115e59',
    primaryFg: '#f0fdfa',
    pageBg: '#f4faf8',
    containerBg: '#ffffff',
    text: '#12201d',
    textSecondary: '#47554f',
    textTertiary: '#5f6d67',
    textQuaternary: '#8a9791',
    border: '#d7e0dc',
    borderSecondary: '#e4ebe8',
    secondaryBg: '#eaf1ee',
    accent: '#d8efe9',
    accentFg: '#17453d',
    sidebarBg: '#f2f6f4',
    success: '#1f8a52',
    warning: '#c67c12',
    info: '#0e7490',
    error: '#c8362f',
    compileOk: '#157f5b',
    compileWarnings: '#b37714',
    compileErrors: '#c33f31',
    compileCompiling: '#0e7490',
    compileIdle: '#8a9791',
    statusNeutral: '#4f5a56',
    statusRunning: '#0b6a76',
    statusPaused: '#7d5400',
    statusFinished: '#1f6f18',
    statusFailed: '#b81f2a',
    syntaxName: '#7b2f8f',
    syntaxString: '#06703f',
    syntaxNumber: '#0f5fa8',
    syntaxBoolean: '#0b3f8f',
}

/** Colours of the Evergreen theme in the dark appearance, lifted until they read on its darker surfaces. */
const EVERGREEN_DARK_PALETTE: Palette = {
    primary: '#4fd1c5',
    brand: '#7fe3d6',
    primaryFg: '#07201d',
    pageBg: '#101917',
    containerBg: '#16211f',
    text: '#e3ece9',
    textSecondary: '#b0bfba',
    textTertiary: '#8f9f9a',
    textQuaternary: '#6e7d78',
    border: '#34423e',
    borderSecondary: '#26322f',
    secondaryBg: '#1f2b28',
    accent: '#1d3b36',
    accentFg: '#b8e8df',
    sidebarBg: '#131d1b',
    success: '#45b87f',
    warning: '#e0ab45',
    info: '#4fb6cf',
    error: '#ef6f62',
    compileOk: '#3fba8a',
    compileWarnings: '#dcae4a',
    compileErrors: '#ef6f62',
    compileCompiling: '#4fb6cf',
    compileIdle: '#8f9f9a',
    statusNeutral: '#5b6763',
    statusRunning: '#0f6c78',
    statusPaused: '#8a6318',
    statusFinished: '#26801d',
    statusFailed: '#bf2733',
    syntaxName: '#c77dbb',
    syntaxString: '#7bab6a',
    syntaxNumber: '#6ab0c8',
    syntaxBoolean: '#d59a4a',
}

/**
 * The themes OpenL Studio offers, each in both appearances.
 *
 * A theme decides the hues; the appearance decides whether they are laid on light or on dark surfaces. The
 * two choices are independent, so every theme is picked in either appearance.
 */
export const THEMES = {
    standard: { light: LIGHT_PALETTE, dark: DARK_PALETTE },
    evergreen: { light: EVERGREEN_LIGHT_PALETTE, dark: EVERGREEN_DARK_PALETTE },
} as const

/** The theme a user may pick. */
export type ThemeName = keyof typeof THEMES

/** The themes in the order the switcher offers them, the standard one first. */
export const THEME_ORDER: readonly ThemeName[] = ['standard', 'evergreen']

/** The palette a theme paints with in the given appearance. */
export const paletteOf = (name: ThemeName, isDarkMode: boolean): Palette => THEMES[name][isDarkMode ? 'dark' : 'light']

/** The custom property a colour is published under, e.g. `textSecondary` → `--openl-text-secondary`. */
const variableName = (key: keyof Palette): string =>
    `--openl-${key.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)}`

const paletteKeys = Object.keys(LIGHT_PALETTE) as (keyof Palette)[]

/**
 * The palette as CSS custom properties, for styles that follow the appearance in force.
 *
 * Stable colours shared by list pages, including React fragments mounted outside the main application
 * theme. Each value is a `var(...)` reference, so it may be used anywhere CSS accepts a colour — but not
 * where a real colour is required, such as an Ant Design token or a canvas.
 */
export const LIST_PAGE_COLORS: Palette = paletteKeys.reduce((colors, key) => {
    colors[key] = `var(${variableName(key)})`
    return colors
}, {} as Palette)

/** The declarations that publish {@link palette}, for the `:root` rule of the global stylesheet. */
export const paletteVariables = (palette: Palette): string =>
    paletteKeys.map(key => `${variableName(key)}: ${palette[key]};`).join('\n')

/**
 * The colours OpenL Studio paints itself with, in a light and a dark variant.
 *
 * The two variants carry the same keys, so every colour has a counterpart in the other appearance. The
 * variant in force is published as CSS custom properties by `AppStyles`, and styles refer to a colour
 * through {@link LIST_PAGE_COLORS} — the custom property, not a fixed value — so switching the theme
 * repaints them without re-rendering anything.
 *
 * Ant Design cannot read a custom property: it derives whole palettes from a colour, so the theme
 * configuration takes the variant itself ({@link LIGHT_PALETTE} or {@link DARK_PALETTE}).
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

/** Colours of the light appearance — the Figma mockup palette (oklch → sRGB). */
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
 * Colours of the dark appearance. Each one plays the role its light counterpart plays: the surfaces run
 * from the page through the container to the sidebar, and the text and accent hues are lifted until they
 * read against those surfaces.
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

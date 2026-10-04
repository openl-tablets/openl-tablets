import type { GlobalToken } from 'antd'
import { contrastRatio, mix, opaque } from './colorMath'
import { variantOf, type ThemeName } from './themes'

/**
 * The colours OpenL Studio paints itself with, by name — one set of them per theme and appearance.
 *
 * Every palette carries the same names, so a colour has a counterpart in the other appearance and in every
 * other theme. The palette in force is published as CSS custom properties by `AppStyles`, and styles refer
 * to a colour through {@link LIST_PAGE_COLORS} — the custom property, not a fixed value — so switching the
 * theme or the appearance repaints them without re-rendering anything.
 *
 * The names are known before any palette is worked out, so a module that only refers to the colours never
 * calls on Ant Design while it loads.
 */
const PALETTE_KEYS = [
    'primary',
    // The wordmark in the header, the shading of the logo, and the text on an accent: the primary one step
    // towards the text.
    'brand',
    'primaryFg',
    'pageBg',
    'containerBg',
    'text',
    'textSecondary',
    'textTertiary',
    'textQuaternary',
    'border',
    'borderSecondary',
    'secondaryBg',
    'accent',
    'success',
    'warning',
    'info',
    'error',
    // Fills of the solid status badges that carry white text. They are deepened on purpose: a badge is read at a
    // glance, so white on the fill clears WCAG AA (>= 4.5:1) in every theme and appearance.
    'statusNeutral',
    'statusRunning',
    'statusPaused',
    'statusFinished',
    'statusFailed',
    // Syntax highlighting of a parameter value, in the hues the theme's code editor gives a value.
    'syntaxName',
    'syntaxString',
    'syntaxNumber',
    'syntaxBoolean',
] as const

/**
 * A palette: a colour for every name of {@link PALETTE_KEYS}.
 *
 * Ant Design cannot read a custom property, so a `ThemeConfig` token takes the palette itself — see
 * {@link paletteOf}.
 */
export type Palette = Record<(typeof PALETTE_KEYS)[number], string>

/** The colour, or the shade of it, that white text clears WCAG AA on: the fill of a solid badge. */
const badgeFill = (color: string): string => {
    let fill = color
    for (let share = 0.02; contrastRatio(fill, '#ffffff') < 4.5; share += 0.02) {
        fill = mix(color, '#000000', share)
    }
    return fill
}

/**
 * The palette a theme paints with in the given appearance, read off the Ant Design token worked out for it.
 *
 * The token is the one the theme provider computes anyway, so a style that takes the palette and a component that
 * takes the token agree.
 */
export const paletteOf = (token: GlobalToken, name: ThemeName, isDarkMode: boolean): Palette => {
    const { editor, syntax } = variantOf(name, isDarkMode)
    // Darker on a light surface and lighter on a dark one: the shades Ant Design presses and hovers a primary in.
    const strongPrimary = isDarkMode ? token.colorPrimaryHover : token.colorPrimaryActive
    const plainCode = editor?.foreground ?? token.colorText
    return {
        primary: token.colorPrimary,
        brand: strongPrimary,
        primaryFg: token.colorTextLightSolid,
        pageBg: token.colorBgLayout,
        containerBg: token.colorBgContainer,
        text: token.colorText,
        textSecondary: token.colorTextSecondary,
        textTertiary: token.colorTextTertiary,
        textQuaternary: token.colorTextQuaternary,
        border: token.colorBorder,
        borderSecondary: token.colorBorderSecondary,
        secondaryBg: opaque(token.colorFillSecondary, token.colorBgContainer),
        accent: token.colorPrimaryBg,
        success: token.colorSuccess,
        warning: token.colorWarning,
        info: token.colorInfo,
        error: token.colorError,
        statusNeutral: badgeFill(opaque(token.colorTextSecondary, token.colorBgContainer)),
        statusRunning: badgeFill(token.colorInfo),
        statusPaused: badgeFill(token.colorWarning),
        statusFinished: badgeFill(token.colorSuccess),
        statusFailed: badgeFill(token.colorError),
        syntaxName: syntax.name ?? plainCode,
        syntaxString: syntax.string ?? plainCode,
        syntaxNumber: syntax.number ?? plainCode,
        syntaxBoolean: syntax.boolean ?? plainCode,
    }
}

/** A capital of a camel-case name as a kebab-case name writes it, e.g. `S` → `-s`. */
const kebabLetter = (letter: string): string => `-${letter.toLowerCase()}`

/** The custom property a colour is published under, e.g. `textSecondary` → `--openl-text-secondary`. */
const variableName = (key: keyof Palette): string => `--openl-${key.replace(/[A-Z]/g, kebabLetter)}`

/**
 * The palette as CSS custom properties, for styles that follow the appearance in force.
 *
 * Stable colours shared by list pages, including React fragments mounted outside the main application
 * theme. Each value is a `var(...)` reference, so it may be used anywhere CSS accepts a colour — but not
 * where a real colour is required, such as an Ant Design token or a canvas.
 */
export const LIST_PAGE_COLORS: Palette = PALETTE_KEYS.reduce((colors, key) => {
    colors[key] = `var(${variableName(key)})`
    return colors
}, {} as Palette)

/** The declarations that publish {@link palette}, for the `:root` rule of the global stylesheet. */
export const paletteVariables = (palette: Palette): string =>
    PALETTE_KEYS.map(key => `${variableName(key)}: ${palette[key]};`).join('\n')

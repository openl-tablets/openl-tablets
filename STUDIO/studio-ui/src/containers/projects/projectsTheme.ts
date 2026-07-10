import type { ThemeConfig } from 'antd'

/**
 * Exact design tokens from the Projects Figma mockup (oklch → sRGB), light theme only. Used both by the
 * scoped Ant Design theme below and by co-located `createStyles` where a hue must match pixel-for-pixel and
 * has no Ant Design token (accent hover, sidebar, monospace stack).
 */
export const MOCKUP = {
    primary: '#2757b6',
    primaryFg: '#f9fcff',
    // Neutrals biased a touch toward the brand indigo so the page ground reads chosen, not inherited.
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
    fontMono: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace",
} as const

/**
 * First-class palette for the compilation state — OpenL's core signal — kept deliberately distinct from
 * the brand indigo and refined off the Ant Design semantic defaults. Keyed by {@code ProjectCompileState}.
 */
export const COMPILE_COLORS = {
    ok: '#1f8a63',
    warnings: '#c0851b',
    errors: '#cf4436',
    compiling: '#2a75ba',
    idle: '#8b9199',
} as const

/** Single-line clipping. Interpolated into co-located `css` template literals, like {@link MOCKUP.fontMono}. */
export const ELLIPSIS = 'overflow: hidden; text-overflow: ellipsis; white-space: nowrap;'

/** The monospace base (font, size, line-height) shared by the chip-like labels (MonoChip, RepoBadge). */
export const MONO_TEXT = `font-family: ${MOCKUP.fontMono}; font-size: 12px; line-height: 18px;`

/**
 * Ant Design theme scoped to the Projects tab only (mounted by {@link ProjectsThemeProvider} around the two
 * `/projects` route elements). It never leaks to the shared Header, Editor or Administration screens.
 */
export const PROJECTS_THEME: ThemeConfig = {
    token: {
        colorPrimary: MOCKUP.primary,
        colorInfo: MOCKUP.info,
        colorSuccess: MOCKUP.success,
        colorWarning: MOCKUP.warning,
        colorError: MOCKUP.error,
        colorTextBase: MOCKUP.text,
        colorText: MOCKUP.text,
        colorTextSecondary: MOCKUP.textSecondary,
        colorTextTertiary: MOCKUP.textTertiary,
        colorTextQuaternary: MOCKUP.textQuaternary,
        colorBgLayout: MOCKUP.pageBg,
        colorBorder: MOCKUP.border,
        colorBorderSecondary: MOCKUP.borderSecondary,
        borderRadius: 6,
        borderRadiusLG: 8,
        borderRadiusSM: 4,
        fontSize: 14,
        wireframe: false,
    },
    components: {
        Tabs: {
            horizontalItemPadding: '12px 4px',
            horizontalItemGutter: 20,
            inkBarColor: MOCKUP.primary,
            itemColor: MOCKUP.textTertiary,
            itemSelectedColor: MOCKUP.text,
            itemHoverColor: MOCKUP.text,
        },
        Button: {
            defaultBorderColor: MOCKUP.border,
            primaryShadow: 'none',
            defaultShadow: 'none',
        },
        Segmented: {
            trackBg: MOCKUP.secondaryBg,
            controlHeight: 36,
        },
        Input: {
            controlHeight: 36,
        },
        Select: {
            controlHeight: 36,
        },
    },
}

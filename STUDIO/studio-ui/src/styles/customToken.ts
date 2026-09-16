import type { Palette } from './listPageTheme'

/**
 * The palette of the theme and appearance in force, carried on the antd-style theme.
 *
 * A style that needs a real colour — a canvas, an Ant Design token, a `color-mix` — reads it from here
 * (`createStyles(({ theme }) => theme.openl.primary)`). A style that only needs CSS keeps using
 * `LIST_PAGE_COLORS`, whose custom properties `AppStyles` republishes from this very palette.
 */
declare module 'antd-style' {
    export interface CustomToken {
        openl: Palette
    }
}

export {}

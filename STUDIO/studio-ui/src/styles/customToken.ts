import type { Palette } from './listPageTheme'
import type { TableTheme } from './tableColours'

/**
 * The palette of the theme and appearance in force, carried on the antd-style theme.
 *
 * A style that needs a real colour — a canvas, an Ant Design token, a `color-mix` — reads it from here
 * (`createStyles(({ theme }) => theme.openl.primary)`). A style that only needs CSS keeps using
 * `LIST_PAGE_COLORS`, whose custom properties `AppStyles` republishes from this very palette.
 *
 * How the tables of the workbooks are drawn rides along as `table`: a style of a table or of a mark on its cells
 * reads it through `tableOf(token)`, which a part drawn without the provider, such as one a test draws alone, also
 * answers.
 */
declare module 'antd-style' {
    export interface CustomToken {
        openl: Palette
        table: TableTheme
    }
}

export {}

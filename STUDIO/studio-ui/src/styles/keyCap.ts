import type { GlobalToken } from 'antd'

/**
 * The look of a key of the keyboard written in a text, such as the Enter that sends the search off: a small cap in the
 * faint text of the theme. Every screen that names a key draws it so, the search field of the header first.
 */
export const keyCap = (token: Pick<GlobalToken, 'paddingXXS' | 'fontSizeSM' | 'colorTextTertiary' | 'colorBgContainer'
    | 'colorBorder' | 'borderRadiusSM'>): string => `
    display: inline-block;
    min-width: 20px;
    padding: 0 ${token.paddingXXS}px;
    font: inherit;
    font-size: ${token.fontSizeSM}px;
    line-height: 18px;
    text-align: center;
    color: ${token.colorTextTertiary};
    background: ${token.colorBgContainer};
    border: 1px solid ${token.colorBorder};
    border-radius: ${token.borderRadiusSM}px;
`

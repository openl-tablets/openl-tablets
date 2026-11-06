import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** What stands beside the rail: the header, the messages, and the problems of the project under them. */
    body: css`
        position: relative;
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    crumbValue: css`
        color: ${token.colorTextSecondary};
    `,
    /** The messages fill the canvas and scroll inside themselves. */
    canvas: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        padding: ${token.paddingSM}px ${token.padding}px;
        gap: ${token.marginSM}px;
    `,
    /** The table is given the height left under the band, which a virtual table needs to draw a screenful. */
    measured: css`
        flex: 1;
        min-height: 0;
        overflow: hidden;
    `,
    centered: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: ${token.marginSM}px;
        flex: 1;
        padding: ${token.paddingXL}px;
        text-align: center;
    `,
    /** The rail is dragged by its right edge, which the grip is laid along. */
    rail: css`
        position: relative;
    `,
    railTop: css`
        flex: none;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    railBody: css`
        flex: 1;
        min-height: 0;
        overflow: hidden;
        padding: 4px 8px 12px;
    `,
    /** A column is headed by the name of its language, with the file it is written in under it. */
    columnFile: css`
        display: block;
        font-weight: normal;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextSecondary};
    `,
    key: css`
        font-family: ${token.fontFamilyCode};
        word-break: break-all;
    `,
    /** A text of the language itself. */
    own: css`
        white-space: pre-wrap;
        word-break: break-word;
    `,
    /** A text the language falls back to: faint, and saying where it comes from. */
    inherited: css`
        white-space: pre-wrap;
        word-break: break-word;
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
    origin: css`
        display: block;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextQuaternary};
        font-style: normal;
    `,
    /** No language down to the default one has the key: the rule returns the key itself. */
    missing: css`
        color: ${token.colorWarningText};
    `,
    /** A text left empty on purpose. */
    empty: css`
        color: ${token.colorTextQuaternary};
    `,
    /** A cell is written in by a click anywhere on it, so it fills its place in the row. */
    cell: css`
        position: relative;
        min-height: 22px;
        padding-right: ${token.paddingMD}px;
        cursor: text;
    `,
    /** A cell whose text the reader has changed and not saved yet. */
    changed: css`
        position: relative;
        min-height: 22px;
        cursor: text;
        margin: -4px -8px;
        padding: 4px 24px 4px 5px;
        border-left: 3px solid ${token.colorWarning};
        background: ${token.colorWarningBg};
    `,
    /** The mark of a text with problems, at the end of the cell, its problems in its tooltip. */
    problemMark: css`
        position: absolute;
        top: 2px;
        right: 0;
        color: ${token.colorWarning};
    `,
    errorMark: css`
        position: absolute;
        top: 2px;
        right: 0;
        color: ${token.colorError};
    `,
    problemList: css`
        margin: ${token.marginXXS}px 0 0;
        padding-left: ${token.paddingMD}px;
        font-size: ${token.fontSizeSM}px;
    `,
    /** The problems under a text, each in the colour of a warning or an error, so they read apart from the text. */
    problemLines: css`
        margin: ${token.marginXXS}px 0 0;
        padding: 0;
        list-style: none;
        font-size: ${token.fontSizeSM}px;

        li {
            display: flex;
            align-items: baseline;
            gap: ${token.marginXXS}px;
        }
    `,
    problemWarning: css`
        color: ${token.colorWarningText};
    `,
    problemError: css`
        color: ${token.colorErrorText};
    `,
    /** How many problems stand behind a key or a language, as the tree of tables counts its errors. */
    warningCount: css`
        display: inline-block;
        min-width: 17px;
        margin-left: ${token.marginXS}px;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorWarning};
        color: ${token.colorTextLightSolid};
        font-size: ${token.fontSizeSM}px;
        font-weight: normal;
        line-height: ${token.fontSizeSM + 5}px;
        text-align: center;
    `,
    errorCount: css`
        display: inline-block;
        min-width: 17px;
        margin-left: ${token.marginXS}px;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorError};
        color: ${token.colorTextLightSolid};
        font-size: ${token.fontSizeSM}px;
        font-weight: normal;
        line-height: ${token.fontSizeSM + 5}px;
        text-align: center;
    `,
    inherit: css`
        padding: 0;
        height: auto;
        font-size: ${token.fontSizeSM}px;
    `,
    /** The picked key: its name over its text in every language. */
    keyPanel: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    keyHead: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginSM}px;
        margin-bottom: ${token.marginSM}px;
    `,
    /** A language of the picked key: its name on the left, its text on the right. */
    languageRow: css`
        display: grid;
        grid-template-columns: 220px 1fr;
        gap: ${token.marginSM}px;
        padding: ${token.paddingXS}px ${token.paddingXS}px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    /** The language a link sent the reader to. */
    languageRowAsked: css`
        background: ${token.controlItemBgActive};
    `,
    languageName: css`
        font-weight: ${token.fontWeightStrong};
    `,
}))

export type LocalizationStyles = ReturnType<typeof useStyles>['styles']

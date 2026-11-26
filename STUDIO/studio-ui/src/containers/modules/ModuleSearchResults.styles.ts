import { createStyles } from 'antd-style'
import { keyCap } from '../../styles/keyCap'
import { paperToken } from '../../styles/paper'

/**
 * The results of a search of the cells of a module, in place of the table: a band saying what was found, and the
 * tables found, one line each, by the sheet they are written on.
 *
 * <p>What the text was found in is marked as a highlighter marks a page: the text in the line of a table, and a cell
 * of a table opened under it.
 */
export const useStyles = createStyles(({ css, token }) => ({
    /** What was looked for and how much was found, read as the name of the band. */
    summary: css`
        display: flex;
        flex: 1;
        align-items: center;
        gap: ${token.marginXS}px;
        min-width: 0;
        min-height: ${token.controlHeight}px;
        font-weight: 600;
    `,
    /** The text looked for, quoted as it was typed. */
    text: css`
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    /** Where the search looked, said quietly beside what it found. */
    scope: css`
        color: ${token.colorTextSecondary};
        font-weight: normal;
        white-space: nowrap;
    `,
    /** What Enter pressed again reaches, offered after what was searched. */
    hint: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        margin-left: ${token.marginXS}px;
        color: ${token.colorTextTertiary};
        font-weight: normal;
        white-space: nowrap;
    `,
    /** The Enter key, drawn as the search field of the header draws it. */
    key: css`
        ${keyCap(token)}
    `,
    /** The tables found scroll under the band, as a table does. */
    canvas: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: ${token.paddingXS}px 0 ${token.padding}px;
        background: ${token.colorBgContainer};
    `,
    /** A sheet heads the tables written on it, as the tree groups them when it opens. */
    sheet: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        margin: ${token.marginSM}px ${token.margin}px ${token.marginXXS}px;
        color: ${token.colorTextSecondary};
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
    `,
    /** How many tables of the sheet were found, at the end of its heading. */
    count: css`
        margin-left: auto;
        font-weight: normal;
        font-variant-numeric: tabular-nums;
    `,
    list: css`
        margin: 0;
        padding: 0;
        list-style: none;
    `,
    /** One table found: the way to open it under it, and the way into it. */
    line: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        padding: 0 ${token.padding}px 0 ${token.paddingXS}px;

        &:hover {
            background: ${token.controlItemBgHover};
        }
    `,
    /** Opens the table under its line. */
    twisty: css`
        && {
            flex: none;
            color: ${token.colorTextTertiary};
        }

        .anticon {
            transition: transform ${token.motionDurationFast};
        }

        &[aria-expanded='true'] .anticon {
            transform: rotate(90deg);
        }

        @media (prefers-reduced-motion: reduce) {
            .anticon {
                transition: none;
            }
        }
    `,
    /** The line itself opens the table in the editor: its icon, the line it is written with, where it sits. */
    open: css`
        && {
            display: flex;
            flex: 1;
            align-items: center;
            gap: ${token.marginXS}px;
            min-width: 0;
            height: auto;
            padding: ${token.paddingXXS}px ${token.paddingXS}px;
            color: ${token.colorText};
            text-align: left;
        }

        &&:hover {
            color: ${token.colorLink};
        }
    `,
    /** The header of the table, read as the editor writes it. */
    signature: css`
        flex: 1;
        min-width: 0;
        overflow: hidden;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    /** Where the table sits on its sheet, as Excel names a range. */
    position: css`
        flex: none;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
        font-variant-numeric: tabular-nums;
    `,
    /** The text looked for, wherever the line of a table holds it. */
    found: css`
        padding: 0 1px;
        border-radius: 2px;
        background: ${token.colorWarningBg};
        color: inherit;
    `,
    /** The table opened under its line, as wide as its values need and scrolled beside the list. */
    preview: css`
        margin: ${token.marginXXS}px ${token.margin}px ${token.marginSM}px ${token.marginXL + token.marginXS}px;
        overflow: auto;
    `,
    /** How much of a tall table the preview shows. */
    part: css`
        display: block;
        margin-top: ${token.marginXXS}px;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
    `,
}))

/**
 * The mark on a cell of a table opened under its line that holds the text looked for: on the paper of the workbook
 * where the table is drawn in its formatting ({@link paperToken}), in the colours of the theme in the Studio look.
 */
export const useCellStyles = createStyles(({ css, token }, onPaper: boolean) => {
    const mark = onPaper ? paperToken() : token
    return {
        foundCell: css`
            background: ${mark.colorWarningBg};
            box-shadow: inset 0 0 0 2px ${mark.colorWarningBorder};
        `,
    }
})

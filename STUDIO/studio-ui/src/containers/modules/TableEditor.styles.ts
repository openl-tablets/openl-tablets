import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    /** The band of editing actions, directly above the table it acts on. */
    toolbar: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXS}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    /** What is left of the band once the actions have had their say: how much is waiting to be written. */
    pending: css`
        margin-left: auto;
        color: ${token.colorTextSecondary};
    `,
    /** The cell the reader picked, marked the way a spreadsheet marks it. */
    picked: css`
        outline: 2px solid ${token.colorPrimary};
        outline-offset: -2px;
    `,
    /** A cell written since the table was read, so the reader sees what is waiting to be saved. */
    touched: css`
        box-shadow: inset 0 0 0 100vmax ${token.colorWarningBg};
    `,
    /**
     * The cell being written into.
     *
     * <p>The input takes the whole cell so the value stays where the reader is looking at it, and its own frame
     * is dropped — the cell's outline is the frame.
     */
    input: css`
        width: 100%;
        min-width: 80px;
        margin: -${token.paddingXXS}px -${token.paddingXS}px;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        border-radius: 0;
        font-size: inherit;
    `,
    /** While the table is being edited, a cell reads as something that can be picked. */
    editable: css`
        cursor: cell;
    `,
}))

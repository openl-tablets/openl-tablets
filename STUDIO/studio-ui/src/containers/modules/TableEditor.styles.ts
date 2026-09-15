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
    /**
     * The cell a compilation message was raised against, outlined the way the old editor outlined it.
     *
     * <p>A reader arriving from a message lands on a table of any size; the mark is what tells them which cell
     * the message was about.
     */
    raised: css`
        outline: 2px solid ${token.colorError};
        outline-offset: -2px;
    `,
    /** A cell written since the table was read, so the reader sees what is waiting to be saved. */
    touched: css`
        box-shadow: inset 0 0 0 100vmax ${token.colorWarningBg};
    `,
    /**
     * The field a cell is written in.
     *
     * <p>It takes the cell and no more of it: a field asking for room of its own would widen the column, and
     * the whole table would shift under the reader as they open a cell and settle back as they leave it.
     *
     * <p>It wears the cell's font and alignment, so a value reads while it is being written the way it will read
     * once it is written — which is what the Editor did by putting the cell's own styling on its field. Its
     * ground stays a field's own, whatever colour the cell is painted: a field is what the reader writes in,
     * and a value being typed over a dark fill is a value they cannot read.
     */
    input: css`
        width: 100%;
        min-width: 0;
        border-radius: 0;
        background: ${token.colorBgContainer};
        font: inherit;
        text-align: inherit;
    `,
    /** The open cell, which the field fills. */
    open: css`
        width: 100%;
    `,
    /** While the table is being edited, a cell reads as something that can be picked. */
    editable: css`
        cursor: cell;
    `,
}))

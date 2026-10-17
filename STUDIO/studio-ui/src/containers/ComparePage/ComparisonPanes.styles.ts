import { createStyles } from 'antd-style'
import { paperToken } from '../../styles/paper'

/** The marks a comparison lays on the cells of a workbook, in the colours of its paper ({@link paperToken}). */
export const useCellStyles = createStyles(({ css }) => ({
    // The sign a row of the combined view is read by, in a column of its own before the table.
    combinedLead: css`
        width: 1.5em;
        color: ${paperToken().colorTextTertiary};
        text-align: center;
    `,
    // What the first file had, beside what the second one has in its place.
    combinedBefore: css`
        color: ${paperToken().colorTextTertiary};
        text-decoration: line-through;
    `,
    // The cells that read differently in the other file, in the colour the rest of Studio marks a change with.
    changed: css`
        background: ${paperToken().colorWarningBg};
    `,
    // A cell a branch of a merge changed: framed rather than filled, so its own formatting stays in sight.
    changedFrame: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorWarning};
    `,
    // A cell both branches of a merge changed differently: framed in red, so it stands out among the changed cells.
    conflict: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorError};
    `,
    // A version of a table whose formatting its branch changed: the whole table framed with a dashed line.
    restyled: css`
        width: fit-content;
        max-width: 100%;
        outline: 2px dashed ${paperToken().colorWarning};
        outline-offset: 2px;
    `,
    // A row of the table only the second file has.
    rowAdded: css`
        background: ${paperToken().colorSuccessBg};
    `,
    // A row of the table only the first file has.
    rowRemoved: css`
        background: ${paperToken().colorErrorBg};
    `,
}))

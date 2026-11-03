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
    // The cells that read differently in the other file: framed rather than filled, as a merge frames a change, so
    // their own formatting stays in sight. The sides the frame runs along are set by the classes below.
    changed: css`
        box-shadow: inset var(--frame-left, 0px) 0 0 0 ${paperToken().colorWarning},
            inset calc(-1 * var(--frame-right, 0px)) 0 0 0 ${paperToken().colorWarning},
            inset 0 var(--frame-top, 0px) 0 0 ${paperToken().colorWarning},
            inset 0 calc(-1 * var(--frame-bottom, 0px)) 0 0 ${paperToken().colorWarning};
    `,
    frameTop: css`
        --frame-top: 2px;
    `,
    frameRight: css`
        --frame-right: 2px;
    `,
    frameBottom: css`
        --frame-bottom: 2px;
    `,
    frameLeft: css`
        --frame-left: 2px;
    `,
    // A cell a branch of a merge changed: framed rather than filled, so its own formatting stays in sight.
    changedFrame: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorWarning};
    `,
    // A cell both branches of a merge changed differently: framed in red, so it stands out among the changed cells.
    conflict: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorError};
    `,
    // A cell of a table merged cell by cell the merge took from one branch by itself: framed in the colour of success.
    merged: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorSuccess};
    `,
    // A conflicting cell of a table merged cell by cell that the user chose a version for, in the result and in the
    // version chosen: framed in yellow.
    chosen: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorWarning};
    `,
    // A cell of a table merged cell by cell that the user typed a value for.
    edited: css`
        box-shadow: inset 0 0 0 2px ${paperToken().colorInfo};
    `,
    // The cell of a table merged cell by cell the user picked, outlined over its frame.
    picked: css`
        outline: 2px solid ${paperToken().colorPrimary};
        outline-offset: -4px;
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

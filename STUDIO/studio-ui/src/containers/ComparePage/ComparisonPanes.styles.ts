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
    // A row of the table only the second file has.
    rowAdded: css`
        background: ${paperToken().colorSuccessBg};
    `,
    // A row of the table only the first file has.
    rowRemoved: css`
        background: ${paperToken().colorErrorBg};
    `,
}))

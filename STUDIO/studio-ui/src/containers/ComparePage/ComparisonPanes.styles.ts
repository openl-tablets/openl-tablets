import { createStyles } from 'antd-style'
import { paperToken } from '../../styles/paper'
import type { FrameSides } from './frames'

/**
 * A frame drawn inside a cell, in a colour. It runs along every side the cell does not leave open ({@link sidesOf}).
 */
const framed = (colour: string) => `
    box-shadow: inset var(--frame-left, 2px) 0 0 0 ${colour},
        inset calc(-1 * var(--frame-right, 2px)) 0 0 0 ${colour},
        inset 0 var(--frame-top, 2px) 0 0 ${colour},
        inset 0 calc(-1 * var(--frame-bottom, 2px)) 0 0 ${colour};
`

/** The dashed frame of a table whose formatting changed, drawn round a box as wide as what it holds. */
const dashed = () => `
    width: fit-content;
    outline: 2px dashed ${paperToken().colorWarning};
    outline-offset: 2px;
`

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
    // Which version of a conflict a value of a cell drawn as one table is, written before the value.
    combinedVersion: css`
        color: ${paperToken().colorTextSecondary};
        font-size: ${paperToken().fontSizeSM}px;
    `,
    // The sign between the two values of a conflict, in the colour its frame is drawn in.
    combinedConflict: css`
        color: ${paperToken().colorError};
    `,
    // A cell a branch of a merge changed, or a cell the other file of a comparison reads differently: framed rather
    // than filled, so its own formatting stays in sight.
    changedFrame: css`
        ${framed(paperToken().colorWarning)}
    `,
    // A cell both branches of a merge changed differently: framed in red, so it stands out among the changed cells.
    conflict: css`
        ${framed(paperToken().colorError)}
    `,
    // A cell of a table merged cell by cell the merge took from one branch by itself: framed in the colour of success.
    merged: css`
        ${framed(paperToken().colorSuccess)}
    `,
    // A conflicting cell of a table merged cell by cell that the user chose a version for, in the result and in the
    // version chosen: framed in yellow.
    chosen: css`
        ${framed(paperToken().colorWarning)}
    `,
    // A cell of a table merged cell by cell that the user typed a value for.
    edited: css`
        ${framed(paperToken().colorInfo)}
    `,
    // The sides of a framed cell its frame leaves open, since the cell beyond is framed the same way.
    openTop: css`
        --frame-top: 0px;
    `,
    openRight: css`
        --frame-right: 0px;
    `,
    openBottom: css`
        --frame-bottom: 0px;
    `,
    openLeft: css`
        --frame-left: 0px;
    `,
    // The cell of a table merged cell by cell the user picked, outlined over its frame.
    picked: css`
        outline: 2px solid ${paperToken().colorPrimary};
        outline-offset: -4px;
    `,
    // A version of a table whose formatting its branch changed, in a box that scrolls the table itself: the box is
    // framed with a dashed line and keeps to the room it is given.
    restyled: css`
        max-width: 100%;
        ${dashed()}
    `,
    // A table whose formatting changed, in a pane that scrolls it: the frame is as wide as the table, however far the
    // table runs past the pane, so it is scrolled with the table.
    restyledWhole: css`
        ${dashed()}
    `,
    // A row standing for the equal rows left out between the rows shown, as a diff marks the lines it skips.
    gap: css`
        color: ${paperToken().colorTextTertiary};
        background: ${paperToken().colorFillAlter};
        text-align: center;
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

/** The classes that leave the sides of a frame open. */
type OpenSides = Pick<ReturnType<typeof useCellStyles>['styles'], 'openTop' | 'openRight' | 'openBottom' | 'openLeft'>

/** The classes that leave open the sides of a framed cell its frame does not run along. */
export const sidesOf = (styles: OpenSides, sides: FrameSides | undefined): string => [
    sides?.top === false && styles.openTop,
    sides?.right === false && styles.openRight,
    sides?.bottom === false && styles.openBottom,
    sides?.left === false && styles.openLeft,
].filter(Boolean).join(' ')

import { createStyles, keyframes } from 'antd-style'

/** The height of the bar a virtual table scrolls sideways with, which it lays over the bottom of its rows. */
export const SIDEWAYS_BAR = 8

/**
 * What the screen of the messages draws that no component of antd draws: the room a virtual table is measured in, a
 * text kept as it is written, a piece of a text marked as a code editor marks a problem, and the light of a column or
 * a row just added. The rest is drawn by the components of antd.
 */
export const useStyles = createStyles(({ css, token }) => {
    /** The light of a column or a row just added, fading out. */
    const flash = keyframes`
        0%, 60% {
            background: ${token.colorPrimaryBg};
        }
    `
    return {
        /** The messages fill the canvas under the band. */
        canvas: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        padding: ${token.paddingSM}px ${token.padding}px;
        gap: ${token.marginSM}px;
    `,
        /**
         * The table is given the height left under the band, which a virtual table needs to draw a screenful; the page
         * of a key scrolls in it.
         */
        measured: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
        /** A text keeps its line breaks and its spaces, as it is written in the file. */
        text: css`
        white-space: pre-wrap;
    `,
        /** The piece of a text where it breaks, underlined in the colour of a warning, as a code editor marks one. */
        problemSpot: css`
        text-decoration: underline wavy ${token.colorWarning};
        text-decoration-skip-ink: none;
        text-underline-offset: 3px;
    `,
        /** A text is written in by a click anywhere on it, an empty one too. */
        cell: css`
        min-height: ${token.controlHeightSM}px;
        cursor: text;
    `,
        /** A cell of a column just added, lit for a moment so the eye lands on it. */
        flashed: css`
        animation: ${flash} 1.6s ease-out;
    `,
        /** The row of a key just added, lit for a moment as a column is. */
        flashedRow: css`
        .ant-table-cell {
            animation: ${flash} 1.6s ease-out;
        }
    `,
        /** A table scrolled sideways keeps its bar under its rows, rather than over the last one of them. */
        sideways: css`
        .ant-table-tbody-virtual {
            padding-bottom: ${SIDEWAYS_BAR}px;
        }
    `,
    }
})

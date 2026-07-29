import { createStyles } from 'antd-style'
import { idleControls } from '../tableModals/sharedStyles'

export const useStyles = createStyles(({ css, token }) => ({
    /* Hugs the table rather than the dialog, so a one-column table stays one column wide. */
    sheet: css`
        width: fit-content;
        min-width: 0;
        max-width: 100%;
        max-height: 46vh;
        overflow: auto;
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
    `,
    grid: css`
        /* Only as wide as the table itself: narrower than the dialog it is not stretched, wider it scrolls. */
        width: max-content;
        /* Separate borders: a collapsed border is dropped from a sticky header in every engine. */
        border-collapse: separate;
        border-spacing: 0;

        th,
        td {
            border-right: 1px solid ${token.colorBorderSecondary};
            border-bottom: 1px solid ${token.colorBorderSecondary};
        }

        th:last-child,
        td:last-child {
            border-right: 0;
        }

        tbody tr:last-child td {
            border-bottom: 0;
        }

        /*
         * The cell editor is the cell: full bleed, no inner border, no rounded corner inside a grid. The outlined
         * variant carries its border on the Select root, so the root is what has to be neutralised; the inner box is
         * .ant-select-content in Ant Design v6, renamed from the v5 .ant-select-selector.
         */
        .ant-select,
        .ant-input,
        .ant-input-number,
        .ant-picker {
            width: 100%;
            height: 32px;
        }

        .ant-input,
        .ant-select.ant-select-outlined,
        .ant-input-number.ant-input-number-outlined,
        .ant-picker.ant-picker-outlined {
            border: 0;
            border-radius: 0;
            background: transparent;
            box-shadow: none;
        }

        .ant-input,
        .ant-select .ant-select-content,
        .ant-picker {
            padding: 0 ${token.paddingXS}px;
        }

        .ant-input:focus,
        .ant-select-focused.ant-select-outlined,
        .ant-input-number-focused.ant-input-number-outlined,
        .ant-picker-focused.ant-picker-outlined {
            box-shadow: inset 0 0 0 2px ${token.colorPrimary};
        }
    `,
    /**
     * The OpenL table header. Set in the code face because it is the literal text written to the first cell of the
     * sheet, and spanning the full width because that is the merged region the table is written as.
     */
    headerBand: css`
        th {
            padding: ${token.paddingXS}px ${token.paddingSM}px;
            font-family: ${token.fontFamilyCode};
            font-size: ${token.fontSize}px;
            font-weight: 600;
            color: ${token.colorText};
            text-align: left;
            white-space: nowrap;
            /* Fill tokens carry alpha; laying one over an opaque base keeps a scrolled row from showing through. */
            background-color: ${token.colorBgContainer};
            background-image: linear-gradient(${token.colorFillSecondary}, ${token.colorFillSecondary});
            border-bottom: 1px solid ${token.colorBorder};
        }
    `,
    /** Column titles stay in view while the body scrolls, the way a sheet keeps its column headers. */
    columnRow: css`
        th {
            position: sticky;
            top: 0;
            z-index: 2;
            padding: ${token.paddingXXS}px ${token.paddingXS}px;
            font-size: ${token.fontSizeSM}px;
            font-weight: 500;
            color: ${token.colorTextSecondary};
            text-align: left;
            white-space: nowrap;
            background-color: ${token.colorBgContainer};
            background-image: linear-gradient(${token.colorFillAlter}, ${token.colorFillAlter});
        }
    `,
    /**
     * The rows a table type opens its body with. Tinted to read as structure rather than data, but part of the
     * sheet rather than a caption above it: the band is written to the table, and a corner merged down it cannot
     * stick while the values beside it scroll.
     */
    bandRow: css`
        td {
            background: ${token.colorFillQuaternary};
        }

        th {
            padding: ${token.paddingXXS}px ${token.paddingXS}px;
            font-size: ${token.fontSizeSM}px;
            font-weight: 500;
            color: ${token.colorTextSecondary};
            text-align: left;
            vertical-align: middle;
            white-space: nowrap;
            background: ${token.colorFillAlter};
        }
    `,
    columnHeader: css`
        display: flex;
        min-height: 24px;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;

${idleControls(token.motionDurationMid)}
        th:hover & .ant-space-compact,
        th:focus-within & .ant-space-compact {
            opacity: 1;
        }
    `,
    cell: css`
        min-width: 150px;
        padding: 0;
    `,
    /**
     * Multiple property values need room for several tags, but must wrap instead of making the sheet as wide as
     * every selected value combined.
     */
    propertyValueCell: css`
        width: clamp(320px, 40vw, 480px);
        min-width: 320px;
        max-width: 480px;
        padding: 0;

        .ant-select-multiple {
            height: auto;
            min-height: ${token.controlHeight}px;
        }
    `,
    /** Short Datatype metadata values need less room than names, types, and descriptions. */
    compactCell: css`
        width: 110px;
        min-width: 110px;
        padding: 0;
    `,
    /** Technical and business field names in the fixed part of a transposed table. */
    structureCell: css`
        min-width: 120px;
        padding: 0 ${token.paddingXS}px;
        font-size: ${token.fontSizeSM}px;
        font-weight: 500;
        color: ${token.colorTextSecondary};
        text-align: left;
        white-space: nowrap;
        background: ${token.colorFillQuaternary};
    `,
    /** Row number at rest; the anchor the reader counts rows by, and what OpenL errors refer to. */
    gutter: css`
        width: 34px;
        min-width: 34px;
        padding: 0 ${token.paddingXXS}px;
        font-size: ${token.fontSizeSM}px;
        font-variant-numeric: tabular-nums;
        color: ${token.colorTextQuaternary};
        text-align: center;
        user-select: none;
        background: ${token.colorFillQuaternary};
    `,
    rowActions: css`
        width: 78px;
        min-width: 78px;
        padding: 0 ${token.paddingXXS}px;
        white-space: nowrap;
        background: ${token.colorBgContainer};

${idleControls(token.motionDurationMid)}
        tr:hover & .ant-space-compact,
        tr:focus-within & .ant-space-compact {
            opacity: 1;
        }
    `,
}))

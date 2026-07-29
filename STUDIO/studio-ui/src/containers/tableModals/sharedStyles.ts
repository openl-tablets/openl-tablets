import { createStyles } from 'antd-style'

/**
 * Row controls at rest: present but invisible, so the width they occupy never reflows when they appear.
 *
 * The selector that reveals them differs per row kind — the row itself, its header cell, its table row — so each
 * rule pairs this with its own `:hover`/`:focus-within` selector.
 */
export const idleControls = (motionDuration: string): string => `
        .ant-space-compact {
            opacity: 0;
            transition: opacity ${motionDuration};
        }
`

/**
 * The layout the table dialogs share: the recessed settings strip, its two-column field grid, and the list of
 * editable rows below it — a signature in the create dialog, a property list in the copy dialog.
 *
 * Compose them with the dialog's own styles and override whatever differs — the later class wins:
 *
 * ```tsx
 * const { styles: shared } = useSharedStyles()
 * const { styles, cx } = useStyles()
 * <div className={cx(shared.rowColumns, shared.editableRow)} />
 * ```
 */
export const useSharedStyles = createStyles(({ css, token }) => ({
    form: css`
        display: grid;
        /* A fixed track, not one sized to its content: a wide sheet has to scroll, never widen the dialog. */
        grid-template-columns: minmax(0, 1fr);
        gap: ${token.margin}px;

        @media (prefers-reduced-motion: reduce) {
            .ant-space-compact {
                transition: none;
            }
        }
    `,
    /** Everything that configures the table sits on one recessed strip, so the sheet below reads as the document. */
    settings: css`
        padding: ${token.padding}px ${token.padding}px ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorFillQuaternary};
    `,
    fields: css`
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        column-gap: ${token.marginLG}px;

        @media (max-width: ${token.screenSM}px) {
            grid-template-columns: minmax(0, 1fr);
        }
    `,
    /** The block of editable rows, set off from the fields above it. */
    section: css`
        margin-top: ${token.marginXXS}px;
        padding-top: ${token.padding}px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    rowList: css`
        display: grid;
        gap: ${token.marginXXS}px;
    `,
    /** Two equal value fields and, at the end, room for the pair of row buttons. */
    rowColumns: css`
        display: grid;
        grid-template-columns:
            minmax(0, 1fr)
            minmax(0, 1fr)
            calc(${token.controlHeight}px * 2);
        gap: ${token.marginXS}px;
    `,
    editableRow: css`
${idleControls(token.motionDurationMid)}
        &:hover .ant-space-compact,
        &:focus-within .ant-space-compact {
            opacity: 1;
        }
    `,
    fullWidth: css`
        width: 100%;
    `,
    /** Aligns a check box with the content inset and control height used by the other table cell editors. */
    checkboxEditor: css`
        display: flex;
        width: 100%;
        min-height: ${token.controlHeight}px;
        align-items: center;
        padding: 0 ${token.paddingXS}px;
    `,
}))

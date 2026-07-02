import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    tree: css`
        display: flex;
        flex-direction: column;
        overflow: auto;
    `,
    header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: ${token.colorTextTertiary};
    `,
    // The Total/Self switch is a control, not a heading — reset the heading typography.
    timeToggle: css`
        text-transform: none;
        font-weight: normal;
        letter-spacing: normal;
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        cursor: pointer;
        white-space: nowrap;
        border-left: 2px solid transparent;
        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    frame: css`
        font-weight: 600;
    `,
    current: css`
        border-left-color: ${token.colorPrimary};
    `,
    selected: css`
        background: ${token.colorFillSecondary};
    `,
    runnable: css`
        cursor: pointer;
        &:hover {
            background: ${token.colorPrimaryBg};
        }
    `,
    // Executed and returned: shown for its result only, de-emphasised and not runnable.
    inactive: css`
        opacity: 0.6;
    `,
    // The line the engine is on right now.
    currentStep: css`
        font-weight: 600;
        background: ${token.colorWarningBg};
    `,
    // Not yet reached: a click runs execution here.
    pending: css`
        color: ${token.colorTextTertiary};
    `,
    // Expand/collapse control for an executed branch.
    chevron: css`
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 14px;
        flex: 0 0 auto;
        color: ${token.colorTextTertiary};
        cursor: pointer;
        &:hover {
            color: ${token.colorText};
        }
    `,
    // Keeps labels aligned on rows that have no chevron.
    chevronSlot: css`
        width: 14px;
        flex: 0 0 auto;
    `,
    name: css`
        overflow: hidden;
        text-overflow: ellipsis;
    `,
    // Executed-tree node header: a returned table, kept readable so its name and timing stand out.
    callNode: css`
        color: ${token.colorTextSecondary};
    `,
    // Frame kind (spreadsheet, decision, …) as a quiet eyebrow, not a boxed Tag, so it does not
    // compete with the table name on every row.
    kind: css`
        flex: 0 0 auto;
        font-size: ${token.fontSizeSM}px;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: ${token.colorTextTertiary};
    `,
    // Dispatch badge: the table is one of several versions overloaded by dimension properties; hovering
    // lists the versions with the chosen one marked.
    dispatchTag: css`
        display: inline-flex;
        align-items: center;
        gap: 2px;
        flex: 0 0 auto;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
        background: ${token.colorFillQuaternary};
        cursor: help;
    `,
    dispatchTipTitle: css`
        font-weight: 600;
        margin-bottom: ${token.marginXXS}px;
    `,
    dispatchCandidate: css`
        opacity: 0.75;
    `,
    dispatchChosen: css`
        opacity: 1;
        font-weight: 600;
        &::before {
            content: '✓ ';
        }
    `,
    // Real execution time. A length-based heat bar keeps the timing channel separate from the status
    // colours (amber/red already mean current/error), so a slow call reads by bar length, not by hue.
    duration: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        margin-left: auto;
        flex: 0 0 auto;
    `,
    durationBar: css`
        width: 56px;
        height: 4px;
        flex: 0 0 auto;
        border-radius: ${token.borderRadiusXS}px;
        background: ${token.colorFillTertiary};
        overflow: hidden;
    `,
    durationFill: css`
        display: block;
        height: 100%;
        min-width: 1px;
        border-radius: ${token.borderRadiusXS}px;
        background: ${token.colorTextTertiary};
    `,
    durationValue: css`
        min-width: 46px;
        text-align: right;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    // Re-run the trace and stop at this returned table to inspect it live.
    replay: css`
        flex: 0 0 auto;
        color: ${token.colorTextTertiary};
        cursor: pointer;
        &:hover {
            color: ${token.colorPrimary};
        }
    `,
    leafLabel: css`
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        color: ${token.colorText};
    `,
    // Status is carried by shape as well as colour, so the three step states stay distinct without relying
    // on hue: pending is a hollow ring, executed/error/frame are filled, and the current step gets a halo.
    dot: css`
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex: 0 0 auto;
        box-sizing: border-box;
        background: transparent;
        border: 1.5px solid ${token.colorTextQuaternary};
    `,
    dotExecuted: css`
        background: ${token.colorSuccess};
        border-color: ${token.colorSuccess};
    `,
    dotCurrent: css`
        background: ${token.colorWarning};
        border-color: ${token.colorWarning};
        box-shadow: 0 0 0 2px ${token.colorWarningBg};
    `,
    dotError: css`
        background: ${token.colorError};
        border-color: ${token.colorError};
    `,
    dotFrame: css`
        background: ${token.colorPrimary};
        border-color: ${token.colorPrimary};
    `,
    // Step-reference marker: the formula used a step computed elsewhere; the row links to the original.
    refIcon: css`
        width: 8px;
        flex: 0 0 auto;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    // Short attention pulse on the original step row after a reference jump.
    flashed: css`
        animation: trace-ref-flash 1.6s ease-out;
        @keyframes trace-ref-flash {
            0%, 60% {
                background: ${token.colorPrimaryBg};
            }
            100% {
                background: transparent;
            }
        }
    `,
}))

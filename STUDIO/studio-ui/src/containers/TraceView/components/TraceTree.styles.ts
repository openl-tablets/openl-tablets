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
    // Real execution time of a returned node; coloured by how hot it is relative to the slowest node.
    duration: css`
        margin-left: auto;
        flex: 0 0 auto;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    durationWarm: css`
        color: ${token.colorWarning};
    `,
    durationHot: css`
        color: ${token.colorError};
        font-weight: 600;
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
    dot: css`
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex: 0 0 auto;
        background: ${token.colorTextQuaternary};
    `,
    dotExecuted: css`
        background: ${token.colorSuccess};
    `,
    dotCurrent: css`
        background: ${token.colorWarning};
    `,
    dotError: css`
        background: ${token.colorError};
    `,
    dotFrame: css`
        background: ${token.colorPrimary};
    `,
}))

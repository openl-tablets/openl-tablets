import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    tree: css`
        display: flex;
        flex-direction: column;
        overflow: auto;
    `,
    header: css`
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: ${token.colorTextTertiary};
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
    name: css`
        overflow: hidden;
        text-overflow: ellipsis;
    `,
    leafLabel: css`
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

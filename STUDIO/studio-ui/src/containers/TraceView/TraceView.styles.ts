import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    debugView: css`
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        background: ${token.colorBgContainer};
    `,
    toolbar: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginSM}px;
        flex: 0 0 auto;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    statusPill: css`
        display: inline-flex;
        align-items: center;
        flex: 0 0 auto;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        border-radius: ${token.borderRadius}px;
        background: ${token.colorFillQuaternary};
        white-space: nowrap;

        .ant-badge-status-text {
            font-size: ${token.fontSizeSM}px;
            font-weight: 600;
            letter-spacing: 0.02em;
        }
    `,
    panels: css`
        flex: 1;
        display: flex;
        position: relative;
        min-height: 0;
    `,
    view: css`
        position: absolute;
        inset: 0;
        display: flex;
        background: ${token.colorBgContainer};
    `,
    resizing: css`
        cursor: ew-resize;
        user-select: none;
    `,
    viewError: css`
        display: flex;
        align-items: center;
        justify-content: center;
    `,
    leftPanel: css`
        min-width: 200px;
        max-width: 70%;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        border-right: 1px solid ${token.colorBorderSecondary};
    `,
    viewModeToggle: css`
        margin: ${token.marginXS}px;
        flex: 0 0 auto;
    `,
    viewContent: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        display: flex;
        flex-direction: column;
    `,
    resizer: css`
        width: 5px;
        cursor: ew-resize;
        background: ${token.colorFillTertiary};
        flex-shrink: 0;
        transition: background ${token.motionDurationMid};

        &:hover {
            background: ${token.colorPrimaryBorder};
        }
    `,
    rightPanel: css`
        min-width: 200px;
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        padding: ${token.padding}px;
    `,
    panelDisabled: css`
        pointer-events: none;
    `,
    errorBanner: css`
        flex: 0 0 auto;
        margin: ${token.marginXS}px ${token.marginSM}px 0;
    `,
    errorWhere: css`
        margin-bottom: ${token.marginXXS}px;
    `,
    errorType: css`
        font-family: ${token.fontFamilyCode};
        font-weight: 600;
        margin-bottom: ${token.marginXXS}px;
    `,
    errorStack: css`
        margin: 0;
        max-height: 220px;
        overflow: auto;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        white-space: pre;
        background: ${token.colorFillQuaternary};
        padding: ${token.paddingXS}px;
        border-radius: ${token.borderRadiusSM}px;
    `,
}))

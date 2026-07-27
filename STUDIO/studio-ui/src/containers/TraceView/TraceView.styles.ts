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
    // A solid, filled status badge: saturated background with white text, so the state is readable
    // at a glance — the tinted Tag presets blend the text into the background. The fills are deepened
    // shades (not the raw semantic tokens) chosen so white text clears WCAG AA (≥4.5:1) in either theme;
    // the default tokens are too light for white text (amber colorWarning is only 1.9:1). Contrast ratios
    // against white, computed from the hexes, are noted per fill.
    statusTag: css`
        flex: 0 0 auto;
        margin-inline-end: 0;
        padding-inline: ${token.paddingSM}px;
        font-size: ${token.fontSize}px;
        font-weight: 600;
        letter-spacing: 0.02em;
        line-height: ${token.controlHeightSM}px;
        border: none;
        color: ${token.colorWhite};
    `,
    statusNeutral: css`
        background: #595959; /* 7.0:1 */
    `,
    statusRunning: css`
        background: #0958d9; /* 6.2:1 */
    `,
    statusPaused: css`
        background: #8c5a00; /* 5.9:1 */
    `,
    statusFinished: css`
        background: #237804; /* 5.6:1 */
    `,
    statusFailed: css`
        background: #cf1322; /* 5.6:1 */
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
        flex-shrink: 0;
        width: 9px;
        cursor: ew-resize;
        display: flex;
        align-items: center;
        justify-content: center;

        // A hairline within a wide grab zone: easy to grab, quiet until hovered, then it thickens and lights up.
        &::before {
            content: '';
            width: 1px;
            height: 100%;
            background: ${token.colorBorderSecondary};
            transition: width ${token.motionDurationMid}, background ${token.motionDurationMid};
        }
        &:hover::before {
            width: 3px;
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
    runningOverlay: css`
        position: absolute;
        inset: 0;
        z-index: 10;
        display: flex;
        align-items: center;
        justify-content: center;
        background: ${token.colorBgMask};
    `,
    runningCard: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: ${token.margin}px;
        max-width: 320px;
        padding: ${token.paddingLG}px ${token.paddingXL}px;
        text-align: center;
        background: ${token.colorBgElevated};
        border-radius: ${token.borderRadiusLG}px;
        box-shadow: ${token.boxShadowSecondary};
    `,
    runningText: css`
        color: ${token.colorText};
        font-size: ${token.fontSize}px;
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

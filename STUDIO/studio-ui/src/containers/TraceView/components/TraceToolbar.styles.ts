import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    toolbar: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginSM}px;
        flex: 0 0 auto;
        min-height: ${token.controlHeight}px;
        padding: ${token.paddingXXS}px ${token.paddingSM}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    // The controls (buttons + status, or the settings) take the room left of the pinned gear and scroll
    // sideways rather than push it off when the panel is narrow — the gear stays reachable.
    main: css`
        flex: 1 1 auto;
        min-width: 0;
        overflow-x: auto;
        display: flex;
        align-items: center;
    `,
    // The gear is pinned to the right edge so the run settings are always one click away.
    gear: css`
        flex: 0 0 auto;
    `,
    // A solid, filled status badge: saturated background with white text, readable at a glance. The fills
    // are deepened shades chosen so white text clears WCAG AA (≥4.5:1) in either theme.
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
}))

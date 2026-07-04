import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: flex;
        flex-direction: column;
        gap: 1px;
        padding: 8px;
        min-height: 0;
        height: 100%;
        overflow-y: auto;
    `,
    header: css`
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 8px;
        flex-wrap: wrap;
        margin-bottom: 6px;
        font-weight: 600;
        font-size: 12px;
        color: ${token.colorTextSecondary};
        text-transform: uppercase;
        letter-spacing: 0.04em;
    `,
    summary: css`
        font-weight: 400;
        text-transform: none;
        letter-spacing: 0;
        color: ${token.colorTextTertiary};
    `,
    columns: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 0 4px 2px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 2px 4px;
        border-radius: 3px;

        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    // Heat bar for own time: the primary hot-spot metric, ranked pre-attentively by length.
    bar: css`
        flex: 0 0 64px;
        height: 6px;
        border-radius: 3px;
        background: ${token.colorFillSecondary};
        overflow: hidden;
    `,
    fill: css`
        display: block;
        height: 100%;
        background: ${token.colorWarning};
    `,
    name: css`
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        color: ${token.colorText};
    `,
    kind: css`
        flex: 0 0 auto;
        font-size: ${token.fontSizeSM}px;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: ${token.colorTextTertiary};
    `,
    count: css`
        flex: 0 0 52px;
        text-align: right;
        font-variant-numeric: tabular-nums;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    self: css`
        flex: 0 0 60px;
        text-align: right;
        font-variant-numeric: tabular-nums;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorText};
    `,
    total: css`
        flex: 0 0 60px;
        text-align: right;
        font-variant-numeric: tabular-nums;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextTertiary};
    `,
    replay: css`
        flex: 0 0 16px;
        color: ${token.colorTextTertiary};
        cursor: pointer;

        &:hover {
            color: ${token.colorPrimary};
        }
    `,
    more: css`
        margin-top: 6px;
        font-size: 12px;
        color: ${token.colorTextTertiary};
    `,
}))

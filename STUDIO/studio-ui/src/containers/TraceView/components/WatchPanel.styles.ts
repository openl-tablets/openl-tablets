import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXS}px;
        min-height: 0;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    seriesList: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXS}px;
        min-height: 0;
        max-height: 40vh;
        overflow-y: auto;
    `,
    addRow: css`
        display: flex;
        gap: ${token.marginXS}px;
    `,
    chips: css`
        display: flex;
        flex-wrap: wrap;
        gap: ${token.marginXXS}px;
    `,
    hint: css`
        font-size: 12px;
        color: ${token.colorTextTertiary};
    `,
    truncated: css`
        font-size: 12px;
        color: ${token.colorWarning};
    `,
    series: css`
        display: flex;
        flex-direction: column;
        gap: 2px;
    `,
    seriesTitle: css`
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        color: ${token.colorText};
    `,
    seriesTable: css`
        font-size: 12px;
        color: ${token.colorTextTertiary};
    `,
    point: css`
        display: flex;
        align-items: flex-start;
        gap: ${token.marginXXS}px;
        padding: 1px 2px;
        border-radius: 3px;

        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    pointLabel: css`
        color: ${token.colorTextSecondary};
    `,
    pointValue: css`
        flex: 1;
        min-width: 0;
    `,
    replay: css`
        flex: none;
        color: ${token.colorTextTertiary};
    `,
}))

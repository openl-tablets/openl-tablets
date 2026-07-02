import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding: 8px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-weight: 600;
        font-size: 12px;
        color: ${token.colorTextSecondary};
        text-transform: uppercase;
        letter-spacing: 0.04em;
    `,
    addRow: css`
        display: flex;
        gap: 6px;
    `,
    chips: css`
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
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
        justify-content: space-between;
        gap: 12px;
        padding: 1px 4px;
        border-radius: 3px;
        cursor: pointer;

        &:hover {
            background: ${token.colorFillTertiary};
        }
    `,
    pointLabel: css`
        color: ${token.colorTextSecondary};
    `,
    pointValue: css`
        font-family: ${token.fontFamilyCode};
        color: ${token.colorText};
        font-weight: 600;
    `,
}))

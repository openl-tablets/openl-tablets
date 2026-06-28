import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    section: css`
        display: flex;
        flex-direction: column;
    `,
    header: css`
        font-weight: 600;
        margin-bottom: ${token.marginXS}px;
    `,
    scroll: css`
        overflow: auto;
        max-width: 100%;
    `,
    grid: css`
        border-collapse: collapse;
        font-size: ${token.fontSizeSM}px;
        th,
        td {
            border: 1px solid ${token.colorBorderSecondary};
            padding: 2px 6px;
            vertical-align: top;
        }
    `,
    corner: css`
        background: ${token.colorFillQuaternary};
    `,
    colHeader: css`
        background: ${token.colorFillQuaternary};
        font-weight: 600;
        text-align: center;
        white-space: nowrap;
    `,
    rowHeader: css`
        background: ${token.colorFillQuaternary};
        font-weight: 600;
        text-align: left;
        white-space: nowrap;
    `,
    cell: css`
        min-width: 90px;
    `,
    cellInner: css`
        display: flex;
        align-items: flex-start;
        gap: ${token.marginXXS}px;
    `,
    current: css`
        background: ${token.colorWarningBg};
    `,
    gutter: css`
        margin-top: 4px;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        border: 1px solid ${token.colorBorder};
        flex: 0 0 auto;
        cursor: pointer;
        &:hover {
            border-color: ${token.colorError};
        }
    `,
    gutterActive: css`
        background: ${token.colorError};
        border-color: ${token.colorError};
    `,
    pending: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
}))

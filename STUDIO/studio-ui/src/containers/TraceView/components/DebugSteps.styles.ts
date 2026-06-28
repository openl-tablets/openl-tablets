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
    step: css`
        display: flex;
        align-items: flex-start;
        gap: ${token.marginXS}px;
        padding: 2px 4px;
        border-radius: ${token.borderRadiusSM}px;
    `,
    current: css`
        background: ${token.colorWarningBg};
    `,
    gutter: css`
        margin-top: 4px;
        width: 12px;
        height: 12px;
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
    gutterPlaceholder: css`
        margin-top: 4px;
        width: 12px;
        height: 12px;
        flex: 0 0 auto;
    `,
    body: css`
        flex: 1;
        min-width: 0;
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        flex-wrap: wrap;
    `,
    name: css`
        font-weight: 500;
    `,
    pending: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
}))

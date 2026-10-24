import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    table: css`
        tbody .editable-cell-wrap {
            padding: 5px 12px;
            cursor: pointer;
            /* The cell opens its field from the keyboard, so it is a real button; these undo what that brings with it. */
            display: block;
            width: 100%;
            background: none;
            border: none;
            font: inherit;
            color: inherit;
            text-align: left;
        }

        tbody tr:hover td .editable-cell-wrap {
            padding: 4px 24px 4px 11px;
            border: 1px solid ${token.colorBorder};
            border-radius: ${token.borderRadiusXS}px;
        }
    `,
    /** The tags of a type side by side: the configured ones, then the ones found in projects. */
    tagsCell: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px 4px;
    `,
    /** A value the projects carry that is not configured: dashed, like something not settled yet. */
    found: css`
        display: inline-flex;
        align-items: center;
        height: 22px;
        padding-inline: 7px 1px;
        border: 1px dashed ${token.colorWarningBorder};
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorWarningBg};
        font-size: ${token.fontSizeSM}px;
        line-height: 20px;
    `,
    foundLink: css`
        display: inline-flex;
        align-items: baseline;
        gap: 4px;
        color: ${token.colorText};

        &:hover {
            color: ${token.colorLink};
            text-decoration: underline;
        }
    `,
    foundCount: css`
        color: ${token.colorTextSecondary};
    `,
    foundAdd: css`
        &.ant-btn {
            width: 20px;
            min-width: 20px;
            height: 20px;
            color: ${token.colorWarningText};
        }
    `,
    foundSection: css`
        margin-top: 24px;
    `,
    /** One tag type found in projects: its name, its values, and the action that defines it. */
    foundRow: css`
        display: grid;
        grid-template-columns: minmax(120px, 240px) minmax(0, 1fr) auto;
        align-items: center;
        gap: 12px;
        padding: 10px 12px;
        border-top: 1px solid ${token.colorBorderSecondary};

        &:last-child {
            border-bottom: 1px solid ${token.colorBorderSecondary};
        }

        @media (max-width: ${token.screenSM}px) {
            grid-template-columns: minmax(0, 1fr);
        }
    `,
    foundType: css`
        color: ${token.colorWarningText};
        font-weight: ${token.fontWeightStrong};
        overflow-wrap: anywhere;
    `,
}))

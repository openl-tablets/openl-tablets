import { createStyles } from 'antd-style'

/**
 * The search of the header, drawn as the formula bar of a workbook: a name box saying what is searched, the text,
 * and the control of the screen at the far end. Focused, it wears the ring of a picked cell.
 */
export const useStyles = createStyles(({ css, token }) => ({
    field: css`
        display: flex;
        align-items: stretch;
        /* It shrinks long before the tabs do: they fold into their overflow only once it is at its narrowest. */
        flex: 0 100 560px;
        min-width: 200px;
        height: ${token.controlHeight}px;
        margin-inline: ${token.marginLG}px ${token.margin}px;
        line-height: ${token.lineHeight};
        background: ${token.colorFillQuaternary};
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;

        &:hover {
            border-color: ${token.colorBorder};
        }
    `,
    focused: css`
        background: ${token.colorBgContainer};

        &, &:hover {
            border-color: ${token.colorPrimary};
        }

        outline: 1px solid ${token.colorPrimary};
    `,
    /** The name box: what the field searches. */
    name: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        flex: none;
        max-width: 40%;
        margin: 0;
        padding: 0 ${token.paddingSM}px;
        font: inherit;
        font-weight: ${token.fontWeightStrong};
        color: ${token.colorTextSecondary};
        background: ${token.colorFillQuaternary};
        border: 0;
        border-inline-end: 1px solid ${token.colorBorderSecondary};
        border-start-start-radius: ${token.borderRadius - 1}px;
        border-end-start-radius: ${token.borderRadius - 1}px;
    `,
    /** The name box of a screen with more than one thing to search, which opens the list of them. */
    nameButton: css`
        cursor: pointer;

        &:hover {
            color: ${token.colorText};
            background: ${token.colorFillTertiary};
        }

        &:focus-visible {
            outline: 2px solid ${token.colorPrimary};
            outline-offset: -2px;
        }

        .anticon {
            font-size: 10px;
            color: ${token.colorTextTertiary};
        }
    `,
    nameText: css`
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    input: css`
        flex: 1;
        min-width: 0;

        /* The ring of the field is drawn around the whole of it, name box and control included. */
        &.ant-input-affix-wrapper,
        &.ant-input-affix-wrapper:focus-within {
            padding-block: 0;
            outline: none;
            box-shadow: none;
        }

        /* The field lies on a filled bar, where the faint magnifier of SearchInput would fade into it. */
        .ant-input-prefix .anticon {
            color: ${token.colorTextTertiary};
        }
    `,
    /** The key that brings the caret here, or the one that sends the text off. */
    key: css`
        display: inline-block;
        min-width: 20px;
        padding: 0 ${token.paddingXXS}px;
        font: inherit;
        font-size: ${token.fontSizeSM}px;
        line-height: 18px;
        text-align: center;
        color: ${token.colorTextTertiary};
        background: ${token.colorBgContainer};
        border: 1px solid ${token.colorBorder};
        border-radius: ${token.borderRadiusSM}px;
    `,
    /** The control of the screen, parted from the text as the name box is. */
    action: css`
        display: inline-flex;
        align-items: center;
        flex: none;
        padding-inline: ${token.paddingXXS}px;
        border-inline-start: 1px solid ${token.colorBorderSecondary};
    `,
    /** A control whose choice narrows the search says so by its colour. */
    narrowed: css`
        &.ant-btn {
            color: ${token.colorPrimary};
        }
    `,
}))

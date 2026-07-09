import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    page: css`
        max-width: 760px;
        margin: 0 auto;
        padding: 24px 28px 56px;
        color: ${token.colorText};
        font-size: 14px;
        line-height: 1.6;
    `,
    header: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
    `,
    actions: css`
        display: flex;
        flex-shrink: 0;
        gap: 8px;
        padding-top: 4px;
    `,
    title: css`
        &.ant-typography {
            margin: 0 0 2px;
            font-weight: 600;
        }
    `,
    titleInput: css`
        &.ant-input {
            font-size: 24px;
            font-weight: 600;
            padding: 2px 8px;
        }
    `,
    lead: css`
        &.ant-typography {
            margin: 0 0 20px;
            color: ${token.colorTextSecondary};
            font-size: 15px;
        }
    `,
    meta: css`
        display: grid;
        grid-template-columns: 132px 1fr;
        row-gap: 6px;
        column-gap: 16px;
        margin: 0;

        dt {
            color: ${token.colorTextTertiary};
            font-weight: 400;
        }

        dd {
            margin: 0;
        }
    `,
    metaEdit: css`
        display: grid;
        grid-template-columns: 132px 1fr;
        row-gap: 10px;
        column-gap: 16px;
        align-items: center;
        margin: 0;

        dt {
            color: ${token.colorTextTertiary};
        }

        dd {
            margin: 0;
        }

        .ant-input,
        .ant-select {
            width: 100%;
        }
    `,
    section: css`
        margin-top: 30px;
    `,
    heading: css`
        margin: 0 0 12px;
        padding-bottom: 6px;
        font-size: 15px;
        font-weight: 600;
        color: ${token.colorText};
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    code: css`
        font-family: ${token.fontFamilyCode};
        font-size: 0.9em;
    `,
    module: css`
        margin-bottom: 12px;
    `,
    moduleName: css`
        font-weight: 600;
        margin-right: 10px;
    `,
    list: css`
        margin: 0;
        padding: 0;
        list-style: none;

        li {
            margin-bottom: 6px;
        }
    `,
    orderedList: css`
        margin: 0;
        padding-left: 20px;

        li {
            margin-bottom: 6px;
        }
    `,
    subtle: css`
        color: ${token.colorTextTertiary};
    `,
    empty: css`
        margin: 0;
        color: ${token.colorTextTertiary};
    `,
    field: css`
        margin-top: 6px;
    `,
    fieldHint: css`
        margin: 0 0 6px;
        color: ${token.colorTextTertiary};
        font-size: 13px;
    `,
    editRow: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
    `,
    grow: css`
        flex: 1;
    `,
    monoInput: css`
        &.ant-input {
            font-family: ${token.fontFamilyCode};
        }
    `,
    typeSelect: css`
        min-width: 120px;
    `,
    addBtn: css`
        padding-left: 0;
    `,
}))

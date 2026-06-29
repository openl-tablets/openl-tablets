import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css, token }) => ({
    section: css`
        background: ${token.colorFillQuaternary};
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        margin-bottom: ${token.marginSM}px;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        line-height: 1.6;
    `,
    header: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        margin-bottom: ${token.marginXXS}px;
    `,
    title: css`
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextTertiary};
    `,
    empty: css`
        font-style: italic;
        color: ${token.colorTextTertiary};
    `,
    list: css`
        display: block;
    `,
    item: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        padding: 2px 0;
        padding-left: 24px;
    `,
    paramTree: css`
        .ant-tree {
            font-family: ${token.fontFamilyCode};
            font-size: ${token.fontSizeSM}px;
            background: transparent;
        }

        .ant-tree .ant-tree-treenode {
            padding: 0;
            align-items: center;
        }

        .ant-tree .ant-tree-treenode:hover {
            background-color: transparent;
        }

        .ant-tree .ant-tree-indent-unit {
            width: 16px;
        }

        .ant-tree .ant-tree-indent-unit::before,
        .ant-tree .ant-tree-switcher-leaf-line::before,
        .ant-tree .ant-tree-switcher-leaf-line::after {
            display: none !important;
        }

        .ant-tree .ant-tree-switcher {
            width: 16px;
            height: 22px;
            line-height: 22px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: ${token.colorTextTertiary};
            flex-shrink: 0;
        }

        .ant-tree .ant-tree-switcher:hover {
            color: ${token.colorPrimary};
        }

        .ant-tree .ant-tree-switcher .ant-tree-switcher-line-icon {
            font-size: 10px;
        }

        .ant-tree .ant-tree-switcher-leaf-line {
            width: 16px;
        }

        .ant-tree .ant-tree-node-content-wrapper {
            padding: 0 4px;
            min-height: 22px;
            line-height: 22px;
            display: flex;
            align-items: center;
        }

        .ant-tree .ant-tree-node-content-wrapper:hover {
            background-color: ${token.colorFillTertiary};
        }

        .ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
            background-color: transparent;
        }
    `,
    treeTitle: css`
        display: inline-flex;
        align-items: center;
        gap: 4px;
    `,
    /* Syntax palette below mirrors a code editor (name / string / number / boolean); these hues have no
       design-token equivalent and intentionally stay fixed. Neutral chrome uses tokens. */
    valueName: css`
        color: #871094;
        font-weight: 500;
    `,
    valueType: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
        margin-left: 4px;

        &::before {
            content: '(';
        }

        &::after {
            content: ')';
        }
    `,
    valueEquals: css`
        color: ${token.colorText};
        margin: 0 4px;
    `,
    valueString: css`
        color: #067d17;
    `,
    valueNumber: css`
        color: #1750eb;
    `,
    valueBoolean: css`
        color: #0033b3;
        font-weight: 600;
    `,
    valueNull: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
    valueDefault: css`
        color: ${token.colorText};
    `,
    valueSummary: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
    valueLazy: css`
        color: ${token.colorPrimary};
        cursor: pointer;
        text-decoration: underline;

        &:hover {
            color: ${token.colorPrimaryHover};
        }
    `,
    valueError: css`
        color: ${token.colorError};
    `,
    valueEmpty: css`
        color: ${token.colorTextTertiary};
        font-style: italic;
    `,
}))

import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    section: css`
        margin-bottom: 12px;
        font-family: Consolas, Monaco, 'Courier New', monospace;
        font-size: 12px;
        line-height: 1.6;
    `,
    header: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;
    `,
    title: css`
        font-weight: 600;
        color: #000;
    `,
    empty: css`
        font-style: italic;
    `,
    list: css`
        display: block;
    `,
    item: css`
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 2px 0;
        padding-left: 24px;
    `,
    paramTree: css`
        .ant-tree {
            font-family: Consolas, Monaco, 'Courier New', monospace;
            font-size: 12px;
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
            color: #6e6e6e;
            flex-shrink: 0;
        }

        .ant-tree .ant-tree-switcher:hover {
            color: #1890ff;
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
            background-color: #f5f5f5;
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
    valueName: css`
        color: #871094;
        font-weight: 500;
    `,
    valueType: css`
        color: #6e6e6e;
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
        color: #000;
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
        color: #6e6e6e;
        font-style: italic;
    `,
    valueDefault: css`
        color: #000;
    `,
    valueSummary: css`
        color: #6e6e6e;
        font-style: italic;
    `,
    valueLazy: css`
        color: #1890ff;
        cursor: pointer;
        text-decoration: underline;

        &:hover {
            color: #40a9ff;
        }
    `,
    valueError: css`
        color: #cf1322;
    `,
    valueEmpty: css`
        color: #6e6e6e;
        font-style: italic;
    `,
}))

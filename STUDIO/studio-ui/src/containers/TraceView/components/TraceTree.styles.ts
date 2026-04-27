import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    tree: css`
        display: flex;
        flex-direction: column;
        height: 100%;
    `,
    options: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px 12px;
        border-bottom: 1px solid #e8e8e8;
        flex-shrink: 0;
    `,
    content: css`
        flex: 1;
        overflow: auto;
        padding: 8px;

        .ant-tree {
            min-width: max-content;
        }

        .ant-tree-treenode {
            white-space: nowrap;
        }

        .ant-tree-node-content-wrapper {
            display: flex;
            align-items: center;
            flex-shrink: 0;
        }

        .ant-tree-title {
            white-space: nowrap;
        }

        [class~='ant-tree-iconEle'] {
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .trace-node-title.trace-node-result {
            font-weight: 600;
        }

        .trace-node-title.trace-node-fail,
        .trace-node-title.trace-node-no-result {
            font-style: italic;
            color: #8c8c8c;
        }

        .trace-node-title.trace-node-error {
            color: #ff4d4f;
        }
    `,
    loading: css`
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
    `,
}))

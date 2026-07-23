import { createStyles } from 'antd-style'

/** The folder tree of a picker dialog: block rows, muted folder icons, single-line names. */
export const useStyles = createStyles(({ css, token }) => ({
    tree: css`
        .ant-tree-node-content-wrapper {
            display: flex;
            align-items: center;
            min-width: 0;
            overflow: hidden;
        }

        .ant-tree-title {
            flex: 1;
            min-width: 0;
        }
    `,
    node: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        min-width: 0;
    `,
    name: css`
        flex: 1;
        min-width: 0;
    `,
    folderIcon: css`
        color: ${token.colorTextTertiary};
    `,
}))

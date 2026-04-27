import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    rowExpired: css`
        background-color: #fff2f0;

        &:hover > td {
            background-color: #ffebe8 !important;
        }
    `,
    codeBlock: css`
        position: relative;
        background-color: #f6f8fa;
        border: 1px solid #d1d9e0;
        border-radius: 6px;
        padding: 16px;
        padding-right: 48px;

        pre {
            margin: 0;
            overflow-x: auto;
            white-space: pre-wrap;
            overflow-wrap: break-word;
        }

        pre code {
            font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace;
            font-size: 13px;
            line-height: 1.5;
            color: #1f2328;
        }
    `,
    codeBlockNoCopy: css`
        padding-right: 16px;
    `,
    codeBlockCopy: css`
        position: absolute;
        top: 8px;
        right: 8px;
        color: #636c76;
        border: 1px solid transparent;
        border-radius: 6px;
        padding: 4px 8px;
        height: auto;

        &:hover {
            background-color: #eaeef2;
            border-color: #d1d9e0;
            color: #1f2328;
        }
    `,
}))

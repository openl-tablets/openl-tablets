import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    overlay: css`
        position: absolute;
        inset: 0;
        background: rgb(255 255 255 / 90%);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
    `,
    content: css`
        text-align: center;

        .ant-result {
            padding: 24px;
        }
    `,
}))

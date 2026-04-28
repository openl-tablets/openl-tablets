import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    tabs: css`
        .ant-tabs-tab {
            justify-content: space-between;
            min-width: 200px;
            max-width: 450px;

            .ant-tabs-tab-btn {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .ant-tabs-tab-remove {
                opacity: 0;
                transition: opacity 0.2s;
            }

            &:hover .ant-tabs-tab-remove {
                opacity: 1;
            }
        }
    `,
}))

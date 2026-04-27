import { createStyles } from 'antd-style'

export const useStyles = createStyles(({ css }) => ({
    header: css`
        &.ant-layout-header {
            background-color: #fff;
            border-bottom: 1px solid rgb(5 5 5 / 6%);
            display: flex;
            align-items: center;
            padding: 0 15px;
            line-height: 48.5px;
            height: 48.5px;
        }

        .header-logo {
            line-height: 14px;
            padding-right: 9px;
        }

        .header-logo img {
            height: 24px;
        }

        .header-title {
            font-size: 20px;
            font-family: Georgia, Verdana, Helvetica, Arial, serif;
            color: rgb(56 79 129);
        }

        ul.ant-menu-overflow {
            border: none;
        }
    `,
}))

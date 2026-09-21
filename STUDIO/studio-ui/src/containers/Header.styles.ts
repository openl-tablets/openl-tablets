import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from '../styles/listPageTheme'

export const useStyles = createStyles(({ css, token }) => ({
    header: css`
        &.ant-layout-header {
            background-color: ${token.colorBgContainer};
            border-bottom: 1px solid ${token.colorSplit};
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

        .header-title a {
            font-size: 20px;
            font-family: Georgia, Verdana, Helvetica, Arial, serif;
            color: ${LIST_PAGE_COLORS.brand};
        }

        ul.ant-menu-overflow {
            border: none;
        }
    `,
}))

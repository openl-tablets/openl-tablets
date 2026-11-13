import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from '../styles/listPageTheme'
import { APP_HEADER_HEIGHT } from '../constants/ui'

export const useStyles = createStyles(({ css, token }) => ({
    header: css`
        &.ant-layout-header {
            background-color: ${token.colorBgContainer};
            border-bottom: 1px solid ${token.colorSplit};
            display: flex;
            align-items: center;
            padding: 0 15px;
            line-height: ${APP_HEADER_HEIGHT}px;
            height: ${APP_HEADER_HEIGHT}px;
        }

        /* The search has the room the tabs leave: on a narrow window the name gives way to it, the cube stays. */
        @media (max-width: 880px) {
            .header-title {
                display: none;
            }
        }

        .header-logo {
            display: block;
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
    brand: css`
        display: flex;
        align-items: center;
        flex: none;
    `,
    /**
     * The tabs take what the search leaves. On a narrow window the search gives way first, and the tabs fold into
     * their overflow, which keeps its room.
     */
    menu: css`
        /* Room for the Projects and Deployments tabs, 202 px, before the search starts to give way. */
        flex: 1 1 240px;
        min-width: 46px;
    `,
    /** The actions of the screen shown, between the tabs and the settings. */
    actions: css`
        display: flex;
        flex: none;
        align-items: center;
        gap: ${token.marginXS}px;
        margin-right: ${token.margin}px;
        line-height: normal;

        &:empty {
            display: none;
        }
    `,
}))

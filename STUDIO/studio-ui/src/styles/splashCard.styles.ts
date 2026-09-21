import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from './listPageTheme'

/**
 * The centred card the application shows when there is no shell around it — the login page, the
 * `403`/`404`/`500` pages and the e-mail verification screen.
 *
 * All five screens wore the same card written out five times in fixed colours, so none of them followed
 * the appearance. The card is defined once here and takes its surface, text and shadow from the theme,
 * so it repaints with the rest of the application.
 */
export const useStyles = createStyles(({ css, token }) => ({
    container: css`
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        background: linear-gradient(135deg, ${LIST_PAGE_COLORS.pageBg} 0%, ${LIST_PAGE_COLORS.accent} 100%);
    `,
    card: css`
        background: ${token.colorBgContainer};
        padding: 48px 40px;
        border-radius: ${token.borderRadiusLG * 2}px;
        box-shadow: ${token.boxShadowSecondary};
        display: flex;
        flex-direction: column;
        align-items: center;
        max-width: 400px;
        width: 100%;
    `,
    /** The status number of an error page, the largest thing on the card. */
    code: css`
        font-size: 72px;
        font-weight: 700;
        color: ${token.colorPrimary};
        margin-bottom: ${token.marginXS}px;
    `,
    title: css`
        font-size: 28px;
        font-weight: 700;
        color: ${token.colorPrimary};
        margin: ${token.marginLG}px 0 ${token.margin}px;
        text-align: center;
    `,
    /** A title for an outcome that failed, so the card itself says what happened. */
    titleError: css`
        color: ${token.colorError};
    `,
    message: css`
        font-size: ${token.fontSizeHeading5}px;
        color: ${token.colorText};
        margin-bottom: ${token.marginLG}px;
        text-align: center;
    `,
    subTitle: css`
        font-size: ${token.fontSizeLG}px;
        color: ${token.colorTextSecondary};
        margin-bottom: ${token.marginLG}px;
        text-align: center;
    `,
    /** An aside under the message — a countdown, or what to do when the action did not work. */
    note: css`
        font-size: ${token.fontSize}px;
        color: ${token.colorTextTertiary};
    `,
}))

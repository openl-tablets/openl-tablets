import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from './listPageTheme'

/** Shared visual structure for list screens and embedded list fragments. */
export const useListPageStyles = createStyles(({ css, token }) => ({
    /** Resets inherited legacy-page typography without requiring a theme provider around an embedded fragment. */
    listPageRoot: css`
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: ${token.fontSize}px;
        line-height: ${token.lineHeight};
    `,
    /** A small uppercase caption for a column header or metadata label. */
    microLabel: css`
        color: ${LIST_PAGE_COLORS.textTertiary};
        font-size: 11px;
        font-weight: 500;
        line-height: 16px;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    /** The screen's header band: title, summary and screen-level actions. */
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        background: ${LIST_PAGE_COLORS.containerBg};
    `,
    headTop: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    pageTitle: css`
        margin: 0;
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: 20px;
        font-weight: 600;
        line-height: 28px;
        letter-spacing: -0.02em;
    `,
    subtitle: css`
        margin-top: 4px;
        color: ${LIST_PAGE_COLORS.textTertiary};
        font-size: 12px;
    `,
    headActions: css`
        display: flex;
        align-items: center;
        gap: 8px;
        flex: none;
    `,
    content: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    stateBox: css`
        margin: 24px;
        padding: 48px;
        border: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${LIST_PAGE_COLORS.containerBg};
    `,
    loading: css`
        padding: 24px;
    `,
    listTable: css`
        width: calc(100% - 32px);
        border-collapse: separate;
        border-spacing: 0;
        margin: 16px;
        border: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${LIST_PAGE_COLORS.containerBg};
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: 14px;
        overflow: hidden;
    `,
    listHead: css`
        background: ${LIST_PAGE_COLORS.containerBg};

        th {
            padding: 8px 12px;
            border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
            text-align: left;
            white-space: nowrap;
        }

        th:first-of-type {
            padding-left: 16px;
        }
    `,
    listRow: css`
        background: ${LIST_PAGE_COLORS.containerBg};

        td {
            padding: 12px;
            border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
            vertical-align: middle;
        }

        td:first-of-type {
            padding-left: 16px;
        }

        &:hover td {
            background: ${LIST_PAGE_COLORS.pageBg};
        }

        &:last-of-type td {
            border-bottom: none;
        }
    `,
}))

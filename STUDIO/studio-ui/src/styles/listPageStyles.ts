import { createStyles } from 'antd-style'
import { LIST_PAGE_COLORS } from './listPageTheme'

/** The smallest type the screens use; a caption never shrinks past it, whatever the density asks for. */
const CAPTION_FLOOR = 10

/** A caption sits one step under the small text size, and never below {@link CAPTION_FLOOR}. */
export const captionSize = (fontSizeSM: number): number => Math.max(fontSizeSM - 1, CAPTION_FLOOR)

/**
 * Shared visual structure for list screens and embedded list fragments.
 *
 * Every measurement that decides how much fits on a screen — the padding of a cell, the gap between two
 * controls, the size of the type — is a token rather than a fixed number of pixels, so the whole structure
 * tightens when the user asks for the compact density and nothing is left standing at its comfortable size.
 */
export const useListPageStyles = createStyles(({ css, token }) => ({
    /** Resets inherited legacy-page typography without requiring a theme provider around an embedded fragment. */
    listPageRoot: css`
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: ${token.fontSize}px;
        line-height: ${token.lineHeight};
    `,
    /**
     * A small uppercase caption for a column header or metadata label.
     *
     * It is already the smallest type on the screen, so the compact density takes it down one pixel and
     * stops at {@link CAPTION_FLOOR} rather than following the tokens all the way down to illegibility.
     */
    microLabel: css`
        color: ${LIST_PAGE_COLORS.textTertiary};
        font-size: ${captionSize(token.fontSizeSM)}px;
        font-weight: 500;
        line-height: ${captionSize(token.fontSizeSM) + 5}px;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    /** The screen's header band: title, summary and screen-level actions. */
    header: css`
        padding: ${token.paddingSM}px ${token.padding}px;
        border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        background: ${LIST_PAGE_COLORS.containerBg};
    `,
    headTop: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: ${token.marginSM}px;
        margin-bottom: ${token.marginSM}px;
    `,
    pageTitle: css`
        margin: 0;
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: ${token.fontSizeXL}px;
        font-weight: 600;
        line-height: ${token.lineHeightHeading3};
        letter-spacing: -0.02em;
    `,
    subtitle: css`
        margin-top: ${token.marginXXS}px;
        color: ${LIST_PAGE_COLORS.textTertiary};
        font-size: ${token.fontSizeSM}px;
    `,
    headActions: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        flex: none;
    `,
    content: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    stateBox: css`
        margin: ${token.marginLG}px;
        padding: ${token.paddingLG * 2}px;
        border: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${LIST_PAGE_COLORS.containerBg};
    `,
    loading: css`
        padding: ${token.paddingLG}px;
    `,
    listTable: css`
        width: calc(100% - ${token.margin * 2}px);
        border-collapse: separate;
        border-spacing: 0;
        margin: ${token.margin}px;
        border: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${LIST_PAGE_COLORS.containerBg};
        color: ${LIST_PAGE_COLORS.text};
        font-family: ${token.fontFamily};
        font-size: ${token.fontSize}px;
        overflow: hidden;
    `,
    listHead: css`
        background: ${LIST_PAGE_COLORS.containerBg};

        th {
            padding: ${token.paddingXS}px ${token.paddingSM}px;
            border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
            text-align: left;
            white-space: nowrap;
        }

        th:first-of-type {
            padding-left: ${token.padding}px;
        }
    `,
    listRow: css`
        background: ${LIST_PAGE_COLORS.containerBg};

        td {
            padding: ${token.paddingSM}px;
            border-bottom: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
            vertical-align: middle;
        }

        td:first-of-type {
            padding-left: ${token.padding}px;
        }

        &:hover td {
            background: ${LIST_PAGE_COLORS.pageBg};
        }

        &:last-of-type td {
            border-bottom: none;
        }
    `,
}))

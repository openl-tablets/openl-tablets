import type { ReactNode } from 'react'
import { Pagination } from 'antd'
import { createStyles } from 'antd-style'
import { PAGE_SIZE_OPTIONS } from '../constants/ui'
import { LIST_PAGE_COLORS } from '../styles/listPageTheme'

const useStyles = createStyles(({ css, token }) => ({
    footer: css`
        display: flex;
        flex: none;
        flex-wrap: wrap;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px ${token.marginSM}px;
        box-sizing: border-box;
        min-height: ${token.controlHeight + token.paddingXS * 2 + 1}px;
        padding: ${token.paddingXS}px ${token.padding}px;
        border-top: 1px solid ${LIST_PAGE_COLORS.borderSecondary};
        background: ${LIST_PAGE_COLORS.containerBg};
        color: ${LIST_PAGE_COLORS.textTertiary};
        font-size: ${token.fontSizeSM}px;
    `,
}))

interface ListFooterProps {
    /** What the list holds, on the left of the bar. */
    summary: ReactNode
    /** How many rows the list has across all its pages. */
    total: number
    current: number
    pageSize: number
    onChange: (page: number, pageSize: number) => void
    /** Names the test ids of the bar, `<prefix>-footer`, and of its page switcher, `<prefix>-pagination`. */
    testIdPrefix: string
}

/**
 * The bar under a list: what the list holds on the left, its pages on the right.
 *
 * The page switcher appears only when the rows need more than one page of the smallest size it offers, so a
 * reader who picked a larger page can still go back to a smaller one. The bar keeps the height of the switcher
 * without it, so the list does not move when the switcher appears.
 */
export const ListFooter = ({ summary, total, current, pageSize, onChange, testIdPrefix }: ListFooterProps) => {
    const { styles } = useStyles()
    const smallestPageSize = Math.min(pageSize, ...PAGE_SIZE_OPTIONS)
    return (
        <div className={styles.footer} data-testid={`${testIdPrefix}-footer`}>
            {summary}
            {total > smallestPageSize && (
                <Pagination
                    showSizeChanger
                    current={current}
                    data-testid={`${testIdPrefix}-pagination`}
                    onChange={onChange}
                    pageSize={pageSize}
                    pageSizeOptions={PAGE_SIZE_OPTIONS}
                    total={total}
                />
            )}
        </div>
    )
}

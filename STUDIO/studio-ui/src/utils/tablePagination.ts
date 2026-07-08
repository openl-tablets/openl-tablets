import type { TablePaginationConfig } from 'antd'
import type { TFunction } from 'i18next'

/**
 * Shared pagination setup for the administration tables:
 * a page size selector with 10/25/50/100 rows and a visible-rows counter.
 */
export const tablePagination = (t: TFunction): TablePaginationConfig => ({
    defaultPageSize: 10,
    pageSizeOptions: [10, 25, 50, 100],
    showSizeChanger: true,
    showTotal: (total, range) => t('common:table.showing_rows', { from: range[0], to: range[1], total }),
})

import { DEFAULT_PAGE_SIZE } from '../constants/ui'

/**
 * Returns the address parameters of a list with its page and page size written into them.
 *
 * The page and the size change in one step: two updates read from the same snapshot would lose the first.
 * The first page and the default size are left out of the address, so it stays as short as it can be.
 */
export const withPage = (params: URLSearchParams, page: number, pageSize: number): URLSearchParams => {
    const next = new URLSearchParams(params)
    const apply = (key: string, value: string | null) => (value === null ? next.delete(key) : next.set(key, value))
    apply('page', page === 1 ? null : String(page))
    apply('size', pageSize === DEFAULT_PAGE_SIZE ? null : String(pageSize))
    return next
}

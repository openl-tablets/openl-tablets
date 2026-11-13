import { plainText, storedSetting } from '../../utils/localStore'
import { FILTER_PARAMS, listingQueryOf, type ListingQuery } from './projectListing'

/** Where the browser keeps the last state of the projects screen. */
const STORAGE_KEY = 'openl.projects.filters'

/** The remembered parameters as the text of an address; the list as it opens at first stores nothing. */
const filters = storedSetting(STORAGE_KEY, '', plainText)

/**
 * The parts of the URL the screen remembers between visits: what the list is filtered and sorted by, in which
 * direction, and how it is laid out. The page number is deliberately left out — coming back to page 7 of a list
 * that has moved on since helps nobody.
 */
const REMEMBERED = ['q', 'status', 'repo', 'tags', 'branch', 'sort', 'dir', 'view', 'size']

/** Stores the filters of the projects screen, so a user finds the list as they left it. */
export const saveProjectFilters = (params: URLSearchParams): void => {
    const remembered = new URLSearchParams()
    REMEMBERED.forEach(key => params.getAll(key).forEach(value => remembered.append(key, value)))
    filters.write(remembered.toString())
}

/**
 * The filters of the last visit; none when the list was left as it opens at first, or never visited.
 *
 * They are only meant for a plain visit to the screen: a link that carries its own parameters describes
 * what its sender wanted to show and must win.
 */
export const loadProjectFilters = (): URLSearchParams => new URLSearchParams(filters.read())

/** The picks of the Filters view the list was last left with — what the tree beside a project follows. */
export const loadStoredPicks = (): ListingQuery => listingQueryOf(loadProjectFilters())

/**
 * Forgets the picks of the Filters view, so the list comes back in its default view. Its search, sort and layout
 * are kept.
 */
export const clearStoredPicks = (): void => {
    const stored = loadProjectFilters()
    FILTER_PARAMS.forEach(key => stored.delete(key))
    saveProjectFilters(stored)
}

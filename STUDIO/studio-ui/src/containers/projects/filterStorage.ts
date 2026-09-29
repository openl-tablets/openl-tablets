import { readStored, writeStored } from '../../utils/localStore'
import { FILTER_PARAMS, listingQueryOf, type ListingQuery } from './projectListing'

/** Where the browser keeps the last state of the projects screen. */
const STORAGE_KEY = 'openl.projects.filters'

/**
 * The parts of the URL the screen remembers between visits: what the list is filtered and sorted by, and
 * how it is laid out. The page number is deliberately left out — coming back to page 7 of a list that has
 * moved on since helps nobody.
 */
const REMEMBERED = ['q', 'status', 'repo', 'tags', 'branch', 'sort', 'view', 'size']

/** Stores the filters of the projects screen, so a user finds the list as they left it. */
export const saveProjectFilters = (params: URLSearchParams): void => {
    const remembered = new URLSearchParams()
    REMEMBERED.forEach(key => params.getAll(key).forEach(value => remembered.append(key, value)))
    writeStored(STORAGE_KEY, remembered.toString())
}

/**
 * The filters of the last visit, or null when nothing was stored.
 *
 * They are only meant for a plain visit to the screen: a link that carries its own parameters describes
 * what its sender wanted to show and must win.
 */
export const loadProjectFilters = (): URLSearchParams | null => {
    const stored = readStored(STORAGE_KEY)
    // An empty entry is a memory too: the user cleared the filters, and that is how they should return.
    return stored === null ? null : new URLSearchParams(stored)
}

/** The picks of the Filters view the list was last left with — what the tree beside a project follows. */
export const loadStoredPicks = (): ListingQuery => listingQueryOf(loadProjectFilters() ?? new URLSearchParams())

/**
 * Forgets the picks of the Filters view, so the list comes back in its default view. Its search, sort and layout
 * are kept.
 */
export const clearStoredPicks = (): void => {
    const stored = loadProjectFilters()
    if (stored) {
        FILTER_PARAMS.forEach(key => stored.delete(key))
        saveProjectFilters(stored)
    }
}

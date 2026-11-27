import { jsonText, readStored, storedSetting } from './localStore'

/** How many choices a list keeps: the ones a switcher lists first. */
export const RECENT_LIMIT = 10

/**
 * How many lists one store keeps, such as the branches of that many repositories; the least recently used goes first.
 */
const SCOPE_LIMIT = 50

/**
 * The lists of a store as kept: each scope with its choices, the most recent first, the scope used least recently first.
 * A list of pairs rather than an object, whose keys that read as numbers would come first whatever their order.
 */
type StoredLists = [string, string[]][]

const isStoredLists = (value: unknown): value is StoredLists =>
    Array.isArray(value) && value.every(entry => Array.isArray(entry) && entry.length === 2
        && typeof entry[0] === 'string' && Array.isArray(entry[1])
        && (entry[1] as unknown[]).every(item => typeof item === 'string'))

/** The lists of a store by scope, in the order they are kept. */
type RecentLists = ReadonlyMap<string, readonly string[]>

/**
 * The choices the reader made most recently, such as the projects they opened, kept in this browser.
 *
 * <p>A store holds one list per scope — the branches of each repository — or a single list under the empty scope.
 * A list keeps the {@link RECENT_LIMIT} latest choices, the most recent first. A choice the server no longer knows,
 * such as a project removed, is forgotten once a screen says so.
 *
 * <p>A change is told to every reader subscribed, the change made in another tab too, so a list on screen — open or
 * not — shows it at once (`useRecentChoices`). A list stays the same array until it changes, so a reader of one scope
 * is not drawn again for a change of another.
 *
 * <p>The browser may refuse storage, or hold a text another version wrote: the lists then read as empty.
 */
export interface RecentChoices {
    /** The choices of the scope, the most recent first: the same array until they change. */
    read: (scope?: string) => readonly string[]
    /** Puts the choice first in its list, dropping the oldest one beyond the limit. */
    remember: (item: string, scope?: string) => void
    /** Forgets a choice that is gone, such as a branch removed. */
    forget: (item: string, scope?: string) => void
    /** Forgets every choice of the scope that is not among the ones that exist. */
    keepOnly: (existing: Iterable<string>, scope?: string) => void
    /** Calls the listener on every change of the lists, here or in another tab; gives the way to stop. */
    subscribe: (listener: () => void) => () => void
}

const NONE: readonly string[] = []

const sameItems = (one: readonly string[] | undefined, other: readonly string[]): boolean =>
    one?.length === other.length && one.every((item, at) => item === other[at])

/** The recent choices kept under the given key of `localStorage`. */
export const recentChoices = (key: string): RecentChoices => {
    const stored = storedSetting<StoredLists>(key, [], jsonText(isStoredLists))
    const listeners = new Set<() => void>()
    const tell = () => listeners.forEach(listener => listener())
    // The lists as last read, and the text they were read from: read again only once the text changes, keeping the
    // array of every list that did not change.
    let seen: { text: string | null, lists: RecentLists } = { text: null, lists: new Map() }
    const readAll = (): RecentLists => {
        const text = readStored(key)
        if (text !== seen.text) {
            const before = seen.lists
            seen = {
                text,
                lists: new Map(stored.read().map(([scope, list]) => {
                    const known = before.get(scope)
                    return [scope, known !== undefined && sameItems(known, list) ? known : list]
                })),
            }
        }
        return seen.lists
    }
    const update = (scope: string, change: (list: readonly string[]) => string[]) => {
        const lists = readAll()
        const list = lists.get(scope) ?? NONE
        const changed = change(list)
        if (sameItems(list, changed)) {
            return
        }
        // The scope changed last goes to the end, so the least recently used scopes are the first to give way.
        const kept = [...lists]
            .filter(([known]) => known !== scope)
            .slice(-(SCOPE_LIMIT - 1))
            .map(([known, items]): [string, string[]] => [known, [...items]])
        stored.write(changed.length === 0 ? kept : [...kept, [scope, changed]])
        tell()
    }
    // Another tab tells its changes through the storage event, a storage cleared as a whole by a key of none.
    globalThis.addEventListener?.('storage', (event: StorageEvent) => {
        if (event.key === key || event.key === null) {
            tell()
        }
    })
    return {
        read: (scope = '') => readAll().get(scope) ?? NONE,
        remember: (item, scope = '') =>
            update(scope, list => [item, ...list.filter(known => known !== item)].slice(0, RECENT_LIMIT)),
        forget: (item, scope = '') => update(scope, list => list.filter(known => known !== item)),
        keepOnly: (existing, scope = '') => {
            const known = new Set(existing)
            update(scope, list => list.filter(item => known.has(item)))
        },
        subscribe: listener => {
            listeners.add(listener)
            return () => listeners.delete(listener)
        },
    }
}

/** The projects opened most recently, by id. */
export const recentProjects = recentChoices('openl.recent.projects')

/**
 * The branches used most recently in each repository, by the id of the repository: a branch is a branch of the
 * repository, whichever of its projects was read on it.
 */
export const recentBranches = recentChoices('openl.recent.branches')

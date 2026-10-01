import { guideFileUrl, type GuideEntry } from 'services/userGuides'
import { errorHandler } from 'utils/errorHandling'
import type { GuideSearchResult, PageToIndex } from './guideSearch'
import type { GuideSearchRequest } from './guideSearch.worker'
import { pagesOf } from './guidePaths'

/** The search of the guides. */
export interface GuideSearch {
    /** The parts of the pages in the given folder matching the query; empty folder for all of them. */
    search(query: string, scope: string): Promise<GuideSearchResult[]>
    /** Stops the search and drops its index. */
    dispose(): void
}

interface GuideSearchAnswer {
    id: number
    results?: GuideSearchResult[]
    error?: string
}

/** The same search run on the page itself, where no worker starts or the worker fails. */
const pageSearch = (pages: PageToIndex[]): GuideSearch => {
    const index = import('./guideSearch').then(async ({ indexGuides, readPages }) => indexGuides(await readPages(pages)))
    return {
        search: async (query, scope) => (await index).search(query, scope),
        dispose: () => undefined,
    }
}

/** A search the worker has yet to answer, kept so that the page can answer it if the worker fails. */
interface PendingSearch {
    query: string
    scope: string
    resolve: (results: GuideSearchResult[]) => void
    reject: (error: Error) => void
}

/**
 * The search run in a worker of its own, so the page stays responsive while the guides are read and indexed.
 *
 * A worker that does not load, as under a security policy forbidding workers, or that breaks tells so only after it
 * was started. The page then takes the search over, with the searches the worker had yet to answer.
 */
const workerSearch = (pages: PageToIndex[]): GuideSearch => {
    const worker = new Worker(new URL('./guideSearch.worker.ts', import.meta.url), { type: 'module' })
    const pending = new Map<number, PendingSearch>()
    let last = 0
    let onPage: GuideSearch | undefined
    worker.onmessage = (event: MessageEvent<GuideSearchAnswer>) => {
        const { id, results, error } = event.data
        const waiting = pending.get(id)
        pending.delete(id)
        if (error === undefined) {
            waiting?.resolve(results ?? [])
        } else {
            waiting?.reject(new Error(error))
        }
    }
    worker.onerror = event => {
        // Handled here, so the browser does not report it as an error of the page as well.
        event.preventDefault()
        errorHandler.logError(new Error(`The user guides are searched on the page, because the worker failed: ${event.message || 'it did not load'}`))
        worker.terminate()
        const fallback = pageSearch(pages)
        onPage = fallback
        for (const { query, scope, resolve, reject } of pending.values()) {
            fallback.search(query, scope).then(resolve, reject)
        }
        pending.clear()
    }
    worker.postMessage({ type: 'index', pages } satisfies GuideSearchRequest)
    return {
        search: (query, scope) => onPage?.search(query, scope) ?? new Promise((resolve, reject) => {
            const id = ++last
            pending.set(id, { query, scope, resolve, reject })
            worker.postMessage({ type: 'search', id, query, scope } satisfies GuideSearchRequest)
        }),
        dispose: () => {
            worker.terminate()
            pending.clear()
        },
    }
}

/**
 * Starts the search of the pages the table of contents lists.
 *
 * The search reads every page once, when it starts, and indexes it; the browser keeps the pages and asks the server
 * again only whether they changed. It runs in a worker, unless the browser refuses to start one: a page whose
 * scripts come from another origin, as from a frontend dev server, or a security policy forbidding workers.
 */
export const startGuideSearch = (contents: GuideEntry): GuideSearch => {
    const pages = [...pagesOf(contents).values()].flatMap(entry => (entry.file
        ? [{ file: entry.file, title: entry.title, url: new URL(guideFileUrl(entry.file), window.location.href).href }]
        : []))
    try {
        return workerSearch(pages)
    } catch (error) {
        // Not a failure of the search, which goes on; logged so that a policy forbidding workers is noticed.
        errorHandler.logError(new Error(`The user guides are searched on the page, because no worker starts: ${String(error)}`))
        return pageSearch(pages)
    }
}

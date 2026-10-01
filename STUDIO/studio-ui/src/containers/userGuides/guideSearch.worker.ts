/// <reference lib="webworker" />
import { type GuideIndex, indexGuides, type PageToIndex, readPages } from './guideSearch'

/** What the viewer asks the worker: to read the pages once, then to search them as often as the reader types. */
export type GuideSearchRequest =
    | { type: 'index', pages: PageToIndex[] }
    | { type: 'search', id: number, query: string, scope: string }

let index: Promise<GuideIndex> | undefined

// The index is built off the page the reader sees, so typing never waits for the guides to be read and parsed.
self.onmessage = (event: MessageEvent<GuideSearchRequest>) => {
    const request = event.data
    if (request.type === 'index') {
        index = readPages(request.pages).then(indexGuides)
        return
    }
    const { id, query, scope } = request
    ;(index ?? Promise.reject(new Error('The guides are not indexed')))
        .then(ready => self.postMessage({ id, results: ready.search(query, scope) }))
        .catch((error: unknown) => self.postMessage({ id, error: String(error) }))
}

import type { GuideEntry } from 'services/userGuides'
import { startGuideSearch } from './guideSearchClient'

const CONTENTS: GuideEntry = {
    title: 'User Guides',
    file: 'index.md',
    children: [{ title: 'Appendices', children: [{ title: 'Error Pages', file: 'appendices/error-pages.md' }]}],
}

/** A worker standing in for the real one: it keeps what it is sent and answers when the test says so. */
class FakeWorker {
    static last: FakeWorker | undefined
    readonly sent: unknown[] = []
    onmessage: ((event: { data: unknown }) => void) | undefined
    onerror: ((event: Event) => void) | undefined
    terminated = false

    constructor(readonly url: URL, readonly options: WorkerOptions) {
        FakeWorker.last = this
    }

    postMessage(message: unknown) {
        this.sent.push(message)
    }

    terminate() {
        this.terminated = true
    }

    answer(data: unknown) {
        this.onmessage?.({ data })
    }

    /** Fails the way a worker whose script does not load does: an error event telling nothing more. */
    fail() {
        this.onerror?.(new Event('error', { cancelable: true }))
    }
}

/** Every page of the guides, as the server answers it. */
const servePages = () => vi.stubGlobal('fetch', vi.fn(async (url: string) => new Response(url.endsWith('error-pages.md')
    ? '# Appendix D: Error Pages\n\nThe 404 page.'
    : 'Welcome.', { status: 200 })))

describe('startGuideSearch', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        FakeWorker.last = undefined
    })

    it('hands the worker every page at its absolute address, then the queries', async () => {
        vi.stubGlobal('Worker', FakeWorker)

        const search = startGuideSearch(CONTENTS)
        const found = search.search('error', 'appendices/')
        const worker = FakeWorker.last as FakeWorker
        worker.answer({ id: 1, results: [{ file: 'appendices/error-pages.md', title: 'Error Pages', heading: '', anchor: '', snippet: []}]})

        expect(await found).toEqual([expect.objectContaining({ file: 'appendices/error-pages.md' })])
        expect(worker.options).toEqual({ type: 'module' })
        expect(worker.sent).toEqual([
            {
                type: 'index',
                pages: [
                    { file: 'index.md', title: 'User Guides', url: 'http://localhost:3000/docs/index.md' },
                    { file: 'appendices/error-pages.md', title: 'Error Pages', url: 'http://localhost:3000/docs/appendices/error-pages.md' },
                ],
            },
            { type: 'search', id: 1, query: 'error', scope: 'appendices/' },
        ])
    })

    it('fails a search the worker fails, and stops the worker when disposed', async () => {
        vi.stubGlobal('Worker', FakeWorker)
        const search = startGuideSearch(CONTENTS)
        const found = search.search('error', '')

        FakeWorker.last?.answer({ id: 1, error: 'broken' })
        search.dispose()

        await expect(found).rejects.toThrow('broken')
        expect(FakeWorker.last?.terminated).toBe(true)
    })

    it('searches on the page where no worker can be started', async () => {
        vi.stubGlobal('Worker', class {
            constructor() {
                throw new DOMException('Script cannot be accessed from origin', 'SecurityError')
            }
        })
        servePages()

        const search = startGuideSearch(CONTENTS)

        expect((await search.search('404', '')).map(result => result.file)).toEqual(['appendices/error-pages.md'])
        search.dispose()
    })

    it('searches on the page when the worker fails, answering the searches the worker had yet to answer', async () => {
        vi.stubGlobal('Worker', FakeWorker)
        servePages()
        const search = startGuideSearch(CONTENTS)
        const waiting = search.search('404', '')
        const worker = FakeWorker.last as FakeWorker

        worker.fail()

        expect((await waiting).map(result => result.file)).toEqual(['appendices/error-pages.md'])
        expect((await search.search('welcome', '')).map(result => result.file)).toEqual(['index.md'])
        expect(worker.terminated).toBe(true)
        expect(worker.sent).toHaveLength(2)
    })
})

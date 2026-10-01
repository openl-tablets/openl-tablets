import type { GuideSearchRequest } from './guideSearch.worker'

const send = (request: GuideSearchRequest) =>
    (self.onmessage as (event: MessageEvent<GuideSearchRequest>) => void)(new MessageEvent('message', { data: request }))

describe('guideSearch.worker', () => {
    const posted = vi.fn()

    beforeAll(async () => {
        await import('./guideSearch.worker')
    })

    beforeEach(() => {
        vi.spyOn(self, 'postMessage').mockImplementation(posted)
    })

    afterEach(() => {
        vi.restoreAllMocks()
        vi.unstubAllGlobals()
    })

    it('answers a search before the index with an error', async () => {
        send({ type: 'search', id: 1, query: 'table', scope: '' })

        await vi.waitFor(() => expect(posted).toHaveBeenCalledWith({ id: 1, error: 'Error: The guides are not indexed' }))
    })

    it('reads the pages once and searches them', async () => {
        vi.stubGlobal('fetch', vi.fn(async () => new Response('# Decision Tables\n\nA decision table.', { status: 200 })))

        send({ type: 'index', pages: [{ file: 'decision.md', title: 'Decision Tables', url: 'http://host/docs/decision.md' }]})
        send({ type: 'search', id: 2, query: 'decision', scope: '' })

        await vi.waitFor(() => expect(posted).toHaveBeenCalledWith({
            id: 2,
            results: [expect.objectContaining({ file: 'decision.md', anchor: 'decision-tables' })],
        }))
        expect(fetch).toHaveBeenCalledTimes(1)
    })
})

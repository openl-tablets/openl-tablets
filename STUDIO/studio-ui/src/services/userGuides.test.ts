import { fetchGuidePage, fetchGuidesContents, guideFileUrl } from './userGuides'

describe('userGuides', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('addresses a file of the guides below /docs, each part of its path encoded', () => {
        expect(guideFileUrl('getting-started/samples/Tutorial 1.xlsx')).toBe('/docs/getting-started/samples/Tutorial%201.xlsx')
    })

    it('reads the table of contents and a page', async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce(new Response('{"title":"User Guides"}', { status: 200 }))
            .mockResolvedValueOnce(new Response('# Guides', { status: 200 }))
        vi.stubGlobal('fetch', fetchMock)

        expect(await fetchGuidesContents()).toEqual({ title: 'User Guides' })
        expect(await fetchGuidePage('index.md')).toBe('# Guides')
        expect(fetchMock).toHaveBeenNthCalledWith(1, '/docs/toc.json')
        expect(fetchMock).toHaveBeenNthCalledWith(2, '/docs/index.md')
    })

    it('fails on a page the server does not answer with', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 404 })))

        await expect(fetchGuidePage('missing.md')).rejects.toThrow('Failed to read missing.md of the user guides: 404')
    })
})

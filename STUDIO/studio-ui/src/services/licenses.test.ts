import { canOpenLicense, fetchLicenses, openLicense } from './licenses'
import { jsonResponse } from 'testing/responses'

describe('licenses', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        vi.restoreAllMocks()
    })

    it('reads the list of a side from the file the build writes under /licenses', async () => {
        const libraries = [
            { name: 'org.slf4j:slf4j-api', version: '2.0.17', identifier: 'MIT', url: 'https://opensource.org/license/mit' },
        ]
        const fetchMock = vi.fn().mockResolvedValue(jsonResponse(libraries))
        vi.stubGlobal('fetch', fetchMock)

        expect(await fetchLicenses('backend')).toEqual(libraries)
        expect(fetchMock).toHaveBeenCalledWith('/licenses/backend-licenses.json')
    })

    it('fails on a list the server does not answer with', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 404 })))

        await expect(fetchLicenses('frontend')).rejects.toThrow('Failed to read the frontend licenses: 404')
    })

    it('opens the text of a license as plain text in a new window, keeping no address behind', async () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)
        const createObjectURL = vi.fn((_blob: Blob) => 'blob:license')
        const revokeObjectURL = vi.fn()
        vi.stubGlobal('URL', Object.assign(class extends URL {}, { createObjectURL, revokeObjectURL }))

        openLicense({ name: 'react', version: '19.3.0', identifier: 'MIT', text: 'MIT License' })

        const blob = createObjectURL.mock.calls[0]![0]
        expect(blob.type).toBe('text/plain;charset=utf-8')
        expect(await blob.text()).toBe('MIT License')
        expect(open).toHaveBeenCalledWith('blob:license', '_blank', 'noopener')
        expect(revokeObjectURL).toHaveBeenCalledWith('blob:license')
    })

    it('opens the address of a license that comes without its text', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)

        openLicense({ name: 'org.slf4j:slf4j-api', version: '2.0.17', url: 'https://opensource.org/license/mit' })

        expect(open).toHaveBeenCalledWith('https://opensource.org/license/mit', '_blank', 'noopener,noreferrer')
    })

    it('opens a license only when its text or its address is known', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)
        const library = { name: 'unlicensed', version: '1.0.0' }

        openLicense(library)

        expect(open).not.toHaveBeenCalled()
        expect(canOpenLicense(library)).toBe(false)
        expect(canOpenLicense({ ...library, text: 'MIT License' })).toBe(true)
        expect(canOpenLicense({ ...library, url: 'https://opensource.org/license/mit' })).toBe(true)
    })
})

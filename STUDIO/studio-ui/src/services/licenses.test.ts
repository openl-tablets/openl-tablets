import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expressionParts, fetchLicenses, openText, publicLicense } from './licenses'
import { jsonResponse } from 'testing/responses'

/** The licenses `npm run build` lets the frontend libraries have. */
const frontendLicenses = (): string[] => {
    const { scripts } = JSON.parse(readFileSync(resolve(process.cwd(), 'package.json'), 'utf8')) as
        { scripts: Record<string, string> }
    return /--onlyAllow "([^"]+)"/.exec(scripts['build'] ?? '')![1]!.split(';')
}

/** The licenses `npm run licenses` of `studio-mcp` lets the libraries of the MCP server have. */
const mcpLicenses = (): string[] => {
    const { scripts } = JSON.parse(readFileSync(resolve(process.cwd(), '../studio-mcp/package.json'), 'utf8')) as
        { scripts: Record<string, string> }
    return /--onlyAllow "([^"]+)"/.exec(scripts['licenses'] ?? '')![1]!.split(';')
}

/** The licenses the war build lets the backend libraries have. */
const backendLicenses = (): string[] => {
    const pom = readFileSync(resolve(process.cwd(), '../studio-backend/pom.xml'), 'utf8')
    return [...pom.matchAll(/<includedLicense>([^<]+)<\/includedLicense>/g)].map(([, license]) => license!)
}

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

    it('opens a text as plain text in a new window, keeping no address behind', async () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(null)
        const createObjectURL = vi.fn((_blob: Blob) => 'blob:license')
        const revokeObjectURL = vi.fn()
        vi.stubGlobal('URL', Object.assign(class extends URL {}, { createObjectURL, revokeObjectURL }))

        openText('MIT License')

        const blob = createObjectURL.mock.calls[0]![0]
        expect(blob.type).toBe('text/plain;charset=utf-8')
        expect(await blob.text()).toBe('MIT License')
        expect(open).toHaveBeenCalledWith('blob:license', '_blank', 'noopener')
        expect(revokeObjectURL).toHaveBeenCalledWith('blob:license')
    })

    it('splits an SPDX expression into its licenses and what joins them', () => {
        expect(expressionParts('MIT')).toEqual(['MIT'])
        expect(expressionParts('(MPL-2.0 OR Apache-2.0)')).toEqual(['(', 'MPL-2.0', ' ', 'OR', ' ', 'Apache-2.0', ')'])
        expect(expressionParts('GPL-2.0-only  WITH Classpath-exception-2.0'))
            .toEqual(['GPL-2.0-only', '  ', 'WITH', ' ', 'Classpath-exception-2.0'])
    })

    it('knows where a standard license is published, and no address for any other', () => {
        expect(publicLicense('Apache-2.0')).toBe('https://www.apache.org/licenses/LICENSE-2.0.txt')
        expect(publicLicense('The Apache Software License, Version 2.0')).toBeUndefined()
        expect(publicLicense('OR')).toBeUndefined()
    })

    it('knows where every license the builds accept is published', () => {
        const licenses = [...frontendLicenses(), ...mcpLicenses(), ...backendLicenses()]
            .flatMap(license => license.split(' WITH '))

        expect(licenses).toEqual(expect.arrayContaining(['MIT', 'GPL-2.0-only', 'Classpath-exception-2.0']))
        expect(licenses.filter(license => !publicLicense(license))).toEqual([])
    })
})

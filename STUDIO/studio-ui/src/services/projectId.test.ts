import { encodeProjectId, encodeProjectPath, toUrlSafeId } from './projectId'

describe('toUrlSafeId', () => {
    it('maps the standard alphabet onto the URL-safe one', () => {
        expect(toUrlSafeId('ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L/Qu9Cw0L0=')).toBe(
            'ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0='
        )
        expect(toUrlSafeId('ZGVzaWduOtCU0L7Qs9C+0LLQvtGA')).toBe('ZGVzaWduOtCU0L7Qs9C-0LLQvtGA')
    })

    it('leaves an already URL-safe id untouched', () => {
        expect(toUrlSafeId('ZGVzaWduOlByb2plY3Q=')).toBe('ZGVzaWduOlByb2plY3Q=')
    })
})

describe('encodeProjectId', () => {
    it('encodes an ASCII name the way the server does', () => {
        expect(encodeProjectId('design', 'Project')).toBe('ZGVzaWduOlByb2plY3Q=')
    })

    // EPBDS-16402: btoa() throws on a name outside Latin-1, so the name must be read as UTF-8 first.
    it('encodes a non-ASCII name as UTF-8 without a slash', () => {
        const id = encodeProjectId('design', 'Тарифный план')

        expect(id).toBe('ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0=')
        expect(id).not.toContain('/')
        expect(id).not.toContain('+')
    })

    it('encodes a name whose standard form would carry a plus', () => {
        expect(encodeProjectId('design', 'Договор')).toBe('ZGVzaWduOtCU0L7Qs9C-0LLQvtGA')
    })
})

describe('encodeProjectPath', () => {
    it('keeps the separators and encodes each segment', () => {
        expect(encodeProjectPath('rules/My Module#1.xlsx')).toBe('rules/My%20Module%231.xlsx')
    })
})

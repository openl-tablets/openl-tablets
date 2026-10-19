import { fieldValue } from './utils'

describe('fieldValue', () => {
    it('reads a setting sent alone', () => {
        expect(fieldValue('multi')).toBe('multi')
        expect(fieldValue(false)).toBe(false)
    })

    it('reads a setting sent wrapped with its flags', () => {
        expect(fieldValue({ value: 'saml', readOnly: true })).toBe('saml')
        expect(fieldValue({ value: 0 })).toBe(0)
    })

    it('reads nothing from a setting that is absent', () => {
        expect(fieldValue(undefined)).toBeUndefined()
    })
})

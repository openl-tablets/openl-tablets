/// <reference types="jest" />
import { isFormValuesEqual, SENSITIVE_EMPTY_FIELDS } from 'containers/repositories/formComparison'

describe('isFormValuesEqual', () => {
    it('returns true when saved is null', () => {
        expect(isFormValuesEqual({ name: 'a' }, null)).toBe(true)
    })

    it('returns true when current and saved are equal (shallow)', () => {
        expect(isFormValuesEqual({ name: 'repo' }, { name: 'repo' })).toBe(true)
    })

    it('returns false when current and saved differ', () => {
        expect(isFormValuesEqual({ name: 'repo1' }, { name: 'repo2' })).toBe(false)
    })

    it('treats empty password and undefined password as equal (sensitive field)', () => {
        expect(isFormValuesEqual({ password: '' }, {})).toBe(true)
        expect(isFormValuesEqual({ password: '' }, { password: undefined })).toBe(true)
        expect(isFormValuesEqual({}, { password: '' })).toBe(true)
    })

    it('treats empty secretKey and undefined as equal', () => {
        expect(isFormValuesEqual({ secretKey: '' }, {})).toBe(true)
    })

    it('treats empty accountKey and undefined as equal', () => {
        expect(isFormValuesEqual({ accountKey: '' }, {})).toBe(true)
    })

    it('returns false when password has a value and saved has none', () => {
        expect(isFormValuesEqual({ password: 'secret' }, {})).toBe(false)
        expect(isFormValuesEqual({ password: 'secret' }, { password: '' })).toBe(false)
    })

    it('treats nested sensitive fields (settings.password) as equal when both are empty/undefined', () => {
        // InputPassword component automatically sets field to undefined when value.secret is true
        // So we need to treat undefined/empty as equal for nested sensitive fields
        expect(isFormValuesEqual({ settings: { password: '' } }, { settings: {} })).toBe(true)
        expect(isFormValuesEqual({ settings: { password: undefined } }, { settings: {} })).toBe(true)
        expect(isFormValuesEqual({ settings: {} }, { settings: { password: '' } })).toBe(true)
        expect(isFormValuesEqual({ settings: { password: '' } }, { settings: { password: undefined } })).toBe(true)
    })

    it('treats nested sensitive fields (settings.secretKey) as equal when both are empty/undefined', () => {
        expect(isFormValuesEqual({ settings: { secretKey: '' } }, { settings: {} })).toBe(true)
        expect(isFormValuesEqual({ settings: {} }, { settings: { secretKey: '' } })).toBe(true)
    })

    it('returns false when nested password has a value and saved has none', () => {
        // User entered a password - this is a real change
        expect(isFormValuesEqual({ settings: { password: 'newpassword' } }, { settings: {} })).toBe(false)
        expect(isFormValuesEqual({ settings: { password: 'newpassword' } }, { settings: { password: '' } })).toBe(false)
    })

    it('returns false when nested password has non-empty values in both form and saved (user entered new password)', () => {
        // When both have non-empty values, user entered a new password (can't compare encrypted values,
        // but if form has a value, it means user typed something)
        expect(isFormValuesEqual({ settings: { password: 'newpassword' } }, { settings: { password: 'oldpassword' } })).toBe(false)
    })

    it('treats empty form password and non-empty saved password as equal (InputPassword sets undefined for secret values)', () => {
        // InputPassword automatically sets field to undefined when value.secret is true
        // This means password exists in saved data but form shows undefined - not a change
        expect(isFormValuesEqual({ settings: { password: undefined } }, { settings: { password: 'encryptedpassword' } })).toBe(true)
        expect(isFormValuesEqual({ settings: { password: '' } }, { settings: { password: 'encryptedpassword' } })).toBe(true)
        expect(isFormValuesEqual({ settings: {} }, { settings: { password: 'encryptedpassword' } })).toBe(true)
    })

    it('compares nested objects recursively', () => {
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a', mainBranchOnly: false } },
                { settings: { uri: 'http://a', mainBranchOnly: false } }
            )
        ).toBe(true)
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a' } },
                { settings: { uri: 'http://b' } }
            )
        ).toBe(false)
    })

    it('treats false and undefined boolean values as equal (for mainBranchOnly)', () => {
        // This is important for Deployment repositories where mainBranchOnly may be false or undefined
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a', mainBranchOnly: false } },
                { settings: { uri: 'http://a' } }
            )
        ).toBe(true)
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a' } },
                { settings: { uri: 'http://a', mainBranchOnly: false } }
            )
        ).toBe(true)
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a', mainBranchOnly: true } },
                { settings: { uri: 'http://a' } }
            )
        ).toBe(false) // true !== false (undefined treated as false)
        expect(
            isFormValuesEqual(
                { settings: { uri: 'http://a', mainBranchOnly: true } },
                { settings: { uri: 'http://a', mainBranchOnly: false } }
            )
        ).toBe(false)
    })

    it('returns false when one object has extra key with different value', () => {
        expect(isFormValuesEqual({ id: '1', name: 'r' }, { id: '1' })).toBe(false)
        expect(isFormValuesEqual({ id: '1' }, { id: '1', name: 'r' })).toBe(false)
    })

    it('SENSITIVE_EMPTY_FIELDS contains expected keys', () => {
        expect(SENSITIVE_EMPTY_FIELDS).toContain('password')
        expect(SENSITIVE_EMPTY_FIELDS).toContain('secretKey')
        expect(SENSITIVE_EMPTY_FIELDS).toContain('accountKey')
    })

    it('treats distinct arrays with the same elements as equal', () => {
        expect(isFormValuesEqual({ tags: ['a', 'b'] }, { tags: ['a', 'b'] })).toBe(true)
    })

    it('treats arrays with different elements as NOT equal', () => {
        expect(isFormValuesEqual({ tags: ['a', 'b'] }, { tags: ['a', 'c'] })).toBe(false)
    })

    it('treats arrays with different lengths as NOT equal', () => {
        expect(isFormValuesEqual({ tags: ['a', 'b'] }, { tags: ['a'] })).toBe(false)
    })

    it('treats array and non-array as NOT equal', () => {
        expect(isFormValuesEqual({ tags: ['a'] }, { tags: 'a' })).toBe(false)
    })

    it('treats empty arrays as equal', () => {
        expect(isFormValuesEqual({ tags: [] }, { tags: [] })).toBe(true)
    })

    it('compares nested arrays recursively', () => {
        expect(isFormValuesEqual({ items: [['a'], ['b']] }, { items: [['a'], ['b']] })).toBe(true)
        expect(isFormValuesEqual({ items: [['a'], ['b']] }, { items: [['a'], ['c']] })).toBe(false)
    })

    it('compares arrays with nested objects', () => {
        expect(
            isFormValuesEqual(
                { regions: [{ id: '1', description: 'US' }] },
                { regions: [{ id: '1', description: 'US' }] }
            )
        ).toBe(true)
        expect(
            isFormValuesEqual(
                { regions: [{ id: '1', description: 'US' }] },
                { regions: [{ id: '1', description: 'EU' }] }
            )
        ).toBe(false)
    })

    it('handles AWSS3RepositorySettings arrays (allAllowedRegions, allSseAlgorithms)', () => {
        const settings1 = {
            allAllowedRegions: [
                { id: 'us-east-1', description: 'US East' },
                { id: 'us-west-2', description: 'US West' },
            ],
            allSseAlgorithms: ['AES256', 'aws:kms'],
        }
        const settings2 = {
            allAllowedRegions: [
                { id: 'us-east-1', description: 'US East' },
                { id: 'us-west-2', description: 'US West' },
            ],
            allSseAlgorithms: ['AES256', 'aws:kms'],
        }
        const settings3 = {
            allAllowedRegions: [
                { id: 'us-east-1', description: 'US East' },
            ],
            allSseAlgorithms: ['AES256'],
        }
        expect(isFormValuesEqual({ settings: settings1 }, { settings: settings2 })).toBe(true)
        expect(isFormValuesEqual({ settings: settings1 }, { settings: settings3 })).toBe(false)
    })
})

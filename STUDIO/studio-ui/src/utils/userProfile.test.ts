import { describe, expect, it } from 'vitest'
import { changedValues, isUserProfileComplete } from './userProfile'

const completeProfile = {
    email: 'jane@example.com',
    firstName: 'Jane',
    lastName: 'Doe',
    displayName: 'Jane Doe',
}

describe('changedValues', () => {
    it('keeps only the fields that differ from the values the form opened with', () => {
        const opened = { ...completeProfile, showHeader: false, testsPerPage: 5 }
        const values = { ...completeProfile, firstName: 'Janet', showHeader: true, testsPerPage: 5 }

        expect(changedValues(values, opened)).toEqual({ firstName: 'Janet', showHeader: true })
    })

    it('keeps every field of a form that opened without values', () => {
        expect(changedValues(completeProfile, undefined)).toEqual(completeProfile)
    })
})

describe('isUserProfileComplete', () => {
    it('requires only email and display name', () => {
        expect(isUserProfileComplete(completeProfile)).toBe(true)
        expect(isUserProfileComplete({ ...completeProfile, firstName: '  ', lastName: '' })).toBe(true)
        expect(isUserProfileComplete({ ...completeProfile, email: '  ' })).toBe(false)
        expect(isUserProfileComplete({ ...completeProfile, displayName: '' })).toBe(false)
    })

    it('rejects an absent profile', () => {
        expect(isUserProfileComplete()).toBe(false)
        expect(isUserProfileComplete(null)).toBe(false)
    })
})

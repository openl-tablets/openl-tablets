import { describe, expect, it } from 'vitest'
import { isUserProfileComplete } from './userProfile'

const completeProfile = {
    email: 'jane@example.com',
    firstName: 'Jane',
    lastName: 'Doe',
    displayName: 'Jane Doe',
}

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

import { describe, expect, it } from 'vitest'
import { DisplayUserName } from '../constants'
import { deriveDisplayNameMode, formatDisplayName } from './displayName'

describe('deriveDisplayNameMode', () => {
    it('detects the First Last ordering', () => {
        expect(deriveDisplayNameMode({ firstName: 'Ada', lastName: 'Lovelace', displayName: 'Ada Lovelace' })).toBe(
            DisplayUserName.FirstLast
        )
    })

    it('detects the Last First ordering', () => {
        expect(deriveDisplayNameMode({ firstName: 'Ada', lastName: 'Lovelace', displayName: 'Lovelace Ada' })).toBe(
            DisplayUserName.LastFirst
        )
    })

    it('falls back to a custom value when neither ordering matches', () => {
        expect(deriveDisplayNameMode({ firstName: 'Ada', lastName: 'Lovelace', displayName: 'Countess' })).toBe(
            DisplayUserName.Other
        )
    })

    it('treats a missing display name as custom', () => {
        expect(deriveDisplayNameMode({ firstName: 'Ada', lastName: 'Lovelace' })).toBe(DisplayUserName.Other)
    })
})

describe('formatDisplayName', () => {
    it('formats First Last', () => {
        expect(formatDisplayName(DisplayUserName.FirstLast, 'Ada', 'Lovelace')).toBe('Ada Lovelace')
    })

    it('formats Last First', () => {
        expect(formatDisplayName(DisplayUserName.LastFirst, 'Ada', 'Lovelace')).toBe('Lovelace Ada')
    })

    it('returns null for a custom mode so the caller keeps its value', () => {
        expect(formatDisplayName(DisplayUserName.Other, 'Ada', 'Lovelace')).toBeNull()
    })

    it('trims the separator when a part is missing', () => {
        expect(formatDisplayName(DisplayUserName.FirstLast, 'Ada', '')).toBe('Ada')
        expect(formatDisplayName(DisplayUserName.LastFirst, undefined, 'Lovelace')).toBe('Lovelace')
    })
})

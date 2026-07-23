import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadProjectFilters, saveProjectFilters } from './filterStorage'

// jsdom ships no Web Storage, so the tests bring their own — per test, so it never outlives this file.
const memory = new Map<string, string>()

describe('project filter storage', () => {
    beforeEach(() => {
        memory.clear()
        vi.stubGlobal('localStorage', {
            getItem: (key: string) => memory.get(key) ?? null,
            setItem: (key: string, value: string) => memory.set(key, value),
            removeItem: (key: string) => memory.delete(key),
            clear: () => memory.clear(),
        })
    })

    it('remembers what the list is filtered, sorted and laid out by', () => {
        saveProjectFilters(new URLSearchParams('q=rates&status=EDITING&repo=design&tags=x&sort=name&view=grid&size=50'))

        const restored = loadProjectFilters()!

        expect(restored.get('q')).toBe('rates')
        expect(restored.get('status')).toBe('EDITING')
        expect(restored.get('repo')).toBe('design')
        expect(restored.get('tags')).toBe('x')
        expect(restored.get('sort')).toBe('name')
        expect(restored.get('view')).toBe('grid')
        expect(restored.get('size')).toBe('50')
    })

    it('keeps every value of a repeated filter', () => {
        saveProjectFilters(new URLSearchParams('status=EDITING&status=CLOSED'))

        expect(loadProjectFilters()!.getAll('status')).toEqual(['EDITING', 'CLOSED'])
    })

    it('forgets the page, which has moved on by the next visit', () => {
        saveProjectFilters(new URLSearchParams('q=rates&page=7'))

        expect(loadProjectFilters()!.has('page')).toBe(false)
    })

    it('remembers cleared filters as cleared', () => {
        saveProjectFilters(new URLSearchParams('q=rates'))
        saveProjectFilters(new URLSearchParams())

        expect(loadProjectFilters()!.toString()).toBe('')
    })

    it('has nothing to restore on a first visit', () => {
        expect(loadProjectFilters()).toBeNull()
    })
})

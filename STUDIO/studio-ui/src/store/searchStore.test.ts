import { beforeEach, describe, expect, it, vi } from 'vitest'
import { activeScope, listedScopes, type SearchScope, useSearchStore } from './searchStore'
import { searchScopeOf } from '../testing/search'

const scope = searchScopeOf

const store = () => useSearchStore.getState()

describe('searchStore', () => {
    beforeEach(() => useSearchStore.setState({ scopes: [], picked: null }))

    it('keeps the scopes of the screen in the order they appeared', () => {
        store().mount(scope('tree'))
        store().mount(scope('files'))

        expect(store().scopes.map(known => known.id)).toEqual(['tree', 'files'])
    })

    it('replaces a scope whose text changed, and leaves the field alone when nothing it draws did', () => {
        store().mount(scope('files'))
        const before = store().scopes

        // Another render of the screen: new handlers, the same texts.
        store().mount(scope('files'))
        expect(store().scopes).toBe(before)

        store().mount(scope('files', { value: 'xml' }))
        expect(store().scopes).not.toBe(before)
        expect(store().scopes[0]?.value).toBe('xml')
    })

    it.each<[string, Partial<SearchScope>]>([
        ['its name', { label: 'Other' }],
        ['its hint', { placeholder: 'Other' }],
        ['its rank', { secondary: true }],
        ['a key to send the text off', { onEnter: vi.fn() }],
        ['a control added', { action: { icon: null, label: 'Extended search', onClick: vi.fn() } }],
        ['a list to choose from', {
            action: { choice: 'a', choices: [{ value: 'a', label: 'A' }], icon: null, label: 'Where' },
        }],
    ])('redraws the field for %s', (_what, change) => {
        store().mount(scope('files'))
        const before = store().scopes

        store().mount(scope('files', change))

        expect(store().scopes).not.toBe(before)
    })

    it('redraws the field once a list to choose from gets the handler of the choice', () => {
        const action = { choice: '', choices: [{ value: '', label: 'All Guides' }], icon: null, label: 'Where' }
        store().mount(scope('guides', { action }))
        const before = store().scopes

        store().mount(scope('guides', { action: { ...action, onChoice: vi.fn() } }))

        expect(store().scopes).not.toBe(before)
        expect(store().scopes[0]?.action?.onChoice).toBeDefined()
    })

    it('redraws the field when the choice or the choices of a control change', () => {
        const choices = [{ value: '', label: 'All Guides' }, { value: 'studio/', label: 'Studio' }]
        const action = { choice: '', choices, icon: null, label: 'Where' }
        store().mount(scope('guides', { action }))
        const first = store().scopes

        store().mount(scope('guides', { action: { ...action, choice: 'studio/' } }))
        const narrowed = store().scopes
        expect(narrowed).not.toBe(first)

        store().mount(scope('guides', { action: { ...action, choice: 'studio/', choices: choices.slice(0, 1) } }))
        expect(store().scopes).not.toBe(narrowed)
    })

    it('searches the main part of the screen before a panel beside it, and the later of two alike', () => {
        expect(activeScope(store())).toBeUndefined()

        store().mount(scope('tree', { secondary: true }))
        expect(activeScope(store())?.id).toBe('tree')

        store().mount(scope('files'))
        store().mount(scope('rail', { secondary: true }))
        expect(activeScope(store())?.id).toBe('files')

        store().mount(scope('preview'))
        expect(activeScope(store())?.id).toBe('preview')
    })

    it('searches what the reader chose while it stays on screen', () => {
        store().mount(scope('tree', { secondary: true }))
        store().mount(scope('files'))

        store().pick('tree')
        expect(activeScope(store())?.id).toBe('tree')

        // The panel is put away: the choice goes with it, and does not come back when the panel does.
        store().unmount('tree')
        expect(activeScope(store())?.id).toBe('files')
        store().mount(scope('tree', { secondary: true }))
        expect(activeScope(store())?.id).toBe('files')
    })

    it('keeps the choice of the reader when another scope leaves the screen', () => {
        store().mount(scope('tree', { secondary: true }))
        store().mount(scope('files'))
        store().pick('tree')

        store().unmount('files')

        expect(store().picked).toBe('tree')
        expect(store().scopes.map(known => known.id)).toEqual(['tree'])
    })

    it('lists the main part of the screen before the panels beside it', () => {
        const scopes = [scope('tree', { secondary: true }), scope('files'), scope('revisions')]

        expect(listedScopes(scopes).map(known => known.id)).toEqual(['files', 'revisions', 'tree'])
    })
})

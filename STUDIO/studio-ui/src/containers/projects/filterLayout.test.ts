import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    EMPTY_LAYOUT,
    loadFilterLayout,
    moveGroup,
    orderGroups,
    saveFilterLayout,
} from './filterLayout'

const groups = [{ id: 'repository' }, { id: 'tag:Domain' }, { id: 'status' }]

/** The arrangement lives in the browser; each test starts from its own, empty memory. */
const stubStorage = () => {
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
        getItem: (key: string) => store[key] ?? null,
        setItem: (key: string, value: string) => { store[key] = value },
        removeItem: (key: string) => { delete store[key] },
        clear: () => Object.keys(store).forEach(key => delete store[key]),
    })
}

describe('orderGroups', () => {
    it('shows the groups in the order the user arranged them', () => {
        const arranged = orderGroups(groups, ['status', 'tag:Domain', 'repository'])

        expect(arranged.map(group => group.id)).toEqual(['status', 'tag:Domain', 'repository'])
    })

    it('lets a group the arrangement never saw fall in at its default place', () => {
        // A tag type added since is not in the stored order; it follows the groups that are.
        const arranged = orderGroups([...groups, { id: 'tag:LOB' }], ['status', 'repository'])

        expect(arranged.map(group => group.id)).toEqual(['status', 'repository', 'tag:Domain', 'tag:LOB'])
    })

    it('ignores a group the arrangement remembers but the rail no longer has', () => {
        const arranged = orderGroups(groups, ['tag:Gone', 'status'])

        expect(arranged.map(group => group.id)).toEqual(['status', 'repository', 'tag:Domain'])
    })
})

describe('moveGroup', () => {
    it('puts the dragged group where the one it was dropped on stood', () => {
        expect(moveGroup(['a', 'b', 'c'], 'c', 'a')).toEqual(['c', 'a', 'b'])
        expect(moveGroup(['a', 'b', 'c'], 'a', 'c')).toEqual(['b', 'c', 'a'])
    })

    it('leaves the order alone when there is nothing to move', () => {
        const order = ['a', 'b']

        expect(moveGroup(order, 'a', 'a')).toBe(order)
        expect(moveGroup(order, 'a', 'nope')).toBe(order)
    })
})

describe('the stored arrangement', () => {
    beforeEach(stubStorage)

    it('comes back as it was left', () => {
        saveFilterLayout({ order: ['status'], hidden: ['tag:Domain'], collapsed: ['repository']})

        expect(loadFilterLayout()).toEqual({ order: ['status'], hidden: ['tag:Domain'], collapsed: ['repository']})
    })

    it('is empty when nothing was stored, or what was stored cannot be read', () => {
        expect(loadFilterLayout()).toEqual(EMPTY_LAYOUT)

        localStorage.setItem('openl.projects.filters.layout', 'not json')
        expect(loadFilterLayout()).toEqual(EMPTY_LAYOUT)

        localStorage.setItem('openl.projects.filters.layout', JSON.stringify({ order: 'nonsense' }))
        expect(loadFilterLayout()).toEqual(EMPTY_LAYOUT)
    })
})

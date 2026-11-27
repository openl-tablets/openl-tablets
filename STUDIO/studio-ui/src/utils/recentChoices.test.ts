import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RECENT_LIMIT, recentBranches, recentChoices, recentProjects } from './recentChoices'

describe('recentChoices', () => {
    beforeEach(() => localStorage.clear())

    it('keeps the latest choices first, each once, up to the limit', () => {
        const recent = recentChoices('test.recent')
        for (let at = 0; at < RECENT_LIMIT + 2; at++) {
            recent.remember(`p${at}`)
        }
        recent.remember('p5')

        const read = recent.read()
        expect(read).toHaveLength(RECENT_LIMIT)
        expect(read.slice(0, 3)).toEqual(['p5', 'p11', 'p10'])
        expect(read).not.toContain('p0')
        expect(read.filter(item => item === 'p5')).toHaveLength(1)
    })

    it('keeps a list per scope', () => {
        recentBranches.remember('dev', 'design')
        recentBranches.remember('main', 'rating')

        expect(recentBranches.read('design')).toEqual(['dev'])
        expect(recentBranches.read('rating')).toEqual(['main'])
        expect(recentBranches.read('claims')).toEqual([])
    })

    it('forgets a choice that is gone, and the choices the server no longer knows', () => {
        recentProjects.remember('a')
        recentProjects.remember('b')
        recentProjects.remember('c')

        recentProjects.forget('b')
        expect(recentProjects.read()).toEqual(['c', 'a'])

        recentProjects.keepOnly(['a', 'x'])
        expect(recentProjects.read()).toEqual(['a'])
    })

    it('stores nothing once every choice is forgotten', () => {
        recentProjects.remember('p1')

        recentProjects.forget('p1')

        expect(localStorage.getItem('openl.recent.projects')).toBeNull()
    })

    it('keeps the lists of the scopes used most recently once there are too many', () => {
        for (let at = 0; at < 55; at++) {
            recentBranches.remember('main', `p${at}`)
        }
        recentBranches.remember('dev', 'p0')

        expect(recentBranches.read('p0')).toEqual(['dev'])
        expect(recentBranches.read('p1')).toEqual([])
        expect(recentBranches.read('p54')).toEqual(['main'])
        expect(JSON.parse(localStorage.getItem('openl.recent.branches') ?? '[]')).toHaveLength(50)
    })

    it('tells its readers of a change, made here or in another tab, and stops when asked', () => {
        const listener = vi.fn()
        const stop = recentBranches.subscribe(listener)
        recentBranches.remember('dev', 'design')
        expect(listener).toHaveBeenCalledTimes(1)

        // The same choice again changes nothing, and tells nothing.
        recentBranches.remember('dev', 'design')
        expect(listener).toHaveBeenCalledTimes(1)

        globalThis.dispatchEvent(new StorageEvent('storage', { key: 'openl.recent.branches' }))
        globalThis.dispatchEvent(new StorageEvent('storage', { key: 'openl.recent.projects' }))
        expect(listener).toHaveBeenCalledTimes(2)

        stop()
        recentBranches.remember('main', 'design')
        expect(listener).toHaveBeenCalledTimes(2)
    })

    it('keeps a list the same array until it changes, whatever happens to the others', () => {
        recentBranches.remember('dev', 'design')
        recentBranches.remember('main', 'rating')
        const design = recentBranches.read('design')
        const rating = recentBranches.read('rating')
        expect(recentBranches.read('claims')).toBe(recentBranches.read('other'))

        recentBranches.remember('main', 'design')

        expect(recentBranches.read('design')).not.toBe(design)
        expect(recentBranches.read('design')).toEqual(['main', 'dev'])
        expect(recentBranches.read('rating')).toBe(rating)
    })

    it('reads a list another tab wrote', () => {
        recentProjects.remember('p1')
        expect(recentProjects.read()).toEqual(['p1'])

        localStorage.setItem('openl.recent.projects', '[["",["p2","p1"]]]')

        expect(recentProjects.read()).toEqual(['p2', 'p1'])
    })

    it('keeps the order the scopes were used in, a scope named by a number included', () => {
        recentBranches.remember('main', 'design')
        recentBranches.remember('main', '1')
        recentBranches.remember('dev', 'design')

        expect(JSON.parse(localStorage.getItem('openl.recent.branches') ?? '[]').map(([scope]: [string]) => scope))
            .toEqual(['1', 'design'])
    })

    it('reads nothing from a text another version wrote', () => {
        localStorage.setItem('openl.recent.projects', '["a"]')
        localStorage.setItem('openl.recent.branches', '[["p1",[1]]]')

        expect(recentProjects.read()).toEqual([])
        expect(recentBranches.read('p1')).toEqual([])
    })
})

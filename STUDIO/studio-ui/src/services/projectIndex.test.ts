import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from './apiCall'
import { getProjectIndex, invalidateProjectIndex, isProjectIndexStale } from './projectIndex'
import { getProjects } from './repositories'

vi.mock('./repositories', () => ({ getProjects: vi.fn() }))

const page = (...names: string[]) => ({ content: names.map(name => ({ id: name, name })), statuses: []})

describe('the projects snapshot', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        invalidateProjectIndex()
        vi.mocked(getProjects).mockResolvedValue(page('Alpha') as never)
    })

    it('is read once and shared, so the list and the tree cost one request between them', async () => {
        const [first, second] = await Promise.all([getProjectIndex(), getProjectIndex()])

        expect(getProjects).toHaveBeenCalledTimes(1)
        expect(first).toBe(second)
        expect(first.projects.map(project => project.name)).toEqual(['Alpha'])
    })

    it('is read again after anything on the server changed, wherever the change was made', async () => {
        await getProjectIndex()
        vi.mocked(getProjects).mockResolvedValue(page('Alpha', 'Beta') as never)

        // A project deleted on its own page: the list the user goes back to must not show it.
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        const reread = await getProjectIndex()

        expect(getProjects).toHaveBeenCalledTimes(2)
        expect(reread.projects.map(project => project.name)).toEqual(['Alpha', 'Beta'])
    })

    it('does not remember a failed read as the answer', async () => {
        vi.mocked(getProjects).mockRejectedValueOnce(new Error('offline'))

        await expect(getProjectIndex()).rejects.toThrow('offline')
        await expect(getProjectIndex()).resolves.toMatchObject({ projects: [{ name: 'Alpha' }]})
    })

    describe('the staleness policy', () => {
        afterEach(() => {
            vi.useRealTimers()
        })

        it('re-reads a snapshot that outlived its trust window, even with no invalidation', async () => {
            vi.useFakeTimers()
            await getProjectIndex()
            expect(isProjectIndexStale()).toBe(false)

            // Pings can be lost while a laptop sleeps: age alone must not be trusted forever.
            vi.setSystemTime(Date.now() + 5 * 60_000 + 1)

            expect(isProjectIndexStale()).toBe(true)
            await getProjectIndex()
            expect(getProjects).toHaveBeenCalledTimes(2)
        })

        it('keeps serving a snapshot inside its trust window', async () => {
            vi.useFakeTimers()
            await getProjectIndex()

            vi.setSystemTime(Date.now() + 60_000)

            await getProjectIndex()
            expect(getProjects).toHaveBeenCalledTimes(1)
        })
    })
})

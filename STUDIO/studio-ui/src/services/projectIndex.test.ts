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

    it('coalesces changes that land while a read is on the wire into a single re-read', async () => {
        let answerFirst: (page: unknown) => void = () => undefined
        vi.mocked(getProjects).mockReturnValueOnce(new Promise(resolve => {
            answerFirst = resolve
        }) as never)

        // A read is on the wire; two changes land before it answers. They must not each start their own
        // request — but the answer must still reflect them.
        const inflight = getProjectIndex()
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        expect(getProjectIndex()).toBe(inflight)
        expect(getProjects).toHaveBeenCalledTimes(1)

        vi.mocked(getProjects).mockResolvedValue(page('Alpha', 'Beta') as never)
        answerFirst(page('Alpha'))

        // The caller that asked during the read is answered with the workspace as it is after the changes,
        // not with what was on the wire before them.
        expect((await inflight).projects.map(project => project.name)).toEqual(['Alpha', 'Beta'])
        expect(getProjects).toHaveBeenCalledTimes(2)

        // The re-read is the shared snapshot now, so a later reader costs nothing.
        await getProjectIndex()
        expect(getProjects).toHaveBeenCalledTimes(2)
    })

    it('keeps the snapshot it already read when the coalesced re-read fails', async () => {
        let answerFirst: (page: unknown) => void = () => undefined
        vi.mocked(getProjects).mockReturnValueOnce(new Promise(resolve => {
            answerFirst = resolve
        }) as never)

        const inflight = getProjectIndex()
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        vi.mocked(getProjects).mockRejectedValueOnce(new Error('offline'))
        answerFirst(page('Alpha'))

        // The re-read failed, but the workspace was read successfully a moment ago: show that rather than an error.
        expect((await inflight).projects.map(project => project.name)).toEqual(['Alpha'])

        // The failure is not remembered as the answer either — the next read asks again.
        vi.mocked(getProjects).mockResolvedValue(page('Alpha', 'Beta') as never)
        expect((await getProjectIndex()).projects.map(project => project.name)).toEqual(['Alpha', 'Beta'])
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

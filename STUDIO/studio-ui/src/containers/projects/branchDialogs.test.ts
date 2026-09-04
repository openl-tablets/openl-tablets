import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { openDeleteBranchDialog, openMergeDialog } from './branchDialogs'
import { getProjectBranches } from '../../services/repositories'
import type { Project } from '../../types/projects'

vi.mock('../../services/repositories', () => ({ getProjectBranches: vi.fn() }))

const project = { id: 'p1', name: 'Alpha', repository: 'design', branch: 'feature' } as unknown as Project

const captureEvent = (name: string) => {
    const events: CustomEvent[] = []
    const listener = (event: Event) => events.push(event as CustomEvent)
    window.addEventListener(name, listener)
    return { events, stop: () => window.removeEventListener(name, listener) }
}

describe('branch dialogs', () => {
    let merge: ReturnType<typeof captureEvent>
    let remove: ReturnType<typeof captureEvent>

    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: false, base: true },
            { name: 'feature', protected: false, base: false },
        ])
        merge = captureEvent('openMergeModal')
        remove = captureEvent('openDeleteBranchModal')
    })

    afterEach(() => {
        merge.stop()
        remove.stop()
    })

    // The dialogs seed with the branches that hold the project; the merge dialog widens the list to the
    // whole repository on demand, from inside the modal — see EPBDS-16411.
    it('seeds the dialogs with the branches holding the project', async () => {
        await openMergeDialog(project, vi.fn())
        await openDeleteBranchDialog(project, vi.fn())

        expect(getProjectBranches).toHaveBeenCalledTimes(2)
        expect(getProjectBranches).toHaveBeenNthCalledWith(1, 'p1')
        expect(getProjectBranches).toHaveBeenNthCalledWith(2, 'p1')
    })

    it('opens the merge dialog seeded with the project branches', async () => {
        const onSuccess = vi.fn()

        await openMergeDialog(project, onSuccess)

        expect(merge.events).toHaveLength(1)
        expect(merge.events[0]!.detail).toMatchObject({
            projectId: 'p1',
            projectName: 'Alpha',
            repositoryId: 'design',
            repositoryType: 'repo-git',
            currentBranch: 'feature',
            onSuccess,
        })
        expect(merge.events[0]!.detail.branches).toHaveLength(2)
        expect(merge.events[0]!.detail.initialStep).toBeUndefined()
    })

    it('opens the merge dialog straight on conflict resolution when asked', async () => {
        await openMergeDialog(project, vi.fn(), 'conflicts')

        expect(merge.events[0]!.detail.initialStep).toBe('conflicts')
    })

    it('opens the delete dialog for the current branch, naming the main one', async () => {
        const onSuccess = vi.fn()

        await openDeleteBranchDialog(project, onSuccess)

        expect(remove.events[0]!.detail).toMatchObject({
            repositoryId: 'design',
            projectName: 'Alpha',
            branch: 'feature',
            mainBranch: 'main',
            onSuccess,
        })
    })

    it('still opens a dialog when the branches cannot be read', async () => {
        vi.mocked(getProjectBranches).mockRejectedValue(new Error('offline'))

        await openDeleteBranchDialog(project, vi.fn())

        expect(remove.events[0]!.detail.mainBranch).toBeUndefined()
    })
})

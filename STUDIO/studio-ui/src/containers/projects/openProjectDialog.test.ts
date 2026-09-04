import { openProjectDialog } from './openProjectDialog'

const project = { id: 'p1', name: 'Standalone', repository: 'design', branch: 'master' }

describe('openProjectDialog', () => {
    it('opens a project that declares nothing without asking', () => {
        const run = vi.fn()
        const dispatch = vi.spyOn(window, 'dispatchEvent')

        openProjectDialog({ ...project, dependencies: []}, run)

        expect(run).toHaveBeenCalledWith(true)
        expect(dispatch).not.toHaveBeenCalled()
        dispatch.mockRestore()
    })

    it('asks about a project that declares dependencies, and hands the answer back', () => {
        const run = vi.fn()
        const events: CustomEvent[] = []
        const listener = (event: Event) => events.push(event as CustomEvent)
        window.addEventListener('openProjectModal', listener)

        try {
            openProjectDialog({ ...project, dependencies: [{ name: 'Provider' }]}, run)
        } finally {
            window.removeEventListener('openProjectModal', listener)
        }

        expect(run).not.toHaveBeenCalled()
        expect(events).toHaveLength(1)
        expect(events[0]!.detail.dependencies).toEqual([{ name: 'Provider' }])
        events[0]!.detail.onConfirm(false)
        expect(run).toHaveBeenCalledWith(false)
    })
})

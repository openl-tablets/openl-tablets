import { describe, it, expect, beforeEach, vi } from 'vitest'

describe('loaderBridge', () => {
    beforeEach(() => {
        vi.resetModules()
        globalThis.openl = undefined
    })

    it('publishes the bridge without clobbering other openl globals', async () => {
        const projectStatus = { fetch: vi.fn(), subscribe: vi.fn() }
        globalThis.openl = { projectStatus }

        await import('./loaderBridge')

        expect(globalThis.openl?.loader).toBeDefined()
        expect(globalThis.openl?.projectStatus).toBe(projectStatus)
    })

    it('show() and hide() drive the loader count in the app store', async () => {
        await import('./loaderBridge')
        const { useAppStore } = await import('../store')

        globalThis.openl?.loader?.show()
        globalThis.openl?.loader?.show()
        expect(useAppStore.getState().loaderCount).toBe(2)

        globalThis.openl?.loader?.hide()
        globalThis.openl?.loader?.hide()
        expect(useAppStore.getState().loaderCount).toBe(0)
    })

    it('ignores hide() without a paired show()', async () => {
        await import('./loaderBridge')
        const { useAppStore } = await import('../store')

        globalThis.openl?.loader?.hide()

        expect(useAppStore.getState().loaderCount).toBe(0)
    })
})

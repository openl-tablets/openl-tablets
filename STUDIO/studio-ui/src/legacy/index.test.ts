vi.mock('antd', () => ({
    notification: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}))

vi.mock('../services/projectStatus', () => ({
    fetchProjectStatus: vi.fn(),
    subscribeProjectStatus: vi.fn(),
}))

describe('legacy bridges entry', () => {
    beforeEach(() => {
        vi.resetModules()
        globalThis.openl = undefined
    })

    it('announces openl:ready only after every bridge is installed', async () => {
        const installed: Array<{ notification: boolean, projectStatus: boolean, loader: boolean }> = []
        document.addEventListener('openl:ready', () => {
            installed.push({
                notification: globalThis.openl?.notification != null,
                projectStatus: globalThis.openl?.projectStatus != null,
                loader: globalThis.openl?.loader != null,
            })
        }, { once: true })

        await import('./index')

        expect(installed).toEqual([{ notification: true, projectStatus: true, loader: true }])
    })
})

const { successMock, errorMock, infoMock } = vi.hoisted(() => ({
    successMock: vi.fn(),
    errorMock: vi.fn(),
    infoMock: vi.fn(),
}))

vi.mock('antd', () => ({
    notification: { success: successMock, error: errorMock, info: infoMock },
}))

describe('notificationBridge', () => {
    beforeEach(() => {
        vi.resetModules()
        globalThis.openl = undefined
        vi.clearAllMocks()
    })

    it('publishes the bridge without clobbering other openl globals', async () => {
        const projectStatus = { fetch: vi.fn(), subscribe: vi.fn() }
        globalThis.openl = { projectStatus }

        await import('./notificationBridge')

        expect(globalThis.openl?.notification).toBeDefined()
        expect(globalThis.openl?.projectStatus).toBe(projectStatus)
    })

    it('shows a short-lived success toast', async () => {
        await import('./notificationBridge')

        globalThis.openl?.notification?.success('Project was saved successfully.')

        expect(successMock).toHaveBeenCalledWith({ title: 'Project was saved successfully.', duration: 4 })
    })

    it('shows a sticky error toast', async () => {
        await import('./notificationBridge')

        globalThis.openl?.notification?.error('Something went wrong.')

        expect(errorMock).toHaveBeenCalledWith({ title: 'Something went wrong.', duration: 0 })
    })

    it('shows a longer-lived info toast', async () => {
        await import('./notificationBridge')

        globalThis.openl?.notification?.info('File generation is too long')

        expect(infoMock).toHaveBeenCalledWith({ title: 'File generation is too long', duration: 8 })
    })
})

describe('projectIdBridge', () => {
    beforeEach(() => {
        globalThis.openl = undefined
        vi.resetModules()
    })

    it('publishes the encoder the React screens use', async () => {
        await import('./projectIdBridge')

        expect(globalThis.openl?.encodeProjectId).toBeTypeOf('function')
    })

    // EPBDS-16402: a legacy page used to build the id with btoa, which throws on a non-ASCII name.
    it('encodes a non-ASCII name the way the server does', async () => {
        await import('./projectIdBridge')

        expect(globalThis.openl?.encodeProjectId?.('design', 'Тарифный план')).toBe(
            'ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0='
        )
    })

    it('keeps bridges installed by other modules', async () => {
        globalThis.openl = { loader: { show: vi.fn(), hide: vi.fn() } }

        await import('./projectIdBridge')

        expect(globalThis.openl?.loader).toBeDefined()
        expect(globalThis.openl?.encodeProjectId).toBeTypeOf('function')
    })
})

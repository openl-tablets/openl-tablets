import { saveFile } from 'utils/download'

describe('saveFile', () => {
    let clickSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        // A save leaves the release of the address waiting for a while. The clock is the test's, so that no
        // release is left behind to fire once the test has finished with the window it would reach into.
        vi.useFakeTimers()
        clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
        // jsdom implements no object URLs.
        URL.createObjectURL = vi.fn(() => 'blob:saved')
        URL.revokeObjectURL = vi.fn()
    })

    afterEach(() => {
        vi.useRealTimers()
        clickSpy.mockRestore()
    })

    it('hands the browser the file under the name it is saved as', () => {
        saveFile('a,b', 'rows.csv', 'text/csv')

        expect(clickSpy).toHaveBeenCalled()
        expect(URL.createObjectURL).toHaveBeenCalled()
    })

    it('releases the address even when the browser refuses the click', () => {
        // Nothing schedules the release but the save itself, so it is scheduled whatever the click does.
        clickSpy.mockImplementation(() => {
            throw new Error('refused')
        })

        expect(() => saveFile(new Blob(['x']), 'run-result.xlsx')).toThrow('refused')

        vi.advanceTimersByTime(60_000)
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:saved')
    })

    it('leaves the address alive long after the click, and releases it in the end', () => {
        // A browser reads the address when it starts the download — a browser asking where to save the file
        // only once the user has answered. Released before then, it would name nothing and the download
        // would be abandoned.
        saveFile(new Blob(['x']), 'run-result.xlsx')

        vi.advanceTimersByTime(10_000)
        expect(URL.revokeObjectURL).not.toHaveBeenCalled()

        vi.advanceTimersByTime(60_000)
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:saved')
    })
})

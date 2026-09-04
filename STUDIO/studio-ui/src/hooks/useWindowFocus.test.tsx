import { render } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useWindowFocus } from './useWindowFocus'

const Probe = ({ onFocus }: { onFocus: () => void }) => {
    useWindowFocus(onFocus)
    return null
}

describe('useWindowFocus', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('fires when the user comes back to the tab', () => {
        const onFocus = vi.fn()
        render(<Probe onFocus={onFocus} />)

        window.dispatchEvent(new Event('focus'))
        document.dispatchEvent(new Event('visibilitychange'))

        expect(onFocus).toHaveBeenCalledTimes(2)
    })

    it('stays quiet while the tab is hidden', () => {
        const onFocus = vi.fn()
        render(<Probe onFocus={onFocus} />)
        vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')

        document.dispatchEvent(new Event('visibilitychange'))

        expect(onFocus).not.toHaveBeenCalled()
    })

    it('stops listening after unmount', () => {
        const onFocus = vi.fn()
        const { unmount } = render(<Probe onFocus={onFocus} />)
        unmount()

        window.dispatchEvent(new Event('focus'))

        expect(onFocus).not.toHaveBeenCalled()
    })
})

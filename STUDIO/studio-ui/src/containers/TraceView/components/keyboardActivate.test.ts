import type React from 'react'
import { onActivate } from 'containers/TraceView/components/keyboardActivate'

const keyEvent = (key: string): React.KeyboardEvent => {
    const preventDefault = vi.fn()
    return { key, preventDefault } as unknown as React.KeyboardEvent & { preventDefault: ReturnType<typeof vi.fn> }
}

describe('onActivate', () => {
    it('runs the action and prevents the default on Enter', () => {
        const action = vi.fn()
        const event = keyEvent('Enter')

        onActivate(action)(event)

        expect(action).toHaveBeenCalledTimes(1)
        expect(event.preventDefault).toHaveBeenCalledTimes(1)
    })

    it('runs the action and prevents the default on Space', () => {
        const action = vi.fn()
        const event = keyEvent(' ')

        onActivate(action)(event)

        expect(action).toHaveBeenCalledTimes(1)
        expect(event.preventDefault).toHaveBeenCalledTimes(1)
    })

    it('ignores other keys — no action, no preventDefault', () => {
        const action = vi.fn()
        const tab = keyEvent('Tab')
        const arrow = keyEvent('ArrowDown')
        const letter = keyEvent('a')

        const handler = onActivate(action)
        handler(tab)
        handler(arrow)
        handler(letter)

        expect(action).not.toHaveBeenCalled()
        expect(tab.preventDefault).not.toHaveBeenCalled()
        expect(arrow.preventDefault).not.toHaveBeenCalled()
        expect(letter.preventDefault).not.toHaveBeenCalled()
    })
})

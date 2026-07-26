import { render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectChanges } from '../services/projectChanges'
import { useLiveProjectChanges } from './useLiveProjectChanges'

vi.mock('../services/projectChanges', () => ({ subscribeProjectChanges: vi.fn() }))

const Probe = ({ projectId, onChange }: { projectId: string | undefined, onChange: (files: string[]) => void }) => {
    useLiveProjectChanges(projectId, onChange)
    return null
}

describe('useLiveProjectChanges', () => {
    let ping: (files: string[]) => void
    const unsubscribe = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        vi.useFakeTimers()
        vi.mocked(subscribeProjectChanges).mockImplementation((_, onPing) => {
            ping = onPing
            return { unsubscribe }
        })
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('collapses a burst of pings into one reload, merging the files they named', () => {
        const onChange = vi.fn()
        render(<Probe onChange={onChange} projectId="p1" />)
        expect(subscribeProjectChanges).toHaveBeenCalledWith('p1', expect.any(Function))

        ping(['rules/A.xlsx'])
        ping(['rules/B.xlsx'])
        vi.advanceTimersByTime(500)

        expect(onChange).toHaveBeenCalledTimes(1)
        expect(onChange).toHaveBeenCalledWith(['rules/A.xlsx', 'rules/B.xlsx'])
    })

    it('waits until the page knows its project', () => {
        const { rerender } = render(<Probe onChange={vi.fn()} projectId={undefined} />)
        expect(subscribeProjectChanges).not.toHaveBeenCalled()

        // The project loaded — now there is something to watch.
        rerender(<Probe onChange={vi.fn()} projectId="p1" />)
        expect(subscribeProjectChanges).toHaveBeenCalledWith('p1', expect.any(Function))
    })

    it('moves the subscription when the page navigates to another project', () => {
        const { rerender } = render(<Probe onChange={vi.fn()} projectId="p1" />)
        rerender(<Probe onChange={vi.fn()} projectId="p2" />)

        expect(unsubscribe).toHaveBeenCalledTimes(1)
        expect(subscribeProjectChanges).toHaveBeenLastCalledWith('p2', expect.any(Function))
    })
})

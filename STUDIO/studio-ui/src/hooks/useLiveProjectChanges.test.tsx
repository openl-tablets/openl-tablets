import { render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WORKSPACE_CHANGED_EVENT } from '../services/apiCall'
import type { ChangePing } from '../services/changePing'
import { CLIENT_ID } from '../services/clientId'
import { subscribeProjectChanges } from '../services/projectChanges'
import { useLiveProjectChanges } from './useLiveProjectChanges'

vi.mock('../services/projectChanges', () => ({ subscribeProjectChanges: vi.fn() }))

/** A ping of somebody else's change, naming the files it touched. */
const changed = (...files: string[]): ChangePing => ({ files, origins: ['another-tab'], scope: 'project' })

const Probe = ({ projectId, onChange }: { projectId: string | undefined, onChange: (files: string[]) => void }) => {
    useLiveProjectChanges(projectId, onChange)
    return null
}

describe('useLiveProjectChanges', () => {
    let ping: (ping: ChangePing) => void
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

        ping(changed('rules/A.xlsx'))
        ping(changed('rules/B.xlsx'))
        vi.advanceTimersByTime(500)

        expect(onChange).toHaveBeenCalledTimes(1)
        expect(onChange).toHaveBeenCalledWith(['rules/A.xlsx', 'rules/B.xlsx'])
    })

    it('ignores the echo of a change this tab made itself', () => {
        vi.setSystemTime(new Date('2000-07-01T00:00:00Z'))
        const onChange = vi.fn()
        render(<Probe onChange={onChange} projectId="p1" />)

        // The page reloaded itself when its own mutation succeeded; the ping only repeats that.
        window.dispatchEvent(new Event(WORKSPACE_CHANGED_EVENT))
        ping({ files: ['rules/A.xlsx'], origins: [CLIENT_ID], scope: 'project' })
        vi.advanceTimersByTime(3000)
        expect(onChange).not.toHaveBeenCalled()

        // A ping standing for this tab's change and another session's is not an echo: dropping it
        // would drop the other session's change with it.
        ping({ files: ['rules/A.xlsx'], origins: [CLIENT_ID, 'another-tab'], scope: 'project' })
        vi.advanceTimersByTime(500)
        expect(onChange).toHaveBeenCalledWith(['rules/A.xlsx'])
    })

    it('keeps the named files when the id-free workspace ping rides in the same batch', () => {
        // Every change of a project pings twice — its own ping names the files, the workspace ping
        // never names any — and both land in one batch. Reading the second as "anything changed"
        // would throw the file names away on every change there is.
        const onChange = vi.fn()
        render(<Probe onChange={onChange} projectId="p1" />)

        ping({ files: ['rules/A.xlsx'], origins: ['another-tab'], scope: 'project' })
        ping({ files: [], origins: ['another-tab'], scope: 'workspace' })
        vi.advanceTimersByTime(500)

        expect(onChange).toHaveBeenCalledWith(['rules/A.xlsx'])
    })

    it('lets a project-wide ping swallow the files of the others it is merged with', () => {
        const onChange = vi.fn()
        render(<Probe onChange={onChange} projectId="p1" />)

        // An empty list stands for "anything may have changed", so the batch cannot claim to cover
        // one file only — the open file pane would then keep showing what the change replaced.
        ping(changed('rules/A.xlsx'))
        ping(changed())
        vi.advanceTimersByTime(500)

        expect(onChange).toHaveBeenCalledWith([])
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

    it('leaves the batch of the project it watched behind when the page moves to another', () => {
        const onChange = vi.fn()
        const { rerender } = render(<Probe onChange={onChange} projectId="p1" />)

        // Gathered for p1 and still waiting out the coalesce window when the page navigates.
        ping(changed('rules/A.xlsx'))
        rerender(<Probe onChange={onChange} projectId="p2" />)
        vi.advanceTimersByTime(3000)

        expect(onChange).not.toHaveBeenCalled()
    })
})

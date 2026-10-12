import apiCall, { readTaskResult } from '../../services/apiCall'
import { subscribeTopic } from '../../services/stompTopic'
import { ResultNotReadyError } from '../../services/taskResult'
import { jsonResponse } from '../../testing/responses'
import { mergeStatusTopic } from '../execution/topics'
import { QUIET_SPELL_MS } from '../execution/useExecutionProgress'
import { mergeBranches } from './mergeBranches'
import type { MergeResultResponse } from './types'

vi.mock('../../services/apiCall', () => ({
    default: vi.fn(),
    readTaskResult: vi.fn(),
}))

vi.mock('../../services/stompTopic', () => ({
    subscribeTopic: vi.fn(),
}))

const mockApiCall = vi.mocked(apiCall)
const mockReadTaskResult = vi.mocked(readTaskResult)
const mockSubscribeTopic = vi.mocked(subscribeTopic)

const SUCCESS: MergeResultResponse = { status: 'success', conflictGroups: []}

describe('mergeBranches', () => {
    let report: (body: string) => void
    const unsubscribe = vi.fn()

    beforeEach(() => {
        report = () => undefined
        mockSubscribeTopic.mockImplementation((_destination, onBody) => {
            report = onBody
            return { unsubscribe }
        })
        mockApiCall.mockResolvedValue(undefined)
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('starts the merge and answers with its result', async () => {
        mockReadTaskResult.mockResolvedValueOnce(jsonResponse(SUCCESS))

        await expect(mergeBranches('proj-1', 'receive', 'feature', false)).resolves.toEqual(SUCCESS)

        expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/merge', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: 'receive', otherBranch: 'feature' }),
        }, expect.objectContaining({ throwError: true }))
        expect(mockReadTaskResult).toHaveBeenCalledWith('/projects/proj-1/merge', undefined, expect.anything())
        expect(unsubscribe).toHaveBeenCalled()
    })

    it('watches the merge before it starts, so its end cannot go unheard', async () => {
        mockReadTaskResult.mockResolvedValueOnce(jsonResponse(SUCCESS))

        await mergeBranches('proj-1', 'send', 'feature', true)

        expect(mockSubscribeTopic).toHaveBeenCalledWith(mergeStatusTopic('proj-1'), expect.any(Function))
        expect(mockSubscribeTopic.mock.invocationCallOrder[0])
            .toBeLessThan(mockApiCall.mock.invocationCallOrder[0]!)
        expect(mockApiCall).toHaveBeenCalledWith('/projects/proj-1/merge?force=true', expect.anything(),
            expect.anything())
    })

    it('reads the result again once the merge reports its end', async () => {
        const conflicts: MergeResultResponse = {
            status: 'conflicts',
            conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['Main.xlsx']}],
        }
        mockReadTaskResult
            .mockRejectedValueOnce(new ResultNotReadyError())
            .mockResolvedValueOnce(jsonResponse(conflicts))

        const merged = mergeBranches('proj-1', 'receive', 'feature', false)
        await vi.waitFor(() => expect(mockReadTaskResult).toHaveBeenCalledTimes(1))
        // A status the merge has not ended with does not read the result again.
        report('"STARTED"')
        report('"COMPLETED"')

        await expect(merged).resolves.toEqual(conflicts)
        expect(mockReadTaskResult).toHaveBeenCalledTimes(2)
    })

    it('asks again after a while when the topic stays silent', async () => {
        vi.useFakeTimers()
        mockReadTaskResult
            .mockRejectedValueOnce(new ResultNotReadyError())
            .mockResolvedValueOnce(jsonResponse(SUCCESS))

        const merged = mergeBranches('proj-1', 'receive', 'feature', false)
        await vi.waitFor(() => expect(mockReadTaskResult).toHaveBeenCalledTimes(1))
        await vi.advanceTimersByTimeAsync(QUIET_SPELL_MS)

        await expect(merged).resolves.toEqual(SUCCESS)
    })

    it('reports a refused merge and stops watching', async () => {
        const refusal = new Error('Project has unresolved merge conflicts.')
        mockApiCall.mockRejectedValueOnce(refusal)

        await expect(mergeBranches('proj-1', 'receive', 'feature', false)).rejects.toBe(refusal)

        expect(mockReadTaskResult).not.toHaveBeenCalled()
        expect(unsubscribe).toHaveBeenCalled()
    })

    it('reports what the merge failed with', async () => {
        const failure = new Error('Repository is not available')
        mockReadTaskResult.mockRejectedValueOnce(failure)

        await expect(mergeBranches('proj-1', 'receive', 'feature', false)).rejects.toBe(failure)
    })
})

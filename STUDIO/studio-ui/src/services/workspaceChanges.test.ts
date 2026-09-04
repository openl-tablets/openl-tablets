import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeTopic } from './stompTopic'
import { subscribeWorkspaceChanges } from './workspaceChanges'

vi.mock('./stompTopic', () => ({ subscribeTopic: vi.fn() }))

describe('subscribeWorkspaceChanges', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('watches both the personal workspace pings and the repository broadcast', () => {
        vi.mocked(subscribeTopic).mockReturnValue({ unsubscribe: vi.fn() })
        const onChange = vi.fn()

        subscribeWorkspaceChanges(onChange)

        const destinations = vi.mocked(subscribeTopic).mock.calls.map(([destination]) => destination)
        expect(destinations.sort()).toEqual(['/user/topic/projects/changed', '/user/topic/workspace/changed'])
        // Whichever topic pings, the caller learns about it the same way — and learns who caused it.
        vi.mocked(subscribeTopic).mock.calls.forEach(([, onBody]) => onBody(JSON.stringify({ origins: ['tab-1']})))
        expect(onChange).toHaveBeenCalledTimes(2)
        expect(onChange).toHaveBeenCalledWith({ files: [], origins: ['tab-1'], scope: 'workspace' })
    })

    it('drops both subscriptions on unsubscribe', () => {
        const first = { unsubscribe: vi.fn() }
        const second = { unsubscribe: vi.fn() }
        vi.mocked(subscribeTopic).mockReturnValueOnce(first).mockReturnValueOnce(second)

        subscribeWorkspaceChanges(vi.fn()).unsubscribe()

        expect(first.unsubscribe).toHaveBeenCalled()
        expect(second.unsubscribe).toHaveBeenCalled()
    })
})

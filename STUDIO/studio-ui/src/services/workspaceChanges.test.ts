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
        expect(destinations.sort()).toEqual(['/topic/projects/changed', '/user/topic/workspace/changed'])
        // Whichever topic pings, the caller learns about it the same way.
        vi.mocked(subscribeTopic).mock.calls.forEach(([, onBody]) => onBody('CHANGED'))
        expect(onChange).toHaveBeenCalledTimes(2)
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

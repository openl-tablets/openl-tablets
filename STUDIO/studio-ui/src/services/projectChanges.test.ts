import { beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeProjectChanges } from './projectChanges'
import { subscribeTopic } from './stompTopic'

vi.mock('./stompTopic', () => ({ subscribeTopic: vi.fn() }))

describe('subscribeProjectChanges', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('watches the project\'s pings, the workspace pings and the repository broadcast', () => {
        vi.mocked(subscribeTopic).mockReturnValue({ unsubscribe: vi.fn() })
        const onChange = vi.fn()

        subscribeProjectChanges('design:My Project', onChange)

        const destinations = vi.mocked(subscribeTopic).mock.calls.map(([destination]) => destination)
        expect(destinations.sort()).toEqual([
            '/topic/projects/changed',
            // The project id travels URL-encoded, matching the backend's destination.
            '/user/topic/projects/design%3AMy%20Project/changed',
            // The id-free workspace ping backs the id-keyed one up: the id mutates when the project
            // opens or turns local, and a ping to the new id would miss the old subscription.
            '/user/topic/workspace/changed',
        ])
        vi.mocked(subscribeTopic).mock.calls.forEach(([, onBody]) => onBody(JSON.stringify({ origins: []})))
        expect(onChange).toHaveBeenCalledTimes(3)
    })

    it('hands over the files and the origins the pings name, and nothing for other bodies', () => {
        vi.mocked(subscribeTopic).mockReturnValue({ unsubscribe: vi.fn() })
        const onChange = vi.fn()
        subscribeProjectChanges('p1', onChange)
        const onProjectBody = vi.mocked(subscribeTopic).mock.calls[0]![1]
        const onBroadcastBody = vi.mocked(subscribeTopic).mock.calls[1]![1]

        onProjectBody(JSON.stringify({ files: ['rules/Main.xlsx'], origins: ['tab-1']}))
        // The broadcast never names files, and a malformed body reads as "unknown files".
        onBroadcastBody(JSON.stringify({ origins: ['tab-2']}))
        onProjectBody('not json')

        expect(onChange.mock.calls).toEqual([
            [{ files: ['rules/Main.xlsx'], origins: ['tab-1']}],
            [{ files: [], origins: ['tab-2']}],
            [{ files: [], origins: []}],
        ])
    })

    it('drops every subscription on unsubscribe', () => {
        const subscriptions = [{ unsubscribe: vi.fn() }, { unsubscribe: vi.fn() }, { unsubscribe: vi.fn() }]
        subscriptions.forEach(subscription => vi.mocked(subscribeTopic).mockReturnValueOnce(subscription))

        subscribeProjectChanges('p1', vi.fn()).unsubscribe()

        subscriptions.forEach(subscription => expect(subscription.unsubscribe).toHaveBeenCalled())
    })
})

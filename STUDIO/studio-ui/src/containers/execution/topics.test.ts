import { describe, expect, it } from 'vitest'
import {
    benchmarkStatusTopic,
    runStatusTopic,
    testsTopics,
    traceStatusTopic,
} from './topics'

/**
 * The names below are the ones `ProjectSocketNotificationService` publishes on, and
 * `ProjectSocketNotificationServiceTest` pins them from the other side. Spelled out here in full rather than
 * built, so that a change on either side has to be made on both.
 */
describe('the topics an execution is reported on', () => {
    it('names the status and the results of a test run of a project', () => {
        expect(testsTopics('p1')).toEqual({
            status: '/user/topic/projects/p1/tests/status',
            results: '/user/topic/projects/p1/tests/results',
        })
    })

    it('names the status and the results of a test run of one table', () => {
        expect(testsTopics('p1', 't1')).toEqual({
            status: '/user/topic/projects/p1/tables/t1/tests/status',
            results: '/user/topic/projects/p1/tables/t1/tests/results',
        })
    })

    it('names the status of a run, a benchmark and a trace of one table', () => {
        expect(runStatusTopic('p1', 't1')).toBe('/user/topic/projects/p1/tables/t1/run/status')
        expect(benchmarkStatusTopic('p1', 't1')).toBe('/user/topic/projects/p1/tables/t1/benchmarks/status')
        expect(traceStatusTopic('p1', 't1')).toBe('/user/topic/projects/p1/tables/t1/trace/status')
    })

    it('escapes what a project or a table is called', () => {
        expect(runStatusTopic('a/b', 'c d')).toBe('/user/topic/projects/a%2Fb/tables/c%20d/run/status')
    })
})

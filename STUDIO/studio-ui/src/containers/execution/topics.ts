/**
 * The topics the server reports an execution on.
 *
 * Every screen that listens to an execution names its topic here, so that a name is spelled once and a screen
 * cannot come to listen on a topic nothing is published to. The names are the ones
 * `ProjectSocketNotificationService` publishes on, and its own test pins them from the other side.
 */

/** The topics of a project. */
const projectScope = (projectId: string): string => `/user/topic/projects/${encodeURIComponent(projectId)}`

/** The topics of one table of a project. */
const tableScope = (projectId: string, tableId: string): string =>
    `${projectScope(projectId)}/tables/${encodeURIComponent(tableId)}`

/** Where a table reports the status of a run of it. */
export const runStatusTopic = (projectId: string, tableId: string): string =>
    `${tableScope(projectId, tableId)}/run/status`

/** Where a table reports the status of a benchmark of it. */
export const benchmarkStatusTopic = (projectId: string, tableId: string): string =>
    `${tableScope(projectId, tableId)}/benchmarks/status`

/** Where a table reports the status of a trace of it. */
export const traceStatusTopic = (projectId: string, tableId: string): string =>
    `${tableScope(projectId, tableId)}/trace/status`

/**
 * Where a test run reports itself: its status, and each test table it has finished.
 *
 * The tests of one table report under that table; the tests of a project report under the project.
 */
export const testsTopics = (projectId: string, tableId?: string | null): { status: string, results: string } => {
    const scope = tableId ? tableScope(projectId, tableId) : projectScope(projectId)
    return { status: `${scope}/tests/status`, results: `${scope}/tests/results` }
}

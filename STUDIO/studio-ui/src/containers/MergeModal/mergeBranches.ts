import apiCall, { readTaskResult } from '../../services/apiCall'
import type { ApiCallOptions } from '../../services/apiCall'
import { isFinished, readStatus } from '../../services/executionStatus'
import { subscribeTopic } from '../../services/stompTopic'
import { isStillRunning } from '../../services/taskResult'
import { mergeStatusTopic } from '../execution/topics'
import { QUIET_SPELL_MS } from '../execution/useExecutionProgress'
import type { MergeMode, MergeResultResponse } from './types'

/**
 * The options of every merge request. They leave the projects snapshot alone: a check changes nothing, and a merge
 * commits into a branch the snapshot is told about separately.
 */
export const MERGE_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true, skipWorkspaceEvent: true }

/** Resolves when the merge reports its end, or after a while without a word, whichever comes first. */
interface EndSignal {
    ended: Promise<void>
    cancel(): void
}

/**
 * Merges the branches of a project, and answers with what the merge did.
 *
 * The server validates the merge while the request answers and runs it in the background, so no proxy cuts
 * a long merge off. The merge is watched on its topic from before it starts, so its end cannot go unheard, and
 * its result is read once it has ended. The result is also asked for again after every few seconds without a word,
 * as the screens of runs and tests do: an end reported while the connection was down reaches nobody.
 *
 * @param projectId   the project to merge
 * @param mode        whether the other branch is merged into the current one, or the current one into it
 * @param otherBranch the other branch
 * @param force       confirms the bypass of the protection of the target branch
 * @param onAccepted  called once the server has taken the merge: from then on a cancel finds it
 * @returns the result of the merge: success, the conflicts left to resolve, or cancelled
 * @throws the refusal of the merge, or what the merge failed with
 */
export const mergeBranches = async (
    projectId: string,
    mode: MergeMode,
    otherBranch: string,
    force: boolean,
    onAccepted?: () => void
): Promise<MergeResultResponse> => {
    let signal = endSignal()
    const subscription = subscribeTopic(mergeStatusTopic(projectId), body => {
        if (isFinished(readStatus(body).status ?? null)) {
            signal.cancel()
        }
    })
    try {
        await apiCall(force ? `/projects/${projectId}/merge?force=true` : `/projects/${projectId}/merge`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode, otherBranch }),
        }, MERGE_API_OPTIONS)
        onAccepted?.()
        for (;;) {
            const result = await readResult(projectId)
            if (result) {
                return result
            }
            // One wait at a time: the result is read again once the merge says it has ended, or after a while.
            await signal.ended // NOSONAR typescript:S9382
            signal = endSignal()
        }
    } finally {
        signal.cancel()
        subscription.unsubscribe()
    }
}

/** The result of the merge, or nothing while it is running. */
const readResult = async (projectId: string): Promise<MergeResultResponse | null> => {
    try {
        const response = await readTaskResult(`/projects/${projectId}/merge`, undefined, MERGE_API_OPTIONS)
        return await response.json() as MergeResultResponse
    } catch (error) {
        if (isStillRunning(error)) {
            return null
        }
        throw error
    }
}

/**
 * A wait that ends when the merge reports its end or after {@link QUIET_SPELL_MS}. It is made before the result is
 * read, so an end reported while the result is being read is not missed.
 */
const endSignal = (): EndSignal => {
    let end: () => void = () => undefined
    let timer: ReturnType<typeof setTimeout> | undefined
    const ended = new Promise<void>(resolve => {
        end = resolve
        timer = setTimeout(resolve, QUIET_SPELL_MS)
    })
    return {
        ended,
        cancel: () => {
            clearTimeout(timer)
            end()
        },
    }
}

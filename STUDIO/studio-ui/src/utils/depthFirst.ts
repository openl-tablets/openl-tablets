/** Queues steps of a depth-first walk: they run before anything queued earlier, in the order given. */
export type Later = (...steps: Array<() => void>) => void

/**
 * Walks a tree depth first without using the call stack, so a tree thousands of levels deep is walked as safely as
 * a shallow one.
 *
 * A step that would call itself for the children of a node queues them with `later` instead. The queued steps run
 * before the steps queued earlier, in the order they were given, which is the order a recursive walk visits them.
 * A step queued after the children runs once all of them are done, where a recursive walk would return.
 */
export const walkDepthFirst = (first: (later: Later) => void): void => {
    const pending: Array<() => void> = []
    const later: Later = (...steps) => {
        for (let i = steps.length - 1; i >= 0; i--) {
            pending.push(steps[i] as () => void)
        }
    }
    first(later)
    for (let step = pending.pop(); step; step = pending.pop()) {
        step()
    }
}

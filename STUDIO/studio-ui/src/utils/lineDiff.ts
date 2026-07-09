export type DiffKind = 'context' | 'add' | 'remove'

export interface DiffLine {
    kind: DiffKind
    /** Line number in the old text (null for added lines). */
    oldNumber: number | null
    /** Line number in the new text (null for removed lines). */
    newNumber: number | null
    text: string
}

export const MAX_DIFF_LINES = 4_000
export const MAX_DIFF_MATRIX_CELLS = 2_000_000

export class DiffInputTooLargeError extends Error {
    constructor() {
        super('Line diff input is too large')
        this.name = 'DiffInputTooLargeError'
    }
}

/**
 * Compute a line-level diff between two texts using a longest-common-subsequence backtrace.
 *
 * Unchanged lines are emitted as `context`, lines only in the old text as `remove`, and lines only in
 * the new text as `add`. The result preserves reading order.
 */
export function diffLines(oldText: string, newText: string): DiffLine[] {
    const a = oldText.length === 0 ? [] : oldText.split('\n')
    const b = newText.length === 0 ? [] : newText.split('\n')
    const n = a.length
    const m = b.length
    if (n + m > MAX_DIFF_LINES || n * m > MAX_DIFF_MATRIX_CELLS) {
        throw new DiffInputTooLargeError()
    }

    // lcs[i][j] = length of the LCS of a[i:] and b[j:]
    const lcs: number[][] = Array.from({ length: n + 1 }, () => new Array<number>(m + 1).fill(0))
    for (let i = n - 1; i >= 0; i--) {
        for (let j = m - 1; j >= 0; j--) {
            lcs[i]![j] = a[i] === b[j] ? lcs[i + 1]![j + 1]! + 1 : Math.max(lcs[i + 1]![j]!, lcs[i]![j + 1]!)
        }
    }

    const result: DiffLine[] = []
    let i = 0
    let j = 0
    while (i < n && j < m) {
        if (a[i] === b[j]) {
            result.push({ kind: 'context', oldNumber: i + 1, newNumber: j + 1, text: a[i]! })
            i++
            j++
        } else if (lcs[i + 1]![j]! >= lcs[i]![j + 1]!) {
            result.push({ kind: 'remove', oldNumber: i + 1, newNumber: null, text: a[i]! })
            i++
        } else {
            result.push({ kind: 'add', oldNumber: null, newNumber: j + 1, text: b[j]! })
            j++
        }
    }
    while (i < n) {
        result.push({ kind: 'remove', oldNumber: i + 1, newNumber: null, text: a[i]! })
        i++
    }
    while (j < m) {
        result.push({ kind: 'add', oldNumber: null, newNumber: j + 1, text: b[j]! })
        j++
    }
    return result
}

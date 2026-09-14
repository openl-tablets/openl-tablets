import { useCallback, useRef } from 'react'

/** Reads one case of a test table, with every value it holds. */
export type ReadTestCase<T> = (tableId: string, caseId: string) => Promise<T>

/**
 * Reads a test case only when the screen asks for one of its values.
 *
 * A case is read once and kept, so asking for a second value of the same case costs nothing and reveals only
 * the value that was asked for. A read that failed is forgotten, so a case of a project that was still
 * compiling can be read again.
 *
 * What was read belongs to the reader that read it: a reader that looks elsewhere, such as within one module
 * instead of the whole project, starts with nothing kept.
 *
 * @param readCase reads a whole case
 * @return the case, read at most once
 */
export const useTestCase = <T, >(readCase: ReadTestCase<T>): ReadTestCase<T> => {
    const reading = useRef<{ readCase: ReadTestCase<T>, cases: Record<string, Promise<T>> }>({ readCase, cases: {} })

    if (reading.current.readCase !== readCase) {
        reading.current = { readCase, cases: {} }
    }

    return useCallback((tableId, caseId) => {
        const { cases } = reading.current
        const key = `${tableId} ${caseId}`
        const read = cases[key] ?? readCase(tableId, caseId).catch(error => {
            delete cases[key]
            throw error
        })
        cases[key] = read
        return read
    }, [readCase])
}

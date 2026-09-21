/**
 * Names the cases kept out of a table's cases, the way a range of ids does.
 *
 * Cases kept one after another are named by their two ends, `first - last`, and a case kept on its own by its
 * id, so a long table with a few cases left out is asked for in a few words rather than an id per case. The
 * ends are set apart by a spaced dash, which an id with a dash of its own does not confuse.
 */
export const testRangesOf = (ids: string[], kept: (id: string) => boolean): string => {
    const ranges: string[] = []
    let run: string[] = []
    const closeRun = () => {
        if (run.length === 1) {
            ranges.push(run[0] as string)
        } else if (run.length > 1) {
            ranges.push(`${run[0]} - ${run.at(-1)}`)
        }
        run = []
    }
    for (const id of ids) {
        if (kept(id)) {
            run.push(id)
        } else {
            closeRun()
        }
    }
    closeRun()
    return ranges.join(',')
}

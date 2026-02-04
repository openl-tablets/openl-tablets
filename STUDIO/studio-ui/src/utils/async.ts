/**
 * Runs async operations sequentially; collects errors and continues.
 * Use when you need to attempt all operations and report failures at the end
 * (e.g. batch save with error accumulation).
 *
 * @returns List of failures: { item, error } for each operation that threw.
 */
export async function runSequentialCollectErrors<T>(
    items: T[],
    run: (item: T) => Promise<void>
): Promise<Array<{ item: T; error: unknown }>> {
    const failures: Array<{ item: T; error: unknown }> = []
    for (const item of items) {
        try {
            await run(item)
        } catch (e) {
            failures.push({ item, error: e })
        }
    }
    return failures
}

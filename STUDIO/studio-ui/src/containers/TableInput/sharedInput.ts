/**
 * The input the reader last gave a table, shared by its Run, Trace and Benchmark panels: what was entered to run
 * the table is there when it is traced or measured next. The input of one table is kept at a time, so opening
 * another table starts it afresh.
 *
 * @author Yury Molchan
 */
interface Remembered {
    /** The project and the table the input was given to. */
    table: string
    /** What the table said its parameters are; the input fits only the same parameters. */
    described: string
    value: Record<string, unknown>
}

let remembered: Remembered | null = null

/** The input last given to the table, while it takes the same parameters. */
export const rememberedInput = (table: string, described: string): Record<string, unknown> | undefined =>
    remembered?.table === table && remembered.described === described ? remembered.value : undefined

/** Keeps the input given to a table for the next panel opened on it. */
export const rememberInput = (table: string, described: string, value: Record<string, unknown>): void => {
    remembered = { table, described, value }
}

/** Forgets every input, as a new page does. */
export const forgetInputs = (): void => {
    remembered = null
}

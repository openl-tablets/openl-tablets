/** The families of table written against another table: they list cases to run it over, not rules of their own. */
export const EXERCISING_KINDS = ['Test', 'Run'] as const

/** Whether a table of the kind carries cases of its own to run, rather than taking an input. */
export const carriesCases = (kind: string): boolean => (EXERCISING_KINDS as readonly string[]).includes(kind)

/** Whether the table is a Run table: cases without expected values, which are run rather than tested. */
export const isRunTable = (kind: string): boolean => kind === 'Run'

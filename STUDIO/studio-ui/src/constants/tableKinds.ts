/** The families of table written against another table: they list cases to run it over, not rules of their own. */
export const EXERCISING_KINDS = ['Test', 'Run'] as const

/** Whether a table of the kind carries cases of its own to run, rather than taking an input. */
export const carriesCases = (kind: string): boolean => (EXERCISING_KINDS as readonly string[]).includes(kind)

/** Whether the table is a Run table: cases without expected values, which are run rather than tested. */
export const isRunTable = (kind: string): boolean => kind === 'Run'

/** The families of table that hold records, a record to a row of a Markdown file, which merges none of their cells. */
export const RECORD_KINDS = ['Data', 'Test', 'Run'] as const

/** Whether a table of the kind holds records: a Data, a Test or a Run table. */
export const holdsRecords = (kind: string): boolean => (RECORD_KINDS as readonly string[]).includes(kind)

import type { MessageQuery } from '../../services/localization'

/** What the reader narrows the table of the keys down to, and how the keys are ordered, through its columns. */
export type MessageFilter = MessageQuery

export const NO_FILTER: MessageFilter = { search: '', onlyProblems: false, notTranslatedInto: [], order: null }

/** Whether the filter leaves out any key: an order alone does not. */
export const narrows = (filter: MessageFilter): boolean =>
    filter.search !== '' || filter.onlyProblems || filter.notTranslatedInto.length > 0

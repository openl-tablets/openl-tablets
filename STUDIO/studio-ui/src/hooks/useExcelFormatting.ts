import { excelFormattingOf, useUserStore } from '../store/userStore'

/**
 * Whether the reader asks to see the tables in the formatting of their Excel files (**Show Original Excel Formatting**
 * in My Settings). Otherwise every table is shown formatted with the table theme, in the colours of the theme of
 * OpenL Studio.
 *
 * @returns whether the tables keep the formatting of their Excel files
 */
export const useExcelFormatting = (): boolean => useUserStore(excelFormattingOf)

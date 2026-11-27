import { useContext } from 'react'
import { SystemContext } from '../contexts/SystemContext'
import { useSettingsStore } from '../store/settingsStore'

/**
 * Whether every table saved is formatted with the OpenL table style, as **Format tables on save** in the system
 * settings says.
 *
 * The setting is on unless the server says it is off, as it is by default.
 */
export const useFormatTablesOnSave = (): boolean =>
    useContext(SystemContext).systemSettings?.formatTablesOnSave !== false

/**
 * Whether the tables are drawn in the formatting of their Excel files rather than in the colours of the theme.
 *
 * It is the **Excel Formatting** choice of **My Settings**, which counts only while **Format tables on save** is off:
 * a save that formats the table writes the OpenL table style over the formatting of the workbook, so the menu offers
 * no choice then and the tables are drawn in the colours of the theme.
 */
export const useExcelFormatting = (): boolean => {
    const chosen = useSettingsStore(state => state.settings.excelFormatting)
    const formattedOnSave = useFormatTablesOnSave()
    return chosen && !formattedOnSave
}

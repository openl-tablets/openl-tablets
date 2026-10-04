import { useEffect, useState } from 'react'
import type { TableThemeOption } from 'types/tables'
import { getTableThemes, getTableThemesOf } from '../services/tables'
import { errorHandler } from '../utils/errorHandling'

/**
 * The table themes OpenL Studio offers, by name.
 *
 * Undefined until the list arrives. Empty when it cannot be read: a screen then offers no theme to choose rather
 * than failing, and the reason is kept for support.
 */
export const useTableThemes = (): TableThemeOption[] | undefined => {
    const [themes, setThemes] = useState<TableThemeOption[] | undefined>(undefined)
    useEffect(() => {
        let current = true
        getTableThemes()
            .then(found => {
                if (current) {
                    setThemes(found)
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    setThemes([])
                }
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)),
                    { message: 'The table themes cannot be read' })
            })
        return () => {
            current = false
        }
    }, [])
    return themes
}

/**
 * The table themes that style a table, by name, asked for once per table while `asked` holds.
 *
 * The themes style some kinds of table only, and the server says whether they suit the table. The answer is kept
 * for the table while it stays open.
 *
 * Undefined until the answer arrives. A table whose themes cannot be read is offered none, and is asked again the
 * next time `asked` holds.
 */
export const useTableThemesOf = (projectId: string, tableId: string, moduleName: string | undefined,
    asked: boolean): TableThemeOption[] | undefined => {
    const [found, setFound] = useState<{ tableId: string, themes: TableThemeOption[] } | null>(null)
    const known = found?.tableId === tableId
    useEffect(() => {
        if (!asked || known) {
            return undefined
        }
        let current = true
        getTableThemesOf(projectId, tableId, moduleName)
            .then(themes => {
                if (current) {
                    setFound({ tableId, themes })
                }
            })
            .catch((error: unknown) => errorHandler.logError(
                error instanceof Error ? error : new Error(String(error)),
                { message: 'The table themes of the table cannot be read' }
            ))
        return () => {
            current = false
        }
    }, [asked, known, moduleName, projectId, tableId])
    return found?.tableId === tableId ? found.themes : undefined
}

/** The themes as the options of a select: each by its identifier, shown by its name. */
export const toThemeOptions = (themes: TableThemeOption[] | undefined): { value: string, label: string }[] =>
    (themes ?? []).map(theme => ({ value: theme.id, label: theme.name }))

/** The theme the settings name, while Studio offers it; undefined otherwise. */
export const offeredTheme = (themes: TableThemeOption[] | undefined, named: string | undefined)
    : string | undefined => (themes?.some(theme => theme.id === named) ? named : undefined)

/**
 * The theme a reader starts from: the one their settings name, where Studio still offers it, otherwise the first one
 * offered. Undefined while no theme is offered.
 */
export const defaultThemeOf = (themes: TableThemeOption[] | undefined, named: string | undefined)
    : string | undefined => offeredTheme(themes, named) ?? themes?.[0]?.id

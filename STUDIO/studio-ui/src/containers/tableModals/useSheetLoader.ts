import { type Dispatch, type SetStateAction, useCallback, useEffect, useRef, useState } from 'react'
import { notification } from 'antd'
import { getModuleSheets } from 'services/projects'
import { errorMessage } from 'utils/errorMessage'
import type { ModuleOption } from './shared'

export interface SheetLoader {
    /** Worksheets of the module currently chosen; empty for a module the project does not declare yet. */
    sheets: string[]
    sheetName: string
    setSheetName: Dispatch<SetStateAction<string>>
    loading: boolean
    /** Reads a module's worksheets and points the sheet field at one of them. */
    load: (projectId: string, moduleName: string, declared: ModuleOption[]) => Promise<void>
    /** Takes worksheets already in hand — those read while the dialog was opening — without asking again. */
    prime: (moduleName: string, loaded: string[]) => void
}

/**
 * The sheet a table is written to, and the worksheets the chosen module offers for it.
 *
 * <p>A module the project does not declare is one the author is still naming: it has no workbook to read, and the
 * sheet they typed is the sheet its workbook will be created with.
 *
 * <p>Each module's worksheets are read once. They are behind a server-side compilation of the module, and choosing
 * a table type reselects the destination on its own, which would otherwise pay for that compilation on every
 * toggle. Only the newest request may answer: switching modules twice in quick succession must not leave the
 * first module's sheets on screen.
 */
export const useSheetLoader = (errorTitle: string, initialSheetName = ''): SheetLoader => {
    const [sheets, setSheets] = useState<string[]>([])
    const [sheetName, setSheetName] = useState(initialSheetName)
    const [loading, setLoading] = useState(false)
    const token = useRef(0)
    const cache = useRef(new Map<string, string[]>())

    // Closing the dialog is one more answer nobody is waiting for. Without this, a request still in flight when the
    // dialog goes away still reports its failure — a notification over a screen the dialog has left.
    useEffect(() => () => {
        token.current++
    }, [])

    /**
     * The sheet a module's tables go into. A sheet the module does not have would be created in it, so a module
     * that already has sheets offers its first rather than keeping a sheet that belonged to the module before it.
     */
    const selectSheet = useCallback((loaded: string[]) => {
        setSheetName(current => !loaded.length || loaded.includes(current) ? current : loaded[0] ?? current)
    }, [])

    // Priming is itself the answer, so it ends a request still in flight — including the spinner that
    // request put up, which its own reply no longer hides once this call has taken the count over.
    const prime = useCallback((moduleName: string, loaded: string[]) => {
        cache.current.set(moduleName, loaded)
        token.current++
        setLoading(false)
        setSheets(loaded)
        selectSheet(loaded)
    }, [selectSheet])

    const load = useCallback(async (projectId: string, moduleName: string, declared: ModuleOption[]) => {
        // Taken before the undeclared-module branch returns: that branch is itself a new answer, and leaving the
        // count alone would let a request started for the module named before it still land on this one.
        const current = ++token.current
        // Trimmed, exactly as the create request reads it: the raw value would ask the server for a module named
        // with the author's trailing space.
        const normalized = moduleName.trim()
        if (!declared.some(module => module.name === normalized)) {
            setSheets([])
            setLoading(false)
            return
        }
        setLoading(true)
        try {
            const cached = cache.current.get(normalized)
            const loaded = cached ?? await getModuleSheets(projectId, normalized)
            cache.current.set(normalized, loaded)
            if (current === token.current) {
                setSheets(loaded)
                selectSheet(loaded)
            }
        } catch (error) {
            if (current === token.current) {
                // The module could not be read, so it offers nothing: leaving the previous module's sheets up would
                // suggest this one has them.
                setSheets([])
                notification.error({ title: errorTitle, description: errorMessage(error) })
            }
        } finally {
            if (current === token.current) {
                setLoading(false)
            }
        }
    }, [errorTitle, selectSheet])

    return { sheets, sheetName, setSheetName, loading, load, prime }
}

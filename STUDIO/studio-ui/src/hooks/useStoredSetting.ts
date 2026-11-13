import { useCallback, useState } from 'react'
import type { StoredSetting } from '../utils/localStore'

/**
 * A choice of the reader kept in the browser, as the state of a component.
 *
 * The component starts from the choice kept, and every change is shown and kept at once, so the next visit starts
 * from it. A change back to the default forgets it, as {@link StoredSetting} does.
 *
 * @param setting the choice, kept under its key
 * @param initial what the component starts from instead of the choice kept, when its caller decides that; a change
 *                made later is still kept
 */
export const useStoredSetting = <T>(setting: StoredSetting<T>, initial?: T): [T, (next: T) => void] => {
    const [value, setValue] = useState<T>(() => initial ?? setting.read())
    const change = useCallback((next: T) => {
        setting.write(next)
        setValue(next)
    }, [setting])
    return [value, change]
}

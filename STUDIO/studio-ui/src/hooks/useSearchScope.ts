import { useEffect, useMemo, useRef } from 'react'
import { type SearchScope, useSearchStore } from '../store/searchStore'

/**
 * Hands the search field of the header to a part of the screen: what it searches, the text and where a change of
 * it goes, and the control it adds to the field.
 *
 * The part keeps the text itself, as it would for a field of its own, and filters by it. The scope leaves the
 * field when the part leaves the screen, or while it passes `null` — a tab that is not shown, a list with nothing
 * to search.
 */
export const useSearchScope = (scope: SearchScope | null): void => {
    const latest = useRef(scope)
    const id = scope?.id
    // The field calls the handlers of the latest render, whichever render it took its texts from.
    const handlers = useMemo(() => ({
        onChange: (value: string) => latest.current?.onChange(value),
        onClick: (text: string) => latest.current?.action?.onClick?.(text),
        onChoice: (value: string) => latest.current?.action?.onChoice?.(value),
        onEnter: (text: string) => latest.current?.onEnter?.(text),
    }), [])

    useEffect(() => {
        latest.current = scope
        if (!scope) {
            return
        }
        const { action } = scope
        useSearchStore.getState().mount({
            ...scope,
            onChange: handlers.onChange,
            ...(scope.onEnter && { onEnter: handlers.onEnter }),
            ...(action && {
                action: {
                    ...action,
                    ...(action.onClick && { onClick: handlers.onClick }),
                    ...(action.onChoice && { onChoice: handlers.onChoice }),
                },
            }),
        })
    })

    useEffect(() => {
        if (id === undefined) {
            return undefined
        }
        return () => useSearchStore.getState().unmount(id)
    }, [id])
}

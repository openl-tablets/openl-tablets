import { useCallback, useEffect, useState } from 'react'

/**
 * Listens for the dialog-opening event the legacy pages dispatch, and closes the dialog again.
 *
 * A dialog is closed by re-dispatching its own event with no detail, so the same event name is the whole
 * protocol: one side opens with a payload, the other clears it.
 */
export const useGlobalEvents = <T>(eventName: string) => {
    const [detail, setDetail] = useState<T>()

    useEffect(() => {
        const handler = (event: Event) => {
            const customEvent = event as CustomEvent
            setDetail(customEvent.detail)
        }
        window.addEventListener(eventName, handler)

        return () => {
            window.removeEventListener(eventName, handler)
        }
    }, [eventName])

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent(eventName, { detail: null }))
    }, [eventName])

    return { detail, close }
}

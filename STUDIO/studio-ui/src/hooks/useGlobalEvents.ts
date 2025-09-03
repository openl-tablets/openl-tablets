import { useEffect, useState } from 'react'

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

    return { detail }
}
import { useEffect, useState } from 'react'

export const useGlobalEvents = (eventName: string) => {
    const [details, setDetails] = useState({})

    useEffect(() => {
        const handler = (event: Event) => {
            const customEvent = event as CustomEvent
            setDetails(customEvent.detail)
        }
        window.addEventListener(eventName, handler)

        return () => {
            window.removeEventListener(eventName, handler)
        }
    }, [eventName])

    return { details }
}
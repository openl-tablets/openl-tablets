import { useEffect } from 'react'

export const useScript = (src: string | string[] | undefined) => {
    useEffect(() => {
        if (!src) {
            return () => {}
        }
        let scripts: string[] = []
        if (Array.isArray(src)) {
            scripts = src
        } else {
            scripts = [src]
        }
        // Load all scripts sequentially
        for (let script of scripts) {
            const newScript = document.createElement('script')

            newScript.type = 'text/javascript'
            newScript.src = script
            newScript.async = true
            document.body.appendChild(newScript)
        }
        // Cleanup function to remove the script when the component unmounts
        return () => {
            scripts.forEach(scriptSrc => {
                const existingScript = document.querySelector(`script[src="${scriptSrc}"]`)
                if (existingScript) {
                    document.body.removeChild(existingScript)
                }
            })
        }
    }, [src])
}

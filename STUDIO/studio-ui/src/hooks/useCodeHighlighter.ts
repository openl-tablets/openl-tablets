import { useEffect, useState } from 'react'
import { errorHandler } from '../utils/errorHandling'

/** The code highlighter: the grammars and the colours of the code editor, loaded as a chunk of their own. */
export type CodeHighlighter = typeof import('../utils/codeHighlight')

/** The highlighter once loaded, so a screen opened after the first one draws its code highlighted at once. */
let loaded: CodeHighlighter | undefined

const load = async (): Promise<CodeHighlighter> => (loaded ??= await import('../utils/codeHighlight'))

/**
 * The code highlighter, loaded the first time a screen asks for it, so a screen without code never downloads the
 * grammars.
 *
 * Undefined until it arrives, and while nothing asks for it. A highlighter that cannot be loaded stays undefined, so
 * the code shows as plain text, and the reason is kept for support.
 *
 * @param asked whether the screen has code to highlight
 */
export const useCodeHighlighter = (asked = true): CodeHighlighter | undefined => {
    const [highlighter, setHighlighter] = useState(loaded)

    useEffect(() => {
        if (highlighter || !asked) {
            return undefined
        }
        let current = true
        load()
            .then(module => {
                if (current) {
                    setHighlighter(module)
                }
            })
            .catch((error: unknown) => errorHandler.logError(error instanceof Error ? error : new Error(String(error))))
        return () => {
            current = false
        }
    }, [asked, highlighter])

    return highlighter
}

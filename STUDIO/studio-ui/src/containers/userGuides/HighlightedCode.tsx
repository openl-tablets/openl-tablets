import React, { useEffect, useMemo, useState } from 'react'
import { errorHandler } from 'utils/errorHandling'

type Highlighter = typeof import('./codeHighlight')

/** The highlighter once loaded, so a page opened after the first one draws its code highlighted at once. */
let loaded: Highlighter | undefined

const loadHighlighter = async (): Promise<Highlighter> => (loaded ??= await import('./codeHighlight'))

interface HighlightedCodeProps {
    /** The code of the block. */
    code: string
    /** The language the block is written in. */
    language: string
}

/**
 * Draws a code block of a page, highlighted in its language.
 *
 * The grammars are those of the code editor. They are loaded with the first code block a reader opens, so a page
 * without code never downloads them. Until they arrive, and in a language the guides do not use, the code shows as
 * plain text.
 */
export const HighlightedCode: React.FC<HighlightedCodeProps> = ({ code, language }) => {
    const [highlighter, setHighlighter] = useState(loaded)
    const pieces = useMemo(() => highlighter?.highlight(code, language), [highlighter, code, language])

    useEffect(() => {
        if (highlighter) {
            return undefined
        }
        let current = true
        loadHighlighter()
            .then(module => {
                if (current) {
                    setHighlighter(module)
                }
            })
            .catch((error: unknown) => errorHandler.logError(error instanceof Error ? error : new Error(String(error))))
        return () => {
            current = false
        }
    }, [highlighter])

    const tokens = pieces?.map(({ at, text, classes }) => (classes ? <span key={at} className={classes}>{text}</span> : text))
    return (
        <pre>
            <code className={`language-${language}`}>{tokens ?? code}</code>
        </pre>
    )
}

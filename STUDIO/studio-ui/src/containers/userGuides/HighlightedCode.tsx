import React, { type PropsWithChildren, useEffect, useMemo, useState } from 'react'
import { useThemeMode } from 'antd-style'
import { useAppTheme } from 'providers/AppThemeProvider'
import { codeBlockColors } from 'styles/themes'
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
 * A code block of a page, in the background and the text colour of the theme's code editor.
 *
 * The colours are the theme's own, known before the highlighter loads, so a block is drawn in them from the start —
 * a block of plain text as much as one in a language.
 */
export const CodeBlock: React.FC<PropsWithChildren> = ({ children }) => {
    const { themeName } = useAppTheme()
    const { isDarkMode } = useThemeMode()
    return <pre style={codeBlockColors(themeName, isDarkMode)}>{children}</pre>
}

/**
 * Draws a code block of a page, highlighted in its language and in the colours of the theme in force.
 *
 * The grammars and the colours are those of the code editor, so a block looks as the same code does in the editor.
 * They are loaded with the first code block a reader opens, so a page without code never downloads them. Until
 * they arrive, and in a language the guides do not use, the code shows as plain text in the colours of the theme.
 */
export const HighlightedCode: React.FC<HighlightedCodeProps> = ({ code, language }) => {
    const { themeName } = useAppTheme()
    const { isDarkMode } = useThemeMode()
    const [highlighter, setHighlighter] = useState(loaded)
    const pieces = useMemo(
        () => highlighter?.highlight(code, language, themeName, isDarkMode),
        [highlighter, code, language, themeName, isDarkMode]
    )

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

    const tokens = pieces?.map(({ at, text, style }) => (style ? <span key={at} style={style}>{text}</span> : text))
    return (
        <CodeBlock>
            <code className={`language-${language}`}>{tokens ?? code}</code>
        </CodeBlock>
    )
}

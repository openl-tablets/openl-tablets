import { jsonLanguage } from '@codemirror/lang-json'
import { xmlLanguage } from '@codemirror/lang-xml'
import { yamlLanguage } from '@codemirror/lang-yaml'
import { type Language, StreamLanguage } from '@codemirror/language'
import { java } from '@codemirror/legacy-modes/mode/clike'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { properties } from '@codemirror/legacy-modes/mode/properties'
import { shell } from '@codemirror/legacy-modes/mode/shell'
import { classHighlighter, highlightCode } from '@lezer/highlight'

/** A piece of a code block, between two tokens or a token itself. */
export interface CodePiece {
    /** Where the piece starts in the code. */
    at: number
    text: string
    /** The `tok-*` classes naming the kind of token, empty for the text between two tokens. */
    classes: string
}

/** The grammars of the languages the guides use, the ones the code editor highlights with. */
const LANGUAGES = new Map<string, Language>([
    ['bash', StreamLanguage.define(shell)],
    ['groovy', StreamLanguage.define(groovy)],
    ['java', StreamLanguage.define(java)],
    ['json', jsonLanguage],
    ['properties', StreamLanguage.define(properties)],
    ['xml', xmlLanguage],
    ['yaml', yamlLanguage],
])

/**
 * Splits code into the tokens of its language, the line breaks kept.
 *
 * Code in a language the guides do not use is not split: the answer is `undefined`.
 */
export const highlight = (code: string, language: string): CodePiece[] | undefined => {
    const grammar = LANGUAGES.get(language)
    if (!grammar) {
        return undefined
    }
    const pieces: CodePiece[] = []
    let at = 0
    const put = (text: string, classes: string) => {
        pieces.push({ at, text, classes })
        at += text.length
    }
    highlightCode(code, grammar.parser.parse(code), classHighlighter, put, () => put('\n', ''))
    return pieces
}

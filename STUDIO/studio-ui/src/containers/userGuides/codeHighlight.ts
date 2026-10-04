import type { CSSProperties } from 'react'
import { jsonLanguage } from '@codemirror/lang-json'
import { xmlLanguage } from '@codemirror/lang-xml'
import { yamlLanguage } from '@codemirror/lang-yaml'
import { type HighlightStyle, type Language, StreamLanguage, type TagStyle } from '@codemirror/language'
import { java } from '@codemirror/legacy-modes/mode/clike'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { properties } from '@codemirror/legacy-modes/mode/properties'
import { shell } from '@codemirror/legacy-modes/mode/shell'
import { highlightCode, tagHighlighter, type Highlighter } from '@lezer/highlight'
import { codeHighlightStyle } from '../../styles/codeMirrorThemes'
import type { ThemeName } from '../../styles/themes'
import { cached } from '../../utils/cached'

/** A piece of a code block, between two tokens or a token itself. */
export interface CodePiece {
    /** Where the piece starts in the code. */
    at: number
    text: string
    /** How the token is drawn; absent for the text between two tokens and for a token the theme leaves plain. */
    style?: CSSProperties
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

/** The properties a rule sets: what is a style, not the tags it applies to, a class name, or a nested rule. */
const cssOf = (rule: TagStyle): CSSProperties => Object.fromEntries(Object.entries(rule)
    .filter(([key, value]) => key !== 'tag' && key !== 'class' && ['string', 'number'].includes(typeof value)))

/** A highlight style turned from classes into inline styles. */
interface InlineStyles {
    /** Names a token by the numbers of the rules it matches, separated by spaces. */
    highlighter: Highlighter
    /** The inline style of a token matching the given rules. */
    styleOf: (rules: string) => CSSProperties
}

/**
 * Turns a highlight style into inline styles, so code drawn outside the editor takes the editor's colours.
 *
 * A highlight style is a list of rules, each giving a style to the code of some style tags. The editor turns every
 * rule into a generated class and a stylesheet. Here the rules are numbered instead, and the same matching of tags
 * — `tagHighlighter`, which a highlight style is built on — names each token by the numbers of the rules it
 * matches. A token can match more than one: one for each tag its grammar gives it, and one for each enclosing node
 * whose tag reaches the nodes inside it, such as a heading around emphasis. Their styles are laid over each other
 * in the order the rules are written, which is the order the stylesheet applies them in, so where two rules set the
 * same property the later one wins.
 */
const inlineStylesOf = (style: HighlightStyle): InlineStyles => {
    const styles = style.specs.map(cssOf)
    // A block holds many tokens of few kinds, so the style of each set of rules is put together once.
    const merged = new Map<string, CSSProperties>()
    return {
        highlighter: tagHighlighter(style.specs.map((rule, number) => ({ tag: rule.tag, class: String(number) }))),
        styleOf: rules => cached(merged, rules, () => Object.assign({}, ...rules.split(' ').map(Number)
            .sort((a, b) => a - b).map(rule => styles[rule]))),
    }
}

const inlineStyles = new Map<HighlightStyle, InlineStyles>()

/**
 * Splits code into the tokens of its language, each in the colours the theme's code editor gives it, the line
 * breaks kept.
 *
 * The grammar parses the code into a syntax tree whose nodes carry style tags; `highlightCode` walks the tree and
 * hands over the code a piece at a time, each with the rules its tags match, and a line break as a piece of its own.
 * Code in a language the guides do not use is not split: the answer is `undefined`.
 */
export const highlight = (
    code: string,
    language: string,
    themeName: ThemeName,
    isDarkMode: boolean
): CodePiece[] | undefined => {
    const grammar = LANGUAGES.get(language)
    if (!grammar) {
        return undefined
    }
    const style = codeHighlightStyle(themeName, isDarkMode)
    const { highlighter, styleOf } = cached(inlineStyles, style, () => inlineStylesOf(style))
    const pieces: CodePiece[] = []
    let at = 0
    const put = (text: string, rules: string) => {
        pieces.push(rules ? { at, text, style: styleOf(rules) } : { at, text })
        at += text.length
    }
    highlightCode(code, grammar.parser.parse(code), highlighter, put, () => put('\n', ''))
    return pieces
}

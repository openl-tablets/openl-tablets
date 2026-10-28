import type { CSSProperties } from 'react'
import { jsonLanguage } from '@codemirror/lang-json'
import { xmlLanguage } from '@codemirror/lang-xml'
import { yamlLanguage } from '@codemirror/lang-yaml'
import {
    type HighlightStyle, type Language, StreamLanguage, type StreamParser, type StringStream, type TagStyle,
} from '@codemirror/language'
import { clike, java } from '@codemirror/legacy-modes/mode/clike'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { properties } from '@codemirror/legacy-modes/mode/properties'
import { shell } from '@codemirror/legacy-modes/mode/shell'
import { highlightCode, tagHighlighter, type Highlighter } from '@lezer/highlight'
import { codeHighlightStyle } from '../styles/codeMirrorThemes'
import type { ThemeName } from '../styles/themes'
import { cached } from './cached'

/** A piece of a code block, between two tokens or a token itself. */
export interface CodePiece {
    /** Where the piece starts in the code. */
    at: number
    text: string
    /** How the token is drawn; absent for the text between two tokens and for a token the theme leaves plain. */
    style?: CSSProperties
}

/** A set of words, as the grammars of `clike` take them. */
const words = (text: string): Record<string, boolean> => Object.fromEntries(text.split(' ').map(word => [word, true]))

/** A pattern matching any of the given ones at the start of the text, and no further than a whole word. */
const anyOf = (...patterns: RegExp[]): RegExp =>
    new RegExp(String.raw`^(?:${patterns.map(pattern => pattern.source).join('|')})(?![\w$])`)

/**
 * A number as OpenL writes one: a date (`2026-10-15`), a time (`12:30`), an amount in thousands (`1,000.50`), a
 * percentage (`10%`), a number of thousands, millions or billions (`1K`, `2.5M`, `3B`), and a number as Java writes it.
 * Each is one token, so it is coloured whole, its separators included.
 */
const OPENL_NUMBER = anyOf(
    /\d{4}-\d{2}-\d{2}/,
    /\d{2}:\d{2}(?::\d{2}(?:\.\d{3})?)?/,
    /0[xX][\da-fA-F]+/,
    /\d{1,3}(?:,\d{3})+(?:\.\d+)?(?:[eE][-+]?\d+)?(?:%|[KMB]|[lLfFdD])?/,
    /\d+(?:\.\d+)?(?:[eE][-+]?\d+)?(?:%|[KMB]|[lLfFdD])?/,
    /\.\d+(?:[eE][-+]?\d+)?%?/
)

/**
 * The operators OpenL writes in words of more than one, such as `is less than` or `select all having`. A grammar of
 * single words cannot tell them, so they are matched before the word they start with.
 */
const OPENL_PHRASES = anyOf(
    /select\s+(?:all|first)\s+(?:having|where)/,
    /order\s+(?:(?:increasing|decreasing)\s+)?by/,
    /split\s+by/,
    /transform\s+(?:unique\s+)?to/,
    /equals\s+to/,
    /is\s+(?:the\s+)?same\s+as/,
    /does\s+not\s+equal\s+to/,
    /is\s+different\s+from/,
    /is\s+(?:no\s+)?(?:less|more)\s+than/,
    /is\s+(?:less|more)\s+or\s+equal/,
    /is\s+in/,
    /(?:less|more)\s+than/,
    /or\s+less/,
    /and\s+more/,
    /of\s+the/
)

/** The operations of a TBasic table, which the compiler reads in any case. */
const TBASIC_OPERATIONS = /^(?:IF|ELSE|END|WHILE|FOR|EACH|SUB|FUNCTION|RETURN|BREAK|CONTINUE|GOTO|SET|VAR)(?![\w$])/i

/**
 * The grammar of an OpenL expression: the Java grammar of the code editor, with the words OpenL keeps of Java, its
 * word operators and its numbers. A `$` is a part of a name, such as `$Premium`, and never starts a number. The dots
 * between the bounds of a range (`18..25`) are an operator, not the start of a number.
 */
const openlGrammar = clike({
    name: 'openl',
    keywords: words('break case const continue default do else for goto if instanceof new return super throw while'
        + ' and or not where'),
    types: words('void boolean byte char double float int long short Boolean Byte Character Date Double Float Integer'
        + ' Long Object Short String BigDecimal BigInteger SpreadsheetResult'),
    atoms: words('true false null'),
    number: OPENL_NUMBER,
    hooks: {
        '.': (stream: StringStream) => (stream.match(/^\.\.?/) ? 'operator' : false),
    },
})

/** A grammar that reads the given words as keywords before it reads anything else. */
const withKeywords = (grammar: StreamParser<unknown>, name: string, ...keywords: RegExp[]): StreamParser<unknown> => ({
    ...grammar,
    name,
    token: (stream, state) => (keywords.some(pattern => stream.match(pattern)) ? 'keyword' : grammar.token(stream, state)),
})

const openl = withKeywords(openlGrammar, 'openl', OPENL_PHRASES)

/** The grammar of a TBasic table: an OpenL expression, and the operations of the table. */
const tbasic = withKeywords(openlGrammar, 'tbasic', OPENL_PHRASES, TBASIC_OPERATIONS)

/**
 * The grammars of the languages the guides use, the ones the code editor highlights with, and of the code of OpenL
 * tables: `openl` for an expression, `tbasic` for the steps of a TBasic table.
 */
const LANGUAGES = new Map<string, Language>([
    ['bash', StreamLanguage.define(shell)],
    ['groovy', StreamLanguage.define(groovy)],
    ['java', StreamLanguage.define(java)],
    ['json', jsonLanguage],
    ['openl', StreamLanguage.define(openl)],
    ['properties', StreamLanguage.define(properties)],
    ['tbasic', StreamLanguage.define(tbasic)],
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

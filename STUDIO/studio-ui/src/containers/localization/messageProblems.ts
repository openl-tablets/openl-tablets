import { cellOf, type Bundle, type BundleLanguage } from './bundle'

/**
 * What can be wrong with the text of a key in a language.
 *
 * - `duplicate` — the file writes the key more than once, and the last one silently wins.
 * - `placeholders` — the text has other placeholders than the text of the default language.
 * - `apostrophe` — an apostrophe is left open: once values are passed, the rest of the text is printed as it is.
 * - `brace` — a brace is left open, or a placeholder is not a number: the message cannot take values.
 * - `untranslated` — the language has no text of its own, nor its parent language, so it shows the default text.
 * - `no_separator` — the line of the key has no equals sign or colon, so the key has no text at all and a rule
 *   asking for it gets the key itself, whatever other languages write.
 * - `no_default` — the default language has no text, so every language without its own returns the key itself.
 */
export type MessageProblem =
    | 'duplicate'
    | 'placeholders'
    | 'apostrophe'
    | 'brace'
    | 'untranslated'
    | 'no_separator'
    | 'no_default'

/** The placeholders a text takes values into, and whether its pattern is broken. */
interface Pattern {
    /** The numbers of the placeholders, `{0}` and `{1,number}` giving 0 and 1. */
    indices: Set<number>
    error: 'apostrophe' | 'brace' | null
}

/**
 * Reads a text as `java.text.MessageFormat` reads its pattern.
 *
 * <p>A pair of apostrophes stands for one; a single one quotes the text up to the next one, braces included. A
 * placeholder is a number in braces, followed by a type and a style that may hold braces of their own.
 */
export const patternOf = (text: string): Pattern => {
    const indices = new Set<number>()
    let quoted = false
    let depth = 0
    let argument = ''
    let error: Pattern['error'] = null
    for (let i = 0; i < text.length; i++) {
        const ch = text.charAt(i)
        if (ch === '\'' && text.charAt(i + 1) === '\'') {
            i++
            argument += depth > 0 ? '\'' : ''
        } else if (ch === '\'') {
            quoted = !quoted
        } else if (quoted) {
            argument += depth > 0 ? ch : ''
        } else if (ch === '{') {
            depth++
            argument += depth > 1 ? ch : ''
        } else if (ch === '}' && depth > 0) {
            depth--
            if (depth === 0) {
                const index = argument.split(',')[0]?.trim() ?? ''
                if (/^\d+$/.test(index)) {
                    indices.add(Number(index))
                } else {
                    error = 'brace'
                }
                argument = ''
            } else {
                argument += ch
            }
        } else if (depth > 0) {
            argument += ch
        }
    }
    if (depth > 0) {
        error = 'brace'
    } else if (quoted && error === null) {
        error = 'apostrophe'
    }
    return { indices, error }
}

const sameIndices = (a: Set<number>, b: Set<number>) => a.size === b.size && [...a].every(index => b.has(index))

/** How many times the file of a language writes a key. */
const writtenTimes = (language: BundleLanguage, key: string) =>
    language.entries.reduce((count, entry) => count + (entry.key === key ? 1 : 0), 0)

/**
 * What is wrong with the text of a key in a language, as it stands with the edits of the reader.
 *
 * <p>The text the reader is still typing is checked as well: `textOf` answers it for the cell being written.
 */
export const problemsOf = (
    bundle: Bundle,
    language: BundleLanguage,
    key: string,
    textOf: (language: BundleLanguage, key: string) => string | null | undefined
): MessageProblem[] => {
    const problems: MessageProblem[] = []
    const own = textOf(language, key)
    if (own !== undefined && writtenTimes(language, key) > 1) {
        problems.push('duplicate')
    }
    if (own === null) {
        problems.push('no_separator')
        return problems
    }
    if (own !== undefined) {
        const pattern = patternOf(own)
        if (pattern.error !== null) {
            problems.push(pattern.error)
        }
        const defaultLanguage = bundle.languages.find(candidate => candidate.tag === '')
        const defaultText = defaultLanguage === undefined || language === defaultLanguage
            ? undefined
            : textOf(defaultLanguage, key) ?? undefined
        if (defaultText !== undefined && !sameIndices(pattern.indices, patternOf(defaultText).indices)) {
            problems.push('placeholders')
        }
        return problems
    }
    const cell = cellOf(bundle, language, key, textOf)
    if (language.tag === '') {
        problems.push('no_default')
    } else if (cell.kind === 'inherited' && cell.from === '') {
        problems.push('untranslated')
    }
    return problems
}

/** A message that cannot take values is an error; everything else only warns. */
export const isError = (problem: MessageProblem): boolean => problem === 'brace'

/** The problems of one key over all its languages. */
export interface KeyProblems {
    count: number
    /** Whether any of them is an error. */
    error: boolean
}

/** The problems of all the keys, counted by key and by language. */
export interface ProblemIndex {
    byKey: Map<string, KeyProblems>
    /** How many problems each language has, by its tag. */
    byLanguage: Map<string, number>
}

/** Counts the problems of every key in every language, as they stand with the edits of the reader. */
export const indexProblems = (
    bundle: Bundle,
    keys: readonly string[],
    textOf: (language: BundleLanguage, key: string) => string | null | undefined
): ProblemIndex => {
    const byKey = new Map<string, KeyProblems>()
    const byLanguage = new Map<string, number>()
    keys.forEach(key => bundle.languages.forEach(language => {
        const problems = problemsOf(bundle, language, key, textOf)
        if (problems.length > 0) {
            const known = byKey.get(key) ?? { count: 0, error: false }
            byKey.set(key, { count: known.count + problems.length, error: known.error || problems.some(isError) })
            byLanguage.set(language.tag, (byLanguage.get(language.tag) ?? 0) + problems.length)
        }
    }))
    return { byKey, byLanguage }
}

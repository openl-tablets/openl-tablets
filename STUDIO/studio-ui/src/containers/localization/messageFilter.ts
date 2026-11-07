import { cellOf, showsDefaultText, type Bundle, type BundleLanguage, type TextOf } from './bundle'
import type { ProblemIndex } from './messageProblems'

/** What the reader narrows the keys down to. */
export interface MessageFilter {
    /** A piece of a key or of a text in any language, in any case. */
    search: string
    /** Only the keys with a problem in some language. */
    onlyProblems: boolean
    /** Only the keys a language shows the default text for, as none of its own nor of a parent; null for any. */
    notTranslatedInto: string | null
}

export const NO_FILTER: MessageFilter = { search: '', onlyProblems: false, notTranslatedInto: null }

/**
 * Whether a language shows the default text of a key, having no text of its own or of a parent language. A key
 * the default language lacks too is not untranslated: it has no text to translate.
 */
const notTranslated = (bundle: Bundle, language: BundleLanguage, key: string, textOf: TextOf) =>
    showsDefaultText(cellOf(bundle, language, key, textOf))

/** Whether a key or one of its texts holds what the reader looks for. */
const matches = (bundle: Bundle, key: string, wanted: string, textOf: TextOf) =>
    key.toLowerCase().includes(wanted)
    || bundle.languages.some(language => textOf(language, key)?.toLowerCase().includes(wanted) === true)

/** The keys the filter leaves, in the order they come in. */
export const filterKeys = (
    bundle: Bundle,
    keys: string[],
    textOf: TextOf,
    problems: ProblemIndex,
    filter: MessageFilter
): string[] => {
    const wanted = filter.search.trim().toLowerCase()
    const language = bundle.languages.find(candidate => candidate.tag === filter.notTranslatedInto)
    return keys.filter(key => (wanted === '' || matches(bundle, key, wanted, textOf))
        && (!filter.onlyProblems || problems.byKey.has(key))
        && (language === undefined || notTranslated(bundle, language, key, textOf)))
}

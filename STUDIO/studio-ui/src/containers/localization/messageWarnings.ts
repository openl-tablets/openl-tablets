import type { TextSpot } from '../../services/projectStatus'
import type { MessageRowWarning } from '../../types/localization'

/** What the check of the files warns about the text of a key in a language. */
export interface MessageWarning {
    summary: string
    /** The pieces of the text the warning is about; none for the text as a whole. */
    spots: readonly TextSpot[]
}

const NONE: readonly MessageWarning[] = []

/**
 * What tells each warning apart from the others of a text, in their order: its words and the pieces of the text it is
 * about, and how many alike came before it, as two pieces of a text may be warned about in the same words.
 */
export const warningKeys = (warnings: readonly MessageWarning[]): string[] => {
    const seen = new Map<string, number>()
    return warnings.map(({ summary, spots }) => {
        const told = [summary, ...spots.map(({ start, end }) => `${start}-${end}`)].join('|')
        const before = seen.get(told) ?? 0
        seen.set(told, before + 1)
        return `${told}#${before}`
    })
}

/** The warnings of a key about its text in a language. */
export const warningsAt = (warnings: readonly MessageRowWarning[] | undefined, tag: string): readonly MessageWarning[] => {
    const found = (warnings ?? []).filter(warning => warning.language === tag)
    return found.length === 0 ? NONE : found.map(warning => ({ summary: warning.summary, spots: warning.spots ?? []}))
}

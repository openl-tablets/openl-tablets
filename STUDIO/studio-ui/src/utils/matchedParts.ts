/** A piece of a text, and whether it is what a search matched. */
export interface MatchedPart {
    text: string
    match: boolean
}

const escaped = (term: string): string => term.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`)

/**
 * The pattern that finds any of the given terms as they are written, whatever the case of their letters; the longest
 * first, so a term holding another is found whole. None for no terms.
 */
export const termsPattern = (terms: readonly string[]): RegExp | undefined => (terms.length === 0
    ? undefined
    : new RegExp([...terms].map(escaped).sort((a, b) => b.length - a.length).join('|'), 'gi'))

/**
 * The text in the pieces between the matches of the pattern and the matches themselves, in order. The pieces join back
 * into the text; an empty piece between two matches, or at an end, is kept, and a text without a pattern is one piece.
 */
export const matchedParts = (text: string, pattern: RegExp | undefined): MatchedPart[] => {
    const parts: MatchedPart[] = []
    let from = 0
    for (const found of pattern ? text.matchAll(pattern) : []) {
        parts.push({ text: text.slice(from, found.index), match: false }, { text: found[0], match: true })
        from = found.index + found[0].length
    }
    parts.push({ text: text.slice(from), match: false })
    return parts
}

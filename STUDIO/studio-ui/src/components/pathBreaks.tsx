import React from 'react'

/**
 * A dot that joins two parts of a path: it follows a name, an index or a call, and a name of two characters or more
 * follows it.
 *
 * A dot between digits, as in `1105.25`, a dot followed by a space or ending the text, and a dot of an abbreviation,
 * as in `e.g.`, join nothing.
 */
const JOINING_DOT = /(?<=[\p{L}\p{M}\p{N}_$)\]])\.(?=[\p{L}_$][\p{L}\p{M}\p{N}_$])/uy

/** Whether the dot at the given place of the text joins two parts of a path. */
const joinsParts = (text: string, dot: number): boolean => {
    JOINING_DOT.lastIndex = dot
    return JOINING_DOT.test(text)
}

/**
 * A text, or a piece of it, that may break a line before each dot joining two parts of a path.
 *
 * A field path, such as `_res_.quoteCalculations[0].coverageDetail`, wraps between its parts, before a dot, rather
 * than inside a name. An index stays with its name. A space breaks a line as before. A number, such as `1105.25`, an
 * abbreviation, such as `e.g.`, and a dot that ends a sentence are not broken.
 *
 * A dot is judged by the whole text, so a piece breaks where the whole text would. The places add no character to the
 * text: it is copied, searched and read as written. A name wider than the room on its own still breaks inside where
 * the text is styled to, and runs past it where it is not.
 *
 * @param text the whole text
 * @param start where the piece begins, the beginning of the text by default
 * @param end where the piece ends, the end of the text by default
 */
export const withPathBreaks = (text: string, start = 0, end = text.length): React.ReactNode => {
    const piece = text.slice(start, end)
    const pieces: React.ReactNode[] = []
    let from = start
    for (let at = piece.indexOf('.'); at !== -1; at = piece.indexOf('.', at + 1)) {
        const dot = start + at
        if (joinsParts(text, dot)) {
            pieces.push(text.slice(from, dot), <wbr key={dot} />)
            from = dot
        }
    }
    if (pieces.length === 0) {
        return piece
    }
    pieces.push(text.slice(from, end))
    return pieces
}

import type { ConflictChoice, ConflictDecision, ConflictedTextBlock, ConflictTextBlock } from './types'

/** A choice that takes lines of the versions of a conflict, rather than lines the user wrote. */
type VersionChoice = Exclude<ConflictChoice, 'edited'>

/** A version a conflict can take the lines of into the result: yours or theirs. */
export type Side = 'ours' | 'theirs'

/**
 * What taking the lines of a version makes of a choice: the lines of that version, or, after the lines of the other
 * version alone, both versions in the order they were taken.
 */
export const takeSide = (choice: ConflictChoice | undefined, side: Side): VersionChoice => {
    if (choice === 'ours' && side === 'theirs') {
        return 'ours-theirs'
    }
    return choice === 'theirs' && side === 'ours' ? 'theirs-ours' : side
}

/** Whether a choice already holds the lines of a version. */
export const holdsSide = (choice: ConflictChoice | undefined, side: Side): boolean =>
    choice === side || choice === 'ours-theirs' || choice === 'theirs-ours'

/** How many lines a text holds: a last line without a line break counts as well. */
export const lineCount = (text: string): number => {
    const lineBreaks = text.split('\n').length - 1
    return text.endsWith('\n') || !text ? lineBreaks : lineBreaks + 1
}

/** The texts of a part of a file: its lines, or every version of a conflict. */
const textsOf = (block: ConflictTextBlock): string[] =>
    block.kind === 'merged' ? [block.text] : [block.ours, block.base, block.theirs]

/** How many times texts hold a line break. */
const lineBreakCount = (texts: string[], lineBreak: string): number =>
    texts.reduce((count, text) => count + text.split(lineBreak).length - 1, 0)

/**
 * The line break a file is written with: the one of Windows when it ends most lines of the versions, a line feed
 * alone otherwise.
 *
 * A few lines of one kind among many of the other do not change how the lines written into the file end.
 */
export const lineBreakOf = (blocks: ConflictTextBlock[]): string => {
    const texts = blocks.flatMap(textsOf)
    const windows = lineBreakCount(texts, '\r\n')
    return windows > lineBreakCount(texts, '\n') - windows ? '\r\n' : '\n'
}

/** Puts lines after other lines, ending the first ones with a line break when they end without one. */
export const joinLines = (first: string, second: string, lineBreak: string): string =>
    first && second && !first.endsWith('\n') ? first + lineBreak + second : first + second

/** Whether the lines written in place of a conflict end with a line break: they do when a version of it does. */
const endsWithLineBreak = (block: ConflictedTextBlock): boolean =>
    [block.ours, block.theirs].some(lines => lines.endsWith('\n'))

/** Lines as an editor shows them: the line break that ends the last line opens no line of its own. */
export const withoutLastLineBreak = (lines: string): string => lines.replace(/\r?\n$/, '')

/**
 * The lines of a conflict as the user edits them. The line break that ends them is left out when the versions of the
 * conflict end with one, since the lines the user writes get it back.
 */
export const shownLines = (lines: string, block: ConflictedTextBlock): string =>
    endsWithLineBreak(block) ? withoutLastLineBreak(lines) : lines

/**
 * The lines the user wrote in place of a conflict, as an editor writes them, with the line breaks of Unix, put in the
 * line breaks of the file.
 *
 * They end with a line break when the versions of the conflict do, so the file keeps the line break it ends with.
 * Nothing written gives no lines.
 */
export const writtenLines = (written: string, block: ConflictedTextBlock, lineBreak: string): string => {
    const lines = written.replaceAll('\n', lineBreak)
    return lines && endsWithLineBreak(block) ? lines + lineBreak : lines
}

/** The lines a choice takes of the versions of a conflict. */
export const linesOf = (block: ConflictedTextBlock, choice: VersionChoice, lineBreak: string): string => {
    switch (choice) {
        case 'ours':
            return block.ours
        case 'theirs':
            return block.theirs
        case 'ours-theirs':
            return joinLines(block.ours, block.theirs, lineBreak)
        case 'theirs-ours':
            return joinLines(block.theirs, block.ours, lineBreak)
        case 'base':
            return block.base
    }
}

/**
 * The text of a file whose conflicts are resolved as decided: the lines merged and the lines of each decision, in
 * their order. Lines followed by more lines end with a line break.
 *
 * @param decisions how each conflict of the file is resolved, in their order
 */
export const mergedTextOf = (
    blocks: ConflictTextBlock[],
    decisions: ConflictDecision[],
    lineBreak: string
): string => {
    let conflict = 0
    const pieces: string[] = []
    // Whether the text put together so far ends with a line break, so the next lines need none before them.
    let ended = true
    for (const block of blocks) {
        const lines = block.kind === 'merged' ? block.text : decisions[conflict++]?.lines ?? ''
        if (lines) {
            pieces.push(ended ? lines : lineBreak + lines)
            ended = lines.endsWith('\n')
        }
    }
    return pieces.join('')
}

/**
 * One message of a localization file: its key, its text, and where it is written in the file.
 *
 * <p>The place is a range of the file's text, from the start of the first line of the message to the end of its
 * last line, the line break included. A message continued over several lines spans all of them.
 */
export interface PropertiesEntry {
    key: string
    /** The text, or null for a line without a separator: the engine reads no text for its key. */
    value: string | null
    start: number
    end: number
}

/**
 * Whether a character is a whitespace as Java tells it (`Character.isWhitespace`), which the engine trims a key and a
 * text by. Unlike `\s`, a no-break space and a byte order mark are no whitespace, while the separators of files,
 * groups, records and units are.
 */
const WHITESPACE = /[\t\n\v\f\r\u001c-\u001f \u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]/

const isWhitespace = (ch: string): boolean => WHITESPACE.test(ch)

/**
 * Reads a localization file the way the rules engine reads it (`PropertiesUtils` of `org.openl.commons`), which
 * differs from `java.util.Properties`.
 *
 * <p>Only an equals sign or a colon separates the key, so spaces stay inside a key. A line without a separator is
 * a key with no text at all, not an empty one: a rule asking for it gets the key itself. Leading spaces of a text are skipped, trailing ones are trimmed. A line starting with `#` or
 * `!` is a comment; a backslash at the end of a line continues the message on the next one.
 *
 * <p>Every message is answered in the order it is written, a key written twice included.
 */
export const parseProperties = (text: string): PropertiesEntry[] => {
    const entries: PropertiesEntry[] = []
    let str = ''
    let lastNonWhitespace = 0
    let key: string | null = null
    let newLine = true
    let skipWhitespaces = true
    let skipLine = false
    let backSlash = false
    let lineStart = 0
    let entryStart = -1

    const takeText = () => {
        const taken = str.slice(0, lastNonWhitespace)
        str = ''
        lastNonWhitespace = 0
        return taken
    }

    const endLine = (end: number) => {
        const value = takeText()
        if (key !== null) {
            entries.push({ key, value, start: entryStart, end })
            key = null
        } else if (value !== '') {
            entries.push({ key: value, value: null, start: entryStart, end })
        }
        newLine = true
        skipWhitespaces = true
        skipLine = false
        entryStart = -1
    }

    const readEscaped = (ch: string, at: number): number => {
        const escapes: Record<string, string> = { t: '\t', n: '\n', r: '\r', f: '\f' }
        if (ch === 'u') {
            const hex = text.slice(at + 1, at + 5)
            str += String.fromCharCode(Number.parseInt(hex, 16))
            lastNonWhitespace = str.length
            return 4
        }
        str += escapes[ch] ?? ch
        lastNonWhitespace = str.length
        return 0
    }

    const readContent = (ch: string, at: number): number => {
        if (backSlash) {
            backSlash = false
            return readEscaped(ch, at)
        }
        if (ch === '\\') {
            backSlash = true
        } else if (key === null && (ch === ':' || ch === '=')) {
            key = takeText()
            skipWhitespaces = true
        } else {
            str += ch
            if (!isWhitespace(ch)) {
                lastNonWhitespace = str.length
            }
        }
        return 0
    }

    for (let i = 0; i < text.length; i++) {
        const ch = text.charAt(i)
        const lineBreak = ch === '\r' || ch === '\n'
        if (lineBreak) {
            const end = ch === '\r' && text.charAt(i + 1) === '\n' ? i + 2 : i + 1
            if (backSlash) {
                // The message continues on the next line, without its leading whitespaces.
                backSlash = false
                skipWhitespaces = true
            } else {
                endLine(end)
            }
            i = end - 1
            lineStart = end
        } else if (newLine && (ch === '#' || ch === '!')) {
            skipLine = true
            newLine = false
        } else if (!skipLine && !(skipWhitespaces && isWhitespace(ch))) {
            if (entryStart < 0) {
                entryStart = lineStart
            }
            newLine = false
            skipWhitespaces = false
            i += readContent(ch, i)
        }
    }
    endLine(text.length)
    return entries
}

/** Escapes a whitespace: a space as `\ `, any other by its code. */
const escapeWhitespace = (ch: string): string =>
    (ch === ' ' ? '\\ ' : `\\u${ch.charCodeAt(0).toString(16).padStart(4, '0')}`)

/**
 * Escapes the whitespaces at the ends of a text, which reading drops: the first one, which would be skipped with
 * the others before it, and every one at the end.
 */
const escapeEnds = (text: string): string => {
    let end = text.length
    while (end > 0 && isWhitespace(text.charAt(end - 1))) {
        end--
    }
    const trailing = [...text.slice(end)].map(escapeWhitespace).join('')
    const body = text.slice(0, end)
    const first = body.charAt(0)
    return (first !== '' && isWhitespace(first) ? escapeWhitespace(first) + body.slice(1) : body) + trailing
}

/** Writes a text so that {@link parseProperties} reads it back as it is. */
const escapeText = (text: string): string => escapeEnds(text
    .replaceAll('\\', '\\\\')
    .replaceAll('\f', '\\f')
    .replaceAll('\t', '\\t')
    .replaceAll('\r', '\\r')
    .replaceAll('\n', '\\n'))

/** Writes a key so that {@link parseProperties} reads it back as it is: a separator in it is escaped. */
const escapeKey = (key: string): string => escapeText(key)
    .replaceAll(':', '\\:')
    .replaceAll('=', '\\=')
    .replace(/^([#!])/, '\\$1')

/** The line break a file is written with, so a line added to it does not mix two kinds. */
const lineBreakOf = (text: string): string => (text.includes('\r\n') ? '\r\n' : '\n')

/** How a file separates a key from its text: with spaces around the equals sign, or without them. */
const separatorOf = (text: string, entries: PropertiesEntry[]): string => {
    const first = entries[0]
    if (first === undefined) {
        return ' = '
    }
    return /^[^=:]*[^\s\\] [=:] /.test(text.slice(first.start, first.end)) ? ' = ' : '='
}

/** The line of one message, ready to be written into a file. */
const messageLine = (key: string, value: string, separator: string, lineBreak: string): string =>
    `${escapeKey(key)}${separator}${escapeText(value)}${lineBreak}`

/** What a range of the text is replaced with. */
interface Replacement {
    start: number
    end: number
    text: string
}

/** What one message of the file turns into under the edits, or nothing when the edits leave it alone. */
const replacementOf = (
    text: string,
    entry: PropertiesEntry,
    edited: string | null | undefined,
    isLast: boolean,
    separator: string
): Replacement | null => {
    if (edited === undefined) {
        return null
    }
    if (edited === null) {
        return { start: entry.start, end: entry.end, text: '' }
    }
    if (!isLast) {
        return null
    }
    const ending = /(\r\n|\r|\n)$/.exec(text.slice(entry.start, entry.end))?.[0] ?? ''
    return { start: entry.start, end: entry.end, text: messageLine(entry.key, edited, separator, ending) }
}

/**
 * Writes the edits into the text of a file, leaving everything else of it as it stands.
 *
 * <p>A changed text replaces the lines of the last message of its key, the one the engine reads. A key the file
 * does not have yet is added at the end of the file. A key edited to `null` is removed with every line of it,
 * written twice or not. Comments, blank lines and the order of the other keys stay byte for byte.
 */
export const applyEdits = (text: string, edits: ReadonlyMap<string, string | null>): string => {
    const entries = parseProperties(text)
    const separator = separatorOf(text, entries)
    const last = new Map<string, PropertiesEntry>()
    entries.forEach(entry => last.set(entry.key, entry))
    // Each range is replaced from the end of the text, so the offsets of the ranges before it hold.
    const replacements = entries
        .map(entry => replacementOf(text, entry, edits.get(entry.key), last.get(entry.key) === entry, separator))
        .filter(replacement => replacement !== null)
        .sort((a, b) => b.start - a.start)
    let result = text
    replacements.forEach(({ start, end, text: replacement }) => {
        result = result.slice(0, start) + replacement + result.slice(end)
    })
    const lineBreak = lineBreakOf(text)
    const added = [...edits].filter(([key, value]) => value !== null && !last.has(key))
    if (added.length > 0 && result !== '' && !/[\r\n]$/.test(result)) {
        result += lineBreak
    }
    added.forEach(([key, value]) => {
        result += messageLine(key, value ?? '', separator, lineBreak)
    })
    return result
}

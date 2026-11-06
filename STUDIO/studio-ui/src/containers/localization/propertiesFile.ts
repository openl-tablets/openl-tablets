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

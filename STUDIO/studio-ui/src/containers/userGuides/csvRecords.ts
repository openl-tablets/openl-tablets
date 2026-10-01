/** A value of a record; a quoted one is taken literally, so a quoted `<` merges no cell. */
export interface CsvCell {
    value: string
    quoted: boolean
}

/** A record, starting at the given line of the block, counted from 0. */
export interface CsvRow {
    line: number
    cells: CsvCell[]
}

/**
 * The records of a `csv` or an `openl` code block of a user guide.
 *
 * The records follow RFC 4180: values are separated by commas, and a value holding a comma, a quote or a line break
 * is quoted, with every quote inside it doubled. Spaces around an unquoted value are dropped, and a blank line holds
 * no record.
 *
 * The validator of the guides rejects a block that does not read, so a malformed value is read as well as it can be
 * rather than reported.
 */
export const csvRecords = (text: string): CsvRow[] => {
    const rows: CsvRow[] = []
    let position = 0
    let line = 0

    const lineEnd = () => {
        const end = text.indexOf('\n', position)
        return end < 0 ? text.length : end
    }

    const quoted = (): string => {
        let value = ''
        position++
        while (position < text.length) {
            const char = text[position++]
            if (char === '"' && text[position] === '"') {
                value += char
                position++
            } else if (char === '"') {
                return value
            } else {
                line += char === '\n' ? 1 : 0
                value += char
            }
        }
        return value
    }

    const toBoundary = () => {
        while (position < text.length && text[position] !== ',' && text[position] !== '\n') {
            position++
        }
    }

    const cell = (): CsvCell => {
        while (text[position] === ' ' || text[position] === '\t') {
            position++
        }
        if (text[position] === '"') {
            const value = quoted()
            toBoundary()
            return { value, quoted: true }
        }
        const from = position
        toBoundary()
        return { value: text.slice(from, position).trim(), quoted: false }
    }

    /** The values of a record, up to the line break that ends it or the end of the text. */
    const record = (): CsvCell[] => {
        const cells = [cell()]
        let separator = text[position++]
        while (separator === ',') {
            // A comma closing the text still opens one more value, an empty one.
            cells.push(position < text.length ? cell() : { value: '', quoted: false })
            separator = text[position++]
        }
        line += separator === '\n' ? 1 : 0
        return cells
    }

    while (position < text.length) {
        const end = lineEnd()
        if (text.slice(position, end).trim() === '') {
            position = end + 1
            line++
        } else {
            const start = line
            rows.push({ line: start, cells: record() })
        }
    }
    return rows
}

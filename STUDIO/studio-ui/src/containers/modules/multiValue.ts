/**
 * The several values one cell holds, told apart by the separator the table writes them with.
 *
 * <p>A value may contain that separator itself; the table then writes it after the escaper it declares, and a
 * separator so written belongs to the value rather than ending it.
 */
export const splitValues = (text: string, separator: string, escaper?: string): string[] => {
    if (text === '') {
        return []
    }
    if (!escaper) {
        return text.split(separator).map(one => one.trim())
    }
    const values: string[] = []
    const escapedSeparator = escaper + separator
    let value = ''
    let at = 0
    while (at < text.length) {
        const escaped = text.startsWith(escapedSeparator, at)
        if (escaped || !text.startsWith(separator, at)) {
            value += escaped ? separator : text[at]
            at += escaped ? escapedSeparator.length : 1
        } else {
            values.push(value.trim())
            value = ''
            at += separator.length
        }
    }
    values.push(value.trim())
    return values
}

/** The chosen values written back as the cell holds them, each separator inside a value escaped again. */
export const joinValues = (values: string[], separator: string, escaper?: string): string =>
    values.map(value => (escaper ? value.replaceAll(separator, escaper + separator) : value)).join(separator)

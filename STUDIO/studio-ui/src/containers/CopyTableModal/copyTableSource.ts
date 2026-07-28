import type { RawTable, RawTableCellInput } from 'types/tables'

export interface TablePropertyInput {
    name: string
    value: RawTableCellInput['value']
}

const PROPERTIES_HEADER = 'properties'
/** The kind reported for a table whose header is free text rather than an OpenL keyword. */
const FREE_FORM_KIND = 'Other'

const writableCell = (cell: RawTableCellInput | undefined): RawTableCellInput => ({
    value: cell?.value ?? null,
    ...(cell?.colspan ? { colspan: cell.colspan } : {}),
    ...(cell?.rowspan ? { rowspan: cell.rowspan } : {}),
    ...(cell?.covered ? { covered: true } : {}),
})

const propertySectionSize = (source: RawTableCellInput[][]): number => {
    const marker = source[1]?.[0]
    // Case-sensitive, exactly as OpenL reads the section: a body row opening with `Properties` is data, and taking
    // it for a properties block would drop that row from the copy.
    return marker?.value === PROPERTIES_HEADER ? marker.rowspan ?? 1 : 0
}

/** Properties explicitly written between the table header and its body. */
export const readTableProperties = (source: RawTableCellInput[][]): TablePropertyInput[] => {
    const count = propertySectionSize(source)
    return source.slice(1, count + 1).map(row => ({
        name: String(row[1]?.value ?? ''),
        value: row[2]?.value ?? null,
    }))
}

/**
 * The header of the copy, named after it.
 *
 * <p>A free-form table's header is its name, so the copy's name is the whole of it.
 *
 * <p>Every other header opens with an OpenL keyword and carries the name after it. A table written without a name
 * of its own is reported under its keyword, so the copy is given a name rather than having the keyword — the word
 * that says what kind of table it is — overwritten with one.
 */
const replaceTechnicalName = (header: string, sourceName: string, copyName: string, kind: string): string => {
    if (kind === FREE_FORM_KIND) {
        return copyName
    }
    const escaped = sourceName.replaceAll(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const matches = [...header.matchAll(new RegExp(`(^|\\s)(${escaped})(?=\\s|\\(|$)`, 'gu'))]
    const match = matches.at(-1)
    if (!match || match.index === undefined) {
        throw new Error('The table name could not be found in its header.')
    }
    const prefix = match[1] ?? ''
    const start = match.index + prefix.length
    if (start === 0) {
        return `${sourceName} ${copyName}${header.slice(sourceName.length)}`
    }
    return `${header.slice(0, start)}${copyName}${header.slice(start + sourceName.length)}`
}

const propertySource = (properties: TablePropertyInput[], width: number): RawTableCellInput[][] =>
    properties.map((property, index) => Array.from({ length: width }, (_, column) => {
        if (column === 0) {
            if (index > 0) {
                return { value: null, covered: true }
            }
            return {
                value: PROPERTIES_HEADER,
                ...(properties.length > 1 ? { rowspan: properties.length } : {}),
            }
        }
        if (column === 1) {
            return { value: property.name.trim() }
        }
        return { value: column === 2 ? property.value : null }
    }))

/**
 * Produces the ordinary RawSource create payload for a copy.
 *
 * The browser owns the transformation: it renames the raw header and rebuilds the raw property rows before using
 * the same write endpoint as Create Table.
 */
export const buildCopySource = (
    table: RawTable,
    copyName: string,
    properties: TablePropertyInput[]
): RawTableCellInput[][] => {
    const oldPropertyCount = propertySectionSize(table.source)
    const original = table.source.map(row => row.map(writableCell))
    const header = original[0]
    if (!header || typeof header[0]?.value !== 'string') {
        throw new Error('The table does not have a readable header.')
    }

    const body = original.slice(oldPropertyCount + 1)
    const width = Math.max(
        properties.length ? 3 : 1,
        header.length,
        ...body.map(row => row.length)
    )
    const normalizedHeader = Array.from({ length: width }, (_, column) => {
        if (column === 0) {
            return {
                value: replaceTechnicalName(header[0]!.value as string, table.name, copyName, table.kind),
                ...(width > 1 ? { colspan: width } : {}),
            }
        }
        return { value: null, covered: true }
    })
    const normalizedBody = body.map(row =>
        Array.from({ length: width }, (_, column) => writableCell(row[column])))

    return [
        normalizedHeader,
        ...propertySource(properties, width),
        ...normalizedBody,
    ]
}

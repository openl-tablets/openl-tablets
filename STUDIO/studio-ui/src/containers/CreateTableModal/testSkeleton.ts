import type { DatatypeField, ProjectTable } from 'types/tables'
import { EXPECTED_RESULT, type TableArgument, type TablePreset, type TargetColumn, title } from './tableSkeletons'

/** The fields of a project datatype, or `null` when the name is not one — a value of it stays a single column. */
export type FieldsOfType = (typeName: string) => Promise<DatatypeField[] | null>

/** A Test or Run table under construction: the table it exercises, and the columns that call it. */
export interface TargetStructure {
    /** The tested table, as the project's list reports it. */
    table: ProjectTable
    columns: TargetColumn[]
}

/**
 * The arguments a tested table declares.
 *
 * <p>The signature is the header text after the return type, exactly as the compiler reads it — `Premium(Policy
 * policy, Integer age)`. A header the compiler could not read declares nothing.
 */
export const parseArguments = (signature: string | undefined): TableArgument[] => {
    const parameters = signature?.match(/\(([^)]*)\)/)?.[1]?.trim()
    if (!parameters) {
        return []
    }
    return parameters.split(',')
        .map(parameter => parameter.trim().split(/\s+/))
        .filter(parts => parts.length >= 2)
        .map(parts => ({ type: parts.at(-2) ?? '', name: parts.at(-1) ?? '' }))
}

/**
 * The columns one value of the given type needs, descending into the fields of a datatype.
 *
 * <p>A datatype already on the path is not opened again: one that refers back to itself, directly or through
 * another, describes an endless chain of columns. Each branch carries its own path, so the fields of one level are
 * read at the same time rather than one after another — a signature nesting several datatypes would otherwise wait
 * out one round-trip per type it reaches.
 */
const collectColumns = async (
    type: string,
    path: string,
    names: string,
    ancestors: ReadonlySet<string>,
    fieldsOf: FieldsOfType
): Promise<TargetColumn[]> => {
    const fields = ancestors.has(type) ? null : await fieldsOf(type)
    if (!fields?.length) {
        // The title reads as one phrase, so the whole path is titled at once rather than segment by segment.
        return [{ name: path, title: title(names), type }]
    }
    const onPath = new Set(ancestors).add(type)
    const branches = await Promise.all(fields.map(field =>
        collectColumns(field.type, `${path}.${field.name}`, `${names} ${field.name}`, onPath, fieldsOf)))
    return branches.flat()
}

/**
 * The columns a test of the given signature needs: one for every value a call has to supply.
 *
 * <p>An argument of a datatype is expanded into one column per field, addressed by the path OpenL reads it back
 * with — `driver.address.city`. Nesting goes as deep as the datatypes do.
 *
 * <p>Every other argument stays a single column. A simple value fills one cell, and a collection is filled from
 * several rows of the same test case rather than from a column per element.
 */
export const expandArguments = async (
    argumentsValue: TableArgument[],
    fieldsOf: FieldsOfType
): Promise<TargetColumn[]> => {
    const perArgument = await Promise.all(argumentsValue.map(argument =>
        collectColumns(argument.type, argument.name, argument.name, new Set(), fieldsOf)))
    return perArgument.flat()
}

/**
 * The Test or Run table a tested table calls for.
 *
 * <p>A Test table ends with the column its result is compared against; a Run table only calls, so it has none. The
 * expected result stays one column whatever its type — a whole object is what a generated test compares.
 */
export const buildTargetStructure = async (
    table: ProjectTable,
    preset: 'test' | 'run',
    fieldsOf: FieldsOfType
): Promise<TargetStructure> => {
    const columns = await expandArguments(parseArguments(table.signature), fieldsOf)
    return {
        table,
        columns: preset === 'test'
            ? [...columns, { name: EXPECTED_RESULT, title: 'Result', type: table.returnType ?? '' }]
            : columns,
    }
}

/** The name a generated table opens with: the tested table's, and what the generated table does to it. */
export const targetTableName = (targetName: string, preset: TablePreset): string =>
    `${targetName}${preset === 'run' ? 'Run' : 'Test'}`

/**
 * Tells whether the selected table type may exercise the table.
 *
 * <p>A Run table only calls the target, so it can call a method returning nothing. A Test table must have a result
 * to assert: a table returning nothing gives it nothing to compare against.
 */
export const canTargetTable = (table: ProjectTable, preset: 'test' | 'run'): boolean =>
    preset === 'run' || table.returnType !== 'void'

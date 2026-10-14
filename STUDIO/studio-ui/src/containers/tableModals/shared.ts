import type { DefaultOptionType, SearchConfig } from 'antd/es/select'
import type { ProjectModule } from 'types/projects'
import type { ProjectProperty } from 'types/tables'
import { isWorkbookPath } from '../../utils/workbooks'

/**
 * A legal OpenL table name, matching the identifier rule the compiler applies to the table header.
 *
 * <p>It follows `Character.isJavaIdentifierStart` and `Character.isJavaIdentifierPart`, as the server does. A name
 * starts with a letter of any script, a letter number, a currency sign or a connector such as `_`. Digits, combining
 * marks and format characters may follow. OpenL compiles a Korean, Cyrillic or Hindi name, so a dialog must not be
 * the only thing refusing one.
 *
 * <p>The control characters Java ignores in a name are not accepted here: nobody types them into a dialog.
 */
export const IDENTIFIER = /^[\p{L}\p{Nl}\p{Sc}\p{Pc}][\p{L}\p{Nl}\p{Sc}\p{Pc}\p{Nd}\p{Mn}\p{Mc}\p{Cf}]*$/u

/** The property a copy declares to become a new version of the table it was copied from. */
export const VERSION_PROPERTY = 'version'

/**
 * Whether the value is one the property accepts.
 *
 * <p>The shape comes from the property's own definition, which carries the pattern the compiler validates it with,
 * so the dialog refuses exactly what the module would refuse. A property stating no pattern accepts any value.
 */
export const isValidPropertyValue = (
    definition: ProjectProperty | undefined,
    value: string | number | boolean | null | undefined
): boolean =>
    !definition?.pattern || new RegExp(`^(?:${definition.pattern})$`).test(String(value ?? '').trim())

/** The groups Table Details lists properties under, in its order; a group it does not know follows them. */
const PROPERTY_GROUPS = ['Info', 'Business Dimension', 'Version', 'Dev']

/**
 * The properties a dialog offers: display names under their groups, the way Table Details lists them.
 *
 * <p>An author reads a property as *Effective Date* under *Business Dimension*, not as `effectiveDate` in one flat
 * list, and the grouping is what tells the dimensional properties apart from the rest. The value stays the technical
 * name, which is what a table declares.
 */
export const toPropertyGroups = (properties: ProjectProperty[]) => {
    const groups = new Map<string, { label: string, value: string }[]>()
    for (const property of properties) {
        const group = property.group || ''
        const options = groups.get(group) ?? []
        options.push({ label: property.displayName || property.name, value: property.name })
        groups.set(group, options)
    }
    const rank = (group: string) => {
        const known = PROPERTY_GROUPS.indexOf(group)
        return known < 0 ? PROPERTY_GROUPS.length : known
    }
    for (const options of groups.values()) {
        options.sort((left, right) => left.label.localeCompare(right.label))
    }
    return [...groups.entries()]
        .sort(([left], [right]) => rank(left) - rank(right) || left.localeCompare(right))
        .map(([label, options]) => ({ label, options }))
}

/**
 * How a list of properties built by {@link toPropertyGroups} is searched: by the display name it shows.
 *
 * <p>The technical name each option carries as its value is not what the reader sees, so it is not what they type.
 */
export const PROPERTY_SEARCH: SearchConfig<DefaultOptionType> = { optionFilterProp: 'label' }

/** Characters Excel rejects in a worksheet name; sending one makes the workbook write fail. */
const SHEET_NAME_FORBIDDEN = /[/\\*?[\]:]/
/** An apostrophe quotes a sheet reference in a formula, so Excel refuses a name that opens or closes with one. */
const SHEET_NAME_QUOTED = /^'|'$/
/** Excel stores no longer name than this, and rejects the workbook outright when one is written. */
const SHEET_NAME_MAX = 31

/** A sheet Excel will accept: named, short enough, and free of the characters a worksheet name may not carry. */
export const isValidSheetName = (sheetName: string): boolean => {
    const trimmed = sheetName.trim()
    return Boolean(trimmed) &&
        !SHEET_NAME_FORBIDDEN.test(trimmed) &&
        !SHEET_NAME_QUOTED.test(trimmed) &&
        trimmed.length <= SHEET_NAME_MAX
}

/**
 * The sheet name a table defaults into: the name trimmed, then clipped to the length Excel accepts.
 *
 * <p>A new table names its sheet after itself. Surrounding spaces are dropped first, the way the compiled table
 * name and the submitted sheet name both are, so they neither reach the sheet nor eat into its length. A table name
 * carries no length limit of its own, so a long one is cut to the longest sheet name a workbook can store rather
 * than left as a name Excel would reject.
 */
export const sheetNameFrom = (tableName: string): string => tableName.trim().slice(0, SHEET_NAME_MAX)

/** A module the table can be written to: one the project declares, or one the author is naming. */
export interface ModuleOption {
    name: string
    path?: string
}

/**
 * The named modules of a project a table can be written to, in the shape the destination field works with.
 *
 * <p>Only a workbook takes a table. A Markdown module, a folder of files, is left out: a table goes into it as a
 * file of its own, through the file editor.
 */
export const toModuleOptions = (modules: ProjectModule[]): ModuleOption[] => modules
    .filter(module => module.name && (!module.path || isWorkbookPath(module.path)))
    .map(module => ({ name: module.name ?? '', ...(module.path ? { path: module.path } : {}) }))

/** The destination list, in alphabetical order: a project declares its modules in no particular one. */
export const toSortedOptions = (modules: ModuleOption[]) => modules
    .map(module => ({ label: module.name, value: module.name }))
    .sort((left, right) => left.label.localeCompare(right.label))

export const asOptions = (values: readonly string[]) => values.map(value => ({ label: value, value }))

export const insertAt = <T, >(values: T[], index: number, value: T): T[] => [
    ...values.slice(0, index),
    value,
    ...values.slice(index),
]

export const deleteAt = <T, >(values: T[], index: number): T[] => [
    ...values.slice(0, index),
    ...values.slice(index + 1),
]

/**
 * Keeps one blank entry at the end of an editable list, so there is always a row to type the next value into.
 *
 * <p>An empty list starts with that blank row. A list whose last entry is filled in grows another one; a list still
 * ending in a half-written entry does not, or every keystroke would add a row.
 */
export const withTrailingBlank = <T, >(values: T[], isComplete: (value: T) => boolean, blank: () => T): T[] => {
    // A copy: the row below is pushed onto it, and the caller may well have handed over the state array itself.
    const normalized = values.length ? [...values] : [blank()]
    if (isComplete(normalized.at(-1) ?? blank())) {
        normalized.push(blank())
    }
    return normalized
}

/** Folder a table kind belongs in. A project keeps its Test and Run tables apart from the rules they exercise. */
const moduleFolder = (kind: string): string => kind === 'Test' || kind === 'Run' ? 'tests' : 'rules'

/** Where a module the author just named is written. The author names it; the path follows. */
export const defaultModulePath = (moduleName: string, kind = 'Datatype'): string => {
    const safeName = moduleName.trim().replaceAll(/[\\/:*?"<>|]/g, '-')
    return `${moduleFolder(kind)}/${safeName || 'NewModule'}.xlsx`
}

/** The module a table kind should land in by default: a Test or Run table belongs with the project's tests. */
export const preferredModule = (available: ModuleOption[], kind: string, current: string): string => {
    const folder = `${moduleFolder(kind)}/`
    const inFolder = (module: ModuleOption) => module.path?.startsWith(folder)
    return available.some(module => module.name === current && inFolder(module))
        ? current
        : available.find(inFolder)?.name ?? current
}

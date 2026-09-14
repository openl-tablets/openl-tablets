/** The part of a JSON Schema the input form reads. */
export interface JsonSchema {
    type?: string | string[]
    format?: string
    enum?: unknown[]
    properties?: Record<string, JsonSchema>
    items?: JsonSchema
    additionalProperties?: JsonSchema | boolean
    $ref?: string
    $defs?: Record<string, JsonSchema>
    definitions?: Record<string, JsonSchema>
    anyOf?: JsonSchema[]
    oneOf?: JsonSchema[]
    allOf?: JsonSchema[]
    /** The value a field starts with, as its datatype declares it. */
    default?: unknown
}

/** What kind of editor a schema calls for. */
export type FieldKind =
    | 'object'
    | 'map'
    | 'array'
    | 'enum'
    | 'boolean'
    | 'integer'
    | 'number'
    | 'date'
    | 'datetime'
    | 'string'
    | 'unknown'

// A `$ref` chain longer than this is cyclic. The schema refers to itself through its definitions.
const MAX_REF_DEPTH = 20

const decodePointer = (segment: string): string => segment.replaceAll('~1', '/').replaceAll('~0', '~')

/** Follows a `$ref` inside the root schema. `#/$defs/Name`, `#/definitions/Name` and `#` for the root are read. */
export const resolveRef = (ref: string, root: JsonSchema): JsonSchema | undefined => {
    if (ref === '#') {
        return root
    }
    if (!ref.startsWith('#/')) {
        return undefined
    }
    const target = ref.slice(2).split('/').map(decodePointer).reduce<unknown>(
        (node, segment) => (node && typeof node === 'object' ? (node as Record<string, unknown>)[segment] : undefined),
        root
    )
    return target && typeof target === 'object' ? target as JsonSchema : undefined
}

const isNullSchema = (schema: JsonSchema): boolean => schema.type === 'null'

/**
 * The schema a node is rendered by.
 *
 * A `$ref` is followed to its definition. A union whose only other branch is `null` collapses to that branch. A
 * one-part `allOf` is flattened.
 */
export const resolveSchema = (schema: JsonSchema, root: JsonSchema, depth = 0): JsonSchema => {
    if (depth > MAX_REF_DEPTH) {
        return schema
    }
    if (schema.$ref) {
        const target = resolveRef(schema.$ref, root)
        return target ? resolveSchema(target, root, depth + 1) : {}
    }
    const branches = (schema.anyOf ?? schema.oneOf)?.filter(branch => !isNullSchema(branch))
    if (branches?.length === 1 && branches[0]) {
        const { anyOf: _anyOf, oneOf: _oneOf, ...rest } = schema
        return resolveSchema({ ...rest, ...branches[0] }, root, depth + 1)
    }
    if (schema.allOf?.length === 1 && schema.allOf[0]) {
        const { allOf: _allOf, ...rest } = schema
        return resolveSchema({ ...rest, ...schema.allOf[0] }, root, depth + 1)
    }
    return schema
}

/** The one type a schema declares, `null` set aside. */
export const primaryType = (schema: JsonSchema): string | undefined => {
    if (Array.isArray(schema.type)) {
        return schema.type.find(type => type !== 'null')
    }
    return schema.type === 'null' ? undefined : schema.type
}

/** The formats a text is edited as a date by. */
const DATE_KINDS: Record<string, FieldKind> = { date: 'date', 'date-time': 'datetime' }

/** The kind of editor a resolved schema calls for. */
export const fieldKind = (schema: JsonSchema): FieldKind => {
    if (schema.enum) {
        return 'enum'
    }
    const type = primaryType(schema)
    if (schema.properties) {
        return 'object'
    }
    if (type === 'object') {
        return schema.additionalProperties ? 'map' : 'unknown'
    }
    switch (type) {
        case 'array':
            return 'array'
        case 'boolean':
            return 'boolean'
        case 'integer':
            return 'integer'
        case 'number':
            return 'number'
        case 'string':
            return DATE_KINDS[schema.format ?? ''] ?? 'string'
        default:
            return 'unknown'
    }
}

/** The schema of a map's values, when the map states one. */
export const mapValueSchema = (schema: JsonSchema): JsonSchema =>
    typeof schema.additionalProperties === 'object' ? schema.additionalProperties : {}

/**
 * A value without its `null` leaves.
 *
 * A null field is left out, as an unset field is. The form shows both the same way and neither travels to the
 * rules. A `null` element of a list stays, as it keeps the list's slots.
 */
export const withoutNulls = (value: unknown): unknown => {
    if (Array.isArray(value)) {
        return value.map(item => (item === null ? null : withoutNulls(item)))
    }
    if (value !== null && typeof value === 'object') {
        return Object.fromEntries(Object.entries(value as Record<string, unknown>)
            .filter(([, item]) => item !== null && item !== undefined)
            .map(([key, item]) => [key, withoutNulls(item)]))
    }
    return value
}

/**
 * The value a field starts with when it is created.
 *
 * An object starts with the defaults its datatype declares. A map and a list start empty. A plain value starts
 * with nothing.
 */
export const createValue = (schema: JsonSchema, root: JsonSchema): unknown => {
    const resolved = resolveSchema(schema, root)
    switch (fieldKind(resolved)) {
        case 'object':
            return Object.fromEntries(Object.entries(resolved.properties ?? {})
                .map(([name, property]) => [name, resolveSchema(property, root).default])
                .filter(([, value]) => value !== undefined && value !== null))
        case 'map':
            return {}
        case 'array':
            return []
        default:
            return undefined
    }
}

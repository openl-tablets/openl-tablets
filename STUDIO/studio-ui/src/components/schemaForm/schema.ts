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

/** Whether a field accepts `null`: its type is `null` or lists it, or its union has a `null` branch. */
export const acceptsNull = (schema: JsonSchema): boolean =>
    isNullSchema(schema)
    || (Array.isArray(schema.type) && schema.type.includes('null'))
    || [...(schema.anyOf ?? []), ...(schema.oneOf ?? [])].some(isNullSchema)

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
        // What stands beside the reference is kept, the way the union and the `allOf` branches below keep it:
        // a field describing itself as a datatype declares its own default beside the reference to it. What
        // the field says wins, as it describes this field while the definition describes the datatype. A
        // reference naming nothing leaves the field with what it says about itself.
        const { $ref: _ref, ...rest } = schema
        return resolveSchema({ ...resolveRef(schema.$ref, root), ...rest }, root, depth + 1)
    }
    const branches = (schema.anyOf ?? schema.oneOf)?.filter(branch => !isNullSchema(branch))
    if (branches?.length === 1 && branches[0]) {
        const { anyOf: _anyOf, oneOf: _oneOf, ...rest } = schema
        return resolveSchema({ ...branches[0], ...rest }, root, depth + 1)
    }
    if (schema.allOf?.length === 1 && schema.allOf[0]) {
        const { allOf: _allOf, ...rest } = schema
        return resolveSchema({ ...schema.allOf[0], ...rest }, root, depth + 1)
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
 * It is used for a value the server writes, such as the starting value of a parameter. A field there is `null`
 * only when it has no value, and a field left out gets no value either, so the rules receive the same. A `null`
 * element of a list stays, as it keeps the list's slots.
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
 * The value a field of an object takes when it is cleared.
 *
 * A field that accepts `null` takes `null`, and the rules receive `null`. Its datatype declares a default for it,
 * which a field left out would get instead.
 *
 * A field that cannot be `null`, such as an `int`, goes back to its default. A field with no default is left out,
 * and the rules receive `null`.
 */
export const clearedValue = (schema: JsonSchema, root: JsonSchema): unknown =>
    (acceptsNull(schema) ? null : resolveSchema(schema, root).default)

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

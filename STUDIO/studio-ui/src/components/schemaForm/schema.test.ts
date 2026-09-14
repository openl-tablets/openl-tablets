import { createValue, fieldKind, mapValueSchema, primaryType, resolveRef, resolveSchema, withoutNulls } from 'components/schemaForm/schema'

describe('resolveRef', () => {
    const root = { $defs: { 'Driver~1Info': { type: 'object' } }, definitions: { Car: { type: 'string' } } }

    it('follows definitions under $defs and definitions, decoding pointer escapes', () => {
        expect(resolveRef('#/$defs/Driver~01Info', root)).toEqual({ type: 'object' })
        expect(resolveRef('#/definitions/Car', root)).toEqual({ type: 'string' })
        expect(resolveRef('#', root)).toBe(root)
    })

    it('answers nothing for a missing or external reference', () => {
        expect(resolveRef('#/$defs/Missing', root)).toBeUndefined()
        expect(resolveRef('other.json#/x', root)).toBeUndefined()
    })
})

describe('resolveSchema', () => {
    it('collapses a nullable union to its real branch and follows a reference', () => {
        const root = { $defs: { Age: { type: 'integer' } } }
        expect(resolveSchema({ anyOf: [{ type: 'null' }, { $ref: '#/$defs/Age' }]}, root)).toEqual({ type: 'integer' })
        expect(resolveSchema({ allOf: [{ type: 'string' }], format: 'date' }, root)).toEqual({ type: 'string', format: 'date' })
    })

    it('leaves a real union and a dangling reference alone', () => {
        const union = { oneOf: [{ type: 'string' }, { type: 'integer' }]}
        expect(resolveSchema(union, {})).toBe(union)
        expect(resolveSchema({ $ref: '#/$defs/Missing' }, {})).toEqual({})
        // A reference naming nothing says nothing about the field; what the field says about itself is
        // still there to edit it by.
        expect(resolveSchema({ type: 'string', default: 'DE', $ref: '#/$defs/Missing' }, {}))
            .toEqual({ type: 'string', default: 'DE' })
    })

    it('keeps what stands beside a reference', () => {
        const root = { $defs: { Code: { type: 'string' } } }
        // A field that is a datatype and may be absent is a default beside a union of the reference and
        // null: collapsing the union leaves the default beside the reference, and following it must not
        // throw the default away.
        expect(resolveSchema({ default: 'DE', $ref: '#/$defs/Code' }, root))
            .toEqual({ type: 'string', default: 'DE' })
        expect(resolveSchema({ default: 'DE', anyOf: [{ $ref: '#/$defs/Code' }, { type: 'null' }]}, root))
            .toEqual({ type: 'string', default: 'DE' })
    })

    it('lets what a field declares win over the definition it names', () => {
        // The definition describes the datatype, the field describes itself: a field declaring the value it
        // starts with keeps it, whatever the datatype starts with elsewhere.
        const root = { $defs: { Code: { type: 'string', default: 'US' } } }
        expect(resolveSchema({ default: 'DE', $ref: '#/$defs/Code' }, root))
            .toEqual({ type: 'string', default: 'DE' })
        expect(resolveSchema({ default: 'DE', allOf: [{ $ref: '#/$defs/Code' }]}, root))
            .toEqual({ type: 'string', default: 'DE' })
        expect(resolveSchema({ default: 'DE', anyOf: [{ $ref: '#/$defs/Code' }, { type: 'null' }]}, root))
            .toEqual({ type: 'string', default: 'DE' })
    })

    it('stops on a reference that points back at itself', () => {
        const root = { $defs: { Loop: { $ref: '#/$defs/Loop' } } }
        expect(resolveSchema({ $ref: '#/$defs/Loop' }, root)).toEqual({ $ref: '#/$defs/Loop' })
    })
})

describe('fieldKind', () => {
    it('maps every schema shape the generator emits to an editor', () => {
        expect(fieldKind({ type: 'string', enum: ['A', 'B']})).toBe('enum')
        expect(fieldKind({ type: 'object', properties: { a: {} } })).toBe('object')
        expect(fieldKind({ type: 'object', additionalProperties: { type: 'string' } })).toBe('map')
        expect(fieldKind({ type: 'object' })).toBe('unknown')
        expect(fieldKind({ type: 'array', items: { type: 'string' } })).toBe('array')
        expect(fieldKind({ type: 'boolean' })).toBe('boolean')
        expect(fieldKind({ type: 'integer' })).toBe('integer')
        expect(fieldKind({ type: 'number' })).toBe('number')
        expect(fieldKind({ type: 'string', format: 'date' })).toBe('date')
        expect(fieldKind({ type: 'string', format: 'date-time' })).toBe('datetime')
        expect(fieldKind({ type: ['string', 'null']})).toBe('string')
        expect(fieldKind({})).toBe('unknown')
    })
})

describe('withoutNulls', () => {
    it('drops null fields but keeps null list slots', () => {
        expect(withoutNulls({ code: 'DE', name: null, ratings: [null, { agency: null, rating: 'A' }]}))
            .toEqual({ code: 'DE', ratings: [null, { rating: 'A' }]})
        expect(withoutNulls(5)).toBe(5)
    })
})

describe('primaryType / mapValueSchema / createValue', () => {
    it('sets null aside, reads map values and starts structures empty', () => {
        expect(primaryType({ type: ['null', 'number']})).toBe('number')
        expect(primaryType({})).toBeUndefined()
        expect(mapValueSchema({ additionalProperties: { type: 'integer' } })).toEqual({ type: 'integer' })
        expect(mapValueSchema({ additionalProperties: true })).toEqual({})
        expect(createValue({ type: 'object', properties: { a: {} } }, {})).toEqual({})
        expect(createValue({ type: 'object', additionalProperties: true }, {})).toEqual({})
        expect(createValue({ type: 'array' }, {})).toEqual([])
        expect(createValue({ type: 'string' }, {})).toBeUndefined()
    })

    it('creates an object with the defaults its datatype declares', () => {
        const root = { $defs: { Code: { type: 'string', default: 'DE' } } }
        const schema = { type: 'object', properties: { code: { $ref: '#/$defs/Code' }, name: { type: 'string' }, ok: { type: 'boolean', default: false } } }
        expect(createValue(schema, root)).toEqual({ code: 'DE', ok: false })
    })

    it('creates an object with the defaults declared on a field that may be absent', () => {
        // A nullable datatype field declares its default beside the union naming the datatype.
        const root = { $defs: { Address: { type: 'object', properties: { city: { type: 'string' } } } } }
        const schema = {
            type: 'object',
            properties: {
                home: { default: { city: 'Riga' }, anyOf: [{ $ref: '#/$defs/Address' }, { type: 'null' }]},
            },
        }
        expect(createValue(schema, root)).toEqual({ home: { city: 'Riga' } })
    })
})

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
})

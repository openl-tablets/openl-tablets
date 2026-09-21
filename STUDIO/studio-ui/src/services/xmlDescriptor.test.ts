import { describe, expect, it } from 'vitest'
import { emptyOf, MalformedXmlError, parseXmlRoot, readXml, writeXml, type XmlMapping } from './xmlDescriptor'

interface Sample {
    name: string
    enabled: boolean
    tags: string[]
    extra: string
    rest: string[]
}

const MAPPING: XmlMapping<Sample> = {
    root: 'sample',
    fields: {
        enabled: { kind: 'flag', tag: 'enabled' },
        name: { kind: 'text', tag: 'name', formerly: ['title']},
        tags: { kind: 'list', wrapper: 'tags', item: 'tag' },
        extra: { kind: 'xml', tag: 'extra' },
        rest: { kind: 'rest' },
    },
}

const rootOf = <T>(mapping: XmlMapping<T>, xml: string): Element => parseXmlRoot(xml, mapping.root)!

describe('readXml', () => {
    it('reads each kind of field out of its element', () => {
        const model = readXml(MAPPING, rootOf(MAPPING, `<sample>
            <name> Pricing </name>
            <enabled>1</enabled>
            <tags><tag>a</tag><tag> </tag><tag>b</tag></tags>
            <extra><entry key="x"/></extra>
        </sample>`))

        expect(model).toEqual({ name: 'Pricing', enabled: true, tags: ['a', 'b'], extra: '<entry key="x"/>', rest: []})
    })

    it('reads nothing as blank, off and empty — the model a blank descriptor is', () => {
        const empty = { name: '', enabled: false, tags: [], extra: '', rest: []}

        expect(readXml(MAPPING, rootOf(MAPPING, '<sample/>'))).toEqual(empty)
        expect(emptyOf(MAPPING)).toEqual(empty)
    })

    it('reads a value kept under a former name of its element', () => {
        expect(readXml(MAPPING, rootOf(MAPPING, '<sample><title>Old</title></sample>')).name).toBe('Old')
        // The current name wins over a former one left behind.
        const both = rootOf(MAPPING, '<sample><title>Old</title><name>New</name></sample>')
        expect(readXml(MAPPING, both).name).toBe('New')
    })
})

describe('writeXml', () => {
    it('writes the elements in the order of the mapping, one per line', () => {
        const model = { name: 'Pricing', enabled: true, tags: ['a', 'b'], extra: '<entry key="x"/>', rest: []}

        const xml = writeXml(MAPPING, model)

        expect(xml).toBe(`<sample>
    <enabled>true</enabled>
    <name>Pricing</name>
    <tags>
        <tag>a</tag>
        <tag>b</tag>
    </tags>
    <extra>
        <entry key="x"/>
    </extra>
</sample>
`)
    })

    it('leaves out what has nothing to say: a blank text, a flag that is off, an empty list', () => {
        expect(writeXml(MAPPING, { name: ' ', enabled: false, tags: [' '], extra: '', rest: []})).toBe('<sample/>\n')
    })

    it('escapes the text through the serializer', () => {
        const xml = writeXml(MAPPING, { ...emptyOf(MAPPING), name: 'a&b<c' })

        expect(xml).toContain('<name>a&amp;b&lt;c</name>')
        expect(readXml(MAPPING, rootOf(MAPPING, xml)).name).toBe('a&b<c')
    })

    it('carries the elements it does not manage over as they stand, and drops a former name', () => {
        const original = rootOf(MAPPING, `<sample>
            <title>Old</title>
            <custom attr="1"><inner>kept</inner></custom>
        </sample>`)

        const xml = writeXml(MAPPING, { ...readXml(MAPPING, original), name: 'New' })

        expect(xml).toBe(`<sample>
    <name>New</name>
    <custom attr="1"><inner>kept</inner></custom>
</sample>
`)
    })

    it('lays out a block of XML with its comments on lines of their own, and keeps its text as it is', () => {
        const xml = writeXml(MAPPING, {
            ...emptyOf(MAPPING),
            extra: '<!-- first --><entry key="x"/>\n<!-- second --><note><![CDATA[a < b]]></note>',
        })

        expect(xml).toBe(`<sample>
    <extra>
        <!-- first -->
        <entry key="x"/>
        <!-- second -->
        <note><![CDATA[a < b]]></note>
    </extra>
</sample>
`)
    })

    it('leaves an element with text among its children as it stands', () => {
        const original = rootOf(MAPPING, '<sample><note>Some <b>bold</b> text</note></sample>')

        const xml = writeXml(MAPPING, readXml(MAPPING, original))

        expect(xml).toBe(`<sample>
    <note>Some <b>bold</b> text</note>
</sample>
`)
    })

    it('refuses a block of XML that does not parse', () => {
        const model = { ...emptyOf(MAPPING), extra: '</extra><name>evil</name>' }

        expect(() => writeXml(MAPPING, model)).toThrow(MalformedXmlError)
    })
})

interface Item {
    name: string
    path: string
    on?: { flag: boolean } | undefined
    rest?: string[] | undefined
}

interface Nested {
    kind?: 'ALPHA' | 'BETA' | undefined
    entries: string[]
    words: string[]
    items: Item[]
    inner?: { first: string; second: string } | undefined
}

const NESTED: XmlMapping<Nested> = {
    root: 'nested',
    fields: {
        kind: { kind: 'choice', tag: 'kind', values: ['ALPHA', 'BETA']},
        entries: { kind: 'list', wrapper: 'entries', item: 'entry', attribute: 'path' },
        words: { kind: 'list', item: 'word' },
        items: {
            kind: 'objects',
            wrapper: 'items',
            item: 'item',
            fields: {
                name: { kind: 'text', tag: 'name' },
                path: { kind: 'text', tag: 'root', attribute: 'path' },
                on: { kind: 'object', tag: 'on', secondary: true, fields: { flag: { kind: 'flag', tag: 'flag' } } },
                rest: { kind: 'rest' },
            },
        },
        inner: {
            kind: 'object',
            tag: 'inner',
            fields: { first: { kind: 'text', tag: 'first' }, second: { kind: 'text', tag: 'second', secondary: true } },
        },
    },
}

describe('readXml of nested fields', () => {
    it('reads a choice as the engine spells it, an attribute list, bare repeated elements and nested models', () => {
        const model = readXml(NESTED, rootOf(NESTED, `<nested>
            <kind>beta</kind>
            <entries><entry path="a/"/><entry path=" b/ "/><entry/></entries>
            <word>x</word><word>y</word>
            <items>
                <item><name>One</name><root path="one/*.xlsx"/><on><flag>true</flag></on><extra a="1"/></item>
                <item><root path="two/*.xlsx"/><on><flag>false</flag></on></item>
            </items>
            <inner><first>f</first><second>s</second></inner>
        </nested>`))

        expect(model).toEqual({
            kind: 'BETA',
            entries: ['a/', 'b/'],
            words: ['x', 'y'],
            items: [
                { name: 'One', path: 'one/*.xlsx', on: { flag: true }, rest: ['<extra a="1"/>']},
                { name: '', path: 'two/*.xlsx', on: undefined, rest: []},
            ],
            inner: { first: 'f', second: 's' },
        })
    })

    it('reads nothing from a model whose primary fields are empty, whatever its secondary ones hold', () => {
        const model = readXml(NESTED, rootOf(NESTED, `<nested>
            <kind>gamma</kind>
            <items><item><on><flag>true</flag></on></item></items>
            <inner><second>s</second></inner>
        </nested>`))

        expect(model).toEqual({ kind: undefined, entries: [], words: [], items: [], inner: undefined })
    })
})

describe('writeXml of nested fields', () => {
    it('writes every kind under its element, in the order of the mapping', () => {
        const xml = writeXml(NESTED, {
            kind: 'ALPHA',
            entries: ['a/', ' '],
            words: ['x', 'y'],
            items: [
                { name: 'One', path: 'one/*.xlsx', on: { flag: true }, rest: ['<extra a="1"><deep/></extra>']},
                { name: '', path: 'two/*.xlsx', on: { flag: false } },
                // A row with nothing but a secondary value names no item, so it is not written.
                { name: '', path: '', on: { flag: true } },
            ],
            inner: { first: 'f', second: '' },
        })

        expect(xml).toBe(`<nested>
    <kind>ALPHA</kind>
    <entries>
        <entry path="a/"/>
    </entries>
    <word>x</word>
    <word>y</word>
    <items>
        <item>
            <name>One</name>
            <root path="one/*.xlsx"/>
            <on>
                <flag>true</flag>
            </on>
            <extra a="1"><deep/></extra>
        </item>
        <item>
            <root path="two/*.xlsx"/>
        </item>
    </items>
    <inner>
        <first>f</first>
    </inner>
</nested>
`)
    })

    it('writes nothing for a nested model that says nothing, and for an unknown choice', () => {
        const xml = writeXml(NESTED, {
            kind: 'gamma' as 'ALPHA',
            entries: [],
            words: [],
            items: [],
            inner: { first: ' ', second: 's' },
        })

        expect(xml).toBe('<nested/>\n')
    })

    it('refuses a carried-over element that does not parse', () => {
        expect(() => writeXml(NESTED, {
            entries: [], words: [], items: [{ name: 'One', path: '', rest: ['<broken']}],
        })).toThrow(MalformedXmlError)
    })
})

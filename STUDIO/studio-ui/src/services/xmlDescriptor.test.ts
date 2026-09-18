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

// Shared DOM helpers for the client-side descriptor readers/writers (rules.xml, rules-deploy.xml), and the
// mapping that reads a model out of a descriptor and writes it back through the browser's own XML parser
// and serializer. A descriptor is described once, field by field, the way JSON is: the mapping says which
// element each field is kept in, and the reader and the writer do the rest. The elements the mapping does
// not manage are read into the model as they stand and written back the same way, so an edit never loses
// them.

const childrenOf = (parent: Element, tag: string): Element[] =>
    Array.from(parent.children).filter(child => child.tagName === tag)

/** The first direct child element with the given tag, or null. */
export const directChild = (parent: Element, tag: string): Element | null =>
    Array.from(parent.children).find(child => child.tagName === tag) ?? null

/** The trimmed text of the first direct child with the given tag, or an empty string when absent. */
export const childValue = (parent: Element, tag: string): string =>
    directChild(parent, tag)?.textContent?.trim() ?? ''

/**
 * Whether the first direct child with the given tag says yes.
 *
 * <p>The engine reads these as XML booleans, which are written `true` or `1`; anything else, the element
 * missing included, is no.
 */
export const childFlag = (parent: Element, tag: string): boolean =>
    ['true', '1'].includes(childValue(parent, tag))

/** The trimmed text of every direct child with the given tag, dropping the blank ones. */
export const childValues = (parent: Element, tag: string): string[] =>
    childrenOf(parent, tag).map(child => child.textContent?.trim() ?? '').filter(Boolean)

const serialize = (node: Node): string => new XMLSerializer().serializeToString(node)

/** The serialized inner XML of an element (its children), trimmed. */
const innerXml = (element: Element): string => Array.from(element.childNodes).map(serialize).join('').trim()

/** The root element of an XML string, or null when the string is blank or malformed. */
const parseElement = (xml: string): Element | null => {
    const trimmed = xml.trim()
    if (!trimmed) {
        return null
    }
    const doc = new DOMParser().parseFromString(trimmed, 'application/xml')
    return doc.getElementsByTagName('parsererror').length > 0 ? null : doc.documentElement
}

/**
 * Parses an XML string and returns its root element, or null when the string is blank, malformed, or
 * rooted at a different element than expected. Callers decide whether a null is "empty" or an error.
 */
export const parseXmlRoot = (xml: string, rootTag: string): Element | null => {
    const root = parseElement(xml)
    return root?.tagName === rootTag ? root : null
}

/** The child elements of an element the mapping does not manage, as they stand in the document. */
const unmanagedElements = (parent: Element, managed: Set<string>): Element[] =>
    Array.from(parent.children).filter(child => !managed.has(child.tagName))

/** The children of an element the editor does not manage, each serialized verbatim (no indentation). */
export const unmanagedChildren = (parent: Element, managed: Set<string>): string[] =>
    unmanagedElements(parent, managed).map(serialize)

/**
 * The children of {@link parseXmlRoot}'s result that the editor does not manage, each serialized and
 * indented, ready to be dropped back into the rebuilt document verbatim. A blank or unreadable original
 * preserves nothing.
 */
export const preservedChildren = (xml: string, rootTag: string, managed: Set<string>): string[] => {
    const root = parseXmlRoot(xml, rootTag)
    return root ? unmanagedChildren(root, managed).map(child => `    ${child}`) : []
}

/** A text value: the trimmed text of its element, and no element at all when blank. */
interface TextField {
    kind: 'text'
    tag: string
    /** Names the element went by before. A value under one of them is read as this element's, and is
     * written back under the current name only. */
    formerly?: string[]
}

/**
 * A yes/no value. The element says `true` when the value is on and is left out when it is off, which is
 * what the engine assumes of a descriptor that does not say.
 */
interface FlagField {
    kind: 'flag'
    tag: string
}

/** A list of text values, one `item` element each under `wrapper`; no element at all when the list is empty. */
interface ListField {
    kind: 'list'
    wrapper: string
    item: string
}

/**
 * A block of XML: the inner XML of `tag`, and no element at all when blank. It is laid out like the rest
 * of the file when written.
 */
interface XmlField {
    kind: 'xml'
    tag: string
}

/**
 * The elements of the parent the mapping does not manage, each serialized as it stands. They are read so
 * an edit can write them back, and written verbatim, in the place the field has among the others.
 */
interface RestField {
    kind: 'rest'
}

/** The kinds of field a value of the given type can be kept as. */
type XmlFieldOf<V> =
    [V] extends [boolean] ? FlagField
        : [V] extends [string] ? TextField | XmlField
            : [V] extends [readonly string[]] ? ListField | RestField
                : never

/** How every field of a model is kept in its element, in the order the elements are written. */
type XmlFields<T> = { [K in keyof T]-?: XmlFieldOf<NonNullable<T[K]>> }

/** How a model maps onto a descriptor: the root element, and the element each field is kept in. */
export interface XmlMapping<T> {
    root: string
    fields: XmlFields<T>
}

/** A block of XML that does not parse, so the descriptor it belongs in cannot be written. */
export class MalformedXmlError extends Error {
    constructor(where: string) {
        super(`Malformed XML in ${where}`)
        this.name = 'MalformedXmlError'
    }
}

// The fields as the reader and the writer see them, whatever model they belong to.
type AnyFields = Record<string, AnyField>

type AnyField = TextField | FlagField | ListField | XmlField | RestField

type Model = Record<string, unknown>

/** The elements a field owns and rewrites, the former names included. */
const tagsOf = (field: AnyField): string[] => {
    switch (field.kind) {
        case 'text':
            return [field.tag, ...(field.formerly ?? [])]
        case 'list':
            return [field.wrapper]
        case 'rest':
            return []
        default:
            return [field.tag]
    }
}

const managedTags = (fields: AnyFields): Set<string> => new Set(Object.values(fields).flatMap(tagsOf))

const textOf = (value: unknown): string => (value == null ? '' : String(value).trim())

const textsOf = (value: unknown): string[] =>
    (Array.isArray(value) ? value : []).map(textOf).filter(Boolean)

/** What a field is when the descriptor says nothing about it. */
const emptyValue = (field: AnyField): unknown => {
    switch (field.kind) {
        case 'text':
        case 'xml':
            return ''
        case 'flag':
            return false
        case 'list':
        case 'rest':
            return []
    }
}

/** The model of a descriptor that says nothing: what a blank file reads as. */
export const emptyOf = <T>(mapping: XmlMapping<T>): T =>
    Object.fromEntries(Object.entries(mapping.fields as AnyFields).map(([key, field]) => [key, emptyValue(field)])) as T

const readField = (parent: Element, field: AnyField, fields: AnyFields): unknown => {
    switch (field.kind) {
        case 'text':
            return tagsOf(field).map(tag => childValue(parent, tag)).find(Boolean) ?? ''
        case 'flag':
            return childFlag(parent, field.tag)
        case 'list': {
            const wrapper = directChild(parent, field.wrapper)
            return wrapper ? childValues(wrapper, field.item) : []
        }
        case 'xml': {
            const element = directChild(parent, field.tag)
            return element ? innerXml(element) : ''
        }
        case 'rest':
            return unmanagedElements(parent, managedTags(fields)).map(serialize)
    }
}

/** The model a descriptor holds, field by field. */
export const readXml = <T>(mapping: XmlMapping<T>, root: Element): T => {
    const fields = mapping.fields as AnyFields
    return Object.fromEntries(Object.entries(fields).map(([key, field]) => [key, readField(root, field, fields)])) as T
}

/** What a write needs besides the model: the document written into, and the elements carried over verbatim. */
interface Writing {
    doc: Document
    /** The elements written as they stand, and so left out of the layout. */
    verbatim: Set<Element>
}

/** An element carrying one value as its text. */
const leaf = (writing: Writing, tag: string, value: string): Element => {
    const element = writing.doc.createElement(tag)
    element.textContent = value
    return element
}

/** An element holding other elements. */
const wrap = (writing: Writing, tag: string, children: Element[]): Element => {
    const element = writing.doc.createElement(tag)
    element.append(...children)
    return element
}

/** The element a block of XML makes, parsed under its tag. */
const xmlElement = (writing: Writing, tag: string, fragment: string): Element => {
    const parsed = parseXmlRoot(`<${tag}>${fragment}</${tag}>`, tag)
    if (!parsed) {
        throw new MalformedXmlError(`<${tag}>`)
    }
    return writing.doc.importNode(parsed, true)
}

/** An element carried over as it stands, from the text it was read as. */
const verbatimElement = (writing: Writing, xml: string): Element => {
    const parsed = parseElement(xml)
    if (!parsed) {
        throw new MalformedXmlError(`"${xml.slice(0, 40)}"`)
    }
    const imported = writing.doc.importNode(parsed, true)
    writing.verbatim.add(imported)
    return imported
}

/** The elements a field writes for its value: none when the value is nothing. */
const writeField = (writing: Writing, field: AnyField, value: unknown): Element[] => {
    switch (field.kind) {
        case 'text': {
            const text = textOf(value)
            return text ? [leaf(writing, field.tag, text)] : []
        }
        case 'flag':
            return value ? [leaf(writing, field.tag, 'true')] : []
        case 'list': {
            const items = textsOf(value).map(item => leaf(writing, field.item, item))
            return items.length > 0 ? [wrap(writing, field.wrapper, items)] : []
        }
        case 'xml': {
            const fragment = textOf(value)
            return fragment ? [xmlElement(writing, field.tag, fragment)] : []
        }
        case 'rest':
            return textsOf(value).map(xml => verbatimElement(writing, xml))
    }
}

const INDENT = '    '

/** Whether a node is what an element says — its text, plain or in a CDATA section — rather than its layout. */
const isContent = (node: Node): boolean =>
    node.nodeType === Node.TEXT_NODE || node.nodeType === Node.CDATA_SECTION_NODE

/**
 * Puts every child of an element — an element or a comment — on a line of its own, one step deeper than
 * its parent.
 *
 * <p>An element carried over verbatim is put on its line and left as it is inside. So is an element with
 * text among its children: there the whitespace is part of the text, not layout, and laying it out would
 * change what the element says.
 */
const layOut = (element: Element, depth: number, verbatim: Set<Element>): void => {
    const nodes = Array.from(element.childNodes)
    const children = nodes.filter(node => !isContent(node))
    if (children.length === 0 || nodes.some(node => isContent(node) && node.textContent?.trim())) {
        return
    }
    const doc = element.ownerDocument
    // The whitespace between the children is layout, not content, and is laid out afresh.
    nodes.filter(isContent).forEach(node => node.remove())
    for (const child of children) {
        element.insertBefore(doc.createTextNode(`\n${INDENT.repeat(depth + 1)}`), child)
        if (child.nodeType === Node.ELEMENT_NODE && !verbatim.has(child as Element)) {
            layOut(child as Element, depth + 1, verbatim)
        }
    }
    element.append(doc.createTextNode(`\n${INDENT.repeat(depth)}`))
}

/**
 * Writes a model as a descriptor: the element of each field, in the order of the mapping. The text is
 * laid out one element per line, the way the engine writes a descriptor.
 */
export const writeXml = <T>(mapping: XmlMapping<T>, model: T): string => {
    const doc = document.implementation.createDocument(null, mapping.root)
    const writing: Writing = { doc, verbatim: new Set() }
    const fields = mapping.fields as AnyFields
    doc.documentElement.append(...Object.entries(fields).flatMap(([key, field]) =>
        writeField(writing, field, (model as Model)[key])))
    layOut(doc.documentElement, 0, writing.verbatim)
    return `${serialize(doc)}\n`
}

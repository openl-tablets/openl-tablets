// The mapping that reads a model out of a descriptor (rules.xml, rules-deploy.xml) and writes it back
// through the browser's own XML parser and serializer. A descriptor is described once, field by field,
// the way JSON is: the mapping says which element each field is kept in, and the reader and the writer
// do the rest. The elements the mapping does not manage are read into the model as they stand and
// written back the same way, so an edit never loses them.

const childrenOf = (parent: Element, tag: string): Element[] =>
    Array.from(parent.children).filter(child => child.tagName === tag)

const directChild = (parent: Element, tag: string): Element | null =>
    Array.from(parent.children).find(child => child.tagName === tag) ?? null

/** The value an element carries: the trimmed text of one of its attributes, or of the element itself. */
const valueOf = (element: Element | null, attribute?: string): string =>
    (attribute ? element?.getAttribute(attribute) : element?.textContent)?.trim() ?? ''

const childValue = (parent: Element, tag: string, attribute?: string): string =>
    valueOf(directChild(parent, tag), attribute)

/**
 * Whether the first direct child with the given tag says yes.
 *
 * <p>The engine reads these as XML booleans, which are written `true` or `1`; anything else, the element
 * missing included, is no.
 */
const childFlag = (parent: Element, tag: string): boolean => ['true', '1'].includes(childValue(parent, tag))

/** The values of every direct child with the given tag, dropping the blank ones. */
const childValues = (parent: Element, tag: string, attribute?: string): string[] =>
    childrenOf(parent, tag).map(child => valueOf(child, attribute)).filter(Boolean)

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

/**
 * What every field of a mapping may say about itself.
 *
 * <p>A secondary field does not make its element worth keeping on its own: an object whose primary fields
 * are all empty is nothing, whatever its secondary fields hold, and is neither read nor written.
 */
interface FieldMarks {
    secondary?: boolean
}

/**
 * A text value: the trimmed text of its element, or the value of one of its attributes; and no element at
 * all when blank.
 */
interface TextField extends FieldMarks {
    kind: 'text'
    tag: string
    /** The attribute the value is kept in, when it is not the text of the element. */
    attribute?: string
    /** Names the element went by before. A value under one of them is read as this element's, and is
     * written back under the current name only. */
    formerly?: string[]
}

/** One of a fixed set of words, as the engine spells them; read in any case, and nothing when unknown. */
interface ChoiceField<V extends string> extends FieldMarks {
    kind: 'choice'
    tag: string
    values: readonly V[]
}

/**
 * A yes/no value. The element says `true` when the value is on and is left out when it is off, which is
 * what the engine assumes of a descriptor that does not say.
 */
interface FlagField extends FieldMarks {
    kind: 'flag'
    tag: string
}

/**
 * A list of text values, one `item` element each, under the `wrapper` element when there is one; no
 * element at all when the list is empty.
 */
interface ListField extends FieldMarks {
    kind: 'list'
    item: string
    wrapper?: string
    /** The attribute of each item the value is kept in, when it is not the text of the item. */
    attribute?: string
}

/**
 * A block of XML: the inner XML of `tag`, and no element at all when blank. It is laid out like the rest
 * of the file when written.
 */
interface XmlField extends FieldMarks {
    kind: 'xml'
    tag: string
}

/**
 * The elements of the parent the mapping does not manage, each serialized as it stands. They are read so
 * an edit can write them back, and written verbatim, in the place the field has among the others.
 */
interface RestField extends FieldMarks {
    kind: 'rest'
}

/** A nested model in an element of its own; nothing when the element is absent or says nothing. */
interface ObjectField<V> extends FieldMarks {
    kind: 'object'
    tag: string
    fields: XmlFields<V>
}

/**
 * A list of nested models, one `item` element each, under the `wrapper` element when there is one; no
 * element at all when the list is empty.
 */
interface ObjectsField<V> extends FieldMarks {
    kind: 'objects'
    item: string
    wrapper?: string
    fields: XmlFields<V>
}

/** The kinds of field a value of the given type can be kept as. */
type XmlFieldOf<V> =
    [V] extends [boolean] ? FlagField
        : [V] extends [string] ? TextField | ChoiceField<V> | XmlField
            : [V] extends [readonly string[]] ? ListField | RestField
                : [V] extends [readonly (infer I)[]] ? ObjectsField<I>
                    : [V] extends [object] ? ObjectField<V>
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

type AnyField = TextField | ChoiceField<string> | FlagField | ListField | XmlField | RestField
    | (Omit<ObjectField<unknown>, 'fields'> & { fields: AnyFields })
    | (Omit<ObjectsField<unknown>, 'fields'> & { fields: AnyFields })

type Model = Record<string, unknown>

/** The elements a field owns and rewrites, the former names included. */
const tagsOf = (field: AnyField): string[] => {
    switch (field.kind) {
        case 'text':
            return [field.tag, ...(field.formerly ?? [])]
        case 'list':
        case 'objects':
            return [field.wrapper ?? field.item]
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

const choiceOf = (field: ChoiceField<string>, value: unknown): string | undefined => {
    const text = textOf(value).toUpperCase()
    return field.values.find(choice => choice.toUpperCase() === text)
}

const itemsOf = (value: unknown, fields: AnyFields): unknown[] =>
    (Array.isArray(value) ? value : []).filter(item => says(fields, item))

/** Whether a model has something to say through its primary fields; one that does not is nothing. */
const says = (fields: AnyFields, model: unknown): boolean =>
    model != null
    && Object.entries(fields).some(([key, field]) => !field.secondary && !isEmpty(field, (model as Model)[key]))

/** Whether a value is nothing for its field: blank, off, empty, or a model that says nothing. */
const isEmpty = (field: AnyField, value: unknown): boolean => {
    switch (field.kind) {
        case 'text':
        case 'xml':
            return textOf(value) === ''
        case 'choice':
            return choiceOf(field, value) === undefined
        case 'flag':
            return !value
        case 'list':
        case 'rest':
            return textsOf(value).length === 0
        case 'object':
            return !says(field.fields, value)
        case 'objects':
            return itemsOf(value, field.fields).length === 0
    }
}

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
        case 'objects':
            return []
        case 'choice':
        case 'object':
            return undefined
    }
}

/** The model of a descriptor that says nothing: what a blank file reads as. */
export const emptyOf = <T>(mapping: XmlMapping<T>): T =>
    Object.fromEntries(Object.entries(mapping.fields as AnyFields).map(([key, field]) => [key, emptyValue(field)])) as T

/** The element the items of a list sit in: the wrapper when the list has one, the parent itself otherwise. */
const holderOf = (parent: Element, wrapper: string | undefined): Element | null =>
    wrapper ? directChild(parent, wrapper) : parent

/** The model an element holds, or nothing when it has nothing to say. */
const readObject = (element: Element | null, fields: AnyFields): Model | undefined => {
    if (!element) {
        return undefined
    }
    const model = readFields(element, fields)
    return says(fields, model) ? model : undefined
}

const readField = (parent: Element, field: AnyField, fields: AnyFields): unknown => {
    switch (field.kind) {
        case 'text':
            return tagsOf(field).map(tag => childValue(parent, tag, field.attribute)).find(Boolean) ?? ''
        case 'choice':
            return choiceOf(field, childValue(parent, field.tag))
        case 'flag':
            return childFlag(parent, field.tag)
        case 'list': {
            const holder = holderOf(parent, field.wrapper)
            return holder ? childValues(holder, field.item, field.attribute) : []
        }
        case 'xml': {
            const element = directChild(parent, field.tag)
            return element ? innerXml(element) : ''
        }
        case 'rest':
            return unmanagedElements(parent, managedTags(fields)).map(serialize)
        case 'object':
            return readObject(directChild(parent, field.tag), field.fields)
        case 'objects': {
            const holder = holderOf(parent, field.wrapper)
            return holder
                ? childrenOf(holder, field.item)
                    .map(child => readObject(child, field.fields))
                    .filter(item => item !== undefined)
                : []
        }
    }
}

const readFields = (parent: Element, fields: AnyFields): Model =>
    Object.fromEntries(Object.entries(fields).map(([key, field]) => [key, readField(parent, field, fields)]))

/** The model a descriptor holds, field by field. */
export const readXml = <T>(mapping: XmlMapping<T>, root: Element): T =>
    readFields(root, mapping.fields as AnyFields) as T

/** What a write needs besides the model: the document written into, and the elements carried over verbatim. */
interface Writing {
    doc: Document
    /** The elements written as they stand, and so left out of the layout. */
    verbatim: Set<Element>
}

/** An element carrying one value: in an attribute, or as its text. */
const leaf = (writing: Writing, tag: string, value: string, attribute?: string): Element => {
    const element = writing.doc.createElement(tag)
    if (attribute) {
        element.setAttribute(attribute, value)
    } else {
        element.textContent = value
    }
    return element
}

/** An element holding other elements. */
const wrap = (writing: Writing, tag: string, children: Element[]): Element => {
    const element = writing.doc.createElement(tag)
    element.append(...children)
    return element
}

/** The items of a list as written: under their wrapper when the list has one, and nothing when it is empty. */
const wrapped = (writing: Writing, wrapper: string | undefined, items: Element[]): Element[] => {
    if (items.length === 0) {
        return []
    }
    return wrapper ? [wrap(writing, wrapper, items)] : items
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
            return text ? [leaf(writing, field.tag, text, field.attribute)] : []
        }
        case 'choice': {
            const choice = choiceOf(field, value)
            return choice ? [leaf(writing, field.tag, choice)] : []
        }
        case 'flag':
            return value ? [leaf(writing, field.tag, 'true')] : []
        case 'list':
            return wrapped(writing, field.wrapper,
                textsOf(value).map(item => leaf(writing, field.item, item, field.attribute)))
        case 'xml': {
            const fragment = textOf(value)
            return fragment ? [xmlElement(writing, field.tag, fragment)] : []
        }
        case 'rest':
            return textsOf(value).map(xml => verbatimElement(writing, xml))
        case 'object':
            return says(field.fields, value) ? [objectElement(writing, field.tag, field.fields, value)] : []
        case 'objects':
            return wrapped(writing, field.wrapper,
                itemsOf(value, field.fields).map(item => objectElement(writing, field.item, field.fields, item)))
    }
}

/** The elements the fields of a model write, in the order of the mapping. */
const fieldElements = (writing: Writing, fields: AnyFields, model: unknown): Element[] =>
    Object.entries(fields).flatMap(([key, field]) => writeField(writing, field, (model as Model)[key]))

/** The element a nested model is kept in, holding what its fields write. */
const objectElement = (writing: Writing, tag: string, fields: AnyFields, model: unknown): Element =>
    wrap(writing, tag, fieldElements(writing, fields, model))

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
    doc.documentElement.append(...fieldElements(writing, mapping.fields as AnyFields, model))
    layOut(doc.documentElement, 0, writing.verbatim)
    return `${serialize(doc)}\n`
}

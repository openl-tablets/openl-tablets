// Shared DOM helpers for the client-side descriptor readers/writers (rules.xml, rules-deploy.xml). Both
// files preserve elements they do not manage and rebuild the ones they do, so the parse/preserve
// machinery lives here once.

/** The first direct child element with the given tag, or null. */
export const directChild = (parent: Element, tag: string): Element | null =>
    Array.from(parent.children).find(child => child.tagName === tag) ?? null

/** The trimmed text of the first direct child with the given tag, or an empty string when absent. */
export const childValue = (parent: Element, tag: string): string =>
    directChild(parent, tag)?.textContent?.trim() ?? ''

/** The trimmed text of every direct child with the given tag, dropping the blank ones. */
export const childValues = (parent: Element, tag: string): string[] =>
    Array.from(parent.children)
        .filter(child => child.tagName === tag)
        .map(child => child.textContent?.trim() ?? '')
        .filter(Boolean)

/**
 * Parses an XML string and returns its root element, or null when the string is blank, malformed, or
 * rooted at a different element than expected. Callers decide whether a null is "empty" or an error.
 */
export const parseXmlRoot = (xml: string, rootTag: string): Element | null => {
    const trimmed = xml.trim()
    if (!trimmed) {
        return null
    }
    const doc = new DOMParser().parseFromString(trimmed, 'application/xml')
    const root = doc.documentElement
    if (!root || doc.getElementsByTagName('parsererror').length > 0 || root.tagName !== rootTag) {
        return null
    }
    return root
}

/** The children of an element the editor does not manage, each serialized verbatim (no indentation). */
export const unmanagedChildren = (parent: Element, managed: Set<string>): string[] => {
    const serializer = new XMLSerializer()
    return Array.from(parent.children)
        .filter(child => !managed.has(child.tagName))
        .map(child => serializer.serializeToString(child))
}

/**
 * The children of {@link parseXmlRoot}'s result that the editor does not manage, each serialized and
 * indented, ready to be dropped back into the rebuilt document verbatim. A blank or unreadable original
 * preserves nothing.
 */
export const preservedChildren = (xml: string, rootTag: string, managed: Set<string>): string[] => {
    const root = parseXmlRoot(xml, rootTag)
    return root ? unmanagedChildren(root, managed).map(child => `    ${child}`) : []
}

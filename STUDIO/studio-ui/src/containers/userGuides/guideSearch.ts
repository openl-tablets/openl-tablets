import GithubSlugger from 'github-slugger'
import type { Heading, Nodes } from 'mdast'
import { toString } from 'mdast-util-to-string'
import MiniSearch from 'minisearch'
import remarkGfm from 'remark-gfm'
import remarkParse from 'remark-parse'
import { unified } from 'unified'
import { visit } from 'unist-util-visit'
import { withoutFrontMatter } from './frontMatter'

/** A page to read for the index, at its absolute address. */
export interface PageToIndex {
    file: string
    title: string
    url: string
}

/** A page of the guides with its text, to be searched. */
export interface GuidePageText {
    file: string
    title: string
    text: string
}

/** A part of a page a search finds: the text from one heading to the next, or before the first heading. */
export interface GuideSection {
    id: string
    file: string
    /** The title of the page. */
    title: string
    /** The heading the part starts with, empty for the part before the first heading. */
    heading: string
    /** The id of that heading on the page, empty for the part before the first heading. */
    anchor: string
    text: string
}

/** A piece of the text shown for a result, telling whether it is a word the search matched. */
export interface SnippetPart {
    text: string
    match: boolean
    /** Where the piece starts in the text shown; no two pieces of a snippet start at the same place. */
    at: number
}

/** A part of a page matching a search, with the text around the first word it matched. */
export interface GuideSearchResult {
    file: string
    title: string
    heading: string
    anchor: string
    snippet: SnippetPart[]
}

/** The most results a search lists; a reader narrows a search rather than reads past them. */
const RESULT_LIMIT = 50

/** How many characters of a part a result shows. */
const SNIPPET_LENGTH = 160

/** The containers whose children are blocks of their own, so their texts are kept apart by a space. */
const BLOCKS = new Set(['root', 'blockquote', 'list', 'listItem', 'table', 'tableRow', 'footnoteDefinition'])

const parser = unified().use(remarkParse).use(remarkGfm)

/** The text a reader reads: the alt text of an image and the content of code included, a diagram and raw HTML not. */
const plainText = (node: Nodes): string => {
    if (node.type === 'code') {
        return node.lang === 'mermaid' ? '' : node.value
    }
    if (node.type === 'html') {
        return ''
    }
    if (node.type === 'image') {
        return node.alt ?? ''
    }
    if ('value' in node) {
        return node.value
    }
    if (!('children' in node)) {
        return ''
    }
    return node.children.map(plainText).join(BLOCKS.has(node.type) ? ' ' : '')
}

/**
 * The parts of a page, each starting with a heading of the page.
 *
 * A heading gets the id the viewer gives it, from the same text and by the same rules, so a result opens the page
 * right at the part it found.
 */
export const sectionsOf = (page: GuidePageText): GuideSection[] => {
    const tree = parser.parse(withoutFrontMatter(page.text))
    const slugger = new GithubSlugger()
    const ids = new Map<Heading, string>()
    visit(tree, 'heading', (heading: Heading) => {
        ids.set(heading, slugger.slug(toString(heading, { includeImageAlt: false, includeHtml: false })))
    })
    const sections: GuideSection[] = []
    let heading = ''
    let anchor = ''
    let parts: string[] = []
    const close = () => {
        const text = parts.join(' ').replaceAll(/\s+/g, ' ').trim()
        if (text !== '' || heading !== '') {
            sections.push({ id: `${page.file}#${anchor}`, file: page.file, title: page.title, heading, anchor, text })
        }
    }
    for (const node of tree.children) {
        if (node.type === 'heading') {
            close()
            heading = plainText(node)
            anchor = ids.get(node) ?? ''
            parts = []
        } else {
            parts.push(plainText(node))
        }
    }
    close()
    return sections
}

const escaped = (term: string): string => term.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`)

/** The text around the first word a search matched, every matched word marked. */
export const snippetOf = (text: string, terms: string[]): SnippetPart[] => {
    const words = terms.length === 0 ? undefined : new RegExp(terms.map(escaped).sort((a, b) => b.length - a.length).join('|'), 'gi')
    const first = words ? Math.max(0, text.search(words)) : 0
    const start = first < SNIPPET_LENGTH / 2 ? 0 : text.lastIndexOf(' ', first - SNIPPET_LENGTH / 4) + 1
    const end = Math.min(text.length, start + SNIPPET_LENGTH)
    const shown = text.slice(start, end)
    const parts: Omit<SnippetPart, 'at'>[] = []
    let from = 0
    for (const match of words ? shown.matchAll(words) : []) {
        parts.push({ text: shown.slice(from, match.index), match: false }, { text: match[0], match: true })
        from = match.index + match[0].length
    }
    parts.push({ text: shown.slice(from), match: false })
    if (start > 0) {
        parts.unshift({ text: '…', match: false })
    }
    if (end < text.length) {
        parts.push({ text: '…', match: false })
    }
    let at = 0
    return parts.filter(part => part.text !== '').map(part => {
        const placed = { ...part, at }
        at += part.text.length
        return placed
    })
}

/** Reads the pages to index; a page that cannot be read is left out rather than failing the search of the others. */
export const readPages = async (pages: PageToIndex[]): Promise<GuidePageText[]> => {
    const read = await Promise.allSettled(pages.map(async page => {
        const response = await fetch(page.url)
        if (!response.ok) {
            throw new Error(`${page.file}: ${response.status}`)
        }
        return { file: page.file, title: page.title, text: await response.text() }
    }))
    return read.flatMap(result => (result.status === 'fulfilled' ? [result.value] : []))
}

/** The guides, read and indexed, to be searched. */
export interface GuideIndex {
    /**
     * The parts of the pages matching a query, best first.
     *
     * @param scope the folder the pages are searched in, ending with `/`; empty for all of them
     */
    search(query: string, scope: string): GuideSearchResult[]
}

/**
 * Indexes the parts of the given pages for a full-text search.
 *
 * Every word of a query must match, the last one as a prefix, so the results follow typing; a small typo is
 * tolerated. A word in the title of a page ranks above one in a heading, and that above one in the text.
 */
export const indexGuides = (pages: GuidePageText[]): GuideIndex => {
    const index = new MiniSearch<GuideSection>({
        fields: ['title', 'heading', 'text'],
        storeFields: ['file', 'title', 'heading', 'anchor', 'text'],
        searchOptions: { boost: { title: 3, heading: 2 }, fuzzy: 0.2, combineWith: 'AND' },
    })
    index.addAll(pages.flatMap(sectionsOf))
    return {
        search: (query, scope) => index
            .search(query, {
                prefix: (term, i, terms) => i === terms.length - 1,
                filter: result => String(result['file']).startsWith(scope),
            })
            .slice(0, RESULT_LIMIT)
            .map(result => ({
                file: String(result['file']),
                title: String(result['title']),
                heading: String(result['heading']),
                anchor: String(result['anchor']),
                snippet: snippetOf(String(result['text']), result.terms),
            })),
    }
}

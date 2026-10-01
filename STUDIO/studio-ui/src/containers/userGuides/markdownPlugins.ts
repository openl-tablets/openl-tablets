import GithubSlugger from 'github-slugger'
import { toString } from 'hast-util-to-string'
import type { Element, ElementContent, Root } from 'hast'
import bash from 'highlight.js/lib/languages/bash'
import groovy from 'highlight.js/lib/languages/groovy'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import properties from 'highlight.js/lib/languages/properties'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import { createLowlight } from 'lowlight'
import { defaultSchema, type Options as SanitizeSchema } from 'rehype-sanitize'
import { visit } from 'unist-util-visit'

/** The tag a `[!Note]` quote is turned into, which the viewer draws as an alert. */
export const NOTE_TAG = 'aside'

const NOTE_MARKER = /^\s*\[!note\]\s*/i
const HEADING = /^h[1-6]$/

/** Only the languages the guides use, so no other grammar is bundled. */
const lowlight = createLowlight({ bash, groovy, java, json, properties, xml, yaml })

/**
 * What raw HTML of a page is kept: GitHub's rules, plus a YouTube player in an `iframe`.
 *
 * The guides ship with the application, so this is defence in depth rather than a trust boundary.
 */
export const SANITIZE_SCHEMA: SanitizeSchema = {
    ...defaultSchema,
    tagNames: [...(defaultSchema.tagNames ?? []), 'iframe'],
    attributes: {
        ...defaultSchema.attributes,
        iframe: ['src', 'allow', 'allowFullScreen', 'referrerPolicy', 'frameBorder'],
    },
}

const isElement = (node: ElementContent | undefined, tagName: string): node is Element =>
    node?.type === 'element' && node.tagName === tagName

/** The language a code element is written in, by its `language-*` class. */
export const languageOf = (code: Element | undefined): string | undefined => {
    const classes = code?.properties.className
    const language = Array.isArray(classes)
        ? classes.map(String).find(name => name.startsWith('language-'))
        : undefined
    return language?.slice('language-'.length)
}

/** Gives every heading the id GitHub gives it, so a `#heading` of a link finds it. */
export const rehypeHeadingIds = () => (tree: Root) => {
    const slugger = new GithubSlugger()
    visit(tree, 'element', (node: Element) => {
        if (HEADING.test(node.tagName) && !node.properties.id) {
            node.properties.id = slugger.slug(toString(node))
        }
    })
}

/** Turns a quote opening with `[!Note]` into a note, without the marker. */
export const rehypeNoteAlerts = () => (tree: Root) => {
    visit(tree, 'element', (node: Element) => {
        const paragraph = node.tagName === 'blockquote'
            ? node.children.find((child): child is Element => child.type === 'element')
            : undefined
        const marker = isElement(paragraph, 'p') ? paragraph.children[0] : undefined
        if (paragraph && marker?.type === 'text' && NOTE_MARKER.test(marker.value)) {
            marker.value = marker.value.replace(NOTE_MARKER, '')
            if (paragraph.children.every(child => child.type === 'text' && child.value.trim() === '')) {
                node.children = node.children.filter(child => child !== paragraph)
            }
            node.tagName = NOTE_TAG
        }
    })
}

/** Highlights a code block written in one of the languages the guides use; any other stays plain text. */
export const rehypeHighlightCode = () => (tree: Root) => {
    visit(tree, 'element', (node: Element, _index, parent) => {
        const language = languageOf(node)
        if (node.tagName === 'code' && parent?.type === 'element' && parent.tagName === 'pre'
            && language && lowlight.registered(language)) {
            node.children = lowlight.highlight(language, toString(node)).children as ElementContent[]
        }
    })
}

import type { MessageLanguageView, MessageRow, MessageRowWarning, SharedMessageText } from '../../types/localization'
import type { KeyTexts } from './messageEdits'

/** The name every localization file starts and ends with. */
const BASENAME = 'message'
const EXTENSION = '.properties'

/**
 * One language of the messages: a localization file of the project, or one the reader added and has not saved.
 *
 * <p>The language is told by the file name, as the engine tells it: `message.properties` is the default one,
 * `message_de.properties` German, `message_de_LU.properties` German of Luxembourg.
 */
export interface BundleLanguage {
    /** The language, country and variant of the file name joined by `_`; empty for the default file. */
    tag: string
    /** The tags a key missing here is looked for in, nearest first, down to the default file. */
    parents: string[]
    /** Set when the file is not written in UTF-8: its first save writes it whole in UTF-8. */
    legacy: boolean
}

/** The project a text comes from, when the project being read depends on it. */
export interface TextSource {
    /** Its id, when the workspace holds it. */
    id?: string | undefined
    name: string
}

/**
 * The texts of one key as the screen shows them: the texts of the languages of the project, and the texts the projects
 * it depends on give.
 */
export interface ShownTexts {
    /** The text of every language that writes the key; null for a key written without a text. */
    texts: Readonly<Record<string, string | null>>
    /** The texts the projects the project depends on give the key, by the language. */
    shared: Readonly<Record<string, SharedMessageText>>
}

/** A key as the screen holds it, before what the reader did to it. */
export interface KeyRow {
    key: string
    /** The texts of the key as read, null for a key the reader added. */
    base: KeyTexts | null
    /** The texts the projects the project depends on give the key, by the language. */
    shared: Readonly<Record<string, SharedMessageText>>
    /** What the check of the files warns about the texts as read. */
    warnings: readonly MessageRowWarning[]
}

/** A key as the server answers it. */
export const keyRowOf = (row: MessageRow): KeyRow =>
    ({ key: row.key, base: row.values, shared: row.shared ?? {}, warnings: row.warnings ?? []})

/** A key the reader added and has not saved. */
export const addedKeyRow = (key: string): KeyRow => ({ key, base: null, shared: {}, warnings: []})

/** What a cell of the localization table holds for a key in a language. */
export type MessageCell =
    | { kind: 'own', text: string }
    | { kind: 'empty' }
    | { kind: 'inherited', text: string, from: string, project?: TextSource | undefined }
    | { kind: 'missing' }

/** The name of the file of a language: `de_LU` → `message_de_LU.properties`, the default → `message.properties`. */
export const fileOfTag = (tag: string): string => BASENAME + (tag === '' ? '' : '_' + tag) + EXTENSION

/** The tags a message is looked for in when a language lacks it: `de_LU_X` → `de_LU`, `de`, default. */
const parentTags = (tag: string): string[] => {
    const parts = tag === '' ? [] : tag.split('_')
    const parents: string[] = []
    for (let length = parts.length - 1; length > 0; length--) {
        parents.push(parts.slice(0, length).join('_'))
    }
    if (tag !== '') {
        parents.push('')
    }
    return parents
}

/** The names of the languages in each language of the screen, made once: the languages are sorted by them. */
const displayNames = new Map<string, Intl.DisplayNames>()

const namesIn = (uiLocale: string): Intl.DisplayNames => {
    const known = displayNames.get(uiLocale)
    if (known !== undefined) {
        return known
    }
    const names = new Intl.DisplayNames([uiLocale], { type: 'language', languageDisplay: 'standard' })
    displayNames.set(uiLocale, names)
    return names
}

/** Names a language in the language of the screen, as `German (Luxembourg)`. */
export const languageName = (tag: string, uiLocale: string, defaultName: string): string => {
    if (tag === '') {
        return defaultName
    }
    const [language = '', country = '', ...variant] = tag.split('_')
    try {
        const names = namesIn(uiLocale)
        const named = names.of(country === '' ? language : `${language}-${country}`) ?? tag
        return variant.length === 0 ? named : `${named} ${variant.join('_')}`
    } catch {
        // A code no standard knows is named by itself.
        return tag
    }
}

/**
 * The languages as the table shows them: the default one first, then the languages by name, each followed by its
 * regional ones by name.
 */
export const languagesOf = (views: readonly MessageLanguageView[], nameOf: (tag: string) => string): BundleLanguage[] => {
    const base = (tag: string) => tag.split('_')[0] ?? ''
    const compare = (a: BundleLanguage, b: BundleLanguage) => {
        if (a.tag === '' || b.tag === '') {
            return a.tag === '' ? -1 : 1
        }
        const byLanguage = nameOf(base(a.tag)).localeCompare(nameOf(base(b.tag)))
        if (byLanguage !== 0) {
            return byLanguage
        }
        const aRegional = a.tag.includes('_')
        if (aRegional !== b.tag.includes('_')) {
            return aRegional ? 1 : -1
        }
        return nameOf(a.tag).localeCompare(nameOf(b.tag))
    }
    return views
        .map(view => ({ tag: view.tag, parents: parentTags(view.tag), legacy: view.legacy === true }))
        .toSorted(compare)
}

/**
 * The text a language of the project holds for a key, or else the text a dependency gives that language. Null
 * stands for a key the language writes without a text and no dependency gives one: the engine stops looking there
 * and returns the key itself.
 */
const textIn = (shown: ShownTexts, tag: string): { text: string, project?: TextSource } | null | undefined => {
    const own = shown.texts[tag]
    if (typeof own === 'string') {
        return { text: own }
    }
    const shared = shown.shared[tag]
    if (shared !== undefined) {
        return { text: shared.value, project: { id: shared.projectId, name: shared.project } }
    }
    return own === null ? null : undefined
}

/**
 * Whether a language shows the default text of its own project for a key, having no text of its own, of a parent
 * language or of a dependency in the language. A text the default language of a dependency gives is that
 * project's to translate.
 */
export const showsDefaultText = (cell: MessageCell): boolean =>
    cell.kind === 'inherited' && cell.from === '' && cell.project === undefined

/**
 * What a language shows for a key: its own text, or else the text of the nearest language it falls back to, or
 * nothing at all when no language down to the default one has the key — the rule then returns the key itself.
 *
 * <p>A language the project lacks a text in takes it, as the engine does, from the same language of a project it
 * depends on before it falls back to a parent language: German of Luxembourg, then German, then the default one,
 * each of the project first.
 */
export const cellOf = (shown: ShownTexts, language: BundleLanguage): MessageCell => {
    const own = shown.texts[language.tag]
    if (typeof own === 'string') {
        return own === '' ? { kind: 'empty' } : { kind: 'own', text: own }
    }
    for (const tag of [language.tag, ...language.parents]) {
        const found = textIn(shown, tag)
        if (found === null) {
            return { kind: 'missing' }
        }
        if (found !== undefined) {
            return { kind: 'inherited', text: found.text, from: tag, project: found.project }
        }
    }
    return { kind: 'missing' }
}

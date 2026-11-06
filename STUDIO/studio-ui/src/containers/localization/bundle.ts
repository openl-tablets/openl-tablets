import type { DependencyMessages } from '../../services/localization'
import type { MessageFile } from '../../types/localization'
import { parseProperties, type PropertiesEntry } from './propertiesFile'

/** The name every localization file starts and ends with. */
const BASENAME = 'message'
const EXTENSION = '.properties'

/**
 * One language of the messages: a localization file and the language it is written for.
 *
 * <p>The language is told by the file name, as the engine tells it: `message.properties` is the default one,
 * `message_de.properties` German, `message_de_LU.properties` German of Luxembourg.
 */
export interface BundleLanguage {
    /** The language, country and variant of the file name joined by `_`; empty for the default file. */
    tag: string
    file: MessageFile
    /** The tags a key missing here is looked for in, nearest first, down to the default file. */
    parents: string[]
    /** The messages in the order they are written, a key written twice included. */
    entries: PropertiesEntry[]
    /**
     * The text of every key, the last one written winning, as the engine reads it: null for a key written on a line
     * without a separator, which has no text at all.
     */
    texts: Map<string, string | null>
}

/**
 * The messages of a project the project depends on. A rule reads a key the project lacks in a language from them,
 * before it falls back to a parent language.
 */
export interface SharedMessages {
    projectId: string
    projectName: string
    /** The texts of every key by the tag of the language. */
    texts: Map<string, Map<string, string | null>>
}

/** The project a text comes from, when the project being read depends on it. */
export interface TextSource {
    id: string
    name: string
}

/** All languages of the messages, and every key any of them holds. */
export interface Bundle {
    /** The default language first, then the others by name, a regional language next to its parent. */
    languages: BundleLanguage[]
    /** The keys in the order they are written: those of the default file first, then the rest. */
    keys: string[]
    /** The messages of the projects the project depends on, the nearest first. */
    shared: SharedMessages[]
}

/**
 * The text a language holds for a key: a text, null for a key written on a line without a separator, or undefined
 * for a key the language does not write.
 */
export type TextOf = (language: BundleLanguage, key: string) => string | null | undefined

/** What a cell of the localization table holds for a key in a language. */
export type MessageCell =
    | { kind: 'own', text: string }
    | { kind: 'empty' }
    | { kind: 'inherited', text: string, from: string, project?: TextSource | undefined }
    | { kind: 'missing' }

/** The tag of a localization file name, or null for a file of another bundle (`messages.properties`). */
const tagOfFile = (name: string): string | null => {
    const middle = name.slice(BASENAME.length, name.length - EXTENSION.length)
    if (middle === '') {
        return ''
    }
    return middle.startsWith('_') ? middle.slice(1) : null
}

/** The name of the file of a language: `de_LU` → `message_de_LU.properties`, the default → `message.properties`. */
export const fileOfTag = (tag: string): string => `${BASENAME}${tag === '' ? '' : `_${tag}`}${EXTENSION}`

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

/** Names a language in the language of the screen, as `German (Luxembourg)`. */
export const languageName = (tag: string, uiLocale: string, defaultName: string): string => {
    if (tag === '') {
        return defaultName
    }
    const [language = '', country = '', ...variant] = tag.split('_')
    try {
        const names = new Intl.DisplayNames([uiLocale], { type: 'language', languageDisplay: 'standard' })
        const named = names.of(country === '' ? language : `${language}-${country}`) ?? tag
        return variant.length === 0 ? named : `${named} ${variant.join('_')}`
    } catch {
        // A code no standard knows is named by itself.
        return tag
    }
}

/**
 * Orders the languages as the table shows them: the default one first, then the languages by name, each
 * followed by its regional ones by name.
 */
const orderLanguages = (languages: BundleLanguage[], nameOf: (tag: string) => string): BundleLanguage[] => {
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
    return [...languages].sort(compare)
}

/** The languages the files write, each with its texts, a file of another bundle left out. */
const languagesOf = (files: MessageFile[]): BundleLanguage[] => files.flatMap(file => {
    const tag = tagOfFile(file.name)
    if (tag === null) {
        return []
    }
    const entries = parseProperties(file.text)
    const texts = new Map(entries.map(entry => [entry.key, entry.value]))
    return [{ tag, file, parents: parentTags(tag), entries, texts }]
})

/**
 * Reads the files of a project into one bundle: a column per language, a row per key. The messages of the
 * projects it depends on give the texts the project lacks, but no column and no row of their own.
 */
export const buildBundle = (
    files: MessageFile[],
    nameOf: (tag: string) => string,
    dependencies: DependencyMessages[] = []
): Bundle => {
    const ordered = orderLanguages(languagesOf(files), nameOf)
    const keys = new Set<string>()
    ordered.forEach(language => language.entries.forEach(entry => keys.add(entry.key)))
    const shared = dependencies.map(dependency => ({
        projectId: dependency.projectId,
        projectName: dependency.projectName,
        texts: new Map(languagesOf(dependency.files).map(language => [language.tag, language.texts])),
    }))
    return { languages: ordered, keys: [...keys], shared }
}

/** The text the nearest project the project depends on gives a key in a language, and that project. */
export const sharedTextOf = (bundle: Bundle, tag: string, key: string):
    { text: string, project: TextSource } | undefined => {
    for (const dependency of bundle.shared) {
        const text = dependency.texts.get(tag)?.get(key)
        if (typeof text === 'string') {
            return { text, project: { id: dependency.projectId, name: dependency.projectName } }
        }
    }
    return undefined
}

/**
 * The text a language of the project holds for a key, or else the text a dependency gives that language. Null
 * stands for a key the language writes without a text and no dependency gives one: the engine stops looking there
 * and returns the key itself.
 */
const textIn = (
    bundle: Bundle,
    tag: string,
    key: string,
    textOf: TextOf
): { text: string, project?: TextSource } | null | undefined => {
    const language = bundle.languages.find(candidate => candidate.tag === tag)
    const own = language === undefined ? undefined : textOf(language, key)
    if (typeof own === 'string') {
        return { text: own }
    }
    return sharedTextOf(bundle, tag, key) ?? (own === null ? null : undefined)
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
export const cellOf = (
    bundle: Bundle,
    language: BundleLanguage,
    key: string,
    textOf: TextOf
): MessageCell => {
    const own = textOf(language, key)
    if (typeof own === 'string') {
        return own === '' ? { kind: 'empty' } : { kind: 'own', text: own }
    }
    for (const tag of [language.tag, ...language.parents]) {
        const found = textIn(bundle, tag, key, textOf)
        if (found === null) {
            return { kind: 'missing' }
        }
        if (found !== undefined) {
            return { kind: 'inherited', text: found.text, from: tag, project: found.project }
        }
    }
    return { kind: 'missing' }
}

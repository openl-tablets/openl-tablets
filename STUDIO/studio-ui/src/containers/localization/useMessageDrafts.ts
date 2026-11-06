import { useCallback, useMemo, useState } from 'react'
import type { Bundle, BundleLanguage, TextOf } from './bundle'
import { applyEdits } from './propertiesFile'

/** The edits of one language: a key and its new text, or null where the key is taken out of the language. */
type LanguageDrafts = ReadonlyMap<string, string | null>

/** What the reader has written and not saved yet, language by language. */
export interface MessageDrafts {
    /** The text a language holds for a key with the edits applied, or undefined where it holds none. */
    textOf: TextOf
    /** Whether the reader has changed the text of a key in a language. */
    changed: (tag: string, key: string) => boolean
    /** Sets the text of a key in a language; null takes the key out of the language. */
    write: (language: BundleLanguage, key: string, text: string | null) => void
    /** Takes the key out of every language. */
    remove: (key: string) => void
    /** The keys of the bundle with the edits applied: a new key last, a removed one left out. */
    keys: string[]
    dirty: boolean
    /** The files the edits change, with their texts as they are to be saved. */
    edited: () => Array<{ name: string, text: string }>
    discard: () => void
}

/**
 * Keeps what the reader writes into the messages until it is saved.
 *
 * <p>An edit is kept by language and by key, apart from the files: a file read again keeps the edits made on it.
 * An edit that brings a text back to what the file holds is no edit at all.
 */
export const useMessageDrafts = (bundle: Bundle | null): MessageDrafts => {
    const [drafts, setDrafts] = useState<ReadonlyMap<string, LanguageDrafts>>(new Map())

    const textOf = useCallback((language: BundleLanguage, key: string): string | null | undefined => {
        const edits = drafts.get(language.tag)
        if (edits?.has(key)) {
            return edits.get(key) ?? undefined
        }
        return language.texts.get(key)
    }, [drafts])

    const changed = useCallback((tag: string, key: string) => drafts.get(tag)?.has(key) === true, [drafts])

    const write = useCallback((language: BundleLanguage, key: string, text: string | null) => {
        setDrafts(current => {
            const edits = new Map(current.get(language.tag))
            const unchanged = text === null ? !language.texts.has(key) : language.texts.get(key) === text
            if (unchanged) {
                edits.delete(key)
            } else {
                edits.set(key, text)
            }
            const next = new Map(current)
            if (edits.size === 0) {
                next.delete(language.tag)
            } else {
                next.set(language.tag, edits)
            }
            return next
        })
    }, [])

    const remove = useCallback((key: string) => {
        bundle?.languages
            .filter(language => textOf(language, key) !== undefined || language.texts.has(key))
            .forEach(language => write(language, key, null))
    }, [bundle, textOf, write])

    const keys = useMemo(() => {
        if (bundle === null) {
            return []
        }
        const all = new Set(bundle.keys)
        drafts.forEach(edits => edits.forEach((text, key) => {
            if (text !== null) {
                all.add(key)
            }
        }))
        return [...all].filter(key => bundle.languages.some(language => textOf(language, key) !== undefined))
    }, [bundle, drafts, textOf])

    const edited = useCallback(() => (bundle?.languages ?? []).flatMap(language => {
        const edits = drafts.get(language.tag)
        return edits === undefined ? [] : [{ name: language.file.name, text: applyEdits(language.file.text, edits) }]
    }), [bundle, drafts])

    const discard = useCallback(() => setDrafts(new Map()), [])

    return { textOf, changed, write, remove, keys, dirty: drafts.size > 0, edited, discard }
}

import { useCallback, useMemo, useState } from 'react'
import type { LocalizationAction } from '../../types/localization'
import {
    actionsOf,
    addedKeys,
    addedLanguages,
    editedTexts,
    NO_MESSAGE_EDITS,
    redoStep,
    undoStep,
    withStep,
    type KeyTexts,
    type MessageEditBuffer,
    type MessageStep,
} from './messageEdits'

/** What the reader has done to the messages and not saved yet. */
export interface MessageEdits {
    /** The texts of a key as the reader left them, from what it holds as read: null for a key taken out. */
    textsOf: (key: string, base: KeyTexts | null) => KeyTexts | null
    /** Whether the reader changed the text of a key in a language. */
    changed: (key: string, tag: string) => boolean
    setText: (key: string, base: KeyTexts | null, tag: string, text: string) => void
    /** Gives the text of a language up, so the language takes the one it falls back to. */
    removeText: (key: string, base: KeyTexts | null, tag: string) => void
    /** Adds a key with its texts by the language, the default one among them. */
    addKey: (key: string, texts: Readonly<Record<string, string>>) => void
    /** Takes a key out of every language. */
    removeKey: (key: string, base: KeyTexts | null) => void
    addLanguage: (tag: string) => void
    /** The keys added and not taken out again, in the order they were added. */
    addedKeys: string[]
    /** The languages added, in the order they were added. */
    addedLanguages: string[]
    undo: () => void
    redo: () => void
    canUndo: boolean
    canRedo: boolean
    /** Whether saving would change the files. */
    dirty: boolean
    /** The changes the server takes for what the reader did. */
    actions: LocalizationAction[]
    /** Forgets everything, once it is saved or given up. */
    clear: () => void
}

/**
 * Keeps what the reader does to the messages until it is saved: each change a step, taken back one at a time.
 *
 * <p>A step is kept apart from the texts read, so a page read again keeps what was done on it.
 */
export const useMessageEdits = (): MessageEdits => {
    const [buffer, setBuffer] = useState<MessageEditBuffer>(NO_MESSAGE_EDITS)

    // The texts of each key a step touched, as the steps leave them.
    const finals = useMemo(() => new Map([...buffer.bases.entries()]
        .map(([key, base]) => [key, editedTexts(key, base, buffer.steps)] as const)), [buffer])

    const textsOf = useCallback((key: string, base: KeyTexts | null) => (finals.has(key) ? finals.get(key) ?? null : base),
        [finals])

    const changed = useCallback((key: string, tag: string) => finals.has(key)
        && finals.get(key)?.[tag] !== buffer.bases.get(key)?.[tag], [finals, buffer.bases])

    const take = useCallback((step: MessageStep, base: KeyTexts | null) =>
        setBuffer(current => withStep(current, step, base)), [])

    const actions = useMemo(() => actionsOf(buffer), [buffer])
    const setText = useCallback<MessageEdits['setText']>((key, base, tag, text) =>
        take({ kind: 'setText', key, language: tag, text }, base), [take])
    const removeText = useCallback<MessageEdits['removeText']>((key, base, tag) =>
        take({ kind: 'removeText', key, language: tag }, base), [take])
    const addKey = useCallback<MessageEdits['addKey']>((key, texts) => take({ kind: 'addKey', key, texts }, null), [take])
    const removeKey = useCallback<MessageEdits['removeKey']>((key, base) => take({ kind: 'removeKey', key }, base), [take])
    const addLanguage = useCallback<MessageEdits['addLanguage']>(tag =>
        take({ kind: 'addLanguage', language: tag }, null), [take])
    const undo = useCallback(() => setBuffer(undoStep), [])
    const redo = useCallback(() => setBuffer(redoStep), [])
    const clear = useCallback(() => setBuffer(NO_MESSAGE_EDITS), [])

    // One object while nothing changes, so the table of the keys, which hands it to every cell, is not drawn again.
    return useMemo(() => ({
        textsOf,
        changed,
        setText,
        removeText,
        addKey,
        removeKey,
        addLanguage,
        addedKeys: addedKeys(buffer),
        addedLanguages: addedLanguages(buffer.steps),
        undo,
        redo,
        canUndo: buffer.steps.length > 0,
        canRedo: buffer.undone.length > 0,
        dirty: actions.length > 0,
        actions,
        clear,
    }), [textsOf, changed, setText, removeText, addKey, removeKey, addLanguage, buffer, undo, redo, actions, clear])
}

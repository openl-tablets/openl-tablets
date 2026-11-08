import type { LocalizationAction } from '../../types/localization'

/** The texts of a key by the language; null for a key written without a text. */
export type KeyTexts = Readonly<Record<string, string | null>>

/** One thing the reader did to the messages, kept in the order it was done. */
export type MessageStep =
    | { kind: 'setText', key: string, language: string, text: string }
    | { kind: 'removeText', key: string, language: string }
    | { kind: 'addKey', key: string, texts: Readonly<Record<string, string>> }
    | { kind: 'removeKey', key: string }
    | { kind: 'addLanguage', language: string }

/**
 * What the reader has done, what they took back and may do again, and what each key they touched held when they
 * first touched it: null for a key no language had.
 *
 * <p>The steps are replayed over the texts as they were read, so taking one back needs nothing beyond dropping it.
 * They are turned into the changes the server takes only when the reader saves.
 */
export interface MessageEditBuffer {
    steps: readonly MessageStep[]
    undone: readonly MessageStep[]
    bases: ReadonlyMap<string, KeyTexts | null>
}

export const NO_MESSAGE_EDITS: MessageEditBuffer = { steps: [], undone: [], bases: new Map() }

/** Takes a step, keeping what the key held before the first step of it. Doing something anew drops what was undone. */
export const withStep = (buffer: MessageEditBuffer, step: MessageStep, base: KeyTexts | null): MessageEditBuffer => {
    const bases = step.kind === 'addLanguage' || buffer.bases.has(step.key)
        ? buffer.bases
        : new Map(buffer.bases).set(step.key, base)
    return { steps: [...buffer.steps, step], undone: [], bases }
}

/** Takes the last step back. */
export const undoStep = (buffer: MessageEditBuffer): MessageEditBuffer => {
    const last = buffer.steps.at(-1)
    return last === undefined ? buffer : { ...buffer, steps: buffer.steps.slice(0, -1), undone: [...buffer.undone, last]}
}

/** Does the last step taken back again. */
export const redoStep = (buffer: MessageEditBuffer): MessageEditBuffer => {
    const last = buffer.undone.at(-1)
    return last === undefined ? buffer : { ...buffer, steps: [...buffer.steps, last], undone: buffer.undone.slice(0, -1) }
}

/**
 * The texts of a key after the steps, from what it held as read: null for a key taken out, or never had and not
 * added.
 */
export const editedTexts = (
    key: string,
    base: KeyTexts | null,
    steps: readonly MessageStep[]
): KeyTexts | null => {
    let texts: Record<string, string | null> | null = base === null ? null : { ...base }
    for (const step of steps) {
        if (step.kind !== 'addLanguage' && step.key === key) {
            texts = applied(texts, step)
        }
    }
    return texts
}

const applied = (
    texts: Record<string, string | null> | null,
    step: Exclude<MessageStep, { kind: 'addLanguage' }>
): Record<string, string | null> | null => {
    switch (step.kind) {
        case 'setText':
            return { ...texts, [step.language]: step.text }
        case 'removeText': {
            if (texts === null) {
                return null
            }
            const { [step.language]: _, ...rest } = texts
            return rest
        }
        case 'addKey':
            return { ...step.texts }
        case 'removeKey':
            return null
    }
}

/** The languages the reader added, in the order they were added. */
export const addedLanguages = (steps: readonly MessageStep[]): string[] =>
    steps.flatMap(step => (step.kind === 'addLanguage' ? [step.language] : []))

/** The keys the reader added and did not take out again, in the order they were added. */
export const addedKeys = (buffer: MessageEditBuffer): string[] =>
    [...buffer.bases.entries()]
        .filter(([key, base]) => base === null && editedTexts(key, null, buffer.steps) !== null)
        .map(([key]) => key)

/**
 * The changes the server takes for what the reader did: the least of them that leaves the files as the steps leave
 * the texts.
 *
 * <p>Each key touched is told by what it ends as against what it held: a text written back as it was is no change,
 * a key added and taken out again is none either. A new language is created with every text written in it of the keys
 * the files hold; a new key is added with all its texts, those of a new language included, as the server refuses to
 * add a key a language already has. The languages are added first, so the changes after them may name them.
 */
export const actionsOf = (buffer: MessageEditBuffer): LocalizationAction[] => {
    const languages = addedLanguages(buffer.steps)
    const textsOfNew = new Map<string, Record<string, string>>(languages.map(language => [language, {}]))
    const changes = [...buffer.bases.entries()]
        .flatMap(([key, base]) => keyChanges(key, base, editedTexts(key, base, buffer.steps), textsOfNew))
    const created = languages.map(language => {
        const texts = textsOfNew.get(language) ?? {}
        return Object.keys(texts).length === 0
            ? { operation: 'addLanguage' as const, language }
            : { operation: 'addLanguage' as const, language, values: texts }
    })
    return [...created, ...changes]
}

/**
 * The changes of one key, from what it held to what the steps leave of it. The texts of a new key go with the key; a
 * text of a key the files hold in a new language goes with the language, collected in the texts of the new languages.
 */
const keyChanges = (
    key: string,
    base: KeyTexts | null,
    final: KeyTexts | null,
    textsOfNew: ReadonlyMap<string, Record<string, string>>
): LocalizationAction[] => {
    if (final === null) {
        return base === null ? [] : [{ operation: 'removeKey', key }]
    }
    const changes: LocalizationAction[] = []
    const written: Record<string, string> = {}
    for (const language of new Set([...Object.keys(base ?? {}), ...Object.keys(final)])) {
        const after = final[language]
        const ofNew = textsOfNew.get(language)
        if (base === null) {
            writeText(written, language, after)
        } else if (ofNew !== undefined) {
            writeText(ofNew, key, after)
        } else {
            changes.push(...valueChanges(key, language, base[language], after))
        }
    }
    return Object.keys(written).length > 0 ? [...changes, { operation: 'addKey', key, values: written }] : changes
}

/** Writes a text into what a new language or a new key is created with; a key without a text is not written. */
const writeText = (into: Record<string, string>, name: string, text: string | null | undefined) => {
    if (typeof text === 'string') {
        into[name] = text
    }
}

/** The change of the text of a key in a language the files hold: none when the text is as it was. */
const valueChanges = (
    key: string,
    language: string,
    before: string | null | undefined,
    after: string | null | undefined
): LocalizationAction[] => {
    if (after === undefined) {
        return before === undefined ? [] : [{ operation: 'removeValue', key, language }]
    }
    return typeof after === 'string' && after !== before ? [{ operation: 'setValue', key, language, value: after }] : []
}

import { describe, expect, it } from 'vitest'
import {
    actionsOf,
    addedKeys,
    editedTexts,
    NO_MESSAGE_EDITS,
    redoStep,
    undoStep,
    withStep,
    type KeyTexts,
    type MessageEditBuffer,
    type MessageStep,
} from './messageEdits'

const GREETING: KeyTexts = { '': 'Hello', de: 'Hallo' }

/** The steps taken one after another, each on a key that held what the bases say when first touched. */
const take = (steps: Array<[MessageStep, KeyTexts | null]>): MessageEditBuffer =>
    steps.reduce((buffer, [step, base]) => withStep(buffer, step, base), NO_MESSAGE_EDITS)

describe('messageEdits', () => {
    it('replays the steps over the texts as read', () => {
        const buffer = take([
            [{ kind: 'setText', key: 'greeting', language: 'de', text: 'Servus' }, GREETING],
            [{ kind: 'removeText', key: 'greeting', language: '' }, GREETING],
        ])

        expect(editedTexts('greeting', GREETING, buffer.steps)).toEqual({ de: 'Servus' })
        expect(editedTexts('greeting', GREETING, take([[{ kind: 'removeKey', key: 'greeting' }, GREETING]]).steps))
            .toBeNull()
    })

    it('takes a step back and does it again, and drops what was undone once something else is done', () => {
        let buffer = take([
            [{ kind: 'setText', key: 'greeting', language: 'de', text: 'Servus' }, GREETING],
            [{ kind: 'setText', key: 'greeting', language: 'de', text: 'Grüß Gott' }, GREETING],
        ])

        buffer = undoStep(buffer)
        expect(editedTexts('greeting', GREETING, buffer.steps)?.['de']).toBe('Servus')
        buffer = redoStep(buffer)
        expect(editedTexts('greeting', GREETING, buffer.steps)?.['de']).toBe('Grüß Gott')

        buffer = withStep(undoStep(buffer), { kind: 'removeKey', key: 'greeting' }, GREETING)
        expect(buffer.undone).toEqual([])
        expect(redoStep(buffer)).toBe(buffer)
    })

    it('sends the last text of a cell once, and nothing for a text written back as it was', () => {
        const buffer = take([
            [{ kind: 'setText', key: 'greeting', language: 'de', text: 'Servus' }, GREETING],
            [{ kind: 'setText', key: 'greeting', language: 'de', text: 'Grüß Gott' }, GREETING],
            [{ kind: 'setText', key: 'greeting', language: '', text: 'Hi' }, GREETING],
            [{ kind: 'setText', key: 'greeting', language: '', text: 'Hello' }, GREETING],
        ])

        expect(actionsOf(buffer)).toEqual([{ operation: 'setValue', key: 'greeting', language: 'de', value: 'Grüß Gott' }])
    })

    it('adds a key with its texts at once, and sends nothing for one added and taken out again', () => {
        const buffer = take([
            [{ kind: 'addKey', key: 'note', texts: { '': 'A note' } }, null],
            [{ kind: 'setText', key: 'note', language: 'de', text: 'Eine Notiz' }, null],
            [{ kind: 'addKey', key: 'gone', texts: { '': 'Soon gone' } }, null],
            [{ kind: 'removeKey', key: 'gone' }, null],
        ])

        expect(addedKeys(buffer)).toEqual(['note'])
        expect(actionsOf(buffer)).toEqual([{ operation: 'addKey', key: 'note', values: { '': 'A note', de: 'Eine Notiz' } }])
    })

    it('creates a new language with the texts of the keys the files hold, before the new keys with all their texts', () => {
        const buffer = take([
            [{ kind: 'addLanguage', language: 'fr' }, null],
            [{ kind: 'setText', key: 'greeting', language: 'fr', text: 'Bonjour' }, GREETING],
            [{ kind: 'addKey', key: 'note', texts: { '': 'A note' } }, null],
            [{ kind: 'setText', key: 'note', language: 'fr', text: 'Une note' }, null],
            [{ kind: 'addLanguage', language: 'it' }, null],
        ])

        expect(actionsOf(buffer)).toEqual([
            { operation: 'addLanguage', language: 'fr', values: { greeting: 'Bonjour' } },
            { operation: 'addLanguage', language: 'it' },
            // The server refuses to add a key a language already has: the French text of the new key goes with it.
            { operation: 'addKey', key: 'note', values: { '': 'A note', fr: 'Une note' } },
        ])
    })

    it('removes a key the files have, and the texts given up for the inherited ones', () => {
        const buffer = take([
            [{ kind: 'removeKey', key: 'greeting' }, GREETING],
            [{ kind: 'removeText', key: 'farewell', language: 'de' }, { '': 'Bye', de: 'Tschüss' }],
        ])

        expect(actionsOf(buffer)).toEqual([
            { operation: 'removeKey', key: 'greeting' },
            { operation: 'removeValue', key: 'farewell', language: 'de' },
        ])
    })
})

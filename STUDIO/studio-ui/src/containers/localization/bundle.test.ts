import { describe, expect, it } from 'vitest'
import {
    addedKeyRow,
    cellOf,
    fileOfTag,
    keyRowOf,
    languageName,
    languagesOf,
    showsDefaultText,
    type ShownTexts,
} from './bundle'

const NAMES: Record<string, string> = {
    de: 'German', de_AT: 'German (Austria)', de_LU: 'German (Luxembourg)', fr: 'French', zh_TW: 'Chinese (Taiwan)', zh: 'Chinese',
}
const nameOf = (tag: string) => NAMES[tag] ?? tag
const [defaultLanguage, , german, germanOfLuxembourg] = languagesOf(
    [{ tag: 'de_LU' }, { tag: 'fr' }, { tag: 'de' }, { tag: '' }], nameOf)

describe('bundle', () => {
    it('names the file of a language', () => {
        expect(fileOfTag('')).toBe('message.properties')
        expect(fileOfTag('de_LU')).toBe('message_de_LU.properties')
    })

    it('lists the default language first, then the languages by name, each followed by its regional ones', () => {
        const languages = languagesOf(
            [{ tag: 'zh_TW' }, { tag: 'fr' }, { tag: 'de_LU' }, { tag: 'de_AT' }, { tag: '', legacy: true }, { tag: 'de' }],
            nameOf)

        expect(languages.map(language => language.tag)).toEqual(['', 'zh_TW', 'fr', 'de', 'de_AT', 'de_LU'])
        expect(languages[0]?.legacy).toBe(true)
        expect(languages[1]?.legacy).toBe(false)
    })

    it('falls a regional language back to its language, then to the default one', () => {
        expect(defaultLanguage?.parents).toEqual([])
        expect(german?.parents).toEqual([''])
        expect(germanOfLuxembourg?.parents).toEqual(['de', ''])
    })

    it('names a language in the language of the screen, and a code no standard knows by itself', () => {
        expect(languageName('', 'en', 'Default')).toBe('Default')
        expect(languageName('de_LU', 'en', 'Default')).toBe('German (Luxembourg)')
        expect(languageName('de_DE_POSIX', 'en', 'Default')).toBe('German (Germany) POSIX')
        expect(languageName('not a code', 'en', 'Default')).toBe('not a code')
    })

    it('holds a key as the server answers it, and one the reader added', () => {
        expect(keyRowOf({ key: 'total', values: { '': 'Total' } }))
            .toEqual({ key: 'total', base: { '': 'Total' }, shared: {}, warnings: []})
        expect(addedKeyRow('note')).toEqual({ key: 'note', base: null, shared: {}, warnings: []})
    })

    describe('cellOf', () => {
        const shown = (texts: ShownTexts['texts'], shared: ShownTexts['shared'] = {}): ShownTexts => ({ texts, shared })

        it('shows the text of the language itself, an empty one as empty', () => {
            expect(cellOf(shown({ '': 'Hi', de: 'Hallo' }), german!)).toEqual({ kind: 'own', text: 'Hallo' })
            expect(cellOf(shown({ '': 'Hi', de: '' }), german!)).toEqual({ kind: 'empty' })
        })

        it('falls back to the nearest language that has the key', () => {
            const cell = cellOf(shown({ '': 'Hi', de: 'Hallo' }), germanOfLuxembourg!)

            expect(cell).toEqual({ kind: 'inherited', text: 'Hallo', from: 'de', project: undefined })
            expect(showsDefaultText(cell)).toBe(false)
            expect(showsDefaultText(cellOf(shown({ '': 'Hi' }), german!))).toBe(true)
        })

        it('takes the text a dependency gives the language before a parent language of the project', () => {
            const dependency = { value: 'Servus', project: 'Base', projectId: 'B1' }
            const cell = cellOf(shown({ '': 'Hi', de: 'Hallo' }, { de_LU: dependency }), germanOfLuxembourg!)

            expect(cell).toEqual({ kind: 'inherited', text: 'Servus', from: 'de_LU', project: { id: 'B1', name: 'Base' } })
        })

        it('does not count the default text of a dependency as the default text of the project', () => {
            const cell = cellOf(shown({}, { '': { value: 'Hi', project: 'Base' } }), german!)

            expect(cell).toEqual({ kind: 'inherited', text: 'Hi', from: '', project: { id: undefined, name: 'Base' } })
            expect(showsDefaultText(cell)).toBe(false)
        })

        it('stops at a key written without a text: the rule returns the key itself', () => {
            expect(cellOf(shown({ '': 'Hi', de: null }), germanOfLuxembourg!)).toEqual({ kind: 'missing' })
            expect(cellOf(shown({}), german!)).toEqual({ kind: 'missing' })
        })
    })
})

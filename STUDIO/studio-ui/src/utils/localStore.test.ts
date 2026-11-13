import { beforeEach, describe, expect, it, vi } from 'vitest'
import { booleanText, jsonText, numberBetween, oneOf, plainText, storedSetting } from './localStore'

const isPair = (value: unknown): value is [string, string] =>
    Array.isArray(value) && value.length === 2 && value.every(item => typeof item === 'string')

describe('storedSetting', () => {
    beforeEach(() => localStorage.clear())

    it('gives the default while nothing is stored', () => {
        expect(storedSetting('k', 'standard', plainText).read()).toBe('standard')
    })

    it('stores a choice other than the default, and forgets the default', () => {
        const setting = storedSetting('k', false, booleanText)

        setting.write(true)
        expect(localStorage.getItem('k')).toBe('true')
        expect(setting.read()).toBe(true)

        setting.write(false)
        expect(localStorage.getItem('k')).toBeNull()
        expect(setting.read()).toBe(false)
    })

    it('gives the default for a text that stands for no value', () => {
        const read = <T>(text: string, defaultValue: T, codec: Parameters<typeof storedSetting<T>>[2]): T => {
            localStorage.setItem('k', text)
            return storedSetting('k', defaultValue, codec).read()
        }

        expect(read('yes', false, booleanText)).toBe(false)
        expect(read('cards', 'list', oneOf(['list', 'grid']))).toBe('list')
        for (const text of ['900', '', ' ', '300.5', 'wide']) {
            expect(read(text, 256, numberBetween(200, 720))).toBe(256)
        }
        expect(read('420', 256, numberBetween(200, 720))).toBe(420)
        for (const text of ['not json', '[1, 2]', '["a"]']) {
            expect(read<[string, string]>(text, ['a', 'b'], jsonText(isPair))).toEqual(['a', 'b'])
        }
    })

    it('keeps a value as JSON and forgets one equal to the default', () => {
        const setting = storedSetting<[string, string]>('k', ['a', ''], jsonText(isPair))

        setting.write(['a', 'b'])
        expect(localStorage.getItem('k')).toBe('["a","b"]')
        expect(setting.read()).toEqual(['a', 'b'])

        setting.write(['a', ''])
        expect(localStorage.getItem('k')).toBeNull()
    })

    it('neither reads nor writes in a browser that refuses storage, giving the default', () => {
        vi.stubGlobal('localStorage', {
            getItem: () => { throw new Error('denied') },
            setItem: () => { throw new Error('denied') },
            removeItem: () => { throw new Error('denied') },
        })
        try {
            const setting = storedSetting('k', 'standard', plainText)
            setting.write('dracula')
            expect(setting.read()).toBe('standard')
        } finally {
            vi.unstubAllGlobals()
        }
    })
})

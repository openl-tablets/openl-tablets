import { describe, expect, it } from 'vitest'
import { decodeText } from './decodeText'

/** The bytes of a text written in ISO-8859-1: one byte for each character. */
const latin1 = (text: string): ArrayBuffer => Uint8Array.from(text, character => character.charCodeAt(0)).buffer

describe('decodeText', () => {
    it('reads a file written in UTF-8 as written', () => {
        expect(decodeText(new TextEncoder().encode('Prämie=1\n').buffer)).toBe('Prämie=1\n')
        expect(decodeText(new ArrayBuffer(0))).toBe('')
    })

    it('reads a file in another encoding with every byte a character of its own', () => {
        expect(decodeText(latin1('Prämie=1\n'))).toBe('Prämie=1\n')
        // Lines that differ in a single letter still differ, as they would not with the replacement character.
        expect(decodeText(latin1('Größe'))).not.toBe(decodeText(latin1('Grüße')))
    })
})

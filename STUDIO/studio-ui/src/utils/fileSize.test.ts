import { describe, expect, it } from 'vitest'
import { formatFileSize } from './fileSize'

describe('formatFileSize', () => {
    it('keeps one decimal under ten kilobytes and none above', () => {
        expect(formatFileSize(512, 'en')).toBe('0.5 kB')
        expect(formatFileSize(12_800, 'en')).toBe('13 kB')
    })

    it('writes the size in the UI language', () => {
        expect(formatFileSize(1536, 'de')).toBe('1,5 kB')
    })

    it('gives no text without a size', () => {
        expect(formatFileSize(undefined, 'en')).toBeNull()
    })
})

import { describe, expect, it } from 'vitest'
import { isWorkbookPath, WORKBOOK_ACCEPT } from './workbooks'

describe('isWorkbookPath', () => {
    it('reads the three extensions the engine reads, whatever case they are written in', () => {
        expect(isWorkbookPath('rules/Algorithms.xlsx')).toBe(true)
        expect(isWorkbookPath('rules/Algorithms.XLS')).toBe(true)
        expect(isWorkbookPath('rules/Algorithms.xlsm')).toBe(true)
    })

    it('reads no workbook out of anything else', () => {
        expect(isWorkbookPath('rules/Alg.txt')).toBe(false)
        expect(isWorkbookPath('rules/Alg')).toBe(false)
        expect(isWorkbookPath('')).toBe(false)
        // The extension ends the name; a folder called like one does not make its contents workbooks.
        expect(isWorkbookPath('rules.xlsx/Alg.txt')).toBe(false)
    })

    it('reads no workbook out of the lock file Excel leaves beside an open one', () => {
        expect(isWorkbookPath('rules/~$Algorithms.xlsx')).toBe(false)
        // A path written on Windows ends in a file name just the same.
        expect(isWorkbookPath('rules\\~$Algorithms.xlsx')).toBe(false)
        expect(isWorkbookPath('rules\\Algorithms.xlsx')).toBe(true)
    })

    it('offers the extensions to a file picker', () => {
        expect(WORKBOOK_ACCEPT).toBe('.xlsx,.xls,.xlsm')
    })
})

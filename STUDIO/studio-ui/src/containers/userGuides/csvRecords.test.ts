import { csvRecords } from './csvRecords'

const values = (text: string) => csvRecords(text).map(row => row.cells.map(cell => cell.value))

describe('csvRecords', () => {
    it('reads values separated by commas, without the spaces around them', () => {
        expect(values('Name, Value\none,1\n')).toEqual([['Name', 'Value'], ['one', '1']])
    })

    it('reads a quoted value literally, a comma, a doubled quote and a line break included', () => {
        const rows = csvRecords('"a, b","say ""hi""", "<" ,"two\nlines"\nnext')

        expect(rows.map(row => row.line)).toEqual([0, 2])
        expect(rows[0]?.cells).toEqual([
            { value: 'a, b', quoted: true },
            { value: 'say "hi"', quoted: true },
            { value: '<', quoted: true },
            { value: 'two\nlines', quoted: true },
        ])
    })

    it('skips blank lines and keeps empty values, a comma closing the text included', () => {
        const rows = csvRecords('a,,\n\n  \n,b,')

        expect(rows.map(row => row.line)).toEqual([0, 3])
        expect(rows.map(row => row.cells.map(cell => cell.value))).toEqual([['a', '', ''], ['', 'b', '']])
    })

    it('reads a malformed value as well as it can', () => {
        expect(values('"done" here,y\n"open,\nz')).toEqual([['done', 'y'], ['open,\nz']])
    })
})

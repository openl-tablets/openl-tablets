import { describe, expect, it } from 'vitest'
import type { RawTableCellBorderSide, RawTableRegion } from 'types/tables'
import { cellRoleOf, onPaper, placeOf, regionAt, roleLinesOf, rolesOf, type RoleLines, tablePlaceOf } from './tableRoles'

const line = (color: string): RawTableCellBorderSide => ({ color, style: 'solid', width: 1 })

const LINES: RoleLines = {
    role: line('role'),
    group: line('group'),
    resultBelow: { color: 'below', style: 'double', width: 3 },
}

/** A Spreadsheet of two steps, written from C3: its header, its properties, the titles, the steps and the result. */
const REGIONS: RawTableRegion[] = [
    { role: 'signature', top: 0, left: 0, bottom: 0, right: 1 },
    { role: 'properties', top: 1, left: 0, bottom: 2, right: 1 },
    { role: 'inputTitle', top: 3, left: 0, bottom: 3, right: 1 },
    { role: 'key', top: 4, left: 0, bottom: 5, right: 0 },
    { role: 'value', type: 'number', top: 4, left: 1, bottom: 4, right: 1 },
    { role: 'result', type: 'number', top: 5, left: 1, bottom: 5, right: 1 },
]

const spreadsheet = rolesOf({ pos: 'C3:D8', regions: REGIONS, groups: { rows: [5], columns: [1]} })

describe('tableRoles', () => {
    it('reads a cell named in A1 notation, and nothing else', () => {
        expect(placeOf('A1')).toEqual({ row: 0, column: 0 })
        expect(placeOf('AB12')).toEqual({ row: 11, column: 27 })
        expect(placeOf('a1')).toBeUndefined()
        expect(placeOf('Sheet1!A1')).toBeUndefined()
        expect(placeOf(undefined)).toBeUndefined()
    })

    it('reads no roles for a table read with none, or with no place on its sheet', () => {
        expect(rolesOf({ pos: 'C3:D8' })).toBeUndefined()
        expect(rolesOf({ pos: 'C3:D8', regions: []})).toBeUndefined()
        expect(rolesOf({ regions: REGIONS })).toBeUndefined()
    })

    it('finds the region of a cell by its address, wherever the window of rows it is drawn in starts', () => {
        expect(spreadsheet).toBeDefined()
        const roles = spreadsheet!

        expect(tablePlaceOf(roles, 'D7')).toEqual({ row: 4, column: 1 })
        expect(cellRoleOf(roles, 'C3', undefined)?.region.role).toBe('signature')
        expect(cellRoleOf(roles, 'D7', undefined)?.region.role).toBe('value')
        // A cell outside every region, and a cell with no address, take no role.
        expect(cellRoleOf(roles, 'E3', undefined)).toBeUndefined()
        expect(cellRoleOf(roles, undefined, undefined)).toBeUndefined()
    })

    it('gives a cell the type of its region unless it names its own', () => {
        const roles = spreadsheet!

        expect(cellRoleOf(roles, 'D7', undefined)?.type).toBe('number')
        expect(cellRoleOf(roles, 'D7', 'string')?.type).toBe('string')
        expect(cellRoleOf(roles, 'C7', undefined)?.type).toBeUndefined()
    })

    it('finds a region of a tall table among the rows far from its start', () => {
        const tall = rolesOf({
            pos: 'A1:B500',
            regions: [
                { role: 'signature', top: 0, left: 0, bottom: 0, right: 1 },
                { role: 'value', type: 'string', top: 1, left: 0, bottom: 499, right: 0 },
                { role: 'output', type: 'number', top: 1, left: 1, bottom: 499, right: 1 },
            ],
        })!

        expect(regionAt(tall, { row: 450, column: 1 })?.role).toBe('output')
        expect(regionAt(tall, { row: 0, column: 1 })?.role).toBe('signature')
        expect(regionAt(tall, { row: 500, column: 0 })).toBeUndefined()
        expect(tall.bottom).toBe(499)
    })

    it('draws the line of a role around the header and the properties, and above the titles', () => {
        const roles = spreadsheet!
        const linesAt = (address: string, rows = 1) =>
            roleLinesOf(roles, cellRoleOf(roles, address, undefined)!, { rows, columns: 1 }, LINES)

        expect(linesAt('C3')).toEqual({ top: LINES.role, bottom: LINES.role })
        // The properties take two rows: the lines go above the first of them and below the last.
        expect(linesAt('C4')).toEqual({ top: LINES.role })
        expect(linesAt('C5')).toEqual({ bottom: LINES.role })
        // A merged cell over both rows of the properties draws both.
        expect(linesAt('C4', 2)).toEqual({ top: LINES.role, bottom: LINES.role })
        expect(linesAt('C6')).toEqual({ top: LINES.role })
    })

    it('draws the line of a role above the first row of each region of the titles of what a table gives', () => {
        // A lookup sends a region for each row of its horizontal values; a transposed test one for all its results.
        const titles = (outputs: RawTableRegion[]) => rolesOf({
            pos: 'B2:D5',
            regions: [
                { role: 'signature', top: 0, left: 0, bottom: 0, right: 2 },
                { role: 'inputTitle', top: 1, left: 0, bottom: 2, right: 0 },
                ...outputs,
                { role: 'output', top: 3, left: 1, bottom: 3, right: 2 },
                { role: 'value', top: 3, left: 0, bottom: 3, right: 0 },
            ],
            signature: [],
        })!
        const lookup = titles([
            { role: 'outputTitle', top: 1, left: 1, bottom: 1, right: 2 },
            { role: 'outputTitle', top: 2, left: 1, bottom: 2, right: 2 },
        ])
        const whole = titles([{ role: 'outputTitle', top: 1, left: 1, bottom: 2, right: 2 }])
        const linesAt = (roles: typeof lookup, address: string, rows = 1) =>
            roleLinesOf(roles, cellRoleOf(roles, address, undefined)!, { rows, columns: 1 }, LINES)

        expect(linesAt(lookup, 'C3')).toEqual({ top: LINES.role })
        expect(linesAt(lookup, 'C4')).toEqual({ top: LINES.role })
        expect(linesAt(whole, 'C3')).toEqual({ top: LINES.role })
        expect(linesAt(whole, 'C4')).toBeUndefined()
        // The title of a condition merged down its rows has its line above it only.
        expect(linesAt(lookup, 'B3', 2)).toEqual({ top: LINES.role })
    })

    it('draws the result between its own lines, the double one closing the table', () => {
        const roles = spreadsheet!

        expect(roleLinesOf(roles, cellRoleOf(roles, 'D8', undefined)!, { rows: 1, columns: 1 }, LINES))
            .toEqual({ top: LINES.role, bottom: LINES.resultBelow, left: LINES.group })
    })

    it('draws a group line above the first row of a group and left of the first column of one', () => {
        const roles = spreadsheet!

        // The key of the last step starts a group of rows, and lies on the last row of the table.
        expect(roleLinesOf(roles, cellRoleOf(roles, 'C8', undefined)!, { rows: 1, columns: 1 }, LINES))
            .toEqual({ top: LINES.group, bottom: LINES.role })
        // The value of the first step starts a group of columns.
        expect(roleLinesOf(roles, cellRoleOf(roles, 'D7', undefined)!, { rows: 1, columns: 1 }, LINES))
            .toEqual({ left: LINES.group })
        expect(roleLinesOf(roles, cellRoleOf(roles, 'C7', undefined)!, { rows: 1, columns: 1 }, LINES))
            .toBeUndefined()
    })

    it('draws a table on the paper of its workbook while it has no roles or Excel Formatting is on', () => {
        expect(onPaper(undefined, false)).toBe(true)
        expect(onPaper(spreadsheet, true)).toBe(true)
        expect(onPaper(spreadsheet, false)).toBe(false)
    })
})

import { describe, expect, it } from 'vitest'
import { buildFileChangeMap, normalizeProjectFileChanges } from './fileChanges'

describe('normalizeProjectFileChanges', () => {
    it('reads paths with forward slashes and without the slashes around them', () => {
        expect(normalizeProjectFileChanges([
            { path: '\\rules\\Main.xlsx\\', type: 'modified' },
            { path: '//a//b///', type: 'added' },
            { path: '///', type: 'deleted' },
        ])).toEqual([
            { path: 'rules/Main.xlsx', type: 'modified' },
            { path: 'a//b', type: 'added' },
        ])
    })

    it('reads paths from the project folder and keeps the first change of a path', () => {
        expect(normalizeProjectFileChanges([
            { path: '/projects/Alpha/rules/Main.xlsx', type: 'modified' },
            { path: 'Alpha/rules/Main.xlsx', type: 'deleted' },
            { path: 'projects/Alpha', type: 'added' },
        ], '/projects/', 'Alpha')).toEqual([
            { path: 'rules/Main.xlsx', type: 'modified' },
            { path: 'Alpha', type: 'added' },
        ])
    })
})

describe('buildFileChangeMap', () => {
    it('answers a change by its full path and by its path in the project', () => {
        const changes = buildFileChangeMap([{ path: 'projects/Alpha/rules.xlsx/', type: 'added' }], 'projects', 'Alpha')

        expect(changes.get('projects/Alpha/rules.xlsx')).toBe('added')
        expect(changes.get('rules.xlsx')).toBe('added')
        expect(changes.size).toBe(2)
    })
})

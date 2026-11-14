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
    it('names each change once, by its path in the project', () => {
        // A deleted file is listed in the tree from these names: one under the repository path would list it twice.
        const changes = buildFileChangeMap([
            { path: 'projects/Alpha/i18n/message_fr.properties/', type: 'deleted' },
            { path: 'Alpha/i18n/message_fr.properties', type: 'deleted' },
        ], 'projects', 'Alpha')

        expect([...changes]).toEqual([['i18n/message_fr.properties', 'deleted']])
    })
})

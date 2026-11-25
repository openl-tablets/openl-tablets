import { describe, expect, it } from 'vitest'
import { structureOf } from './projectStructure'

describe('structureOf', () => {
    it('takes the folders of the engine defaults when rules.xml declares nothing', () => {
        expect([...structureOf(undefined)]).toEqual([
            ['rules', 'rules'],
            ['tests', 'tests'],
            ['groovy', 'sources'],
            ['lib', 'libraries'],
            ['i18n', 'messages'],
        ])
        expect(structureOf({ modules: [], sources: [] })).toEqual(structureOf(undefined))
    })

    it('follows the module locations rules.xml declares, by the folder each starts from', () => {
        const structure = structureOf({
            modules: [
                { name: 'Main', path: 'model/Main.xlsx' },
                // A pattern stands for its folder, not for the folders of the modules it matched.
                { path: 'logic/**/*.xlsx', modules: [{ name: 'Deep', path: 'logic/a/b/Deep.xlsx' }] },
                { name: 'Checks', path: './checks/test/Checks.xlsx' },
                { name: 'Root', path: 'Root.xlsx' },
            ],
            sources: ['groovy/'],
        })

        expect(structure.get('model')).toBe('rules')
        expect(structure.get('logic')).toBe('rules')
        expect(structure.has('logic/a/b')).toBe(false)
        expect(structure.get('checks/test')).toBe('tests')
        // The defaults are gone: the project keeps its rules elsewhere.
        expect(structure.has('rules')).toBe(false)
        expect(structure.has('tests')).toBe(false)
        // The root of the project plays no part.
        expect(structure.has('')).toBe(false)
    })

    it('follows the classpath rules.xml declares: folders of code, and folders of jars', () => {
        const structure = structureOf({
            modules: [{ path: 'rules/*.xlsx' }],
            sources: ['scripts', 'classes/', 'vendor/*.jar', 'ext\\drivers\\db.jar', 'jars/**/*.jar'],
        })

        expect(structure.get('scripts')).toBe('sources')
        expect(structure.get('classes')).toBe('sources')
        expect(structure.get('vendor')).toBe('libraries')
        expect(structure.get('ext/drivers')).toBe('libraries')
        expect(structure.get('jars')).toBe('libraries')
        expect(structure.has('groovy')).toBe(false)
        expect(structure.has('lib')).toBe(false)
        expect(structure.get('i18n')).toBe('messages')
    })

    it('keeps the first part found for a folder', () => {
        const structure = structureOf({ modules: [{ path: 'shared/*.xlsx' }], sources: ['shared/'] })

        expect(structure.get('shared')).toBe('rules')
    })
})

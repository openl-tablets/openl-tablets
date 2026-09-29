import { describe, expect, it } from 'vitest'
import { makesOneModule, namesOneModule } from './openApiModules'

describe('namesOneModule', () => {
    it('takes names that differ only in letter case or the spaces around them for one', () => {
        expect(namesOneModule('Models', 'Models')).toBe(true)
        expect(namesOneModule('Models', 'models')).toBe(true)
        expect(namesOneModule(' Models', 'MODELS ')).toBe(true)
    })

    it('tells two names apart, and a name not given yet from any', () => {
        expect(namesOneModule('Algorithms', 'Models')).toBe(false)
        expect(namesOneModule('', '')).toBe(false)
        expect(namesOneModule(undefined, 'Models')).toBe(false)
    })
})

describe('makesOneModule', () => {
    const oneName = { mode: 'GENERATION', algorithmModuleName: 'Models', modelModuleName: 'models' } as const

    it('asks only settings that generate the tables', () => {
        expect(makesOneModule(undefined, oneName)).toBe(true)
        // Reconciled against, or naming no mode, the specification generates nothing and names no module to write.
        expect(makesOneModule(undefined, { mode: 'RECONCILIATION', algorithmModuleName: 'Models', modelModuleName: 'Models' }))
            .toBe(false)
        expect(makesOneModule(undefined, { algorithmModuleName: 'Models', modelModuleName: 'Models' })).toBe(false)
        expect(makesOneModule(undefined, undefined)).toBe(false)
    })

    it('leaves names the saved settings already made one', () => {
        expect(makesOneModule(oneName, oneName)).toBe(false)
        expect(makesOneModule({ ...oneName, modelModuleName: 'Types' }, oneName)).toBe(true)
    })
})

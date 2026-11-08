import { describe, expect, it } from 'vitest'
import { LOCALIZATION_ITEM, switchedRoute, switcherItems } from './moduleSwitcher'

describe('moduleSwitcher', () => {
    it('offers the modules, then the messages of the project', () => {
        const items = switcherItems([{ name: 'Main' }, { name: 'Rates' }] as never, 'Localization')

        expect(items?.map(item => item.key)).toEqual(['Main', 'Rates', LOCALIZATION_ITEM])
        expect(items?.at(-1)).toMatchObject({ search: 'Localization', divided: true })
        expect(switcherItems(null, 'Localization')).toBeNull()
    })

    it('leads to the messages or to the module picked', () => {
        expect(switchedRoute('p1', LOCALIZATION_ITEM)).toContain('/projects/p1/')
        expect(switchedRoute('p1', LOCALIZATION_ITEM)).toContain('localization')
        expect(switchedRoute('p1', 'Main')).not.toContain('localization')
    })
})

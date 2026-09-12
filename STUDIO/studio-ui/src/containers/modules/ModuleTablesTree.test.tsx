import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { ModuleTablesTree } from './ModuleTablesTree'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../store', () => ({ useUserStore: () => undefined }))

const tables: ModuleTable[] = [
    { id: 'one', name: 'Greeting', kind: 'Rules', tableType: 'SimpleRules', sheet: 'Rules' } as ModuleTable,
]

const modules = [{ name: 'Claims' }, { name: 'Pricing' }]

const rail = (compiling: boolean, onSelectModule = vi.fn()) => {
    render(
        <ModuleTablesTree
            compiling={compiling}
            currentModule="Claims"
            modules={modules}
            onSelectModule={onSelectModule}
            onSelectTable={vi.fn()}
            tables={tables}
        />
    )
    return onSelectModule
}

describe('ModuleTablesTree', () => {
    beforeEach(() => localStorage.clear())

    it('takes the width it was dragged to, so a long name can be made room for', () => {
        localStorage.setItem('openl.module.rail.width', '420')

        rail(false)

        expect(screen.getByTestId('module-rail')).toHaveStyle({ width: '420px' })
        expect(screen.getByTestId('module-rail-resizer')).toBeInTheDocument()
    })

    it('opens another module when nothing is being compiled', async () => {
        const onSelectModule = rail(false)

        await userEvent.click(screen.getByText('browser.module.rail_modules'))
        await userEvent.click(screen.getByText('Pricing'))

        expect(onSelectModule).toHaveBeenCalledWith('Pricing')
    })

    it('keeps the other modules shut while this one compiles, and says why', async () => {
        const onSelectModule = rail(true)

        await userEvent.click(screen.getByText('browser.module.rail_modules'))

        expect(screen.getByTestId('module-rail-compiling')).toHaveTextContent('browser.module.switch_blocked')
        await userEvent.click(screen.getByText('Pricing'))
        // A session compiles one module at a time; asking for another would only queue behind this one.
        expect(onSelectModule).not.toHaveBeenCalled()
    })
})

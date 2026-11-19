import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import MainMenu from './MainMenu'
import { PermissionContext } from '../contexts'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => ({
    ...await vi.importActual('react-router'),
    useNavigate: () => mockNavigate,
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ t, i18n }) }
})

const renderMenu = (admin: boolean) => render(
    <PermissionContext.Provider value={{ hasAdminPermission: () => admin }}>
        <MemoryRouter initialEntries={['/administration/user/profile']}>
            <MainMenu />
        </MemoryRouter>
    </PermissionContext.Provider>
)

/** The names of the items, once the menu has laid them out. */
const itemNames = async () => (await screen.findAllByRole('menuitem')).map(item => item.textContent)

describe('MainMenu', () => {
    beforeEach(() => vi.clearAllMocks())

    it('offers the Maven repository to an administrator right after Mail', async () => {
        renderMenu(true)

        const names = await itemNames()
        expect(names.indexOf('common:menu.maven')).toBe(names.indexOf('common:menu.mail') + 1)

        await userEvent.click(screen.getByRole('menuitem', { name: /common:menu.maven/ }))
        expect(mockNavigate).toHaveBeenCalledWith('/administration/maven')
    })

    it('hides the Maven repository from a user who is no administrator', async () => {
        renderMenu(false)

        expect(await itemNames()).not.toContain('common:menu.maven')
    })
})

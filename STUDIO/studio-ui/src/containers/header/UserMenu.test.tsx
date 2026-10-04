import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserMenu } from './UserMenu'
import { PermissionContext, SystemContext } from '../../contexts'

const { aboutModal, appNavigate } = vi.hoisted(() => ({
    aboutModal: vi.fn(),
    appNavigate: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('../../hooks', () => ({ useAppNavigate: () => appNavigate }))
vi.mock('store', async importOriginal => ({
    ...await importOriginal<typeof import('store')>(),
    useUserStore: (select: (state: unknown) => unknown) =>
        select({ userProfile: { username: 'jdoe', email: 'jdoe@example.com' } }),
}))

vi.mock('./AboutModal', () => ({
    AboutModal: (props: { open: boolean, onClose: () => void }) => {
        aboutModal(props)
        return props.open ? <button data-testid="about-modal" onClick={props.onClose} type="button" /> : null
    },
}))

const renderMenu = ({ admin = false, userMode = true } = {}) => {
    const onClose = vi.fn()
    render(
        <PermissionContext.Provider value={{ hasAdminPermission: () => admin }}>
            <SystemContext.Provider
                value={{
                    appVersion: '6.5.0',
                    systemSettings: { userMode } as never,
                    isExternalAuthSystem: false,
                    isUserManagementEnabled: false,
                    isGroupsManagementEnabled: false,
                    isPersonalAccessTokenEnabled: false,
                }}
            >
                <UserMenu isOpen onClose={onClose} />
            </SystemContext.Provider>
        </PermissionContext.Provider>
    )
    return { onClose }
}

const menuItems = async () => (await screen.findAllByRole('menuitem')).map(item => item.textContent)

describe('UserMenu', () => {
    it('offers About right under Help', async () => {
        renderMenu()

        expect(await menuItems()).toEqual([
            'common:user_menu.my_profile',
            'common:user_menu.my_settings',
            'common:user_menu.help',
            'common:user_menu.about',
            'common:user_menu.sign_out',
        ])
    })

    it('offers Administration to an administrator, and Sign Out only where users sign in', async () => {
        renderMenu({ admin: true, userMode: false })

        expect(await menuItems()).toEqual([
            'common:user_menu.my_profile',
            'common:user_menu.my_settings',
            'common:user_menu.administration',
            'common:user_menu.help',
            'common:user_menu.about',
        ])
    })

    it('navigates to the address of an item and closes the panel', async () => {
        const { onClose } = renderMenu()

        await userEvent.click(await screen.findByText('common:user_menu.help'))

        expect(appNavigate).toHaveBeenCalledWith('/help')
        expect(onClose).toHaveBeenCalled()
    })

    it('loads the About dialog only once About is chosen, and keeps it after it is closed', async () => {
        const { onClose } = renderMenu()
        await screen.findByText('common:user_menu.about')
        expect(aboutModal).not.toHaveBeenCalled()

        await userEvent.click(screen.getByText('common:user_menu.about'))

        await userEvent.click(await screen.findByTestId('about-modal'))
        expect(screen.queryByTestId('about-modal')).not.toBeInTheDocument()
        expect(aboutModal).toHaveBeenLastCalledWith(expect.objectContaining({ open: false }))
        expect(appNavigate).not.toHaveBeenCalled()
        expect(onClose).toHaveBeenCalled()
    })
})

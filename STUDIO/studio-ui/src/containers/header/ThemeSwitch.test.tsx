import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeSwitch } from './ThemeSwitch'
import { accentOf, THEME_ORDER } from '../../styles/themes'

const { appThemeRef, setCompact, setThemeMode, setThemeName, themeModeRef } = vi.hoisted(() => ({
    appThemeRef: { current: { compact: false, themeName: 'standard' } },
    setCompact: vi.fn(),
    setThemeMode: vi.fn(),
    setThemeName: vi.fn(),
    themeModeRef: { current: 'auto' },
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    useThemeMode: () => ({ isDarkMode: themeModeRef.current === 'dark', setThemeMode, themeMode: themeModeRef.current }),
}))

vi.mock('../../providers/AppThemeProvider', () => ({
    useAppTheme: () => ({ ...appThemeRef.current, setCompact, setThemeName }),
}))

vi.mock('@ant-design/icons', () => ({
    // The system appearance wears a drawn half-filled circle, passed to the package's custom-icon wrapper.
    default: ({ component }: { component: () => unknown }) => <i data-testid="icon-auto">{component() as never}</i>,
    BgColorsOutlined: ({ style }: { style?: { color?: string } }) => <i data-colour={style?.color} data-testid="icon-theme" />,
    CompressOutlined: () => <i data-testid="icon-compact" />,
    MoonOutlined: () => <i data-testid="icon-dark" />,
    SunOutlined: () => <i data-testid="icon-light" />,
}))

vi.mock('antd', async () => {
    // The theme stays the real one: a theme dot is drawn in the colours Ant Design derives.
    const { theme } = await vi.importActual<typeof import('antd')>('antd')
    interface Item { extra?: unknown, icon?: unknown, key: string, label: unknown, type?: string }
    const Dropdown = ({ children, menu }: {
        children?: unknown
        menu?: { items?: Item[], onClick?: (info: { key: string }) => void, selectedKeys?: string[] }
    }) => (
        <div>
            {children as never}
            <ul>
                {menu?.items?.filter(item => item.type !== 'divider').map(item => (
                    <li key={item.key}>
                        <button
                            data-selected={menu.selectedKeys?.includes(item.key) || undefined}
                            data-testid={`theme-option-${item.key.replace(':', '-')}`}
                            onClick={() => menu.onClick?.({ key: item.key })}
                            type="button"
                        >
                            {item.icon as never}
                            {item.label as never}
                            {item.extra as never}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    )
    const Button = ({ icon, ...rest }: { icon?: unknown }) => <button type="button" {...rest}>{icon as never}</button>
    const Switch = ({ checked, ...rest }: { checked?: boolean }) => (
        <span aria-checked={checked} role="switch" {...rest} />
    )
    return { Button, Dropdown, Switch, theme }
})

describe('ThemeSwitch', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        themeModeRef.current = 'auto'
        appThemeRef.current = { compact: false, themeName: 'standard' }
    })

    it('offers the three appearances and marks the one in force', () => {
        render(<ThemeSwitch />)

        expect(screen.getByTestId('theme-option-light').textContent).toContain('common:theme.light')
        expect(screen.getByTestId('theme-option-dark').textContent).toContain('common:theme.dark')
        expect(screen.getByTestId('theme-option-auto').textContent).toContain('common:theme.system')
        expect(screen.getByTestId('theme-option-auto').getAttribute('data-selected')).toBe('true')
    })

    it('offers the density right under the appearances, and the themes after both', () => {
        render(<ThemeSwitch />)

        const order = screen.getAllByTestId(/^theme-option-/).map(option => option.getAttribute('data-testid'))

        expect(order.slice(0, 5)).toEqual([
            'theme-option-light',
            'theme-option-dark',
            'theme-option-auto',
            'theme-option-compact',
            'theme-option-theme-standard',
        ])
    })

    it('wears the icon of the appearance in force', () => {
        themeModeRef.current = 'dark'

        render(<ThemeSwitch />)

        expect(screen.getByTestId('theme-switch').querySelector('[data-testid="icon-dark"]')).toBeTruthy()
    })

    it('applies the picked appearance', async () => {
        render(<ThemeSwitch />)

        await userEvent.click(screen.getByTestId('theme-option-light'))

        expect(setThemeMode).toHaveBeenCalledWith('light')
    })

    it('offers every theme in order, marking the one in force and showing each in its own colour', () => {
        render(<ThemeSwitch />)

        THEME_ORDER.forEach(name => {
            expect(screen.getByTestId(`theme-option-theme-${name}`).textContent).toContain(`common:theme.names.${name}`)
        })
        expect(screen.getByTestId('theme-option-theme-standard').getAttribute('data-selected')).toBe('true')
        expect(screen.getByTestId('theme-option-theme-dracula').getAttribute('data-selected')).toBeNull()

        const dots = screen.getAllByTestId('icon-theme').map(dot => dot.getAttribute('data-colour'))

        expect(dots).toEqual(THEME_ORDER.map(name => accentOf(name, false)))
    })

    it('draws the theme dots in the variant of the appearance in force', () => {
        themeModeRef.current = 'dark'

        render(<ThemeSwitch />)

        const dots = screen.getAllByTestId('icon-theme').map(dot => dot.getAttribute('data-colour'))

        expect(dots).toEqual(THEME_ORDER.map(name => accentOf(name, true)))
        expect(dots).not.toEqual(THEME_ORDER.map(name => accentOf(name, false)))
    })

    it('applies a newly picked theme without touching the appearance or the density', async () => {
        render(<ThemeSwitch />)

        await userEvent.click(screen.getByTestId('theme-option-theme-dracula'))

        expect(setThemeName).toHaveBeenCalledWith('dracula')
        expect(setThemeMode).not.toHaveBeenCalled()
        expect(setCompact).not.toHaveBeenCalled()
    })

    it('offers the compact density under the themes, switched off to begin with', () => {
        render(<ThemeSwitch />)

        expect(screen.getByTestId('theme-option-compact').textContent).toContain('common:theme.compact')
        expect(screen.getByTestId('theme-compact').getAttribute('aria-checked')).toBe('false')
        expect(screen.getByTestId('theme-option-compact').getAttribute('data-selected')).toBeNull()
    })

    it('turns the density on and off without touching the appearance', async () => {
        render(<ThemeSwitch />)

        await userEvent.click(screen.getByTestId('theme-option-compact'))

        expect(setCompact).toHaveBeenCalledWith(true)
        expect(setThemeMode).not.toHaveBeenCalled()
    })

    it('marks the compact density while it is in force', async () => {
        appThemeRef.current = { compact: true, themeName: 'standard' }

        render(<ThemeSwitch />)

        expect(screen.getByTestId('theme-compact').getAttribute('aria-checked')).toBe('true')
        expect(screen.getByTestId('theme-option-compact').getAttribute('data-selected')).toBe('true')

        await userEvent.click(screen.getByTestId('theme-option-compact'))

        expect(setCompact).toHaveBeenCalledWith(false)
    })
})

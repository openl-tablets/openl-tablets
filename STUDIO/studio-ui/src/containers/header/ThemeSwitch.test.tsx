import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeSwitch } from './ThemeSwitch'
import { DARK_PALETTE, EVERGREEN_LIGHT_PALETTE, LIGHT_PALETTE } from '../../styles/listPageTheme'

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

vi.mock('antd', () => {
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
    return { Button, Dropdown, Switch }
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

    it('offers every theme, marking the one in force and showing each in its own colour', () => {
        render(<ThemeSwitch />)

        expect(screen.getByTestId('theme-option-theme-standard').textContent).toContain('common:theme.names.standard')
        expect(screen.getByTestId('theme-option-theme-evergreen').textContent).toContain('common:theme.names.evergreen')
        expect(screen.getByTestId('theme-option-theme-standard').getAttribute('data-selected')).toBe('true')
        expect(screen.getByTestId('theme-option-theme-evergreen').getAttribute('data-selected')).toBeNull()

        const dots = screen.getAllByTestId('icon-theme').map(dot => dot.getAttribute('data-colour'))

        expect(dots).toEqual([LIGHT_PALETTE.primary, EVERGREEN_LIGHT_PALETTE.primary])
    })

    it('draws the theme dots in the appearance in force', () => {
        themeModeRef.current = 'dark'

        render(<ThemeSwitch />)

        expect(screen.getAllByTestId('icon-theme')[0]?.getAttribute('data-colour')).toBe(DARK_PALETTE.primary)
    })

    it('applies a newly picked theme without touching the appearance or the density', async () => {
        render(<ThemeSwitch />)

        await userEvent.click(screen.getByTestId('theme-option-theme-evergreen'))

        expect(setThemeName).toHaveBeenCalledWith('evergreen')
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

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeSwitch } from './ThemeSwitch'

const { setThemeMode, themeModeRef } = vi.hoisted(() => ({
    setThemeMode: vi.fn(),
    themeModeRef: { current: 'auto' },
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    useThemeMode: () => ({ setThemeMode, themeMode: themeModeRef.current }),
}))

vi.mock('@ant-design/icons', () => ({
    DesktopOutlined: () => <i data-testid="icon-auto" />,
    MoonOutlined: () => <i data-testid="icon-dark" />,
    SunOutlined: () => <i data-testid="icon-light" />,
}))

vi.mock('antd', () => {
    interface Item { icon?: unknown, key: string, label: unknown }
    const Dropdown = ({ children, menu }: {
        children?: unknown
        menu?: { items?: Item[], onClick?: (info: { key: string }) => void, selectedKeys?: string[] }
    }) => (
        <div>
            {children as never}
            <ul>
                {menu?.items?.map(item => (
                    <li key={item.key}>
                        <button
                            data-selected={menu.selectedKeys?.includes(item.key) || undefined}
                            data-testid={`theme-option-${item.key}`}
                            onClick={() => menu.onClick?.({ key: item.key })}
                            type="button"
                        >
                            {item.icon as never}
                            {item.label as never}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    )
    const Button = ({ icon, ...rest }: { icon?: unknown }) => <button type="button" {...rest}>{icon as never}</button>
    return { Button, Dropdown }
})

describe('ThemeSwitch', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        themeModeRef.current = 'auto'
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
})

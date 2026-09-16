import { useMemo, type ReactNode } from 'react'
import { Button, Dropdown, type MenuProps } from 'antd'
import { DesktopOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons'
import { useThemeMode, type ThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'

const MODE_ICONS: Record<ThemeMode, ReactNode> = {
    auto: <DesktopOutlined />,
    dark: <MoonOutlined />,
    light: <SunOutlined />,
}

const MODE_LABELS: Record<ThemeMode, string> = {
    auto: 'common:theme.system',
    dark: 'common:theme.dark',
    light: 'common:theme.light',
}

const MODE_ORDER: readonly ThemeMode[] = ['light', 'dark', 'auto']

/**
 * Picks the appearance of OpenL Studio — light, dark, or the one the operating system asks for.
 *
 * The button wears the icon of the appearance in force and opens the three choices; picking one applies it
 * at once and remembers it for the next visit.
 */
export const ThemeSwitch = () => {
    const { t } = useTranslation()
    const { setThemeMode, themeMode } = useThemeMode()

    const items: MenuProps['items'] = useMemo(() => MODE_ORDER.map(mode => ({
        icon: MODE_ICONS[mode],
        key: mode,
        label: t(MODE_LABELS[mode]),
    })), [t])

    return (
        <Dropdown
            placement="bottomRight"
            trigger={['click']}
            menu={{
                items,
                onClick: ({ key }) => setThemeMode(key as ThemeMode),
                selectedKeys: [themeMode],
            }}
        >
            <Button
                aria-label={t('common:theme.title')}
                data-testid="theme-switch"
                icon={MODE_ICONS[themeMode]}
                shape="circle"
                title={t('common:theme.title')}
                type="text"
            />
        </Dropdown>
    )
}

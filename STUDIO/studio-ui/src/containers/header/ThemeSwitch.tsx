import { useMemo, type ReactNode } from 'react'
import { Button, Dropdown, Switch, type MenuProps } from 'antd'
import Icon, { CompressOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons'
import { useThemeMode, type ThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { useAppTheme } from '../../providers/AppThemeProvider'

/**
 * A circle with one half filled — the sign an appearance that is neither light nor dark is usually given,
 * because it stands for both at once. Ant Design has no such icon, so it is drawn here.
 */
const HalfFilledCircle = () => (
    <svg fill="currentColor" height="1em" viewBox="0 0 1024 1024" width="1em">
        <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" />
        <path d="M512 140v744c205.4 0 372-166.6 372-372S717.4 140 512 140z" />
    </svg>
)

const MODE_ICONS: Record<ThemeMode, ReactNode> = {
    auto: <Icon component={HalfFilledCircle} />,
    dark: <MoonOutlined />,
    light: <SunOutlined />,
}

const MODE_LABELS: Record<ThemeMode, string> = {
    auto: 'common:theme.system',
    dark: 'common:theme.dark',
    light: 'common:theme.light',
}

const MODE_ORDER: readonly ThemeMode[] = ['light', 'dark', 'auto']

/** The density entry is not an appearance, so it carries a key no {@link ThemeMode} can collide with. */
const COMPACT_KEY = 'compact'

/**
 * Picks the appearance of OpenL Studio — light, dark, or the one the operating system asks for — and the
 * density it is laid out in.
 *
 * The button wears the icon of the appearance in force and opens the three choices; picking one applies it
 * at once and remembers it for the next visit. Under the appearances stands the compact density, which
 * tightens the paddings, the controls and the type so that more fits on a screen. The two are independent:
 * compact holds across light and dark alike.
 *
 * The compact row carries a switch that shows the density rather than takes the click — the row itself is
 * the control, so the menu stays one list of choices to a keyboard and a screen reader.
 */
export const ThemeSwitch = () => {
    const { t } = useTranslation()
    const { setThemeMode, themeMode } = useThemeMode()
    const { compact, setCompact } = useAppTheme()

    const items: MenuProps['items'] = useMemo(() => [
        ...MODE_ORDER.map(mode => ({
            icon: MODE_ICONS[mode],
            key: mode,
            label: t(MODE_LABELS[mode]),
        })),
        { type: 'divider' as const },
        {
            extra: <Switch checked={compact} data-testid="theme-compact" size="small" tabIndex={-1} />,
            icon: <CompressOutlined />,
            key: COMPACT_KEY,
            label: t('common:theme.compact'),
        },
    ], [compact, t])

    return (
        <Dropdown
            placement="bottomRight"
            trigger={['click']}
            menu={{
                items,
                onClick: ({ key }) => (key === COMPACT_KEY ? setCompact(!compact) : setThemeMode(key as ThemeMode)),
                selectedKeys: [themeMode, ...(compact ? [COMPACT_KEY] : [])],
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

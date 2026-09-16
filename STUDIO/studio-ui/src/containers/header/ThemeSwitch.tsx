import { useMemo, type ReactNode } from 'react'
import { Button, Dropdown, Switch, type MenuProps } from 'antd'
import Icon, { BgColorsOutlined, CompressOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons'
import { useThemeMode, type ThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { useAppTheme } from '../../providers/AppThemeProvider'
import { paletteOf, THEME_ORDER, type ThemeName } from '../../styles/listPageTheme'

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

/** A theme entry is keyed apart from the appearances and the density, which share the same menu. */
const themeKey = (name: ThemeName): string => `theme:${name}`

const isThemeKey = (key: string): boolean => key.startsWith('theme:')

const themeOf = (key: string): ThemeName => key.slice('theme:'.length) as ThemeName

/**
 * Picks how OpenL Studio looks: the appearance — light, dark, or the one the operating system asks for —
 * the theme its colours come from, and the density it is laid out in.
 *
 * The button wears the icon of the appearance in force and opens the three groups; picking anything applies
 * it at once and remembers it for the next visit. The three are independent — a theme is worn in either
 * appearance, at either density — so the menu offers them as three groups rather than one list of
 * combinations.
 *
 * Each theme is shown beside a dot of its own primary colour, drawn in the appearance in force, so the
 * choice is made by looking rather than by reading. The compact row carries a switch that shows the density
 * rather than takes the click — the row itself is the control, so the menu stays one list of choices to a
 * keyboard and a screen reader.
 */
export const ThemeSwitch = () => {
    const { t } = useTranslation()
    const { isDarkMode, setThemeMode, themeMode } = useThemeMode()
    const { compact, setCompact, setThemeName, themeName } = useAppTheme()

    const items: MenuProps['items'] = useMemo(() => [
        ...MODE_ORDER.map(mode => ({
            icon: MODE_ICONS[mode],
            key: mode,
            label: t(MODE_LABELS[mode]),
        })),
        { type: 'divider' as const },
        ...THEME_ORDER.map(name => ({
            icon: <BgColorsOutlined style={{ color: paletteOf(name, isDarkMode).primary }} />,
            key: themeKey(name),
            label: t(`common:theme.names.${name}`),
        })),
        { type: 'divider' as const },
        {
            extra: <Switch checked={compact} data-testid="theme-compact" size="small" tabIndex={-1} />,
            icon: <CompressOutlined />,
            key: COMPACT_KEY,
            label: t('common:theme.compact'),
        },
    ], [compact, isDarkMode, t])

    const onPick = ({ key }: { key: string }): void => {
        if (key === COMPACT_KEY) {
            setCompact(!compact)
        } else if (isThemeKey(key)) {
            setThemeName(themeOf(key))
        } else {
            setThemeMode(key as ThemeMode)
        }
    }

    return (
        <Dropdown
            placement="bottomRight"
            trigger={['click']}
            menu={{
                items,
                onClick: onPick,
                selectedKeys: [themeMode, themeKey(themeName), ...(compact ? [COMPACT_KEY] : [])],
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

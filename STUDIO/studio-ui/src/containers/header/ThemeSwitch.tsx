import { useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import { Button, Dropdown, Switch, type MenuProps } from 'antd'
import Icon, {
    BgColorsOutlined,
    CompressOutlined,
    FileExcelOutlined,
    MoonOutlined,
    SunOutlined,
} from '@ant-design/icons'
import { useThemeMode, type ThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { useAppTheme } from '../../providers/AppThemeProvider'
import { accentOf, THEME_ORDER, type ThemeName } from '../../styles/themes'

/**
 * A circle with one half filled — the sign an appearance that is neither light nor dark is usually given,
 * because it stands for both at once. Ant Design has no such icon, so it is drawn here.
 */
const HalfFilledCircle = () => (
    <svg aria-hidden="true" fill="currentColor" height="1em" viewBox="0 0 1024 1024" width="1em">
        <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" />
        <path d="M512 140v744c205.4 0 372-166.6 372-372S717.4 140 512 140z" />
    </svg>
)

const MODE_ICONS: Record<ThemeMode, ReactNode> = {
    auto: <Icon component={HalfFilledCircle} />,
    dark: <MoonOutlined />,
    light: <SunOutlined />,
}

/** An appearance picked in place of the system's: light or dark. */
type Appearance = Exclude<ThemeMode, 'auto'>

const MODE_LABELS: Record<Appearance, string> = {
    dark: 'common:theme.dark',
    light: 'common:theme.light',
}

const SYSTEM_DARK = '(prefers-color-scheme: dark)'

const subscribeToSystem = (onChange: () => void): (() => void) => {
    const query = globalThis.matchMedia?.(SYSTEM_DARK)
    query?.addEventListener('change', onChange)
    return () => query?.removeEventListener('change', onChange)
}

/**
 * The appearance the operating system asks for: dark, or light when it asks for light or does not say.
 *
 * Read from the system itself rather than from the appearance in force, which lags behind for a moment after the
 * system is followed again.
 */
const useSystemAppearance = (): Appearance =>
    useSyncExternalStore(subscribeToSystem, () => (globalThis.matchMedia?.(SYSTEM_DARK).matches ? 'dark' : 'light'))

const opposite = (appearance: Appearance): Appearance => appearance === 'dark' ? 'light' : 'dark'

/** The appearance entry is not a {@link ThemeMode} either: it turns one on and off rather than names it. */
const APPEARANCE_KEY = 'appearance'

/** Nor is the density entry, which carries a key of its own as well. */
const COMPACT_KEY = 'compact'

/** Nor is the entry that shows the tables in the formatting of their Excel files. */
const EXCEL_FORMATTING_KEY = 'excelFormatting'

/** A theme entry is keyed apart from the appearances and the switches, which share the same menu. */
const themeKey = (name: ThemeName): string => `theme:${name}`

const isThemeKey = (key: string): boolean => key.startsWith('theme:')

const themeOf = (key: string): ThemeName => key.slice('theme:'.length) as ThemeName

/**
 * Picks how OpenL Studio looks: the appearance — the one the operating system asks for, or the opposite one —
 * the density it is laid out in, how the tables are formatted, and the theme its colours come from.
 *
 * The button wears the icon of the appearance picked — following the system, light, or dark — and opens the menu:
 * the appearance, the density and the formatting of the tables, then the themes. The appearance is one switch,
 * named after the appearance the system does not ask for: **Dark** while the system is light or does not say,
 * **Light** while it is dark. Turning it on keeps that appearance even when the system later switches; turning it
 * off follows the system again, under the same name. When the system has meanwhile switched to the kept appearance,
 * a click keeps the switch on and turns it to the other appearance instead. The tables are drawn in the colours of the theme by what each of their cells means,
 * unless **Excel Formatting** shows them in the formatting of their Excel files.
 *
 * Picking anything applies it at once and remembers it for the next visit. The menu stays open meanwhile, so one
 * choice after another can be tried and compared on the screen behind it; it closes the way it opened, by its
 * button or a click elsewhere. The choices are independent — a theme is worn in either appearance, at either
 * density, with either formatting of the tables — so the menu offers each on its own rather than one list of
 * combinations.
 *
 * Each theme is shown beside a dot of its own accent, in the variant of the appearance in force, so the
 * choice is made by looking rather than by reading. The appearance, the compact and the Excel formatting rows carry
 * a switch that shows the choice rather than takes the click — the row itself is the control, so the menu stays one
 * list of choices to a keyboard and a screen reader.
 */
export const ThemeSwitch = () => {
    const { t } = useTranslation()
    const { isDarkMode, setThemeMode, themeMode } = useThemeMode()
    const { compact, excelFormatting, setCompact, setExcelFormatting, setThemeName, themeName } = useAppTheme()
    const [open, setOpen] = useState(false)

    const system = useSystemAppearance()
    // Off, the entry offers the appearance the system does not ask for; on, it names the one kept.
    const overridden = themeMode !== 'auto'
    const override: Appearance = overridden ? themeMode : opposite(system)

    const items: MenuProps['items'] = useMemo(() => [
        {
            extra: <Switch checked={overridden} data-testid="theme-appearance" size="small" tabIndex={-1} />,
            icon: MODE_ICONS[override],
            key: APPEARANCE_KEY,
            label: t(MODE_LABELS[override]),
        },
        {
            extra: <Switch checked={compact} data-testid="theme-compact" size="small" tabIndex={-1} />,
            icon: <CompressOutlined />,
            key: COMPACT_KEY,
            label: t('common:theme.compact'),
        },
        {
            extra: <Switch checked={excelFormatting} data-testid="theme-excel-formatting" size="small" tabIndex={-1} />,
            icon: <FileExcelOutlined />,
            key: EXCEL_FORMATTING_KEY,
            label: t('common:theme.excel_formatting'),
        },
        { type: 'divider' as const },
        ...THEME_ORDER.map(name => ({
            icon: <BgColorsOutlined style={{ color: accentOf(name, isDarkMode) }} />,
            key: themeKey(name),
            label: t(`common:theme.names.${name}`),
        })),
    ], [compact, excelFormatting, isDarkMode, override, overridden, t])

    const onPick = ({ key }: { key: string }): void => {
        if (key === APPEARANCE_KEY) {
            // An override the system has caught up with stays on, switched to the other appearance.
            setThemeMode(themeMode === opposite(system) ? 'auto' : opposite(system))
        } else if (key === COMPACT_KEY) {
            setCompact(!compact)
        } else if (key === EXCEL_FORMATTING_KEY) {
            setExcelFormatting(!excelFormatting)
        } else if (isThemeKey(key)) {
            setThemeName(themeOf(key))
        }
    }

    return (
        <Dropdown
            // A pick closes the menu by default; only its button and a click elsewhere do here.
            onOpenChange={(next, { source }) => source === 'trigger' && setOpen(next)}
            open={open}
            placement="bottomRight"
            trigger={['click']}
            menu={{
                items,
                onClick: onPick,
                selectedKeys: [
                    ...(overridden ? [APPEARANCE_KEY] : []),
                    themeKey(themeName),
                    ...(compact ? [COMPACT_KEY] : []),
                    ...(excelFormatting ? [EXCEL_FORMATTING_KEY] : []),
                ],
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

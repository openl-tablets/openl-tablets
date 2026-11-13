import { useState } from 'react'
import dayjs from 'dayjs'
import { Button, Dropdown, Switch, Typography, type MenuProps } from 'antd'
import { SettingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ALL_TESTS_ON_A_PAGE, TESTS_PAGE_SIZES } from 'constants/tests'
import { useSettingsStore } from 'store'
import { DATE_FORMATS, TIME_FORMATS, type UserSettings } from 'store/settingsStore'
import { systemPatterns } from 'utils/dateFormat'

/** The settings turned on and off by a row of their own. */
type SwitchSetting = {[K in keyof UserSettings]: UserSettings[K] extends boolean ? K : never }[keyof UserSettings]

const LABELS: Record<SwitchSetting, string> = {
    showHeader: 'common:settings.show_header',
    showFormulas: 'common:settings.show_formulas',
    testsFailuresOnly: 'execution:tests.failuresOnly',
    showComplexResult: 'execution:tests.compoundResult',
    showRealNumbers: 'common:settings.show_numbers_without_formatting',
}

const isSwitchSetting = (key: string): key is SwitchSetting => key in LABELS

/** A page size is keyed apart from the switches, which share the same menu. */
const PAGE_SIZE = 'testsPerPage:'

/** The moment each format is shown on, so a reader sees how a date reads before picking its format. */
const SAMPLE = '2026-12-31T13:45:30'

/** The settings picked among formats; a format is keyed by the setting and its pattern, the system's by none. */
type FormatSetting = 'dateFormat' | 'timeFormat'

const formatKey = (setting: FormatSetting, pattern: string): string => `${setting}:${pattern}`

/** The setting a key of a format picks, and the pattern it picks, or nothing for a key of another row. */
const pickedFormat = (key: string): [FormatSetting, string] | undefined => {
    const [setting, ...pattern] = key.split(':')
    return setting === 'dateFormat' || setting === 'timeFormat' ? [setting, pattern.join(':')] : undefined
}

/**
 * Picks **My Settings**: how the tables, the test results and the trace are shown.
 *
 * The button opens a menu of the settings in four groups — the tables, the testing, the trace and the dates. A
 * setting that is on or off is a row carrying a switch, which shows the choice rather than takes the click: the row
 * itself is the control. **Tests Per Page** opens the page sizes, the one in force marked.
 *
 * **Date Format** and **Time Format** each open the formats, every one shown on the same moment. The first is the
 * format of the regional settings of the browser, marked **System**, which is in force at first; the format in force
 * is shown beside the name.
 *
 * Picking anything applies it at once and remembers it in this browser, for every tab and the next visit: every
 * screen writing a date is subscribed to the formats (`useDateFormats`) and draws its dates again. Nothing
 * is sent to the server, so the settings follow the browser rather than the user. The menu stays open meanwhile,
 * so one setting after another can be changed; it closes the way it opened, by its button or a click elsewhere.
 */
export const SettingsMenu = () => {
    const { t } = useTranslation()
    const settings = useSettingsStore(state => state.settings)
    const setSetting = useSettingsStore(state => state.setSetting)
    const [open, setOpen] = useState(false)

    const switchItem = (key: SwitchSetting) => ({
        extra: <Switch checked={settings[key]} data-testid={`settings-${key}`} size="small" tabIndex={-1} />,
        key,
        label: t(LABELS[key]),
    })

    const sizeLabel = (size: number): string =>
        size === ALL_TESTS_ON_A_PAGE ? t('execution:tests.all') : String(size)

    // Made at every drawing: a day.js date keeps the language it was made in.
    const sample = (pattern: string): string => dayjs(SAMPLE).format(pattern)

    const formatItem = (setting: FormatSetting, label: string, patterns: readonly string[], system: string) => ({
        children: [
            { extra: t('common:settings.system_format'), key: formatKey(setting, ''), label: sample(system) },
            ...patterns.map(pattern => ({ key: formatKey(setting, pattern), label: sample(pattern) })),
        ],
        key: setting,
        label: (
            <>
                {t(label)}
                {' '}
                <Typography.Text type="secondary">{sample(settings[setting] || system)}</Typography.Text>
            </>
        ),
    })

    const items: MenuProps['items'] = [
        {
            children: [switchItem('showHeader'), switchItem('showFormulas')],
            key: 'table',
            label: t('common:settings.table_settings'),
            type: 'group',
        },
        {
            children: [
                {
                    children: TESTS_PAGE_SIZES.map(size => ({ key: PAGE_SIZE + size, label: sizeLabel(size) })),
                    key: 'testsPerPage',
                    label: (
                        <>
                            {t('common:settings.tests_per_page')}
                            {' '}
                            <Typography.Text type="secondary">{sizeLabel(settings.testsPerPage)}</Typography.Text>
                        </>
                    ),
                },
                switchItem('testsFailuresOnly'),
                switchItem('showComplexResult'),
            ],
            key: 'testing',
            label: t('common:settings.testing_settings'),
            type: 'group',
        },
        {
            children: [switchItem('showRealNumbers')],
            key: 'trace',
            label: t('common:settings.trace_settings'),
            type: 'group',
        },
        {
            children: [
                formatItem('dateFormat', 'common:settings.date_format', DATE_FORMATS, systemPatterns().date),
                formatItem('timeFormat', 'common:settings.time_format', TIME_FORMATS, systemPatterns().time),
            ],
            key: 'dateTime',
            label: t('common:settings.date_time_settings'),
            type: 'group',
        },
    ]

    const onPick = ({ key }: { key: string }): void => {
        const format = pickedFormat(key)
        if (format) {
            setSetting(...format)
        } else if (isSwitchSetting(key)) {
            setSetting(key, !settings[key])
        } else if (key.startsWith(PAGE_SIZE)) {
            setSetting('testsPerPage', Number(key.slice(PAGE_SIZE.length)))
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
                    ...(Object.keys(LABELS) as SwitchSetting[]).filter(key => settings[key]),
                    PAGE_SIZE + settings.testsPerPage,
                    formatKey('dateFormat', settings.dateFormat),
                    formatKey('timeFormat', settings.timeFormat),
                ],
            }}
        >
            <Button
                aria-label={t('common:settings.title')}
                data-testid="settings-menu"
                icon={<SettingOutlined />}
                shape="circle"
                title={t('common:settings.title')}
                type="text"
            />
        </Dropdown>
    )
}

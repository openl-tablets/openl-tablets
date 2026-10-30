import { useState } from 'react'
import dayjs from 'dayjs'
import { Button, Dropdown, Switch, Tooltip, Typography, type MenuProps } from 'antd'
import { InfoCircleOutlined, SettingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ALL_TESTS_ON_A_PAGE, TESTS_PAGE_SIZES } from 'constants/tests'
import { useSettingsStore } from 'store'
import { DATE_FORMATS, type UserSettings } from 'store/settingsStore'

/** The settings turned on and off by a row of their own. */
type SwitchSetting = {[K in keyof UserSettings]: UserSettings[K] extends boolean ? K : never }[keyof UserSettings]

const LABELS: Record<SwitchSetting, string> = {
    showHeader: 'common:settings.show_header',
    showFormulas: 'common:settings.show_formulas',
    testsFailuresOnly: 'execution:tests.failuresOnly',
    showComplexResult: 'execution:tests.compoundResult',
    showRealNumbers: 'common:settings.show_numbers_without_formatting',
    useSystemDateFormat: 'common:settings.use_system_formats',
    hours24: 'common:settings.hours_24',
}

const isSwitchSetting = (key: string): key is SwitchSetting => key in LABELS

/** A page size is keyed apart from the switches, which share the same menu. */
const PAGE_SIZE = 'testsPerPage:'

/** The moment each format is shown on, so a reader sees how a date reads before picking its format. */
const SAMPLE = '2026-12-31T13:45:30'

/** A date format is keyed by its pattern, apart from the switches and the page sizes. */
const DATE_FORMAT = 'dateFormat:'

/**
 * Picks **My Settings**: how the tables, the test results and the trace are shown.
 *
 * The button opens a menu of the settings in four groups — the tables, the testing, the trace and the dates. A
 * setting that is on or off is a row carrying a switch, which shows the choice rather than takes the click: the row
 * itself is the control. **Tests Per Page** opens the page sizes, the one in force marked.
 *
 * The dates follow the regional settings of the browser while **Use System Preferences** is on, as it is at first.
 * Turned off, it shows **Date Format**, which opens the formats, each shown on one moment beside its name, and
 * **24-Hour Time**, which picks the clock.
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

    const dateFormatItem = {
        children: DATE_FORMATS.map(({ name, pattern }) => ({
            extra: t(`common:settings.formats.${name}`),
            key: DATE_FORMAT + pattern,
            label: sample(pattern),
        })),
        key: 'dateFormat',
        label: (
            <>
                {t('common:settings.date_format')}
                {' '}
                <Typography.Text type="secondary">{sample(settings.dateFormat)}</Typography.Text>
            </>
        ),
    }

    const systemItem = {
        ...switchItem('useSystemDateFormat'),
        label: (
            <>
                {t(LABELS.useSystemDateFormat)}
                {' '}
                <Tooltip title={t('common:settings.use_system_formats_hint')}>
                    <InfoCircleOutlined data-testid="settings-useSystemDateFormat-hint" />
                </Tooltip>
            </>
        ),
    }

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
            // The format and the clock are the reader's to pick only while the system is not followed.
            children: settings.useSystemDateFormat
                ? [systemItem]
                : [systemItem, dateFormatItem, switchItem('hours24')],
            key: 'dateTime',
            label: t('common:settings.date_time_settings'),
            type: 'group',
        },
    ]

    const onPick = ({ key }: { key: string }): void => {
        if (isSwitchSetting(key)) {
            setSetting(key, !settings[key])
        } else if (key.startsWith(PAGE_SIZE)) {
            setSetting('testsPerPage', Number(key.slice(PAGE_SIZE.length)))
        } else if (key.startsWith(DATE_FORMAT)) {
            setSetting('dateFormat', key.slice(DATE_FORMAT.length))
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
                    DATE_FORMAT + settings.dateFormat,
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

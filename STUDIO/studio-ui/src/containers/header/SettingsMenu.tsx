import { useState } from 'react'
import { Button, Dropdown, Switch, Typography, type MenuProps } from 'antd'
import { SettingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ALL_TESTS_ON_A_PAGE, TESTS_PAGE_SIZES } from 'constants/tests'
import { useSettingsStore } from 'store'
import type { UserSettings } from 'store/settingsStore'

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

/**
 * Picks **My Settings**: how the tables, the test results and the trace are shown.
 *
 * The button opens a menu of the settings in three groups — the tables, the testing and the trace. A setting that
 * is on or off is a row carrying a switch, which shows the choice rather than takes the click: the row itself is
 * the control. **Tests Per Page** opens the page sizes, the one in force marked.
 *
 * Picking anything applies it at once and remembers it in this browser, for every tab and the next visit. Nothing
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
    ]

    const onPick = ({ key }: { key: string }): void => {
        if (isSwitchSetting(key)) {
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

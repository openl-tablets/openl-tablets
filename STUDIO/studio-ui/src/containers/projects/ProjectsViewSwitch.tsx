import type { KeyboardEvent, MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Segmented } from 'antd'
import { AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'

export type ProjectView = 'list' | 'grid'

/** The keys that flip the switch, as a click does. */
const TOGGLE_KEYS = new Set([' ', 'Enter', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'])

const useStyles = createStyles(({ css }) => ({
    switch: css`
        cursor: pointer;
    `,
}))

interface ProjectsViewSwitchProps {
    view: ProjectView
    onView: (value: ProjectView) => void
}

/**
 * Switches the projects between a table and cards.
 *
 * It works as an on/off switch: a click anywhere on it, or Space, Enter or an arrow key, shows the other view.
 *
 * The cards keep the order picked by the column headers of the table, since they have no headers of their own.
 */
export const ProjectsViewSwitch = ({ view, onView }: ProjectsViewSwitchProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const toggle = () => onView(view === 'list' ? 'grid' : 'list')

    // A click on a part of the switch reaches its hidden radio button as a second click, and only that one counts: a
    // screen reader clicks the radio button alone. A click on the frame around the parts reaches no radio button.
    const onClick = (event: MouseEvent) => {
        const target = event.target as Element
        if (target instanceof HTMLInputElement || !target.closest('label')) {
            toggle()
        }
    }

    // A key held down flips the switch once, and a key with a modifier is left to the browser.
    const onKeyDown = (event: KeyboardEvent) => {
        const modified = event.altKey || event.ctrlKey || event.metaKey
        if (TOGGLE_KEYS.has(event.key) && !modified && !event.repeat) {
            event.preventDefault()
            toggle()
        }
    }

    return (
        <Segmented<ProjectView>
            className={styles.switch}
            data-testid="projects-view"
            onClick={onClick}
            onKeyDown={onKeyDown}
            value={view}
            options={[
                { value: 'list', icon: <UnorderedListOutlined />, title: t('home.view_list') },
                { value: 'grid', icon: <AppstoreOutlined />, title: t('home.view_grid') },
            ]}
        />
    )
}

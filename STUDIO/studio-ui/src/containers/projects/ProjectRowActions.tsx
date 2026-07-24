import type { ComponentType, CSSProperties, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dropdown, Tooltip, type MenuProps } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DiffOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    HistoryOutlined,
    MergeOutlined,
    MoreOutlined,
    RocketOutlined,
    SaveOutlined,
} from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import type { Project } from '../../types/projects'
import { availableActions, PROJECT_ACTIONS, type RowActionId } from './projectActions'

export type { RowActionId }

/**
 * Per-row project actions, resolved from the project's server-computed capabilities. `open`, `close`
 * and `save` double as the project's workspace status: a closed project offers Open, an opened one
 * offers Close, and a modified one offers Save.
 */
export type ProjectListHandlers = {
    [K in RowActionId as `on${Capitalize<K>}`]: (project: Project) => void
}

const useStyles = createStyles(({ css }) => ({
    wrap: css`
        display: inline-flex;
        align-items: center;
        gap: 4px;
    `,
}))

/**
 * The glyph of each action on a list row. The open/close pair mirrors the project status: a closed
 * project shows a closed folder (Open), an opened one shows the green open folder (Close).
 */
const ROW_ICONS: Record<RowActionId, ComponentType<{ style?: CSSProperties }>> = {
    open: FolderOutlined,
    close: FolderOpenOutlined,
    save: SaveOutlined,
    copy: CopyOutlined,
    deleteBranch: DeleteOutlined,
    openRevision: HistoryOutlined,
    sync: MergeOutlined,
    deploy: RocketOutlined,
    compare: DiffOutlined,
    export: DownloadOutlined,
    delete: DeleteOutlined,
}

/** The actions a row shows as buttons, in display order; open and close never apply at once. */
const PRIMARY: RowActionId[] = ['copy', 'deleteBranch', 'open', 'close']

/** Everything else, in the order the overflow menu lists it. */
const OVERFLOW: RowActionId[] = ['save', 'openRevision', 'sync', 'deploy', 'compare', 'export', 'delete']

const handlerOf = (id: RowActionId): keyof ProjectListHandlers =>
    `on${id.charAt(0).toUpperCase()}${id.slice(1)}` as keyof ProjectListHandlers

const iconOf = (id: RowActionId, successColor: string): ReactNode => {
    const Icon = ROW_ICONS[id]
    // Close is the one action tinted with its status colour, matching the Opened status mark.
    return id === 'close' ? <Icon style={{ color: successColor }} /> : <Icon />
}

interface ProjectActionsProps {
    project: Project
    handlers: ProjectListHandlers
    /** The action currently running on this project, if any — drives loading. */
    pendingActionId: RowActionId | null
    /**
     * `buttons` keeps the everyday actions in front, as a table row has the width for; `menu` folds every
     * action away, which is what a card has room for.
     */
    layout?: 'buttons' | 'menu'
}

/**
 * The actions of a project in the list. A table row shows the everyday ones as buttons and keeps the rest
 * behind the overflow menu; a card folds them all into the menu, where they do not crowd the tile. Every
 * action is gated by a server-computed capability, so only what the current user may perform is offered.
 */
export const ProjectRowActions = ({
    project,
    handlers,
    pendingActionId,
    layout = 'buttons',
}: ProjectActionsProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const token = useTheme()
    const busy = pendingActionId !== null

    const primary = layout === 'buttons' ? availableActions(project, PRIMARY) : []
    const overflow = layout === 'buttons'
        ? availableActions(project, OVERFLOW)
        : availableActions(project, [...PRIMARY, ...OVERFLOW])
    if (primary.length === 0 && overflow.length === 0) {
        return null
    }

    const items: MenuProps['items'] = []
    overflow.forEach(id => {
        const danger = PROJECT_ACTIONS[id].danger ?? false
        if (danger && items.length > 0) {
            items.push({ type: 'divider' })
        }
        items.push({
            key: id,
            icon: iconOf(id, token.colorSuccess),
            label: t(PROJECT_ACTIONS[id].labelKey),
            danger,
        })
    })

    const runFromMenu: MenuProps['onClick'] = ({ key, domEvent }) => {
        domEvent.stopPropagation()
        handlers[handlerOf(key as RowActionId)](project)
    }

    return (
        <div className={styles.wrap}>
            {primary.map(id => {
                const label = t(PROJECT_ACTIONS[id].labelKey)
                return (
                    <Tooltip key={id} title={label}>
                        <Button
                            aria-label={label}
                            data-testid={`project-action-${id}-${project.id}`}
                            disabled={busy && pendingActionId !== id}
                            icon={iconOf(id, token.colorSuccess)}
                            loading={pendingActionId === id}
                            type="text"
                            onClick={event => {
                                event.stopPropagation()
                                handlers[handlerOf(id)](project)
                            }}
                        />
                    </Tooltip>
                )
            })}
            {items.length > 0 && (
                <Dropdown menu={{ items, onClick: runFromMenu }} trigger={['click']}>
                    <Button
                        aria-label={t('home.row_actions')}
                        data-testid={`project-actions-${project.id}`}
                        icon={<MoreOutlined />}
                        loading={busy}
                        onClick={event => event.stopPropagation()}
                        type="text"
                    />
                </Dropdown>
            )}
        </div>
    )
}

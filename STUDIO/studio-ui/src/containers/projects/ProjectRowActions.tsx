import type { ComponentType, CSSProperties, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dropdown, Tooltip, type MenuProps } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    MoreOutlined,
    RocketOutlined,
    SaveOutlined,
} from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import type { Project } from '../../types/projects'

export type RowActionId = 'open' | 'close' | 'save' | 'copy' | 'deploy' | 'export' | 'delete'

/**
 * Per-row project actions, resolved from the project's server-computed capabilities. `open`, `close`
 * and `save` double as the project's workspace status: a closed project offers Open, an opened one
 * offers Close, and a modified one offers Save.
 */
export interface ProjectListHandlers {
    onOpen: (project: Project) => void
    onClose: (project: Project) => void
    onSave: (project: Project) => void
    onCopy: (project: Project) => void
    onDeploy: (project: Project) => void
    onExport: (project: Project) => void
    onDelete: (project: Project) => void
}

const useStyles = createStyles(({ css }) => ({
    wrap: css`
        display: inline-flex;
        align-items: center;
        gap: 2px;
    `,
}))

interface RowActionMeta {
    id: RowActionId
    Icon: ComponentType<{ style?: CSSProperties }>
    labelKey: string
    danger?: boolean
    /** Tint the glyph success-green — used for Close, whose open-folder icon mirrors the Opened status. */
    green?: boolean
    /** The server-computed capability that gates this action. */
    cap: keyof NonNullable<Project['capabilities']>
    /** The row handler this action invokes. */
    handler: keyof ProjectListHandlers
}

// The open/close glyphs mirror the project status: a closed project shows a closed folder (Open),
// an opened one shows the green open folder (Close). Status actions first, then copy/deploy/export,
// then the destructive delete. Each action carries its own capability gate and handler, so the id set
// lives in exactly one place.
const ACTIONS: RowActionMeta[] = [
    { id: 'open', Icon: FolderOutlined, labelKey: 'browser.open', cap: 'canOpen', handler: 'onOpen' },
    { id: 'close', Icon: FolderOpenOutlined, labelKey: 'browser.close', green: true, cap: 'canClose', handler: 'onClose' },
    { id: 'save', Icon: SaveOutlined, labelKey: 'browser.save', cap: 'canSave', handler: 'onSave' },
    { id: 'copy', Icon: CopyOutlined, labelKey: 'browser.copy', cap: 'canCopy', handler: 'onCopy' },
    { id: 'deploy', Icon: RocketOutlined, labelKey: 'browser.deploy', cap: 'canDeploy', handler: 'onDeploy' },
    { id: 'export', Icon: DownloadOutlined, labelKey: 'browser.export', cap: 'canExport', handler: 'onExport' },
    { id: 'delete', Icon: DeleteOutlined, labelKey: 'browser.delete', danger: true, cap: 'canDelete', handler: 'onDelete' },
]

const iconOf = (action: RowActionMeta, successColor: string): ReactNode =>
    action.green ? <action.Icon style={{ color: successColor }} /> : <action.Icon />

const availableActions = (project: Project): RowActionMeta[] =>
    ACTIONS.filter(action => !!project.capabilities?.[action.cap])

interface ProjectActionsProps {
    project: Project
    handlers: ProjectListHandlers
    /** The action currently running on this project, if any — drives loading. */
    pendingActionId: RowActionId | null
}

/**
 * The inline pictogram actions on a project table row. Every action is an icon button with a tooltip,
 * gated by a server-computed capability, so the row only offers what the current user may perform.
 */
export const ProjectRowActions = ({ project, handlers, pendingActionId }: ProjectActionsProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const token = useTheme()
    const busy = pendingActionId !== null

    const visible = availableActions(project)
    if (visible.length === 0) {
        return null
    }

    return (
        <div className={styles.wrap}>
            {visible.map(action => {
                const label = t(action.labelKey)
                return (
                    <Tooltip key={action.id} title={label}>
                        <Button
                            aria-label={label}
                            danger={action.danger ?? false}
                            data-testid={`project-action-${action.id}-${project.id}`}
                            disabled={busy && pendingActionId !== action.id}
                            icon={iconOf(action, token.colorSuccess)}
                            loading={pendingActionId === action.id}
                            size="small"
                            type="text"
                            onClick={event => {
                                event.stopPropagation()
                                handlers[action.handler](project)
                            }}
                        />
                    </Tooltip>
                )
            })}
        </div>
    )
}

/**
 * The overflow ("⋯") actions menu for a project card, offering the same capability-gated actions as
 * the table's inline pictograms, with the destructive delete past a divider.
 */
export const ProjectActionsMenu = ({ project, handlers, pendingActionId }: ProjectActionsProps) => {
    const { t } = useTranslation('repository')
    const token = useTheme()

    const visible = availableActions(project)
    if (visible.length === 0) {
        return null
    }

    const items: MenuProps['items'] = []
    visible.forEach(action => {
        if (action.danger && items.length > 0) {
            items.push({ type: 'divider' })
        }
        items.push({ key: action.id, icon: iconOf(action, token.colorSuccess), label: t(action.labelKey), danger: action.danger ?? false })
    })

    const onClick: MenuProps['onClick'] = ({ key, domEvent }) => {
        domEvent.stopPropagation()
        const action = ACTIONS.find(item => item.id === key)
        if (action) {
            handlers[action.handler](project)
        }
    }

    return (
        <Dropdown menu={{ items, onClick }} trigger={['click']}>
            <Button
                aria-label={t('home.row_actions')}
                data-testid={`project-actions-${project.id}`}
                icon={<MoreOutlined />}
                loading={pendingActionId !== null}
                onClick={event => event.stopPropagation()}
                size="small"
                type="text"
            />
        </Dropdown>
    )
}

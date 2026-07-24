import { useMemo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Popconfirm } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DiffOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    HistoryOutlined,
    MergeOutlined,
    MinusCircleOutlined,
    RocketOutlined,
    SaveOutlined,
    UnlockOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { SplitButton } from '../../components/SplitButton'
import type { Project } from '../../types/projects'
import { isActionAvailable, PROJECT_ACTIONS, type ActionId } from './projectActions'

export type { ActionId }

const useStyles = createStyles(({ css }) => ({
    bar: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
    `,
}))

const ACTION_ICONS: Record<ActionId, ReactNode> = {
    save: <SaveOutlined />,
    open: <FolderOpenOutlined />,
    close: <MinusCircleOutlined />,
    deploy: <RocketOutlined />,
    compare: <DiffOutlined />,
    copy: <CopyOutlined />,
    openRevision: <HistoryOutlined />,
    sync: <MergeOutlined />,
    deleteBranch: <DeleteOutlined />,
    export: <DownloadOutlined />,
    delete: <DeleteOutlined />,
    unlock: <UnlockOutlined />,
}

/**
 * Every action in display order. Opening leads, then what changes the project, then the read-only
 * operations. Opening an earlier revision is not a button of its own: it hangs off Open while the project
 * can still be opened, and replaces it once it is already open.
 */
const ORDER: ActionId[] = ['save', 'open', 'close', 'sync', 'copy', 'deleteBranch', 'delete', 'deploy',
    'compare', 'export', 'unlock']

/**
 * The first available of these becomes the single primary button. Opening a revision is never it: on an
 * open project the primary action stays Close.
 */
const PRIMARY_LADDER: ActionId[] = ['save', 'open', 'close']

export type ProjectActionHandlers = Record<ActionId, () => void>

interface ActionDesc {
    id: ActionId
    testId: string
    label: string
    run: () => void
    confirm?: string
}

interface ProjectActionBarProps {
    project: Project
    /** Id of the action currently running, or null. Drives per-button loading and bar-wide disabling. */
    pendingId: ActionId | null
    handlers: ProjectActionHandlers
}

/**
 * The single home for every project action, resolved strictly from the server-provided access model.
 * The first available action is the primary button and the rest follow in a fixed order; the two that
 * destroy the project confirm first and are the only red ones. The navigator carries no actions.
 */
export const ProjectActionBar = ({ project, pendingId, handlers }: ProjectActionBarProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')

    const { actions, revision, primaryId } = useMemo(() => {
        const isAvailable = (id: ActionId) => isActionAvailable(project, id)
        const buildDesc = (id: ActionId): ActionDesc => ({
            id,
            testId: `${id}-${project.id}`,
            label: t(PROJECT_ACTIONS[id].labelKey),
            run: handlers[id],
            // Only unlock confirms inline via Popconfirm; delete delegates to the global delete modal.
            ...(id === 'unlock' ? { confirm: t(`browser.${id}_confirm`) } : {}),
        })

        // Opening a revision never takes a slot of its own: it rides in the Open menu, and takes the Open
        // slot itself once the project is already open.
        const openSlot = (['open', 'openRevision'] as const).find(isAvailable) ?? null
        return {
            actions: ORDER.flatMap(id => {
                if (id === 'open') {
                    return openSlot ? [buildDesc(openSlot)] : []
                }
                return isAvailable(id) ? [buildDesc(id)] : []
            }),
            revision: isAvailable('openRevision') ? buildDesc('openRevision') : null,
            primaryId: PRIMARY_LADDER.find(isAvailable) ?? null,
        }
    }, [project, t, handlers])

    const busy = pendingId !== null

    if (actions.length === 0 && !revision) {
        return null
    }

    const buttonProps = (action: ActionDesc) => ({
        'data-testid': action.testId,
        disabled: busy && pendingId !== action.id,
        icon: ACTION_ICONS[action.id],
        loading: pendingId === action.id,
        ...(PROJECT_ACTIONS[action.id].danger ? { danger: true } : {}),
        ...(action.id === primaryId ? { type: 'primary' as const } : {}),
    })

    const renderAction = (action: ActionDesc) => {
        // Open carries opening an earlier revision as its menu item while the project is still closed.
        if (action.id === 'open' && revision) {
            return (
                <SplitButton
                    key={action.id}
                    {...buttonProps(action)}
                    arrowLabel={revision.label}
                    arrowTestId={`${action.testId}-more`}
                    onClick={action.run}
                    menu={{
                        items: [{
                            key: revision.id,
                            label: <span data-testid={revision.testId}>{revision.label}</span>,
                        }],
                        onClick: revision.run,
                    }}
                >
                    {action.label}
                </SplitButton>
            )
        }
        if (action.confirm) {
            return (
                <Popconfirm key={action.id} onConfirm={action.run} title={action.confirm}>
                    <Button {...buttonProps(action)}>{action.label}</Button>
                </Popconfirm>
            )
        }
        return <Button key={action.id} {...buttonProps(action)} onClick={action.run}>{action.label}</Button>
    }

    return (
        <div className={styles.bar} data-testid="project-actions">
            {actions.map(renderAction)}
        </div>
    )
}

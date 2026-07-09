import { useMemo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Popconfirm } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    MinusCircleOutlined,
    RocketOutlined,
    SaveOutlined,
    UnlockOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'

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
    copy: <CopyOutlined />,
    export: <DownloadOutlined />,
    delete: <DeleteOutlined />,
    unlock: <UnlockOutlined />,
}

export type ActionId = 'save' | 'open' | 'close' | 'deploy' | 'copy' | 'export' | 'delete' | 'unlock'

/** Non-destructive actions in display order; the first available one becomes the single primary button. */
const NON_DESTRUCTIVE: ActionId[] = ['save', 'open', 'close', 'deploy', 'copy', 'export']
const PRIMARY_LADDER: ActionId[] = ['save', 'open', 'close']
const DESTRUCTIVE: ActionId[] = ['delete', 'unlock']

export interface ProjectActionHandlers {
    open: () => void
    close: () => void
    save: () => void
    deploy: () => void
    copy: () => void
    export: () => void
    delete: () => void
    unlock: () => void
}

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
 * The first available non-destructive action is the primary button, the rest follow as default
 * buttons, and destructive actions sit past a divider with a confirmation step. The navigator carries
 * no actions.
 */
export const ProjectActionBar = ({ project, pendingId, handlers }: ProjectActionBarProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')

    const { primary, secondary, destructive } = useMemo(() => {
        // Every action is gated by a server-computed capability (ACL ∧ project state); the UI never
        // re-derives access from raw permissions.
        const caps = project.capabilities
        const available: Record<ActionId, boolean> = {
            save: !!caps?.canSave,
            open: !!caps?.canOpen,
            close: !!caps?.canClose,
            deploy: !!caps?.canDeploy,
            copy: !!caps?.canCopy,
            export: !!caps?.canExport,
            delete: !!caps?.canDelete,
            unlock: !!caps?.canUnlock,
        }
        const buildDesc = (id: ActionId): ActionDesc => ({
            id,
            testId: `${id}-${project.id}`,
            label: t(`browser.${id}`),
            run: handlers[id],
            // Only unlock confirms inline via Popconfirm; delete delegates to the global delete modal.
            ...(id === 'unlock' ? { confirm: t(`browser.${id}_confirm`) } : {}),
        })

        const primaryId = PRIMARY_LADDER.find(id => available[id]) ?? null
        return {
            primary: primaryId ? buildDesc(primaryId) : null,
            secondary: NON_DESTRUCTIVE.filter(id => available[id] && id !== primaryId).map(buildDesc),
            destructive: DESTRUCTIVE.filter(id => available[id]).map(buildDesc),
        }
    }, [project, t, handlers])

    const busy = pendingId !== null

    if (!primary && secondary.length === 0 && destructive.length === 0) {
        return null
    }

    return (
        <div className={styles.bar} data-testid="project-actions">
            {primary && (
                <Button
                    data-testid={primary.testId}
                    disabled={busy && pendingId !== primary.id}
                    icon={ACTION_ICONS[primary.id]}
                    loading={pendingId === primary.id}
                    onClick={primary.run}
                    type="primary"
                >
                    {primary.label}
                </Button>
            )}
            {secondary.map(action => (
                <Button
                    key={action.id}
                    data-testid={action.testId}
                    disabled={busy && pendingId !== action.id}
                    icon={ACTION_ICONS[action.id]}
                    loading={pendingId === action.id}
                    onClick={action.run}
                >
                    {action.label}
                </Button>
            ))}
            {destructive.map(action => (
                action.id === 'delete' ? (
                    <Button
                        key={action.id}
                        danger
                        data-testid={action.testId}
                        disabled={busy && pendingId !== action.id}
                        icon={ACTION_ICONS[action.id]}
                        loading={pendingId === action.id}
                        onClick={action.run}
                    >
                        {action.label}
                    </Button>
                ) : (
                    <Popconfirm key={action.id} onConfirm={action.run} title={action.confirm}>
                        <Button
                            danger
                            data-testid={action.testId}
                            disabled={busy && pendingId !== action.id}
                            icon={ACTION_ICONS[action.id]}
                            loading={pendingId === action.id}
                        >
                            {action.label}
                        </Button>
                    </Popconfirm>
                )
            ))}
        </div>
    )
}

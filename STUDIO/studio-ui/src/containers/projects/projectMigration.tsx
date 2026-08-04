import { useEffect, useState } from 'react'
import { App, Button, Tooltip } from 'antd'
import { ThunderboltOutlined } from '@ant-design/icons'
import { errorMessage } from '../../utils/errorMessage'
import {
    EMPTY_MIGRATION,
    getProjectMigration,
    migrateProject,
    type MigrationScope,
    type ProjectMigration,
} from '../../services/migration'

/**
 * Loads what a migrate would do for a project and runs a scoped migrate. Shared by the Overview
 * (rules.xml) and the deploy-config panel (rules-deploy.xml).
 *
 * The info is fetched only when the viewer can write, since only they can act on it — a read-only viewer
 * would pay for a full server-side descriptor parse for a button they never see.
 */
export const useDescriptorMigration = (
    projectId: string,
    canWrite: boolean,
    reloadToken: number | undefined,
    onMigrated: () => void
) => {
    const { notification } = App.useApp()
    const [migration, setMigration] = useState<ProjectMigration>(EMPTY_MIGRATION)
    const [migrating, setMigrating] = useState(false)

    useEffect(() => {
        if (!canWrite) {
            setMigration(EMPTY_MIGRATION)
            return
        }
        let cancelled = false
        getProjectMigration(projectId)
            .then(info => { if (!cancelled) setMigration(info) })
            .catch(() => { if (!cancelled) setMigration(EMPTY_MIGRATION) })
        return () => { cancelled = true }
    }, [projectId, canWrite, reloadToken])

    const run = async (scope: MigrationScope, failedTitle: string) => {
        setMigrating(true)
        try {
            await migrateProject(projectId, scope)
            // A completed migrate always converges the scope to "nothing left to migrate" — update it
            // locally so the button/gating refresh even if the caller never bumps reloadToken.
            setMigration(current => ({
                ...current,
                [scope]: scope === 'rulesXml'
                    ? { movableRootModules: [], migratable: false, newModules: []}
                    : { migratable: false },
            }))
            onMigrated()
        } catch (e) {
            notification.error({ title: failedTitle, description: errorMessage(e) })
        } finally {
            setMigrating(false)
        }
    }

    return { migration, migrating, run }
}

/** The small Migrate button the Overview and the deploy-config panel share. */
export const MigrateButton = ({ tooltip, loading, onClick, label, testId, disabled }: {
    tooltip: string
    loading: boolean
    onClick: () => void
    label: string
    testId: string
    disabled?: boolean
}) => (
    // The button is wrapped so its tooltip still shows while it is disabled — a disabled button fires no
    // hover or focus events of its own, so the reason a migrate is blocked stays reachable by pointer and
    // keyboard. The wrapper takes focus only while disabled, so an enabled button keeps a single tab stop.
    <Tooltip title={tooltip} trigger={['hover', 'focus']}>
        <span
            aria-disabled={disabled ?? false}
            style={{ display: 'inline-flex' }}
            tabIndex={disabled ? 0 : undefined}
        >
            <Button
                data-testid={testId}
                disabled={disabled ?? false}
                icon={<ThunderboltOutlined />}
                loading={loading}
                onClick={onClick}
                size="small"
            >
                {label}
            </Button>
        </span>
    </Tooltip>
)

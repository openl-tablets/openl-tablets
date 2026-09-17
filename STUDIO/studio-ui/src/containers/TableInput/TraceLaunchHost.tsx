import React, { useCallback, useState } from 'react'
import { Button, Checkbox, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { useEventProject } from 'hooks'
import { launchTrace, TRACE_WINDOW_BLOCKED } from 'services/traceLaunch'
import { useUserStore } from 'store'
import type { Project } from 'types/projects'
import { errorMessage } from 'utils/errorMessage'
import { TableInputLauncher, type TableLaunchDetail, type TableLaunchValue } from './TableInputLauncher'

/** What the table page sends to start a trace of the table it shows. */
export type TraceLaunchDetail = TableLaunchDetail

interface TraceLaunchProps {
    detail: TraceLaunchDetail
    project: Project
    onClose: () => void
}

const TraceLaunch: React.FC<TraceLaunchProps> = ({ detail, project, onClose }) => {
    const { t } = useTranslation('trace')
    const showRealNumbers = useUserStore(state => state.userProfile?.showRealNumbers ?? false)
    const [advanced, setAdvanced] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)

    // A window the browser refused says so by name, because only the screen knows how to say it.
    const whatWentWrong = useCallback(
        (launchError: unknown) => (launchError instanceof Error && launchError.message === TRACE_WINDOW_BLOCKED
            ? t('launch.windowBlocked')
            : errorMessage(launchError)),
        [t]
    )

    const start = (value: TableLaunchValue, download: boolean) => {
        setStarting(true)
        setError(null)
        launchTrace({ projectId: project.id, tableId: detail.tableId, ...value, download, advanced, showRealNumbers })
            .then(onClose)
            .catch(launchError => setError(whatWentWrong(launchError)))
            .finally(() => setStarting(false))
    }

    return (
        <TableInputLauncher
            busy={starting}
            caseSelection="single"
            detail={detail}
            error={error}
            onClose={onClose}
            onError={setError}
            project={project}
            actions={launch => (
                <>
                    <Button
                        data-testid="trace-start"
                        loading={starting}
                        onClick={() => launch(value => start(value, false))}
                        type="primary"
                    >
                        {t('launch.trace')}
                    </Button>
                    <Button
                        data-testid="trace-download"
                        disabled={starting}
                        onClick={() => launch(value => start(value, true))}
                    >
                        {t('launch.traceIntoFile')}
                    </Button>
                </>
            )}
            options={() => (
                <Tooltip title={t('launch.advancedHint')}>
                    <Checkbox
                        checked={advanced}
                        data-testid="trace-advanced"
                        onChange={event => setAdvanced(event.target.checked)}
                    >
                        {t('launch.advanced')}
                    </Checkbox>
                </Tooltip>
            )}
        />
    )
}

/**
 * Opens the trace launcher under the Trace button of the table page.
 *
 * The page sends the project id, the table and where the button is. A rule table is traced with the input the
 * panel collects, and a test table with the one case picked. A table that takes no parameters is asked all the
 * same: the panel carries the settings of the trace and the trace into a file.
 */
export const TraceLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TraceLaunchDetail>('openTraceLaunch', 'trace:launch.loadFailed')
    return detail && project
        ? <TraceLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default TraceLaunchHost

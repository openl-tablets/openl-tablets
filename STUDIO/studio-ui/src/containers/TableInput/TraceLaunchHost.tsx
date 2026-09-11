import React, { useCallback, useState } from 'react'
import { Button, Checkbox, notification, Tooltip } from 'antd'
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

    // A table that asks for nothing is traced at once, so what went wrong has nowhere to be shown but a message.
    const traceAtOnce = useCallback((value: TableLaunchValue) => {
        setStarting(true)
        launchTrace({ projectId: project.id, tableId: detail.tableId, ...value, advanced, showRealNumbers })
            .then(onClose)
            .catch(launchError => {
                notification.error({ title: t('launch.startFailed'), description: whatWentWrong(launchError) })
                onClose()
            })
    }, [project.id, detail.tableId, advanced, showRealNumbers, onClose, t, whatWentWrong])

    return (
        <TableInputLauncher
            busy={starting}
            caseSelection="single"
            detail={detail}
            error={error}
            onClose={onClose}
            onError={setError}
            onNothingToAsk={traceAtOnce}
            project={project}
            actions={collect => (
                <>
                    <Button
                        data-testid="trace-start"
                        loading={starting}
                        type="primary"
                        onClick={() => {
                            const value = collect()
                            if (value) {
                                start(value, false)
                            }
                        }}
                    >
                        {t('launch.trace')}
                    </Button>
                    <Button
                        data-testid="trace-download"
                        disabled={starting}
                        onClick={() => {
                            const value = collect()
                            if (value) {
                                start(value, true)
                            }
                        }}
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
 * panel collects, a test table with the one case picked, and a table that takes nothing is traced at once.
 */
export const TraceLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TraceLaunchDetail>('openTraceLaunch', 'trace:launch.loadFailed')
    return detail && project
        ? <TraceLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default TraceLaunchHost

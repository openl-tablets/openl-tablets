import React, { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useEventProject } from 'hooks'
import { startBenchmark } from 'services/execution'
import { launchInWindow } from 'services/executionWindow'
import type { Project } from 'types/projects'
import { errorMessage } from 'utils/errorMessage'
import { TableInputLauncher, type TableLaunchDetail, type TableLaunchValue } from './TableInputLauncher'

interface BenchmarkLaunchProps {
    detail: TableLaunchDetail
    project: Project
    onClose: () => void
}

const BenchmarkLaunch: React.FC<BenchmarkLaunchProps> = ({ detail, project, onClose }) => {
    const { t } = useTranslation('execution')
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)

    /**
     * Measures what the panel collected.
     *
     * A test table is measured over the cases that are picked, and over the whole table when all of them are.
     * Any other table is measured over the input the panel collected. The measurements are shown in the results
     * window, which opens at the click.
     */
    const start = useCallback((value: TableLaunchValue) => {
        setStarting(true)
        setError(null)
        return launchInWindow('benchmark', () => startBenchmark(project.id, detail.tableId, {
            ...(value.testRanges && { testRanges: value.testRanges }),
            ...(value.fromModule && { fromModule: value.fromModule }),
            ...(value.inputJson !== undefined && { inputJson: value.inputJson }),
        }).then(() => ({ kind: 'benchmark' as const, projectId: project.id, tableId: detail.tableId })), {
            blocked: t('windowBlocked'),
            unsaved: t('windowUnsaved'),
        })
            .then(onClose)
            .finally(() => setStarting(false))
    }, [project.id, detail.tableId, onClose, t])

    return (
        <TableInputLauncher
            busy={starting}
            caseSelection="multiple"
            detail={detail}
            error={error}
            onClose={onClose}
            onError={setError}
            project={project}
            actions={[{
                key: 'benchmark-start',
                label: t('benchmark.start'),
                primary: true,
                loading: starting,
                run: value => start(value).catch(startError => setError(errorMessage(startError))),
            }]}
        />
    )
}

/**
 * Opens the benchmark launcher under the Benchmark button of the table page.
 *
 * A test table is measured over the cases that are picked: the whole table at once when every case is asked
 * for, and each of the chosen cases on its own otherwise. Any other table is measured over the input the panel
 * collects; the panel of a table that takes nothing holds only the button, so the results window opens at its click.
 */
export const BenchmarkLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TableLaunchDetail>('openBenchmarkLaunch', 'execution:benchmark.startFailed')
    return detail && project
        ? <BenchmarkLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default BenchmarkLaunchHost

import React, { useCallback, useState } from 'react'
import { Button, notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { BenchmarkResultModal } from 'containers/execution/BenchmarkResultModal'
import { useEventProject } from 'hooks'
import { startBenchmark } from 'services/execution'
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
    const [measuring, setMeasuring] = useState(false)

    /**
     * Measures what the panel collected.
     *
     * A test table is measured over the cases that are picked, and over the whole table when all of them are.
     * Any other table is measured over the input the panel collected.
     */
    const start = useCallback((value: TableLaunchValue) => {
        setStarting(true)
        setError(null)
        return startBenchmark(project.id, detail.tableId, {
            ...(value.testRanges && { testRanges: value.testRanges }),
            ...(value.fromModule && { fromModule: value.fromModule }),
            ...(value.inputJson !== undefined && { inputJson: value.inputJson }),
        })
            .then(() => setMeasuring(true))
            .finally(() => setStarting(false))
    }, [project.id, detail.tableId])

    if (measuring) {
        return <BenchmarkResultModal onClose={onClose} projectId={project.id} tableId={detail.tableId} />
    }

    return (
        <TableInputLauncher
            busy={starting}
            caseSelection="multiple"
            detail={detail}
            error={error}
            onClose={onClose}
            onError={setError}
            project={project}
            actions={collect => (
                <Button
                    data-testid="benchmark-start"
                    loading={starting}
                    type="primary"
                    onClick={() => {
                        const value = collect()
                        if (value) {
                            start(value).catch(startError => setError(errorMessage(startError)))
                        }
                    }}
                >
                    {t('benchmark.start')}
                </Button>
            )}
            onNothingToAsk={value => {
                start(value).catch(startError => {
                    notification.error({ title: t('benchmark.startFailed'), description: errorMessage(startError) })
                    onClose()
                })
            }}
        />
    )
}

/**
 * Opens the benchmark launcher under the Benchmark button of the table page.
 *
 * A test table is measured over the cases that are picked: the whole table at once when every case is asked
 * for, and each of the chosen cases on its own otherwise. Any other table is measured over the input the panel
 * collects, and a table that takes nothing is measured at once.
 */
export const BenchmarkLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TableLaunchDetail>('openBenchmarkLaunch', 'execution:benchmark.startFailed')
    return detail && project
        ? <BenchmarkLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default BenchmarkLaunchHost

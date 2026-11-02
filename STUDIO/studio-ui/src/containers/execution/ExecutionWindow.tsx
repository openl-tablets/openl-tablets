import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Empty, Result } from 'antd'
import { useTranslation } from 'react-i18next'
import {
    currentExecution, EXECUTION_EDITOR_NAVIGATION, EXECUTION_WINDOW_KEY, type ExecutionWindowRequest,
} from 'services/executionWindow'
import { RunningCard } from 'components/RunningCard'
import { useStyles } from './ExecutionScreen.styles'
import { BenchmarkResults } from './BenchmarkResults'
import { ExecutionScreen, ExecutionWindowContext } from './ExecutionScreen'
import { RunResults } from './RunResults'
import { TestsResults } from './TestsResults'

/**
 * The results window: it shows every Test, Run and Benchmark started in the editor, one after another.
 *
 * It follows a new execution as soon as the editor starts it, and shows the latest one again after a refresh.
 */
export const ExecutionWindow: React.FC = () => {
    const { t } = useTranslation('execution')
    const { styles } = useStyles()
    const [request, setRequest] = useState<ExecutionWindowRequest | null>(currentExecution)
    const [version, setVersion] = useState(0)

    useEffect(() => {
        const onStorage = (event: StorageEvent) => {
            if (event.key === EXECUTION_WINDOW_KEY || event.key === null) {
                setRequest(currentExecution())
                setVersion(current => current + 1)
            }
        }
        window.addEventListener('storage', onStorage)
        return () => window.removeEventListener('storage', onStorage)
    }, [])

    useEffect(() => {
        const previousTitle = document.title
        document.title = t('windowTitle')
        return () => { document.title = previousTitle }
    }, [t])

    const close = () => window.close()
    const openTable = useCallback((href: string): boolean => {
        if (!window.opener || window.opener.closed) return window.open(href, '_blank') !== null
        try {
            window.opener.dispatchEvent(new CustomEvent(EXECUTION_EDITOR_NAVIGATION, { detail: href }))
        } catch {
            return false
        }
        try {
            window.opener.focus()
        } catch {
            // The editor has the navigation request even if the browser refuses to focus it.
        }
        return true
    }, [])
    const windowContext = useMemo(() => ({ openTable }), [openTable])
    const content = (() => {
        if (!request) return <Empty className={styles.empty} description={t('noResults')} />
        switch (request.kind) {
            case 'starting':
                return (
                    <ExecutionScreen onClose={close} title={t(`${request.of}.title`)}>
                        <RunningCard data-testid="execution-starting" description={t('starting')} />
                    </ExecutionScreen>
                )
            case 'failed':
                return (
                    <ExecutionScreen onClose={close} title={t(`${request.of}.title`)}>
                        <Result
                            data-testid="execution-start-failed"
                            status="error"
                            subTitle={request.message}
                            title={t(`${request.of}.startFailed`)}
                        />
                    </ExecutionScreen>
                )
            case 'tests':
                return (
                    <TestsResults
                        key={version}
                        onClose={close}
                        options={request.options}
                        projectId={request.projectId}
                        tableId={request.tableId}
                    />
                )
            case 'run':
                return (
                    <RunResults
                        key={version}
                        fileOptions={request.fileOptions}
                        onClose={close}
                        projectId={request.projectId}
                        tableId={request.tableId}
                    />
                )
            case 'benchmark':
                return (
                    <BenchmarkResults
                        key={version}
                        onClose={close}
                        projectId={request.projectId}
                        tableId={request.tableId}
                    />
                )
        }
    })()

    return <ExecutionWindowContext.Provider value={windowContext}>{content}</ExecutionWindowContext.Provider>
}

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import {
    currentExecution, EXECUTION_EDITOR_NAVIGATION, EXECUTION_WINDOW_KEY, type ExecutionWindowRequest,
} from 'services/executionWindow'
import { useStyles } from './ExecutionModal.styles'
import { BenchmarkResultModal } from './BenchmarkResultModal'
import { ExecutionWindowContext } from './ExecutionModal'
import { RunResultModal } from './RunResultModal'
import { TestsResultModal } from './TestsResultModal'

/** A result screen that survives refresh and follows new executions started in the editor. */
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
    const windowContext = useMemo(() => ({ standalone: true, openTable }), [openTable])
    const content = (() => {
        if (!request) return <Empty className={styles.empty} description={t('noResults')} />
        switch (request.kind) {
            case 'tests':
                return (
                    <TestsResultModal
                        key={version}
                        onClose={close}
                        options={request.options}
                        projectId={request.projectId}
                        tableId={request.tableId}
                    />
                )
            case 'run':
                return (
                    <RunResultModal
                        key={version}
                        fileOptions={request.fileOptions}
                        onClose={close}
                        projectId={request.projectId}
                        tableId={request.tableId}
                    />
                )
            case 'benchmark':
                return (
                    <BenchmarkResultModal
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

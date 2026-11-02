import React, { createContext, useState } from 'react'
import { StopOutlined } from '@ant-design/icons'
import { Alert, Button, Flex, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { stopExecution, type ExecutionKind } from 'services/execution'
import { useListPageStyles } from 'styles/listPageStyles'
import type { MessageDescription, TraceParameterValue } from 'types/trace'
import { errorMessage } from 'utils/errorMessage'
import { useStyles } from './ExecutionScreen.styles'

/** The name a value is shown under: the column the table author wrote, or the name of the parameter. */
export const nameOf = (value: TraceParameterValue): string => value.description || value.name

/** What went wrong during an execution, one message each. */
export const ExecutionErrors: React.FC<{ errors?: MessageDescription[] | undefined }> = ({ errors }) => (
    <>
        {(errors ?? []).map(error => (
            <Alert
                key={`${error.summary}-${error.severity}`}
                showIcon
                data-testid="execution-error"
                description={error.detail}
                title={error.summary}
                type={error.severity === 'WARNING' ? 'warning' : 'error'}
            />
        ))}
    </>
)

/** What a screen needs to stop the execution it follows. */
interface ExecutionStop {
    /** Whether the reader stopped the execution from this screen. */
    stopped: boolean
    /** Says why the execution could not be stopped, when it could not. */
    stopFailed: React.ReactNode
    /** The button that stops the execution, shown while it goes on. */
    stopButton: React.ReactNode
}

/**
 * Lets the reader stop the test run, the run or the benchmark a screen follows.
 *
 * The execution stops before its next test case or run of the rules. The screen hears the end the way it hears
 * any other, and knows it was asked for here.
 *
 * @param projectId the project the execution runs in
 * @param kind      what is executed
 */
export const useStopExecution = (projectId: string, kind: ExecutionKind): ExecutionStop => {
    const { t } = useTranslation('execution')
    const [stopping, setStopping] = useState(false)
    const [stopped, setStopped] = useState(false)
    const [stopError, setStopError] = useState<string | null>(null)
    const stop = () => {
        setStopping(true)
        setStopError(null)
        stopExecution(projectId, kind)
            .then(() => setStopped(true))
            .catch(error => setStopError(errorMessage(error)))
            .finally(() => setStopping(false))
    }
    const stopButton = (
        <Button danger data-testid="execution-stop" icon={<StopOutlined />} loading={stopping} onClick={stop}>
            {t('stop')}
        </Button>
    )
    const stopFailed = stopError && <Alert showIcon data-testid="execution-stop-failed" title={stopError} type="error" />
    return { stopped, stopFailed, stopButton }
}

interface ExecutionScreenProps {
    title: string
    /** What the window has to say about the whole of what it shows, under the title. */
    subtitle?: React.ReactNode
    /** The actions of the whole window, shown at its foot before Close. */
    extra?: React.ReactNode
    onClose: () => void
    children: React.ReactNode
}

/** What the results window offers the results it shows. */
export const ExecutionWindowContext = createContext<{
    /**
     * Navigates to a table in the editor that started the execution, or opens it in a new tab. False leaves the
     * navigation to the screen itself.
     */
    openTable: (href: string) => boolean
}>({ openTable: () => false })

/**
 * The result of a run, test run, or benchmark in the results window: its title, actions, and body.
 *
 * The heading reads the way a list screen of OpenL Studio reads: the name of what is shown, and under it one
 * line about the whole of it. The actions stand at the foot of the window.
 */
export const ExecutionScreen: React.FC<ExecutionScreenProps> = ({ title, subtitle, extra, onClose, children }) => {
    const { t } = useTranslation('execution')
    const { styles } = useListPageStyles()
    const { styles: executionStyles } = useStyles()
    return (
        <main className={executionStyles.window}>
            <h1 className={styles.pageTitle}>{title}</h1>
            {subtitle !== undefined && <div className={styles.subtitle}>{subtitle}</div>}
            {children}
            <Flex className={executionStyles.actions} justify="end">
                <Space>
                    {extra}
                    <Button data-testid="execution-close" onClick={onClose}>{t('close')}</Button>
                </Space>
            </Flex>
        </main>
    )
}

export default ExecutionScreen

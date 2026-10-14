import React, { createContext, useContext, useState } from 'react'
import { ExportOutlined } from '@ant-design/icons'
import { Alert, Button, Flex, Modal, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import type { TestsQuery } from 'services/execution'
import { useListPageStyles } from 'styles/listPageStyles'
import type { MessageDescription, TraceParameterValue } from 'types/trace'
import { useStyles } from './ExecutionModal.styles'

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

interface ExecutionModalProps {
    title: string
    /** What the window has to say about the whole of what it shows, under the title. */
    subtitle?: React.ReactNode
    /** The actions of the whole window, shown in the footer before Close. */
    extra?: React.ReactNode
    /** Test result filters and page to carry into a separate window. */
    openOptions?: Partial<TestsQuery>
    onClose: () => void
    children: React.ReactNode
}

/** The same result body can be shown in the editor dialog or its separate window. */
export const ExecutionWindowContext = createContext<{
    /** Whether the result renders outside the editor dialog. */
    standalone?: boolean
    /** Opens the named browser window; false means the browser blocked it. */
    open?: (options?: Partial<TestsQuery>) => boolean
    /** Navigates to a table in the original editor when available. */
    openTable?: (href: string) => boolean
}>({})

/**
 * The result of a run, test run, or benchmark: its title, actions, and body.
 *
 * The heading reads the way a list screen of OpenL Studio reads: the name of what is shown, and under it one
 * line about the whole of it. The body appears in an editor dialog or a separate browser window.
 */
export const ExecutionModal: React.FC<ExecutionModalProps> = ({
    title, subtitle, extra, openOptions, onClose, children,
}) => {
    const { t } = useTranslation('execution')
    const { styles } = useListPageStyles()
    const { styles: executionStyles } = useStyles()
    const executionWindow = useContext(ExecutionWindowContext)
    const [windowBlocked, setWindowBlocked] = useState(false)
    const actions = (
        <Space>
            {extra}
            {executionWindow.open && (
                <Button
                    data-testid="execution-open-window"
                    icon={<ExportOutlined />}
                    onClick={() => setWindowBlocked(!executionWindow.open?.(openOptions))}
                >
                    {t('openInWindow')}
                </Button>
            )}
            <Button data-testid="execution-close" onClick={onClose}>{t('close')}</Button>
        </Space>
    )
    const heading = (
        <>
            <h1 className={styles.pageTitle}>{title}</h1>
            {subtitle !== undefined && <div className={styles.subtitle}>{subtitle}</div>}
        </>
    )
    const body = <>{windowBlocked && <Alert showIcon title={t('windowBlocked')} type="warning" />}{children}</>
    if (executionWindow.standalone) {
        return (
            <main className={executionStyles.window}>
                {heading}
                {body}
                <Flex className={executionStyles.actions} justify="end">{actions}</Flex>
            </main>
        )
    }
    return (
        <Modal
            centered
            destroyOnHidden
            open
            footer={actions}
            onCancel={onClose}
            title={heading}
            width={{ xs: '100%', sm: '100%', md: '92%', lg: '88%', xl: 1200, xxl: 1400 }}
        >
            {body}
        </Modal>
    )
}

export default ExecutionModal

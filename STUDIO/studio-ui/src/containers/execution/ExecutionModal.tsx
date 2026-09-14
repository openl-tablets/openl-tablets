import React from 'react'
import { Alert, Button, Modal, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { useListPageStyles } from 'styles/listPageStyles'
import type { MessageDescription, TraceParameterValue } from 'types/trace'

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

export interface ExecutionModalProps {
    title: string
    /** What the window has to say about the whole of what it shows, under the title. */
    subtitle?: React.ReactNode
    /** The actions of the whole window, shown in the footer before Close. */
    extra?: React.ReactNode
    onClose: () => void
    children: React.ReactNode
}

/**
 * The window a run or a test run reports in: its title, its actions, and what it has to show.
 *
 * The heading reads the way a list screen of OpenL Studio reads: the name of what is shown, and under it one
 * line about the whole of it.
 */
export const ExecutionModal: React.FC<ExecutionModalProps> = ({ title, subtitle, extra, onClose, children }) => {
    const { t } = useTranslation('execution')
    const { styles } = useListPageStyles()
    return (
        <Modal
            centered
            destroyOnHidden
            open
            onCancel={onClose}
            width={{ xs: '100%', sm: '100%', md: '92%', lg: '88%', xl: 1200, xxl: 1400 }}
            footer={(
                <Space>
                    {extra}
                    <Button data-testid="execution-close" onClick={onClose}>{t('close')}</Button>
                </Space>
            )}
            title={(
                <>
                    <h1 className={styles.pageTitle}>{title}</h1>
                    {subtitle !== undefined && <div className={styles.subtitle}>{subtitle}</div>}
                </>
            )}
        >
            {children}
        </Modal>
    )
}

export default ExecutionModal

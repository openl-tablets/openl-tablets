import React, { useCallback } from 'react'
import { Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { TraceParameterValue } from 'types/trace'
import { ParameterValueList, ParameterValueTree } from 'components/values/ParameterValues'
import { useStyles } from './TraceParameters.styles'

const { Text } = Typography

interface ParameterTreeProps {
    param: TraceParameterValue
    paramKey: string
}

/**
 * One traced value as a line of the debugger, `name (type) = value`.
 *
 * A value too large to travel with the frame is read from the trace session when the user asks for it.
 */
export const ParameterTree: React.FC<ParameterTreeProps> = ({ param, paramKey }) => {
    const { fetchLazyParameter } = useTraceStore()
    const { parameterId } = param
    const load = useCallback(
        (): Promise<TraceParameterValue> => fetchLazyParameter(parameterId as number),
        [fetchLazyParameter, parameterId]
    )
    return <ParameterValueTree onLoad={parameterId == null ? undefined : load} param={param} paramKey={paramKey} />
}

/** The frame every group of traced values sits in: its title, the copy button, and the values themselves. */
const Section: React.FC<{
    title: string
    copyButton?: React.ReactNode | undefined
    children: React.ReactNode
}> = ({ title, copyButton, children }) => {
    const { styles } = useStyles()
    return (
        <div className={styles.section}>
            <div className={styles.header}>
                <span className={styles.title}>{title}:</span>
                {copyButton}
            </div>
            {children}
        </div>
    )
}

interface TraceParametersProps {
    parameters?: TraceParameterValue[] | undefined
    title: string
    emptyText?: string | undefined
    /** Optional copy button to display next to the title */
    copyButton?: React.ReactNode | undefined
}

/**
 * The parameters a traced step received, each shown the way a debugger shows it.
 *
 * A step that received none says so instead.
 */
const TraceParameters: React.FC<TraceParametersProps> = ({ parameters, title, emptyText, copyButton }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const { fetchLazyParameter } = useTraceStore()
    const load = useCallback(
        (index: number): Promise<TraceParameterValue | undefined> => {
            // A value the trace did not register cannot be read on its own; the line keeps what it has.
            const parameterId = parameters?.[index]?.parameterId
            return parameterId == null ? Promise.resolve(undefined) : fetchLazyParameter(parameterId)
        },
        [fetchLazyParameter, parameters]
    )

    if (!parameters || parameters.length === 0) {
        return (
            <Section copyButton={copyButton} title={title}>
                <Text className={styles.empty} type="secondary">{emptyText || t('details.noParameters')}</Text>
            </Section>
        )
    }
    return (
        <Section copyButton={copyButton} title={title}>
            <ParameterValueList keyPrefix="param" onLoad={load} parameters={parameters} />
        </Section>
    )
}

/** One value of a traced step shown on its own, such as the runtime context or the result. */
export const SingleParameter: React.FC<{
    parameter?: TraceParameterValue | undefined
    title: string
    emptyText?: string | undefined
    /** Optional copy button to display next to the title */
    copyButton?: React.ReactNode | undefined
}> = ({ parameter, title, emptyText, copyButton }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()

    if (!parameter) {
        return (
            <Section copyButton={copyButton} title={title}>
                <Text className={styles.empty} type="secondary">{emptyText || t('details.noResult')}</Text>
            </Section>
        )
    }
    return (
        <Section copyButton={copyButton} title={title}>
            <ParameterTree param={parameter} paramKey="single-param" />
        </Section>
    )
}

export default TraceParameters

import React from 'react'
import { InputNumber, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useSharedStyles } from './sharedStyles'

interface VersionInputProps {
    'aria-label'?: string
    'data-testid': string
    /** Version the table being copied stands for, shown beside the field; absent when there is none to show. */
    current?: string | undefined
    onChange: (value: string) => void
    /** Marks the editor as holding a version the table cannot take. */
    status?: '' | 'error'
    value: string
}

/** Major, minor and variant: the three numbers the engine orders versions by, named for React to key them by. */
const PARTS = ['major', 'minor', 'variant'] as const

/** The three numbers a version stands for. A part that is not a number reads as zero. */
const numbersOf = (version: string): number[] => {
    const parts = version.trim().split('.')
    return PARTS.map((_, index) => {
        const part = Number(parts[index])
        return Number.isInteger(part) && part >= 0 ? part : 0
    })
}

/**
 * An editor for a table version: one number field per part, and the version the table stands for beside them.
 *
 * <p>Versions are ordered by their major, minor and variant numbers, so a version is entered as those numbers
 * rather than typed as free text — a version of any other shape leaves the engine unable to order two tables.
 *
 * <p>The three fields share the width of an ordinary value field and sit on its one line, so a property row does
 * not grow taller than the rows around it.
 */
export const VersionInput: React.FC<VersionInputProps> = ({
    current,
    onChange,
    status = '',
    value,
    'aria-label': ariaLabel,
    'data-testid': testId,
}) => {
    const { t } = useTranslation()
    const { styles } = useSharedStyles()
    const numbers = numbersOf(value)
    const labels = [
        t('project:copy_table_modal.version_major'),
        t('project:copy_table_modal.version_minor'),
        t('project:copy_table_modal.version_variant'),
    ]

    const change = (index: number, part: number | null) => onChange(
        numbers.map((number, at) => at === index ? Math.max(0, Math.trunc(part ?? 0)) : number).join('.')
    )

    return (
        <div className={styles.versionEditor} data-testid={testId}>
            {numbers.map((number, index) => (
                <React.Fragment key={PARTS[index]}>
                    {index > 0 ? <span aria-hidden>.</span> : null}
                    <InputNumber
                        aria-label={`${ariaLabel ?? ''} ${labels[index]}`.trim()}
                        controls={false}
                        data-testid={`${testId}-${index}`}
                        min={0}
                        onChange={part => change(index, part)}
                        status={status}
                        title={labels[index]}
                        value={number}
                    />
                </React.Fragment>
            ))}
            {current ? (
                <Typography.Text data-testid={`${testId}-current`} type="secondary">
                    {t('project:copy_table_modal.version_current', { version: current })}
                </Typography.Text>
            ) : null}
        </div>
    )
}

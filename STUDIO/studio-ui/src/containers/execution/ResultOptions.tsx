import React from 'react'
import { Checkbox, Select, Space, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { ALL_FAILURES, FAILURES_PER_TEST, FAILURES_PER_TEST_OPTIONS } from 'constants/tests'
import type { UserProfile } from 'types/user'

const { Text } = Typography

interface CountSelectProps {
    /** What the count is of, written beside it. */
    label: string
    value: number
    /** The counts offered, one of which stands for all of them. */
    counts: readonly number[]
    /** The count that means all of them; it is written as `All` rather than as a number. */
    allValue: number
    onChange: (count: number) => void
    disabled?: boolean
    'data-testid': string
}

/**
 * A count the reader picks from a few, the largest of which is `All`.
 *
 * <p>How many failures of one test table to list, and how many test tables to a page, are both read this way.
 */
export const CountSelect: React.FC<CountSelectProps> = ({
    label, value, counts, allValue, onChange, disabled, 'data-testid': testId,
}) => {
    const { t } = useTranslation('execution')
    return (
        <Space size={4}>
            <Text type="secondary">{label}</Text>
            <Select<number>
                data-testid={testId}
                disabled={disabled ?? false}
                onChange={onChange}
                size="small"
                style={{ width: 80 }}
                value={value}
                options={counts.map(count => ({
                    value: count,
                    label: count === allValue ? t('tests.all') : String(count),
                }))}
            />
        </Space>
    )
}

/** What the results show of the cases that failed: only those, and how many of one test table. */
export interface FailuresChoice {
    failuresOnly: boolean
    failures: number
}

/**
 * The option as the user last saved it.
 *
 * <p>A count the server would refuse falls back to the default: the profile takes whatever is written to it,
 * and nothing there keeps a count out of it.
 */
export const savedFailuresOption = (profile: UserProfile | null | undefined): FailuresChoice => {
    const saved = profile?.testsFailuresPerTest ?? 0
    return {
        failuresOnly: profile?.testsFailuresOnly ?? false,
        failures: saved >= 1 || saved === ALL_FAILURES ? saved : FAILURES_PER_TEST,
    }
}

interface FailuresOptionProps extends FailuresChoice {
    onChange: (change: Partial<FailuresChoice>) => void
}

/**
 * Whether only the failures are listed, and how many of one test table.
 *
 * <p>The two belong together: the count says how long a list of failures is allowed to get, so it is only
 * read while the failures are what is listed, and it waits, disabled, until then.
 *
 * <p>The panel that starts a run and the window that reports it both carry it, so a run starts with the
 * option the results are then read with.
 */
export const FailuresOption: React.FC<FailuresOptionProps> = ({ failuresOnly, failures, onChange }) => {
    const { t } = useTranslation('execution')
    return (
        <Space size="middle">
            <Checkbox
                checked={failuresOnly}
                data-testid="tests-failures-only"
                onChange={event => onChange({ failuresOnly: event.target.checked })}
            >
                {t('tests.failuresOnly')}
            </Checkbox>
            <CountSelect
                allValue={ALL_FAILURES}
                counts={FAILURES_PER_TEST_OPTIONS}
                data-testid="tests-failures"
                disabled={!failuresOnly}
                label={t('tests.failuresPerTest')}
                onChange={count => onChange({ failures: count })}
                value={failures}
            />
        </Space>
    )
}

interface CompoundResultOptionProps {
    checked: boolean
    onChange: (checked: boolean) => void
}

/** Whether every case also carries the whole value the rule returned, and not only the compared values. */
export const CompoundResultOption: React.FC<CompoundResultOptionProps> = ({ checked, onChange }) => {
    const { t } = useTranslation('execution')
    return (
        <Checkbox
            checked={checked}
            data-testid="tests-compound-result"
            onChange={event => onChange(event.target.checked)}
        >
            {t('tests.compoundResult')}
        </Checkbox>
    )
}

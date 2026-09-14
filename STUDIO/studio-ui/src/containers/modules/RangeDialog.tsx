import React, { useEffect, useState } from 'react'
import { Alert, InputNumber, Modal, Select, Space, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { numberOnly } from './numberOnly'
import { formatRange, NO_RANGE, parseRange, type RangeBounds, rangeProblem } from './rangeValue'

interface RangeDialogProps {
    open: boolean
    /** What the cell holds now, which the bounds start from. */
    value: string
    /** Whether the cell holds whole numbers only. */
    intOnly?: boolean | undefined
    /** Told the text to write into the cell. */
    onWrite: (value: string) => void
    onCancel: () => void
}

/**
 * The bounds of a range, entered as bounds rather than typed as text.
 *
 * <p>A cell holding a range is written the way OpenL prints one, so what is entered here reads back exactly as
 * it was meant. Leaving a bound empty leaves that side open: only a lower bound reads "greater than", only an
 * upper one "less than".
 */
export const RangeDialog: React.FC<RangeDialogProps> = ({ open, value, intOnly, onWrite, onCancel }) => {
    const { t } = useTranslation('repository')
    const [bounds, setBounds] = useState<RangeBounds>(NO_RANGE)

    // Each opening starts from what the cell holds at that moment.
    useEffect(() => {
        if (open) {
            setBounds(parseRange(value))
        }
    }, [open, value])

    const numeric = numberOnly(intOnly)
    const problem = rangeProblem(bounds)
    const written = formatRange(bounds)

    const bound = (
        side: 'from' | 'to',
        included: 'fromIncluded' | 'toIncluded',
        closed: string,
        opened: string
    ) => (
        <Space>
            <Typography.Text>{t(`browser.module.range_${side}`)}</Typography.Text>
            <Select
                data-testid={`range-${side}-bound`}
                onChange={chosen => setBounds(current => ({ ...current, [included]: chosen === 'closed' }))}
                style={{ width: 72 }}
                value={bounds[included] ? 'closed' : 'open'}
                options={[
                    { value: 'closed', label: closed },
                    { value: 'open', label: opened },
                ]}
            />
            <InputNumber
                data-testid={`range-${side}`}
                onChange={entered => setBounds(current => ({
                    ...current,
                    [side]: entered == null ? '' : String(entered),
                }))}
                {...(intOnly ? { precision: 0 } : {})}
                stringMode
                onKeyDown={numeric.onKeyDown}
                onPaste={numeric.onPaste}
                style={{ width: 160 }}
                value={bounds[side] === '' ? null : bounds[side]}
            />
        </Space>
    )

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ disabled: problem !== null, 'data-testid': 'range-write' }}
            okText={t('browser.module.range_write')}
            onCancel={onCancel}
            onOk={() => onWrite(written)}
            open={open}
            title={t('browser.module.range_title')}
            width={420}
        >
            <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                {bound('from', 'fromIncluded', '≥', '>')}
                {bound('to', 'toIncluded', '≤', '<')}
                {problem === null
                    ? (
                        <Typography.Text data-testid="range-preview" type="secondary">
                            {t('browser.module.range_preview', { value: written })}
                        </Typography.Text>
                    )
                    : <Alert showIcon message={t(`browser.module.range_${problem}`)} type="warning" />}
            </Space>
        </Modal>
    )
}

export default RangeDialog

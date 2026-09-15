import React, { useState } from 'react'
import { Alert, Button, Checkbox, Divider, InputNumber, Segmented, Space, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { createStyles } from 'antd-style'
import { numberOnly } from './numberOnly'
import { formatRange, parseRange, type RangeBounds, rangeProblem, type RangeShape, shapeOf } from './rangeValue'

/**
 * The class the panel is marked with, so the screen around it can tell a click inside the panel from one that
 * leaves it. A test id says what a test looks for; what the page itself acts on is a class of its own.
 */
export const RANGE_PANEL = 'openl-range-panel'

const useStyles = createStyles(({ css, token }) => ({
    /** The panel hangs under the cell, so it is sized by what it holds rather than by the cell. */
    panel: css`
        display: flex;
        flex-direction: column;
        min-width: 240px;
    `,
    /** The shapes stand in a column beside the bounds, the way the old editor stacked them. */
    shapes: css`
        display: flex;
        gap: ${token.margin}px;
        align-items: flex-start;
    `,
    bounds: css`
        flex: 1;
        width: 100%;
    `,
    /** What the cell will hold, read back in the wording OpenL prints. */
    result: css`
        text-align: center;
        font-weight: 600;
    `,
    done: css`
        display: flex;
        justify-content: center;
        margin-top: ${token.marginXS}px;
    `,
}))

/** The shapes the reader picks between, in the order the old editor stacked them. */
const SHAPES = [
    { value: 'at-least' as const, label: '>', title: 'range_at_least' },
    { value: 'at-most' as const, label: '<', title: 'range_at_most' },
    { value: 'between' as const, label: '−', title: 'range_between' },
    { value: 'exact' as const, label: '=', title: 'range_exact' },
]

/**
 * The bounds as the chosen shape holds them.
 *
 * <p>Changing shape keeps the number the reader already entered — a "less than 100" made "more than" is about
 * 100 still — and drops the bound the new shape has no room for.
 */
const reshape = (bounds: RangeBounds, shape: RangeShape): RangeBounds => {
    const single = bounds.from.trim() !== '' ? bounds.from : bounds.to
    switch (shape) {
        case 'at-least':
            return { ...bounds, from: single, to: '' }
        case 'at-most':
            return { ...bounds, from: '', to: single }
        case 'exact':
            return { from: single, to: single, fromIncluded: true, toIncluded: true }
        case 'between':
        default:
            return { ...bounds, from: single, to: bounds.to.trim() === '' ? bounds.from : bounds.to }
    }
}

interface RangeEditorProps {
    /** What the cell holds now, which the bounds start from. */
    value: string
    /** Whether the cell holds whole numbers only. */
    intOnly?: boolean | undefined
    /** Told the text to write into the cell. */
    onWrite: (value: string) => void
}

/**
 * The bounds of a range, entered under the cell rather than typed into it.
 *
 * <p>A cell holding a range is written the way OpenL prints one, so what is entered here reads back exactly as
 * it was meant. The shape is picked first — more than, less than, between, exactly — and the bounds it takes
 * follow from it, as the old editor did it.
 */
export const RangeEditor: React.FC<RangeEditorProps> = ({ value, intOnly, onWrite }) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    // The panel opens on what the cell holds, read once: it is drawn afresh for each cell it hangs under, and
    // that cell cannot be typed into while it stands open.
    const [entered, setEntered] = useState(() => {
        const read = parseRange(value)
        return { bounds: read, shape: shapeOf(read) }
    })
    const { bounds, shape } = entered
    const setBounds = (next: (current: RangeBounds) => RangeBounds) =>
        setEntered(current => ({ ...current, bounds: next(current.bounds) }))

    const numeric = numberOnly(intOnly)
    const problem = rangeProblem(bounds)
    const written = formatRange(bounds)

    /** A bound the reader enters. The exact shape has one number standing for both bounds, so it writes both. */
    const enter = (side: 'from' | 'to', entered: string) => setBounds(current => (shape === 'exact'
        ? { ...current, from: entered, to: entered }
        : { ...current, [side]: entered }))

    const number = (side: 'from' | 'to') => (
        <InputNumber
            data-testid={`range-${side}`}
            onChange={entered => enter(side, entered == null ? '' : String(entered))}
            {...(intOnly ? { precision: 0 } : {})}
            stringMode
            onKeyDown={numeric.onKeyDown}
            onPaste={numeric.onPaste}
            size="small"
            style={{ width: '100%' }}
            value={bounds[side] === '' ? null : bounds[side]}
        />
    )

    const include = (side: 'fromIncluded' | 'toIncluded') => (
        <Checkbox
            checked={bounds[side]}
            data-testid={`range-${side === 'fromIncluded' ? 'from' : 'to'}-included`}
            onChange={event => setBounds(current => ({ ...current, [side]: event.target.checked }))}
        >
            {t('browser.module.range_include')}
        </Checkbox>
    )

    return (
        <div className={cx(styles.panel, RANGE_PANEL)} data-testid="range-editor">
            <div className={styles.shapes}>
                <Segmented
                    vertical
                    size="small"
                    value={shape}
                    onChange={picked => setEntered(current => ({
                        shape: picked,
                        bounds: reshape(current.bounds, picked),
                    }))}
                    options={SHAPES.map(({ value: picked, label, title }) => ({
                        value: picked,
                        label: (
                            <Tooltip placement="left" title={t(`browser.module.${title}`)}>
                                <span data-testid={`range-shape-${picked}`}>{label}</span>
                            </Tooltip>
                        ),
                    }))}
                />
                <Space className={styles.bounds} orientation="vertical" size={4}>
                    {shape === 'between' && (
                        <>
                            <Typography.Text type="secondary">{t('browser.module.range_from')}</Typography.Text>
                            {number('from')}
                            {include('fromIncluded')}
                            <Typography.Text type="secondary">{t('browser.module.range_to')}</Typography.Text>
                        </>
                    )}
                    {number(shape === 'at-least' || shape === 'exact' ? 'from' : 'to')}
                    {shape !== 'exact' && include(shape === 'at-least' ? 'fromIncluded' : 'toIncluded')}
                </Space>
            </div>
            <Divider size="small" />
            {problem === null
                ? (
                    <Typography.Text className={styles.result} data-testid="range-preview">
                        {written}
                    </Typography.Text>
                )
                : <Alert showIcon message={t(`browser.module.range_${problem}`)} type="warning" />}
            <div className={styles.done}>
                <Button
                    data-testid="range-write"
                    disabled={problem !== null}
                    onClick={() => onWrite(written)}
                    size="small"
                >
                    {t('browser.module.range_write')}
                </Button>
            </div>
        </div>
    )
}

export default RangeEditor

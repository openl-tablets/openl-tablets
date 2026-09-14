import React, { useEffect, useState } from 'react'
import { Alert, Button, Checkbox, Divider, InputNumber, Segmented, Space, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { createStyles } from 'antd-style'
import { numberOnly } from './numberOnly'
import { formatRange, parseRange, type RangeBounds, rangeProblem } from './rangeValue'

/** The four shapes a range takes, which the reader picks between as the old editor let them. */
type RangeShape = 'at-least' | 'at-most' | 'between' | 'exact'

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

/** The shape the bounds already have, which is the one the panel opens on. */
const shapeOf = (bounds: RangeBounds): RangeShape => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    if (lower !== '' && upper !== '') {
        return lower === upper && bounds.fromIncluded && bounds.toIncluded ? 'exact' : 'between'
    }
    return upper !== '' ? 'at-most' : 'at-least'
}

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
    const { styles } = useStyles()
    const [bounds, setBounds] = useState<RangeBounds>(() => parseRange(value))
    const [shape, setShape] = useState<RangeShape>(() => shapeOf(parseRange(value)))

    // The panel opens on what the cell holds at that moment, whichever cell it was opened over.
    useEffect(() => {
        const read = parseRange(value)
        setBounds(read)
        setShape(shapeOf(read))
    }, [value])

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

    const shapes = [
        { value: 'at-least' as const, label: '>', title: t('browser.module.range_at_least') },
        { value: 'at-most' as const, label: '<', title: t('browser.module.range_at_most') },
        { value: 'between' as const, label: '−', title: t('browser.module.range_between') },
        { value: 'exact' as const, label: '=', title: t('browser.module.range_exact') },
    ]

    return (
        <div className={styles.panel} data-testid="range-editor">
            <div className={styles.shapes}>
                <Segmented
                    vertical
                    size="small"
                    value={shape}
                    onChange={picked => {
                        setShape(picked)
                        setBounds(current => reshape(current, picked))
                    }}
                    options={shapes.map(({ value: shapeValue, label, title }) => ({
                        value: shapeValue,
                        label: <Tooltip placement="left" title={title}><span data-testid={`range-shape-${shapeValue}`}>{label}</span></Tooltip>,
                    }))}
                />
                <Space className={styles.bounds} orientation="vertical" size={4} style={{ width: '100%' }}>
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

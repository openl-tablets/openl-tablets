import React from 'react'
import { Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { DiffInputTooLargeError, diffLines, type DiffLine } from 'utils/lineDiff'
import { useStyles } from './ComparePage.styles'

interface TextDiffViewProps {
    /** The version the comparison starts from: a line only it has is removed. */
    first: string
    /** The version it is compared with: a line only it has is added. */
    second: string
    /** Whether the line numbers of the second version come first, as when it is shown on the left. */
    secondOnLeft?: boolean | undefined
}

/** What a line is drawn with: the sign a reader of a diff expects before it. */
export const SIGNS: Record<DiffLine['kind'], string> = { add: '+', remove: '-', context: ' ' }

/**
 * The two versions of a file that is not a workbook, line by line.
 *
 * A line only the second version has is added, one only the first has is removed, and the rest reads
 * as it stands in both. Two versions that read the same say so rather than showing nothing.
 *
 * The line numbers of the first version come first, unless the second version is shown on the left: the signs stay
 * as they are, since the comparison still starts from the first version.
 */
export const TextDiffView: React.FC<TextDiffViewProps> = ({ first, second, secondOnLeft = false }) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()

    let lines: DiffLine[]
    try {
        lines = diffLines(first, second)
    } catch (error) {
        if (!(error instanceof DiffInputTooLargeError)) {
            throw error
        }
        return <Empty description={t('too_large')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    if (lines.every(line => line.kind === 'context')) {
        return <Empty description={t('identical')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
        <div className={styles.diff} data-testid="compare-text-diff">
            {lines.map((line, index) => (
                <div
                    key={`${line.oldNumber ?? ''}-${line.newNumber ?? ''}-${index}`}
                    className={cx(styles.diffLine,
                        line.kind === 'add' && styles.add,
                        line.kind === 'remove' && styles.remove)}
                >
                    <span className={styles.diffNumber}>{(secondOnLeft ? line.newNumber : line.oldNumber) ?? ''}</span>
                    <span className={styles.diffNumber}>{(secondOnLeft ? line.oldNumber : line.newNumber) ?? ''}</span>
                    <span className={styles.diffText}>{`${SIGNS[line.kind]} ${line.text}`}</span>
                </div>
            ))}
        </div>
    )
}

export default TextDiffView

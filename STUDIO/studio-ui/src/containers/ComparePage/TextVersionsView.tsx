import React, { useEffect, useState } from 'react'
import { Alert, Empty, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorMessage } from 'utils/errorMessage'
import { SIGNS, TextDiffView } from './TextDiffView'
import { useStyles } from './ComparePage.styles'

interface TextVersionsViewProps {
    /** Reads the two versions of the file: the one shown first, then the one it is compared with. */
    read: () => Promise<[string, string]>
    /** The names of the two versions, when the lines are to say which version they come from. */
    titles?: { first: string; second: string } | undefined
    /** Whether the second version is shown on the left: it heads the legend and its line numbers come first. */
    secondOnLeft?: boolean | undefined
}

/**
 * How many first characters of a version are looked through for a NUL character. Git looks through as many first
 * bytes of a file to tell binary content, and a character takes one byte or more, so the view looks at least as far.
 */
const BINARY_HEAD = 8000

/** Whether a version is binary content, such as a library or a picture, rather than text. */
const isBinary = (version: string): boolean => version.slice(0, BINARY_HEAD).includes('\u0000')

/**
 * Two versions of a file that is not a workbook, read line by line.
 *
 * The versions are read once, as the view is shown; a failure to read them is told in place of the diff. Named
 * versions are told above the lines: a line removed is one of the first version, a line added one of the second.
 *
 * Versions of binary content, such as a library or a picture, have no lines to compare: the view says so instead.
 */
export const TextVersionsView: React.FC<TextVersionsViewProps> = ({ read, titles, secondOnLeft = false }) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    // The two versions of the file, or null while they are being read.
    const [versions, setVersions] = useState<[string, string] | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let cancelled = false
        read()
            .then((texts) => {
                if (!cancelled) {
                    setVersions(texts)
                }
            })
            .catch((readError: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(readError) || t('failed'))
                }
            })
        return () => {
            cancelled = true
        }
    }, [read, t])

    // What the signs of the lines stand for: a line removed is of the first version, a line added of the second.
    const legend = titles && [
        { sign: SIGNS.remove, className: styles.removed, name: titles.first },
        { sign: SIGNS.add, className: styles.added, name: titles.second },
    ]
    // Binary content has no lines to compare, and so no signs to explain.
    const binary = !!versions && versions.some(isBinary)

    return (
        <div className={styles.column}>
            {legend && !binary && (
                <div className={cx(styles.legend, styles.textLegend)} data-testid="compare-text-legend">
                    {(secondOnLeft ? legend.toReversed() : legend).map(({ sign, className, name }) => (
                        <span key={sign}>
                            <span className={cx(styles.legendSign, className)}>{sign}</span>
                            {name}
                        </span>
                    ))}
                </div>
            )}
            <div className={styles.body} data-testid="compare-conflict-text">
                {error && <Alert showIcon data-testid="compare-error" title={error} type="error" />}
                {!error && !versions && (
                    <div className={styles.center}>
                        <Spin description={t('comparing')} />
                    </div>
                )}
                {binary && (
                    <div className={styles.center} data-testid="compare-not-text">
                        <Empty description={t('not_text')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    </div>
                )}
                {versions && !binary && (
                    <TextDiffView first={versions[0]} second={versions[1]} secondOnLeft={secondOnLeft} />
                )}
            </div>
        </div>
    )
}

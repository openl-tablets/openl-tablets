import React, { useEffect, useState } from 'react'
import { Alert, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorMessage } from 'utils/errorMessage'
import { TextDiffView } from './TextDiffView'
import { useStyles } from './ComparePage.styles'

interface TextVersionsViewProps {
    /** Reads the two versions of the file: the one shown first, then the one it is compared with. */
    read: () => Promise<[string, string]>
}

/**
 * Two versions of a file that is not a workbook, read line by line.
 *
 * The versions are read once, as the view is shown; a failure to read them is told in place of the diff.
 */
export const TextVersionsView: React.FC<TextVersionsViewProps> = ({ read }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
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

    return (
        <div className={styles.column}>
            <div className={styles.body} data-testid="compare-conflict-text">
                {error && <Alert showIcon data-testid="compare-error" title={error} type="error" />}
                {!error && !versions && (
                    <div className={styles.center}>
                        <Spin description={t('comparing')} />
                    </div>
                )}
                {versions && <TextDiffView first={versions[0]} second={versions[1]} />}
            </div>
        </div>
    )
}

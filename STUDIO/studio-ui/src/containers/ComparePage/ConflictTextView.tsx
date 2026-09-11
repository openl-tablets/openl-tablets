import React, { useEffect, useState } from 'react'
import { Alert, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { getConflictFileText } from 'services/compare'
import { errorMessage } from 'utils/errorMessage'
import { TextDiffView } from './TextDiffView'
import { useStyles } from './ComparePage.styles'

interface ConflictTextViewProps {
    projectId: string
    /** The conflicted file, by its path inside the project. */
    path: string
}

/** The two versions of the file, or null while they are being read. */
interface Versions {
    theirs: string
    ours: string
}

/**
 * The two versions of a conflicted file that is not a workbook, read line by line.
 *
 * The version being merged in is read first and the one the workspace holds second, so the diff reads
 * as what the merge would bring.
 */
export const ConflictTextView: React.FC<ConflictTextViewProps> = ({ projectId, path }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [versions, setVersions] = useState<Versions | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let cancelled = false
        Promise.all([
            getConflictFileText(projectId, path, 'THEIRS'),
            getConflictFileText(projectId, path, 'OURS'),
        ])
            .then(([theirs, ours]) => {
                if (!cancelled) {
                    setVersions({ theirs, ours })
                }
            })
            .catch((failure: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(failure) || t('failed'))
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, path, t])

    return (
        <div className={styles.column}>
            <div className={styles.body} data-testid="compare-conflict-text">
                {error && <Alert showIcon data-testid="compare-error" message={error} type="error" />}
                {!error && !versions && (
                    <div className={styles.center}>
                        <Spin description={t('comparing')} />
                    </div>
                )}
                {versions && <TextDiffView first={versions.theirs} second={versions.ours} />}
            </div>
        </div>
    )
}

export default ConflictTextView

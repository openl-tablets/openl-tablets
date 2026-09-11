import React from 'react'
import { useTranslation } from 'react-i18next'
import type { ConflictFileStatus } from 'services/compare'
import { useStyles } from './ComparePage.styles'

interface ConflictHeadProps {
    /** The conflicted file, by its path inside the repository. */
    path: string
    /** What the merge did to it, or null while that is still being read. */
    status: ConflictFileStatus | null
}

/**
 * Which file is compared and what the merge did to it, as the old comparison window said it.
 *
 * The window is opened away from the screen that asked for it, so it names the file itself rather than
 * leaving the reader to remember which of the conflicted files this is.
 */
export const ConflictHead: React.FC<ConflictHeadProps> = ({ path, status }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()

    return (
        <div className={styles.conflictHead} data-testid="compare-conflict-head">
            <span>{`${t('file_name')}: ${path.split('/').pop() ?? path}`}</span>
            {status && <span>{`${t('file_status')}: ${t(`status_${status}`)}`}</span>}
        </div>
    )
}

export default ConflictHead

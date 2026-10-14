import React from 'react'
import { useTranslation } from 'react-i18next'
import type { ConflictFileStatus } from 'services/compare'
import { useStyles } from './ComparePage.styles'

interface ConflictHeadProps {
    /** The compared file, by its path. */
    path: string
    /** What the merge did to a conflicted file; null while that is still being read, and for any other file. */
    status: ConflictFileStatus | null
    /** What the head offers at the end of its line: for a conflicted file, the views of it. */
    children?: React.ReactNode
}

/** What the merge did to the file, in the words the rest of the comparison uses for it. */
const statusKey = (status: ConflictFileStatus): string => `status_${status}`

/**
 * Which file is compared and, for a conflicted file, what the merge did to it, as the old comparison window said it.
 *
 * The window is opened away from the screen that asked for it, so it names the file itself rather than
 * leaving the reader to remember which file this is.
 */
export const ConflictHead: React.FC<ConflictHeadProps> = ({ path, status, children }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()

    return (
        <div className={styles.conflictHead} data-testid="compare-conflict-head">
            <span>{`${t('file_name')}: ${path.split('/').pop() ?? path}`}</span>
            {status && <span>{`${t('file_status')}: ${t(statusKey(status))}`}</span>}
            {children && <div className={styles.headTail}>{children}</div>}
        </div>
    )
}

export default ConflictHead

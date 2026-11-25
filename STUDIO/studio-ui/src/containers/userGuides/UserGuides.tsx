import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Empty, Skeleton } from 'antd'
import { useTranslation } from 'react-i18next'
import { useLocation, useParams } from 'react-router'
import { ResizeHandle, useDragSize } from 'components/ResizeHandle'
import { fetchGuidesContents, type GuideEntry } from 'services/userGuides'
import { errorHandler } from 'utils/errorHandling'
import { useStoredSetting } from '../../hooks/useStoredSetting'
import { FoldedRail, FoldRailButton, railFolded } from '../projects/RailFold'
import { useSharedStyles } from '../projects/sharedStyles'
import { GuidePage } from './GuidePage'
import { GuidesSearch } from './GuidesSearch'
import { GuidesTree } from './GuidesTree'
import { pagesOf, routeKey } from './guidePaths'
import { useStyles } from './UserGuides.styles'

const WIDTH_STORAGE_KEY = 'openl.guides.rail.width'
/** How wide the table of contents may be dragged, so the titles of the pages fit. */
const WIDTH = { min: 200, max: 640, fallback: 300 }
/** Whether the reader folded the table of contents away, kept for the next visit. */
const contentsFolded = railFolded('openl.guides.rail.collapsed')

/**
 * The user guides OpenL Studio ships, opened at `/docs`: the table of contents on the left, the page on the right.
 *
 * The address below `/docs` names the page the way the documentation site does, so a page is opened, bookmarked and
 * linked at the same path as there. The screen is loaded with the first visit to the guides, with everything it draws
 * them with.
 *
 * The table of contents folds away, as the rail of the projects does, to give the page the whole width. Folded, it
 * keeps the folders the reader opened, and a search typed in the header unfolds it to show what it found.
 */
const UserGuides: React.FC = () => {
    const { t } = useTranslation()
    const { styles, cx } = useStyles()
    const { styles: shared } = useSharedStyles()
    const { '*': route = '' } = useParams()
    const { hash } = useLocation()
    const { size: railWidth, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'right', WIDTH)
    const [folded, fold] = useStoredSetting(contentsFolded)
    const contentRef = useRef<HTMLElement>(null)
    const [contents, setContents] = useState<GuideEntry>()
    const [failed, setFailed] = useState(false)

    useEffect(() => {
        let current = true
        fetchGuidesContents()
            .then(read => {
                if (current) {
                    setContents(read)
                }
            })
            .catch((error: unknown) => {
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
                if (current) {
                    setFailed(true)
                }
            })
        return () => {
            current = false
        }
    }, [])

    const pages = useMemo(() => (contents ? pagesOf(contents) : undefined), [contents])
    const entry = pages?.get(routeKey(route))

    if (failed) {
        return <Alert showIcon className={styles.state} title={t('guides:load_failed')} type="error" />
    }
    if (!contents) {
        return <Skeleton active className={styles.state} />
    }
    return (
        <div className={styles.frame}>
            {folded && <FoldedRail name={t('guides:rail')} onUnfold={() => fold(false)} testId="guides-rail" />}
            {/* Folded, the rail stays in the page out of sight: the header keeps its search of the guides. */}
            <nav className={styles.rail} data-testid="guides-rail" hidden={folded} style={{ width: railWidth }}>
                <div className={cx(shared.railHead, styles.railHead)}>
                    {t('guides:contents')}
                    <span className={shared.railHeadEnd}>
                        <FoldRailButton onFold={() => fold(true)} testId="guides-rail" />
                    </span>
                </div>
                <GuidesSearch contents={contents} current={entry} onSearch={() => fold(false)}>
                    <GuidesTree contents={contents} current={entry} />
                </GuidesSearch>
                <ResizeHandle edge="right" onPointerDown={startResize} testId="guides-rail-resizer" />
            </nav>
            <section ref={contentRef} className={styles.content}>
                {entry?.file
                    ? <GuidePage container={contentRef} file={entry.file} hash={hash} title={entry.title} />
                    : <Empty className={styles.state} description={t('guides:page_not_found')} />}
            </section>
        </div>
    )
}

export default UserGuides

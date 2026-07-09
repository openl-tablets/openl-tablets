import { useCallback, useEffect, useRef, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Button, Empty, Input, notification, Skeleton, Switch, Tag } from 'antd'
import { DiffOutlined, FolderOpenOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    getProjectRevisions,
    isProjectModifiedConflict,
    openProjectRevision,
    REVISIONS_PAGE_SIZE,
    type ProjectRevision,
} from '../../services/repositories'
import { formatDateTime } from '../../utils/dateFormat'
import { MOCKUP } from './projectsTheme'
import { GitCommitMessage } from './GitCommitMessage'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { ProjectRevisionCompareModal } from './ProjectRevisionCompareModal'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        padding: 16px;
    `,
    filters: css`
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 16px;
        flex-wrap: wrap;
    `,
    search: css`
        max-width: 360px;
    `,
    techToggle: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
    compareAction: css`
        margin-left: auto;
    `,
    timeline: css`
        margin: 0;
        padding: 0;
        list-style: none;
    `,
    item: css`
        display: flex;
        gap: 12px;
    `,
    rail: css`
        display: flex;
        flex-direction: column;
        align-items: center;
    `,
    dot: css`
        margin-top: 6px;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: ${token.colorBorder};
        box-shadow: 0 0 0 4px ${token.colorBgContainer};
        z-index: 1;
    `,
    dotCurrent: css`
        background: ${token.colorPrimary};
    `,
    line: css`
        flex: 1;
        width: 1px;
        background: ${token.colorBorderSecondary};
    `,
    card: css`
        flex: 1;
        margin-bottom: 12px;
        padding: 12px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
    `,
    cardHead: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
    `,
    msgRow: css`
        display: flex;
        align-items: center;
        gap: 8px;
    `,
    msg: css`
        font-size: 14px;
    `,
    tag: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 11px;
    `,
    meta: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 4px 12px;
        margin-top: 4px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    actions: css`
        display: flex;
        gap: 4px;
        flex: none;
    `,
    hash: css`
        font-family: ${MOCKUP.fontMono};
    `,
    loadMore: css`
        display: flex;
        justify-content: center;
        margin-top: 4px;
    `,
}))

interface RevisionsPanelProps {
    /** URL-safe project id, used for opening a revision. */
    projectId: string
    /** Design repository id, project name and current branch — the history endpoint's coordinates. */
    repositoryId: string
    projectName: string
    /** Revision currently opened by the project. */
    currentRevision?: string | null
    branch?: string | null
    searchable?: boolean | undefined
    canCompare?: boolean | undefined
    onOpened: () => void
    /** Bumped when the project reloads (e.g. after a save), forcing the history to refetch. */
    reloadToken?: number
}

const authorName = (revision: ProjectRevision): string =>
    revision.author?.displayName || revision.author?.email || '—'

/**
 * A project's revision history as a timeline, backed by the design-repository history API (the same one
 * the legacy UI uses). Supports a text search, a technical-revisions toggle and incremental paging. Any
 * revision can be opened for viewing, mirroring the legacy UI. History refetches whenever the project
 * reloads, so a new revision from a save appears immediately.
 */
export const RevisionsPanel = ({
    projectId,
    repositoryId,
    projectName,
    currentRevision,
    branch,
    searchable = true,
    canCompare = false,
    onOpened,
    reloadToken,
}: RevisionsPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [items, setItems] = useState<ProjectRevision[]>([])
    const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
    const [page, setPage] = useState(0)
    const [pageInfo, setPageInfo] = useState({ pageSize: REVISIONS_PAGE_SIZE, numberOfElements: 0, total: null as number | null })
    const [loadingMore, setLoadingMore] = useState(false)
    const [search, setSearch] = useState('')
    const [appliedSearch, setAppliedSearch] = useState('')
    const [techRevs, setTechRevs] = useState(false)
    const [opening, setOpening] = useState<string | null>(null)
    const [discardOpenRevision, setDiscardOpenRevision] = useState<string | null>(null)
    const [compareSelection, setCompareSelection] = useState<string[]>([])
    const [comparePair, setComparePair] = useState<{ from: ProjectRevision, to: ProjectRevision } | null>(null)
    const fetchGeneration = useRef(0)
    const revisionQuery = useCallback((page: number) => ({
        ...(searchable && appliedSearch ? { search: appliedSearch } : {}),
        techRevs: searchable && techRevs,
        page,
    }), [appliedSearch, searchable, techRevs])

    // Debounce the search box so typing does not fire a request per keystroke.
    useEffect(() => {
        const id = setTimeout(() => setAppliedSearch(search), 300)
        return () => clearTimeout(id)
    }, [search])

    useEffect(() => {
        let cancelled = false
        const generation = fetchGeneration.current + 1
        fetchGeneration.current = generation
        setStatus('loading')
        setPage(0)
        setLoadingMore(false)
        getProjectRevisions(repositoryId, projectName, branch ?? null, revisionQuery(0))
            .then(response => {
                if (cancelled || generation !== fetchGeneration.current) {
                    return
                }
                setItems(response.content)
                setPageInfo({ pageSize: response.pageSize, numberOfElements: response.numberOfElements, total: response.total })
                setStatus('ready')
            })
            .catch(() => { if (!cancelled && generation === fetchGeneration.current) setStatus('error') })
        return () => { cancelled = true }
    }, [repositoryId, projectName, branch, reloadToken, revisionQuery])

    const hasMore = searchable && (pageInfo.total !== null
        ? items.length < pageInfo.total
        : pageInfo.numberOfElements === pageInfo.pageSize && pageInfo.numberOfElements > 0)

    useEffect(() => {
        setCompareSelection(prev => prev.filter(revisionNo =>
            items.some(revision => revision.revisionNo === revisionNo && !revision.deleted && !revision.technicalRevision)
        ))
    }, [items])

    const loadMore = async () => {
        const next = page + 1
        const generation = fetchGeneration.current
        setLoadingMore(true)
        try {
            const response = await getProjectRevisions(repositoryId, projectName, branch ?? null, revisionQuery(next))
            if (generation !== fetchGeneration.current) {
                return
            }
            setItems(prev => [...prev, ...response.content])
            setPage(next)
            setPageInfo({ pageSize: response.pageSize, numberOfElements: response.numberOfElements, total: response.total })
        } catch (e) {
            if (generation === fetchGeneration.current) {
                notification.error({ title: t('browser.history.error'), description: errorMessage(e) })
            }
        } finally {
            if (generation === fetchGeneration.current) {
                setLoadingMore(false)
            }
        }
    }

    const open = async (revisionNo: string, discardChanges = false) => {
        setOpening(revisionNo)
        try {
            await openProjectRevision(projectId, revisionNo, discardChanges ? { discardChanges: true } : {})
            onOpened()
        } catch (e) {
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardOpenRevision(revisionNo)
                return
            }
            notification.error({ title: t('browser.history.open_failed'), description: errorMessage(e) })
        } finally {
            setOpening(null)
        }
    }

    const toggleCompare = (revisionNo: string) => {
        setCompareSelection(prev => prev.includes(revisionNo)
            ? prev.filter(selected => selected !== revisionNo)
            : [...prev, revisionNo].slice(0, 2)
        )
    }

    const openCompare = () => {
        const selected = items.filter(revision => compareSelection.includes(revision.revisionNo))
        if (selected.length !== 2) {
            return
        }
        setComparePair({ from: selected[1]!, to: selected[0]! })
    }

    const compareButton = canCompare && (
        <Button
            className={styles.compareAction}
            data-testid="revisions-compare"
            disabled={compareSelection.length !== 2}
            icon={<DiffOutlined />}
            onClick={openCompare}
            size="small"
        >
            {compareSelection.length === 2
                ? t('browser.history.compare')
                : t('browser.history.compare_count', { n: compareSelection.length })}
        </Button>
    )

    const filters = (searchable || canCompare) && (
        <div className={styles.filters}>
            {searchable && (
                <>
                    <Input.Search
                        allowClear
                        className={styles.search}
                        data-testid={`revisions-search-${projectId}`}
                        onChange={event => setSearch(event.target.value)}
                        placeholder={t('browser.history.search')}
                        value={search}
                    />
                    <label className={styles.techToggle}>
                        <Switch checked={techRevs} data-testid="revisions-tech" onChange={setTechRevs} size="small" />
                        {t('browser.history.tech_revs')}
                    </label>
                </>
            )}
            {compareButton}
        </div>
    )

    return (
        <div className={styles.panel}>
            {filters}
            {status === 'loading' && <Skeleton active paragraph={{ rows: 6 }} />}
            {status === 'error' && <Empty description={t('browser.history.error')} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
            {status === 'ready' && items.length === 0 && (
                <Empty description={appliedSearch ? t('browser.history.no_match') : t('browser.history.none')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
            {status === 'ready' && items.length > 0 && (
                <>
                    <ol className={styles.timeline} data-testid={`revisions-${projectId}`}>
                        {items.map((revision, index) => {
                            const current = !!currentRevision && revision.revisionNo === currentRevision
                            const comparable = canCompare && !revision.deleted && !revision.technicalRevision
                            const selectedForCompare = compareSelection.includes(revision.revisionNo)
                            return (
                                <li key={revision.revisionNo} className={styles.item}>
                                    <div className={styles.rail}>
                                        <span className={cx(styles.dot, current && styles.dotCurrent)} />
                                        {index < items.length - 1 && <span className={styles.line} />}
                                    </div>
                                    <div className={styles.card}>
                                        <div className={styles.cardHead}>
                                            <div>
                                                <div className={styles.msgRow}>
                                                    <GitCommitMessage
                                                        strong
                                                        className={styles.msg}
                                                        message={revision.fullComment}
                                                        testId={`revision-comment-${revision.revisionNo}`}
                                                    />
                                                    {current && (
                                                        <Tag
                                                            className={styles.tag}
                                                            color="blue"
                                                            data-testid={`revision-current-${revision.revisionNo}`}
                                                        >
                                                            {t('browser.history.current')}
                                                        </Tag>
                                                    )}
                                                    {revision.technicalRevision && <Tag className={styles.tag}>{t('browser.history.technical')}</Tag>}
                                                </div>
                                                <div className={styles.meta}>
                                                    <span>{authorName(revision)}</span>
                                                    <span>{formatDateTime(revision.createdAt) ?? '—'}</span>
                                                    <span className={styles.hash}>{revision.shortRevisionNo}</span>
                                                </div>
                                            </div>
                                            <div className={styles.actions}>
                                                {comparable && (
                                                    <Button
                                                        data-testid={`revision-compare-${revision.revisionNo}`}
                                                        disabled={compareSelection.length >= 2 && !selectedForCompare}
                                                        icon={<DiffOutlined />}
                                                        onClick={() => toggleCompare(revision.revisionNo)}
                                                        size="small"
                                                        type={selectedForCompare ? 'primary' : 'default'}
                                                    >
                                                        {t('browser.history.compare_select')}
                                                    </Button>
                                                )}
                                                {!current && !revision.technicalRevision && (
                                                    <Button
                                                        data-testid={`revision-open-${revision.revisionNo}`}
                                                        icon={<FolderOpenOutlined />}
                                                        loading={opening === revision.revisionNo}
                                                        onClick={() => open(revision.revisionNo)}
                                                        size="small"
                                                    >
                                                        {t('browser.history.open')}
                                                    </Button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </li>
                            )
                        })}
                    </ol>
                    {hasMore && (
                        <div className={styles.loadMore}>
                            <Button data-testid="revisions-load-more" loading={loadingMore} onClick={loadMore}>
                                {t('browser.history.load_more')}
                            </Button>
                        </div>
                    )}
                </>
            )}
            <DiscardChangesModal
                cancelButtonTestId="revision-discard-open-cancel"
                confirmButtonTestId="revision-discard-open-confirm"
                confirmText={t('browser.open_revision_discard_confirm_unsafe')}
                onCancel={() => setDiscardOpenRevision(null)}
                open={discardOpenRevision !== null}
                warning={t('browser.open_revision_discard_warning')}
                onConfirm={() => {
                    const revisionNo = discardOpenRevision
                    setDiscardOpenRevision(null)
                    if (revisionNo) {
                        void open(revisionNo, true)
                    }
                }}
            />
            <ProjectRevisionCompareModal
                fromRevision={comparePair?.from ?? null}
                onClose={() => setComparePair(null)}
                open={comparePair !== null}
                projectId={projectId}
                toRevision={comparePair?.to ?? null}
            />
        </div>
    )
}

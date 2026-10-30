import { useCallback, useEffect, useRef, useState } from 'react'
import type { ConfirmDiscard } from '../modules/useDiscardConfirm'
import { GO_AHEAD } from '../modules/useConfirmBefore'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { App, Button, Empty, Skeleton, Space, Switch, Tag, Tooltip, Typography } from 'antd'
import { CheckCircleFilled, DownOutlined, RightOutlined, SearchOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { SearchInput } from '../../components/SearchInput'
import {
    getProjectRevisions,
    isProjectModifiedConflict,
    openProjectRevision,
    REVISIONS_PAGE_SIZE,
    type ProjectRevision,
} from '../../services/repositories'
import { formatDateTime, useDateFormats } from '../../utils/dateFormat'
import { useSharedStyles } from './sharedStyles'
import { GitCommitMessage } from './GitCommitMessage'
import { authorName, shortRevision } from './revisions'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { RevisionChanges } from './RevisionChanges'
import { openRevisionsCompareWindow } from './compare'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        padding: 16px;
    `,
    filters: css`
        display: flex;
        align-items: center;
        gap: 16px;
        min-height: ${token.controlHeight}px;
        margin-bottom: 16px;
        flex-wrap: wrap;
    `,
    selection: css`
        margin-left: auto;
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
    // The marker is a check box of its own, drawn as the dot of the timeline: the box lies over the dot unseen.
    marker: css`
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-top: 2px;
        width: 18px;
        height: 18px;
        border-radius: 50%;
        cursor: pointer;
        z-index: 1;

        &:hover:not(:has([aria-disabled='true'])) > [data-dot],
        &:has(:focus-visible) > [data-dot] {
            transform: scale(1.4);
        }

        &:hover:not(:has([aria-disabled='true'])) > [data-dot]:not([data-current]),
        &:has(:focus-visible) > [data-dot]:not([data-current]) {
            background: ${token.colorPrimaryBorderHover};
        }

        &:has([aria-disabled='true']) {
            cursor: default;
        }
    `,
    markerBox: css`
        position: absolute;
        inset: 0;
        margin: 0;
        opacity: 0;
        cursor: inherit;
    `,
    dot: css`
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: ${token.colorBorder};
        box-shadow: 0 0 0 4px ${token.colorBgContainer};
    `,
    picked: css`
        color: ${token.colorPrimary};
        font-size: 16px;
        background: ${token.colorBgContainer};
        border-radius: 50%;
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
    // The arrow is the one control that expands the revision, stretched over the whole head; the controls the head
    // holds besides it lie above it.
    cardHead: css`
        position: relative;
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;

        & :is(button, a):not([data-expand]) {
            position: relative;
            z-index: 1;
        }
    `,
    headMain: css`
        display: flex;
        align-items: flex-start;
        gap: 8px;
        min-width: 0;
    `,
    expand: css`
        display: inline-flex;
        flex: none;
        margin-top: 6px;
        padding: 0;
        border: 0;
        background: none;
        color: ${token.colorTextTertiary};
        font-size: 10px;
        line-height: 1;
        cursor: pointer;

        &::after {
            content: '';
            position: absolute;
            inset: 0;
            border-radius: ${token.borderRadiusSM}px;
        }

        &:focus-visible {
            outline: none;
        }

        &:focus-visible::after {
            outline: 2px solid ${token.colorPrimaryBorder};
            outline-offset: 2px;
        }
    `,
    changes: css`
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid ${token.colorBorderSecondary};
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
        border-radius: ${token.borderRadiusSM}px;
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
    loadMore: css`
        display: flex;
        justify-content: center;
        margin-top: 4px;
    `,
}))

interface RevisionsPanelProps {
    /** URL-safe project id — what the history is asked about and what opens a revision. */
    projectId: string
    /** Revision currently opened by the project. */
    currentRevision?: string | null
    /** The branch the history is on, or nothing in a repository without branches. */
    branch?: string | null | undefined
    searchable?: boolean | undefined
    onOpened: () => void
    /**
     * Runs the opening after asking whatever has to be asked first; absent where nothing has to be.
     *
     * <p>Opening a revision replaces the copy of the project in the workspace, so anything written on screen
     * and not yet saved is gone with the copy it was written in.
     */
    beforeOpen?: ConfirmDiscard | undefined
    /** Bumped when the project reloads (e.g. after a save), forcing the history to refetch. */
    reloadToken?: number
}

/** How many revisions are compared with each other. */
const COMPARED_REVISIONS = 2

/**
 * A project's revision history as a timeline, backed by the project history API. Supports a text search, a
 * technical-revisions toggle and incremental paging. Any revision can be opened for viewing, mirroring the
 * legacy UI. History refetches whenever the project reloads, so a new revision from a save appears
 * immediately.
 *
 * <p>A click on a revision expands it and lists the files it changed, read the first time it is expanded and kept
 * while the history stays the same.
 *
 * <p>A click on the marker of a revision on the timeline picks it, and two revisions picked are compared with each
 * other in the comparison window: the files changed between them, each compared there, the earlier revision as the
 * base whatever order they were picked in. Nothing is opened for it, so the project stays on the revision it is on.
 * A new search or the technical revisions toggled let the picked revisions go.
 */
export const RevisionsPanel = ({
    projectId,
    currentRevision,
    branch,
    searchable = true,
    onOpened,
    beforeOpen,
    reloadToken,
}: RevisionsPanelProps) => {
    useDateFormats()
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
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
    const [expanded, setExpanded] = useState<ReadonlySet<string>>(new Set())
    const [shown, setShown] = useState<ReadonlySet<string>>(new Set())
    const [picked, setPicked] = useState<readonly string[]>([])
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
        setExpanded(new Set())
        setShown(new Set())
        setPicked([])
        getProjectRevisions(projectId, revisionQuery(0))
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
    }, [projectId, reloadToken, revisionQuery])

    const hasMore = searchable && (pageInfo.total !== null
        ? items.length < pageInfo.total
        : pageInfo.numberOfElements === pageInfo.pageSize && pageInfo.numberOfElements > 0)

    const loadMore = async () => {
        const next = page + 1
        const generation = fetchGeneration.current
        setLoadingMore(true)
        try {
            const response = await getProjectRevisions(projectId, revisionQuery(next))
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

    const toggle = (revisionNo: string) => {
        setExpanded(previous => {
            const next = new Set(previous)
            if (!next.delete(revisionNo)) {
                next.add(revisionNo)
            }
            return next
        })
        setShown(previous => previous.has(revisionNo) ? previous : new Set(previous).add(revisionNo))
    }

    /** Picks a revision for a comparison, or lets it go; a third one waits until one of the two is let go. */
    const togglePick = (revisionNo: string): void => setPicked(previous => {
        if (previous.includes(revisionNo)) {
            return previous.filter(other => other !== revisionNo)
        }
        return previous.length < COMPARED_REVISIONS ? [...previous, revisionNo] : previous
    })

    /** Whether two revisions are picked, so no other can be. */
    const isFull = picked.length >= COMPARED_REVISIONS

    const markerHint = (isPicked: boolean): string => {
        if (isPicked) {
            return t('browser.history.unpick')
        }
        return isFull ? t('browser.history.picked_two') : t('browser.history.pick')
    }

    /** The picked revisions as the history orders them, the newest first, so the later one leads. */
    const pickedInOrder = items.map(revision => revision.revisionNo).filter(revisionNo => picked.includes(revisionNo))

    const compare = () => {
        const [later, earlier] = pickedInOrder
        if (later && earlier) {
            openRevisionsCompareWindow(projectId, branch ?? undefined, earlier, later)
        }
    }

    const selection = picked.length > 0 && (
        <Space className={styles.selection} data-testid="revisions-selection">
            <Typography.Text type="secondary">
                {pickedInOrder.length < COMPARED_REVISIONS
                    ? t('browser.history.pick_another')
                    : t('browser.history.picked', {
                        base: shortRevision(pickedInOrder[1]!),
                        revision: shortRevision(pickedInOrder[0]!),
                    })}
            </Typography.Text>
            <Button
                data-testid="revisions-compare"
                disabled={pickedInOrder.length < COMPARED_REVISIONS}
                onClick={compare}
                size="small"
                type="primary"
            >
                {t('browser.history.compare_revisions')}
            </Button>
            <Button data-testid="revisions-clear" onClick={() => setPicked([])} size="small">
                {t('browser.history.clear_selection')}
            </Button>
        </Space>
    )

    /** Opens a revision, asking first where the screen has something of its own to lose. */
    const openAsked = (revisionNo: string) => (beforeOpen ?? GO_AHEAD)(() => void open(revisionNo))

    const open = async (revisionNo: string, discardChanges = false) => {
        if (opening !== null) {
            return
        }
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

    // The row is there whether there is anything to filter or not, so the picked revisions never move the history.
    const filters = (
        <div className={styles.filters}>
            {searchable && (
                <>
                    <SearchInput
                        className={styles.search}
                        data-testid={`revisions-search-${projectId}`}
                        onChange={event => setSearch(event.target.value)}
                        placeholder={t('browser.history.search')}
                        size="small"
                        value={search}
                    />
                    <label className={styles.techToggle}>
                        <Switch checked={techRevs} data-testid="revisions-tech" onChange={setTechRevs} size="small" />
                        {t('browser.history.tech_revs')}
                    </label>
                </>
            )}
            {selection}
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
                            const isExpanded = expanded.has(revision.revisionNo)
                            const isPicked = picked.includes(revision.revisionNo)
                            return (
                                <li key={revision.revisionNo} className={styles.item}>
                                    <div className={styles.rail}>
                                        {/* The marker of the revision on the timeline picks it for a comparison. Two
                                            revisions are compared, so a third waits until one is let go. */}
                                        <Tooltip title={markerHint(isPicked)}>
                                            <label className={styles.marker}>
                                                <input
                                                    // Not disabled: a disabled box tells nothing on hover, and the hint
                                                    // says why it does nothing.
                                                    aria-disabled={!isPicked && isFull}
                                                    checked={isPicked}
                                                    className={styles.markerBox}
                                                    data-testid={`revision-pick-${revision.revisionNo}`}
                                                    onChange={() => togglePick(revision.revisionNo)}
                                                    type="checkbox"
                                                    aria-label={t('browser.history.pick_revision', {
                                                        revision: shortRevision(revision.revisionNo),
                                                    })}
                                                />
                                                {isPicked
                                                    ? <CheckCircleFilled className={styles.picked} />
                                                    : (
                                                        <span
                                                            data-dot
                                                            className={cx(styles.dot, current && styles.dotCurrent)}
                                                            data-current={current || undefined}
                                                        />
                                                    )}
                                            </label>
                                        </Tooltip>
                                        {index < items.length - 1 && <span className={styles.line} />}
                                    </div>
                                    <div className={styles.card}>
                                        <div className={styles.cardHead}>
                                            {/* The arrow stands in a column of its own, so the comment and the line
                                                under it start at the same place. */}
                                            <div className={styles.headMain}>
                                                <button
                                                    data-expand
                                                    aria-expanded={isExpanded}
                                                    aria-label={t('browser.history.changes')}
                                                    className={styles.expand}
                                                    data-testid={`revision-toggle-${revision.revisionNo}`}
                                                    onClick={() => toggle(revision.revisionNo)}
                                                    type="button"
                                                >
                                                    {isExpanded ? <DownOutlined /> : <RightOutlined />}
                                                </button>
                                                <div>
                                                    <div className={styles.msgRow}>
                                                        <GitCommitMessage
                                                            strong
                                                            className={styles.msg}
                                                            message={revision.fullComment}
                                                            testId={`revision-comment-${revision.revisionNo}`}
                                                        />
                                                        {revision.technicalRevision && <Tag className={cx(shared.chipTag, styles.tag)}>{t('browser.history.technical')}</Tag>}
                                                    </div>
                                                    <div className={styles.meta}>
                                                        <span>{authorName(revision)}</span>
                                                        <span>{formatDateTime(revision.createdAt) ?? '—'}</span>
                                                        <span className={shared.valueText}>{shortRevision(revision.revisionNo)}</span>
                                                    </div>
                                                </div>
                                            </div>
                                            <div className={styles.actions}>
                                                {!current && !revision.technicalRevision && (
                                                    <Tooltip title={t('browser.history.open')}>
                                                        <Button
                                                            aria-label={t('browser.history.open')}
                                                            data-testid={`revision-open-${revision.revisionNo}`}
                                                            // One revision opens at a time: the workspace holds one.
                                                            disabled={opening !== null && opening !== revision.revisionNo}
                                                            icon={<SearchOutlined />}
                                                            loading={opening === revision.revisionNo}
                                                            onClick={() => openAsked(revision.revisionNo)}
                                                            size="small"
                                                        />
                                                    </Tooltip>
                                                )}
                                            </div>
                                        </div>
                                        {shown.has(revision.revisionNo) && (
                                            // Kept while collapsed, so expanding it again reads nothing.
                                            <div className={styles.changes} hidden={!isExpanded}>
                                                <RevisionChanges
                                                    branch={branch}
                                                    projectId={projectId}
                                                    revision={revision.revisionNo}
                                                />
                                            </div>
                                        )}
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
        </div>
    )
}

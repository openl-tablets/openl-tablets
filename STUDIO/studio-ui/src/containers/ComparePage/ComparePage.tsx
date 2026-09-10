import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Checkbox, Spin, Splitter, Upload } from 'antd'
import {
    ArrowLeftOutlined,
    InboxOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    PaperClipOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { isApiHttpError } from 'services'
import { errorMessage } from 'utils/errorMessage'
import { dropComparison, getComparison, getComparisonTable, startFileComparison } from 'services/compare'
import type { Comparison, ComparisonTable } from 'types/compare'
import { ComparisonPanes } from './ComparisonPanes'
import { ComparisonTree } from './ComparisonTree'
import { isFinished, useComparisonProgress } from './useComparisonProgress'
import { useStyles } from './ComparePage.styles'

const ACCEPTED = '.xls,.xlsx,.xlsm'
/** How long a screen that cannot hear the topic waits before asking again, in milliseconds. */
const ASK_AGAIN = 2000
const FILES_TO_COMPARE = 2


/**
 * Compares two Excel files, in a window of its own.
 *
 * The page has two steps: the files to compare are picked first, and the comparison is shown after
 * that - what the two files hold, element by element, and the two versions of the element the user
 * picks. The step of the files is a click away, so another pair can be compared in the same window.
 */
export const ComparePage: React.FC = () => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()

    const [files, setFiles] = useState<File[]>([])

    /**
     * Takes the files that were dropped or chosen, up to the two that are compared. The list of them
     * is drawn by this page rather than by the upload control, whose own list is left empty.
     */
    const pick = useCallback((file: File, batch: File[]) => {
        // The control asks once per file of a batch, and hands the whole batch every time.
        if (file === batch[0]) {
            setFiles(current => [...current, ...batch].slice(0, FILES_TO_COMPARE))
        }
        return false
    }, [])
    const [comparisonId, setComparisonId] = useState<string | null>(null)
    const [comparison, setComparison] = useState<Comparison | null>(null)
    /** Counts the times the result was asked for, so that asking again runs the effect that asks. */
    const [attempt, setAttempt] = useState(0)
    const [selectedId, setSelectedId] = useState<string | null>(null)
    const [table, setTable] = useState<ComparisonTable | null>(null)
    const [tableLoading, setTableLoading] = useState(false)
    const [tableError, setTableError] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [showEqualElements, setShowEqualElements] = useState(false)
    const [showEqualRows, setShowEqualRows] = useState(false)
    // The width the list of elements was last given, and whether it is shown at all. The list is
    // hidden and brought back by a button of its own rather than by the divider, so the control is
    // always in sight.
    const [treeWidth, setTreeWidth] = useState<number | string>('30%')
    const [treeHidden, setTreeHidden] = useState(false)

    const progress = useComparisonProgress(comparisonId)
    // The comparison is on screen from the moment it is started; until it answers, its progress is.
    const comparing = !!comparisonId
    const running = comparing && !comparison && !error

    // The result is read when the comparison says it has finished, and again as soon as the page is
    // listening: a comparison of two small files can be over before then, and what was pushed to the
    // topic by then is not repeated. A comparison still running answers 409, and the topic then says
    // when to ask again.
    useEffect(() => {
        if (!comparisonId) {
            return
        }
        if (isFinished(progress.status) && progress.status !== 'COMPLETED') {
            setError(progress.error ?? t(progress.status === 'INTERRUPTED' ? 'interrupted' : 'failed'))
            return
        }
        if (progress.status && progress.status !== 'COMPLETED') {
            return
        }
        let cancelled = false
        let asking: number | undefined
        getComparison(comparisonId)
            .then(result => {
                if (!cancelled) setComparison(result)
            })
            .catch((failure: unknown) => {
                if (cancelled) {
                    return
                }
                if (isApiHttpError(failure) && failure.status === 409) {
                    // Still running. The topic says when it ends; a screen that is not listening to it -
                    // a connection that never opened - would wait for a word that never comes, so it asks.
                    if (!progress.subscribed) {
                        asking = window.setTimeout(() => setAttempt(count => count + 1), ASK_AGAIN)
                    }
                    return
                }
                setError(errorMessage(failure) || t('failed'))
            })
        return () => {
            cancelled = true
            window.clearTimeout(asking)
        }
    }, [comparisonId, progress.status, progress.error, progress.subscribed, attempt, t])

    // The two sides of an element are read when it is picked, because each of them is a whole table.
    useEffect(() => {
        if (!comparisonId || !selectedId) {
            setTable(null)
            return
        }
        let cancelled = false
        setTableLoading(true)
        setTableError(null)
        getComparisonTable(comparisonId, selectedId)
            .then(result => {
                if (!cancelled) setTable(result)
            })
            .catch((failure: unknown) => {
                if (!cancelled) setTableError(errorMessage(failure) || t('table_failed'))
            })
            .finally(() => {
                if (!cancelled) setTableLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [comparisonId, selectedId, t])

    // A comparison holds both workbooks parsed, so closing the window puts them down at once instead
    // of leaving them until the session ends.
    useEffect(() => {
        if (!comparisonId) {
            return
        }
        const release = () => {
            void dropComparison(comparisonId).catch(() => {
                // The comparison is released with the session in any case.
            })
        }
        window.addEventListener('pagehide', release)
        return () => window.removeEventListener('pagehide', release)
    }, [comparisonId])

    /** Keeps the width the divider was dragged to, so hiding the list and bringing it back restores it. */
    const rememberTreeWidth = useCallback((width: number | undefined) => {
        if (width !== undefined && width > 0) {
            setTreeWidth(width)
        }
    }, [])

    const compare = useCallback(async () => {
        if (files.length !== FILES_TO_COMPARE) {
            return
        }
        setStarting(true)
        setError(null)
        try {
            setComparisonId(await startFileComparison(files[0]!, files[1]!))
        } catch (failure) {
            setError(errorMessage(failure) || t('failed'))
        } finally {
            setStarting(false)
        }
    }, [files, t])

    /** Back to the files, leaving the comparison behind: another pair is compared from here. */
    const pickOtherFiles = useCallback(() => {
        if (comparisonId) {
            void dropComparison(comparisonId).catch(() => {
                // The comparison is released with the session in any case.
            })
        }
        // The files that were compared are let go with it, so the next pair is the pair that is picked.
        setFiles([])
        setComparisonId(null)
        setComparison(null)
        setSelectedId(null)
        setTable(null)
        setTableError(null)
        setError(null)
        setShowEqualRows(false)
    }, [comparisonId])

    // The way back to the files never hides with the list of elements: it heads the list while the
    // list is shown, and joins the control that brings it back when it is not.
    const back = (
        <Button
            data-testid="compare-back"
            icon={<ArrowLeftOutlined />}
            onClick={pickOtherFiles}
            type="link"
        >
            {t('back')}
        </Button>
    )

    return (
        <div className={styles.page}>
            {!comparing && (
                <div className={styles.step}>
                    <div className={styles.picker}>
                        <Upload.Dragger
                            multiple
                            accept={ACCEPTED}
                            beforeUpload={pick}
                            data-testid="compare-files"
                            fileList={[]}
                            showUploadList={false}
                        >
                            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                            <p className="ant-upload-text">{t('select')}</p>
                            <p className="ant-upload-hint">{t('select_hint')}</p>
                        </Upload.Dragger>
                        <ul className={styles.files} data-testid="compare-file-list">
                            {files.map((file, index) => (
                                <li key={`${index}-${file.name}`} className={styles.file}>
                                    <PaperClipOutlined />
                                    <span className={styles.fileName}>{file.name}</span>
                                    <Button
                                        data-testid="compare-file-clear"
                                        onClick={() => setFiles(rest => rest.filter((_, at) => at !== index))}
                                        size="small"
                                        type="link"
                                    >
                                        {t('clear')}
                                    </Button>
                                </li>
                            ))}
                        </ul>
                    </div>
                    <Checkbox
                        checked={showEqualElements}
                        data-testid="compare-show-equal-elements"
                        onChange={event => setShowEqualElements(event.target.checked)}
                    >
                        {t('show_equal_elements')}
                    </Checkbox>
                    <div>
                        <Button
                            data-testid="compare-start"
                            disabled={files.length !== FILES_TO_COMPARE}
                            loading={starting}
                            onClick={() => void compare()}
                            type="primary"
                        >
                            {t('compare')}
                        </Button>
                    </div>
                    {error && <Alert showIcon data-testid="compare-error" message={error} type="error" />}
                </div>
            )}
            {comparing && (
                <Splitter className={styles.result} onResize={sizes => rememberTreeWidth(sizes[0])}>
                    <Splitter.Panel
                        max="60%"
                        min={treeHidden ? 0 : 200}
                        resizable={!treeHidden}
                        size={treeHidden ? 0 : treeWidth}
                    >
                        {/* Nothing is drawn in a hidden panel: a control that cannot be seen must not
                            be reachable by the keyboard either. */}
                        {!treeHidden && (
                            <div className={styles.column}>
                                {/* The controls of the comparison head their own column, on the line the
                                two files are named on. */}
                                <div className={styles.head}>
                                    {back}
                                    {comparison && (
                                        <Checkbox
                                            checked={showEqualRows}
                                            data-testid="compare-show-equal-rows"
                                            onChange={event => setShowEqualRows(event.target.checked)}
                                        >
                                            {t('show_equal_rows')}
                                        </Checkbox>
                                    )}
                                    <Button
                                        className={styles.headAction}
                                        data-testid="compare-tree-hide"
                                        icon={<MenuFoldOutlined />}
                                        onClick={() => setTreeHidden(true)}
                                        size="small"
                                        title={t('hide_tree')}
                                        type="text"
                                    />
                                </div>
                                <div className={styles.body} data-testid="compare-tree">
                                    {error && <Alert showIcon data-testid="compare-error" message={error} type="error" />}
                                    {comparison?.identical && (
                                        <Alert
                                            showIcon
                                            data-testid="compare-identical"
                                            message={t('identical')}
                                            type="info"
                                        />
                                    )}
                                    {running && (
                                        <div className={styles.center}>
                                            <Spin description={t('comparing')} />
                                        </div>
                                    )}
                                    {comparison && (
                                        <ComparisonTree
                                            comparison={comparison}
                                            onSelect={setSelectedId}
                                            showEqualElements={showEqualElements}
                                        />
                                    )}
                                </div>
                            </div>
                        )}
                    </Splitter.Panel>
                    <Splitter.Panel>
                        <ComparisonPanes
                            error={tableError}
                            loading={tableLoading}
                            showEqualRows={showEqualRows}
                            table={table}
                            leading={treeHidden && (
                                <>
                                    <Button
                                        data-testid="compare-tree-show"
                                        icon={<MenuUnfoldOutlined />}
                                        onClick={() => setTreeHidden(false)}
                                        size="small"
                                        title={t('show_tree')}
                                        type="text"
                                    />
                                    {back}
                                </>
                            )}
                        />
                    </Splitter.Panel>
                </Splitter>
            )}
        </div>
    )
}

export default ComparePage

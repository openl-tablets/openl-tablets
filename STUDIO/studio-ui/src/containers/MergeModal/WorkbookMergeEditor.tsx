import React, { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react'
import { Alert, Button, Flex, Modal, Pagination, Radio, Space, Spin, Tabs, Tag, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { RawTableGrid, type CellDecoration } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import { getConflictWorkbook, mergeConflictWorkbook, PALETTE_CONFLICT, type ConflictSide } from '../../services/compare'
import { errorMessage } from '../../utils/errorMessage'
import { openConflictCompareWindow } from '../projects/compare'
import { basename } from '../projects/projectPaths'
import { useCellStyles } from '../ComparePage/ComparisonPanes.styles'
import { ButtonColumns, Columns, ColumnTitles, ConflictHeading, DiscardButton, TakeButton } from './MergeColumns'
import type { Side } from './textMerge'
import type { ConflictSheet, ConflictTable, ConflictTableVersion, ConflictWorkbook, MergedWorkbook } from './types'
import {
    cellAt,
    conflictsOf,
    conflictWindowOf,
    decisionOf,
    decisionsOf,
    isConflicting,
    isWhole,
    progressOf,
    resolvedOf,
    textOf,
    type RowWindow,
    type WorkbookDecisions,
} from './workbookMerge'

/** How a resolved conflict is told, by the version it is taken from. */
const STATUSES: Record<ConflictSide, string> = {
    ours: 'merge:editor.status_yours',
    theirs: 'merge:editor.status_theirs',
    base: 'merge:editor.status_discarded',
}

/** The choice of a sheet that leaves its tables to be resolved one by one. */
const TABLE_BY_TABLE = 'tables'

/** How many conflicting tables of a sheet are shown at a time: each shows three versions of up to 200 rows. */
const TABLES_PER_PAGE = 5

/** How many cells both branches changed differently a table names above its versions. */
const NAMED_CONFLICTS = 5

/**
 * The classes a version of a table is marked with: a cell its branch changed, a cell both changed differently, and the
 * whole table when its branch changed its formatting.
 */
interface CellMarks {
    changedFrame: string
    conflict: string
    restyled: string
}

/** Tells the editor the version a conflict is taken from, or none to leave it to resolve. */
type Decide = (id: string, side: ConflictSide | undefined) => void

interface WorkbookMergeEditorProps {
    projectId: string
    /** The conflicted workbook, by its path. */
    filePath: string
    /** The workbook as it was merged here before: the editor starts from its conflicts and the decisions made. */
    merged?: MergedWorkbook | undefined
    /** What names your and their version beside the titles of their columns, such as their branches. */
    versionLabels?: Partial<Record<Side, React.ReactNode>> | undefined
    /** Takes the merged workbook once every conflict is resolved. */
    onApply: (merged: MergedWorkbook) => void
    onCancel: () => void
}

/**
 * Merges a conflicted workbook in place, table by table, laid out as the merge of a text file is: your version on the
 * left, the result in the middle and their version on the right.
 *
 * The workbook is shown sheet by sheet, each sheet with conflicts on a tab of its own. What only one branch changed,
 * and what both changed in ways the merge combines, is merged already and not shown. A table both branches changed in
 * ways the merge cannot combine is a conflict: the user takes it whole from your version, from their version, or from
 * the base version, discarding both changes. A sheet whose conflict lies outside its tables is taken whole the same
 * way, and the comparison of the workbook tells what each branch changed in it. Any conflicting sheet can be taken
 * whole at once.
 *
 * The workbook is merged on the server once every conflict is resolved; the user edits it further after the merge.
 * A merge still under way when the window closes is dropped.
 */
export const WorkbookMergeEditor: React.FC<WorkbookMergeEditorProps> = ({
    projectId,
    filePath,
    merged: earlier,
    versionLabels,
    onApply,
    onCancel,
}) => {
    const { t } = useTranslation()
    // A workbook merged here before is shown as it was: the versions of a conflict do not change.
    const [workbook, setWorkbook] = useState<ConflictWorkbook | null>(earlier?.workbook ?? null)
    const [decisions, setDecisions] = useState<WorkbookDecisions>(earlier?.decisions ?? {})
    const [error, setError] = useState<string | null>(null)
    const [applyError, setApplyError] = useState<string | null>(null)
    const [applying, setApplying] = useState(false)
    const [picked, setPicked] = useState<string>()
    const open = useRef(true)
    // The marks of the changed cells are read once for every table the editor draws. They are kept by their class
    // names, which stay the same while the theme does, so a table not decided again is not drawn again.
    const { styles: { changedFrame, conflict, restyled } } = useCellStyles()
    const marks = useMemo<CellMarks>(() => ({ changedFrame, conflict, restyled }), [changedFrame, conflict, restyled])

    useEffect(() => {
        open.current = true
        return () => {
            open.current = false
        }
    }, [])

    useEffect(() => {
        if (earlier) {
            return undefined
        }
        let cancelled = false
        getConflictWorkbook(projectId, filePath)
            .then(answer => {
                if (!cancelled) {
                    setWorkbook(answer)
                }
            })
            .catch((readError: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(readError) || t('merge:workbook.load_failed'))
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, filePath, earlier, t])

    const progress = useMemo(() => workbook && progressOf(workbook, decisions), [workbook, decisions])
    const resolved = !!progress && progress.resolved === progress.count
    // A sheet the merge takes by itself leaves nothing to look at or decide, so only the conflicting ones are shown.
    const sheets = useMemo(() => workbook?.sheets.filter(isConflicting) ?? [], [workbook])
    const compare = useCallback(() => openConflictCompareWindow(projectId, filePath), [projectId, filePath])

    // One function for every conflict, so a table redraws only when its own decision changes.
    const decide = useCallback<Decide>((id, side) => {
        setApplyError(null)
        setDecisions(current => {
            const next = { ...current }
            if (side) {
                next[id] = side
            } else {
                delete next[id]
            }
            return next
        })
    }, [])

    const apply = async () => {
        if (!workbook || !resolved) {
            return
        }
        setApplying(true)
        setApplyError(null)
        try {
            const file = await mergeConflictWorkbook(projectId, filePath, decisionsOf(workbook, decisions))
            // Every decision is kept, the ones a sheet taken whole stands for included, for Edit to show them again.
            if (open.current) {
                onApply({ file, decisions, workbook })
            }
        } catch (mergeError: unknown) {
            setApplyError(errorMessage(mergeError) || t('merge:workbook.apply_failed'))
        } finally {
            setApplying(false)
        }
    }

    // The editor opens on the first conflicting sheet.
    const sheet = picked ?? sheets[0]?.name ?? ''

    return (
        <Modal
            centered
            destroyOnHidden
            open
            onCancel={onCancel}
            // The three columns take the width of the window. The tables scroll inside the dialog, under the titles
            // of the columns, which keeps its buttons within the window.
            styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
            width="96vw"
            footer={(
                <Flex align="center" gap="small" justify="space-between">
                    {/* A merge the server refuses is told beside Apply, in sight however far the tables scroll. */}
                    {applyError
                        ? <Alert showIcon data-testid="workbook-merge-apply-error" title={applyError} type="error" />
                        : (
                            <Typography.Text data-testid="workbook-merge-progress" type="secondary">
                                {progress && t('merge:editor.progress', progress)}
                            </Typography.Text>
                        )}
                    <Space>
                        <Button onClick={onCancel}>{t('merge:buttons.cancel')}</Button>
                        <Button
                            data-testid="workbook-merge-apply"
                            disabled={!resolved}
                            loading={applying}
                            onClick={apply}
                            type="primary"
                        >
                            {t('merge:editor.apply')}
                        </Button>
                    </Space>
                </Flex>
            )}
            title={(
                <Flex vertical gap="small">
                    {t('merge:workbook.title', { name: basename(filePath) })}
                    <ColumnTitles versionLabels={versionLabels} />
                </Flex>
            )}
        >
            {error && <Alert showIcon data-testid="workbook-merge-error" title={error} type="error" />}
            {!error && !workbook && <Spin description={t('merge:workbook.loading')} />}
            {workbook && (
                <Flex vertical gap="small">
                    {workbook.palette && (
                        <PalettePart decision={decisionOf(decisions, PALETTE_CONFLICT)} onDecide={decide} />
                    )}
                    <Tabs
                        activeKey={sheet}
                        onChange={setPicked}
                        items={sheets.map(item => ({
                            key: item.name,
                            label: <SheetLabel decisions={decisions} sheet={item} />,
                            children: (
                                <SheetPart
                                    decisions={decisions}
                                    marks={marks}
                                    onCompare={compare}
                                    onDecide={decide}
                                    sheet={item}
                                />
                            ),
                        }))}
                    />
                </Flex>
            )}
        </Modal>
    )
}

/** The name of a conflicting sheet on its tab, with how many of its conflicts are left. */
const SheetLabel: React.FC<{ sheet: ConflictSheet; decisions: WorkbookDecisions }> = ({ sheet, decisions }) => {
    const { t } = useTranslation()
    const left = conflictsOf(sheet).length - resolvedOf(sheet, decisions)
    return (
        <Space size={4}>
            {sheet.name}
            {left === 0
                ? <Tag color="success">{t('merge:workbook.resolved')}</Tag>
                : <Tag color="warning">{t('merge:workbook.left', { count: left })}</Tag>}
        </Space>
    )
}

interface TablesPartProps {
    sheet: ConflictSheet
    decisions: WorkbookDecisions
    onDecide: Decide
    marks: CellMarks
}

interface SheetPartProps extends TablesPartProps {
    /** Opens the comparison of the versions of the workbook. */
    onCompare: () => void
}

/** A conflicting sheet of the workbook: its conflicts and the buttons that resolve them. */
const SheetPart: React.FC<SheetPartProps> = ({ sheet, decisions, onCompare, onDecide, marks }) => isWhole(sheet)
    ? (
        <WholeSheetPart
            decision={decisionOf(decisions, sheet.name)}
            onCompare={onCompare}
            onDecide={onDecide}
            sheet={sheet}
        />
    )
    : <TablesPart decisions={decisions} marks={marks} onDecide={onDecide} sheet={sheet} />

/** The conflicting tables of a sheet, a page at a time, under the choice that takes the whole sheet at once. */
const TablesPart: React.FC<TablesPartProps> = ({ sheet, decisions, onDecide, marks }) => {
    const { t } = useTranslation()
    const [page, setPage] = useState(1)
    const labelId = useId()
    const whole = decisionOf(decisions, sheet.name)
    const first = (page - 1) * TABLES_PER_PAGE
    return (
        <Flex vertical gap="small">
            <Typography.Text data-testid="workbook-merge-legend" type="secondary">
                {t('merge:workbook.legend')}
            </Typography.Text>
            <Flex wrap align="center" gap="small">
                <Typography.Text id={labelId}>{t('merge:workbook.whole_sheet')}</Typography.Text>
                <Radio.Group
                    aria-labelledby={labelId}
                    data-testid="workbook-merge-sheet-choice"
                    optionType="button"
                    size="small"
                    value={whole ?? TABLE_BY_TABLE}
                    onChange={event => onDecide(sheet.name,
                        event.target.value === TABLE_BY_TABLE ? undefined : event.target.value as ConflictSide)}
                    options={[
                        { label: t('merge:workbook.table_by_table'), value: TABLE_BY_TABLE },
                        { label: t('merge:editor.take_yours'), value: 'ours' },
                        { label: t('merge:editor.discard'), value: 'base' },
                        { label: t('merge:editor.take_theirs'), value: 'theirs' },
                    ]}
                />
            </Flex>
            {sheet.tables.slice(first, first + TABLES_PER_PAGE).map((table, offset) => (
                <TablePart
                    key={table.id}
                    count={sheet.tables.length}
                    decision={whole ?? decisionOf(decisions, table.id)}
                    index={first + offset}
                    locked={!!whole}
                    marks={marks}
                    onDecide={onDecide}
                    table={table}
                />
            ))}
            <Pagination
                hideOnSinglePage
                align="center"
                current={page}
                data-testid="workbook-merge-pages"
                onChange={setPage}
                pageSize={TABLES_PER_PAGE}
                showSizeChanger={false}
                size="small"
                total={sheet.tables.length}
            />
        </Flex>
    )
}

interface ChoiceButtonsProps {
    decision: ConflictSide | undefined
    /** What discarding both changes keeps; none for a conflict that is taken from your or their version only. */
    hint?: string | undefined
    /** Whether the choice is made for something the conflict belongs to, such as its whole sheet. */
    disabled?: boolean
    onDecide: (side: ConflictSide) => void
    testId: string
}

/** The buttons that take a conflict from a version, each in the column of the version they take. */
const ChoiceButtons: React.FC<ChoiceButtonsProps> = ({ decision, hint, disabled = false, onDecide, testId }) => {
    const { t } = useTranslation()
    return (
        <ButtonColumns
            ours={(
                <TakeButton
                    disabled={disabled || decision === 'ours'}
                    label={t('merge:editor.take_yours')}
                    onTake={() => onDecide('ours')}
                    side="ours"
                    testId={`${testId}-take-ours`}
                />
            )}
            result={hint && (
                <DiscardButton
                    disabled={disabled || decision === 'base'}
                    hint={hint}
                    onDiscard={() => onDecide('base')}
                    testId={`${testId}-discard`}
                />
            )}
            theirs={(
                <TakeButton
                    disabled={disabled || decision === 'theirs'}
                    label={t('merge:editor.take_theirs')}
                    onTake={() => onDecide('theirs')}
                    side="theirs"
                    testId={`${testId}-take-theirs`}
                />
            )}
        />
    )
}

interface WholeSheetPartProps {
    sheet: ConflictSheet
    decision: ConflictSide | undefined
    onCompare: () => void
    onDecide: Decide
}

/**
 * A sheet resolved only as a whole: why, which versions have it, and the buttons that take it from one of them. What
 * each branch changed in it is read in the comparison of the workbook.
 */
const WholeSheetPart: React.FC<WholeSheetPartProps> = ({ sheet, decision, onCompare, onDecide }) => {
    const { t } = useTranslation()
    const titleId = useId()
    const presence = (has: boolean) => has
        ? <Typography.Text>{t('merge:workbook.sheet_present')}</Typography.Text>
        : <Typography.Text italic type="secondary">{t('merge:workbook.sheet_absent')}</Typography.Text>
    return (
        <Flex vertical aria-labelledby={titleId} data-testid="workbook-merge-sheet" gap="small" role="group">
            {sheet.reason && (
                <Alert
                    showIcon
                    title={t(`merge:workbook.sheet_reason.${sheet.reason}`)}
                    type="info"
                    action={(
                        <Button data-testid="workbook-merge-sheet-compare" onClick={onCompare} size="small">
                            {t('merge:workbook.compare_versions')}
                        </Button>
                    )}
                />
            )}
            <ConflictHeading
                status={decision && t(STATUSES[decision])}
                title={t('merge:workbook.sheet', { name: sheet.name })}
                titleId={titleId}
            />
            <ChoiceButtons
                decision={decision}
                hint={t('merge:workbook.discard_sheet_hint')}
                onDecide={side => onDecide(sheet.name, side)}
                testId="workbook-merge-sheet"
            />
            <Columns
                ours={presence(sheet.ours)}
                result={presence(decision ? sheet[decision] : sheet.base)}
                theirs={presence(sheet.theirs)}
            />
        </Flex>
    )
}

interface TablePartProps {
    table: ConflictTable
    /** The place of the table among the conflicting tables of its sheet, from 0. */
    index: number
    count: number
    decision: ConflictSide | undefined
    /** Whether the sheet of the table is taken whole, which decides the table too. */
    locked: boolean
    onDecide: Decide
    marks: CellMarks
}

/**
 * A conflicting table: why its changes conflict, the cells both branches changed differently, each version of it beside
 * the result, and the buttons that take it from a version. Until it is resolved, the result shows the base version.
 *
 * A table with such cells shows the rows around them, the same rows in every version, until the whole table is asked
 * for.
 */
const TablePart = React.memo(function TablePart({
    table,
    index,
    count,
    decision,
    locked,
    onDecide,
    marks,
}: TablePartProps) {
    const { t } = useTranslation()
    const titleId = useId()
    const [whole, setWhole] = useState(false)
    const around = useMemo(() => conflictWindowOf(table), [table])
    const shown = whole ? undefined : around
    const number = index + 1
    const testId = `workbook-merge-table-${number}`
    return (
        <Flex vertical aria-labelledby={titleId} data-testid={testId} gap="small" role="group">
            <ConflictHeading
                status={decision && t(STATUSES[decision])}
                title={t('merge:workbook.table', { number, count, name: table.name })}
                titleId={titleId}
            />
            <Typography.Text type="secondary">{t(`merge:workbook.table_reason.${table.reason}`)}</Typography.Text>
            {table.ours?.formattingChanged && table.theirs?.formattingChanged && (
                <Typography.Text data-testid={`${testId}-formatting`} type="warning">
                    {t('merge:workbook.formatting_both')}
                </Typography.Text>
            )}
            <ConflictCells table={table} />
            <ChoiceButtons
                decision={decision}
                disabled={locked}
                hint={t('merge:workbook.discard_table_hint')}
                onDecide={side => onDecide(table.id, side)}
                testId={testId}
            />
            {around && (
                <Flex wrap align="center" gap="small">
                    <Typography.Text data-testid={`${testId}-rows`} type="secondary">
                        {shown
                            ? t('merge:workbook.rows_window', {
                                from: shown.from + 1,
                                to: shown.to + 1,
                                count: (table.base ?? table.ours ?? table.theirs)?.rows ?? 0,
                            })
                            : t('merge:workbook.rows_all')}
                    </Typography.Text>
                    <Button data-testid={`${testId}-whole`} onClick={() => setWhole(!whole)} size="small" type="link">
                        {whole ? t('merge:workbook.show_conflict_rows') : t('merge:workbook.show_whole_table')}
                    </Button>
                </Flex>
            )}
            <Columns
                ours={<VersionGrid
                    absent={t('merge:workbook.table_absent')}
                    marks={marks}
                    shownRows={shown}
                    version={table.ours}
                />}
                result={<VersionGrid
                    absent={t('merge:workbook.table_left_out')}
                    marks={marks}
                    shownRows={shown}
                    version={decision ? table[decision] : table.base}
                />}
                theirs={<VersionGrid
                    absent={t('merge:workbook.table_absent')}
                    marks={marks}
                    shownRows={shown}
                    version={table.theirs}
                />}
            />
        </Flex>
    )
})

/** The cells both branches changed differently, each with what your and their version hold in it. */
const ConflictCells: React.FC<{ table: ConflictTable }> = ({ table }) => {
    const { t } = useTranslation()
    const cells = (table.base ?? table.ours)?.conflictCells ?? []
    if (cells.length === 0) {
        return null
    }
    return (
        <Flex vertical data-testid="workbook-merge-conflict-cells" gap={2}>
            <Typography.Text strong>{t('merge:workbook.conflict_cells')}</Typography.Text>
            {cells.slice(0, NAMED_CONFLICTS).map((cell, place) => (
                <Typography.Text key={cell}>
                    {t('merge:workbook.conflict_cell', {
                        cell,
                        ours: textOf(cellAt(table.ours, table.ours?.conflictCells[place] ?? '')),
                        theirs: textOf(cellAt(table.theirs, table.theirs?.conflictCells[place] ?? '')),
                    })}
                </Typography.Text>
            ))}
            {cells.length > NAMED_CONFLICTS && (
                <Typography.Text type="secondary">
                    {t('merge:workbook.more_conflicts', { count: cells.length - NAMED_CONFLICTS })}
                </Typography.Text>
            )}
        </Flex>
    )
}

interface VersionGridProps {
    version: ConflictTableVersion | undefined
    /** What a version without the table tells. */
    absent: string
    marks: CellMarks
    /** The rows shown, or none to show every row. */
    shownRows: RowWindow | undefined
}

/**
 * A version of a table in its own formatting: the cells both branches changed differently framed in red, the other
 * cells its branch changed framed in the colour of a change. A version whose formatting its branch changed is framed
 * whole and says so. A version without the table says so too.
 */
const VersionGrid: React.FC<VersionGridProps> = ({ version, absent, marks, shownRows }) => {
    const { t } = useTranslation()
    const changed = useMemo(() => new Set(version?.changedCells ?? []), [version])
    const conflicting = useMemo(() => new Set(version?.conflictCells ?? []), [version])
    if (!version) {
        return <Typography.Text italic type="secondary">{absent}</Typography.Text>
    }
    const decorate = (cell: RawTableCell): CellDecoration | undefined => {
        if (!cell.cell) {
            return undefined
        }
        if (conflicting.has(cell.cell)) {
            return { className: marks.conflict }
        }
        return changed.has(cell.cell) ? { className: marks.changedFrame } : undefined
    }
    const rows = shownRows ? version.cells.slice(shownRows.from, shownRows.to + 1) : version.cells
    return (
        <Flex vertical gap={4}>
            <Flex wrap align="center" gap="small">
                <Typography.Text type="secondary">{version.range}</Typography.Text>
                {version.formattingChanged && (
                    <Tag color="warning" data-testid="workbook-merge-formatting-changed">
                        {t('merge:workbook.formatting_changed')}
                    </Tag>
                )}
            </Flex>
            {/* A table wider than its column scrolls on its own, as the versions of a comparison do. */}
            <Flex className={version.formattingChanged ? marks.restyled : undefined} style={{ overflowX: 'auto' }}>
                <RawTableGrid decorate={decorate} rows={rows} />
            </Flex>
            {!shownRows && version.rows > version.cells.length && (
                <Typography.Text type="secondary">
                    {t('merge:workbook.rows_shown', { shown: version.cells.length, count: version.rows })}
                </Typography.Text>
            )}
        </Flex>
    )
}

/** The palette of an .xls workbook both branches changed differently, taken from your or their version. */
const PalettePart: React.FC<{ decision: ConflictSide | undefined; onDecide: Decide }> = ({ decision, onDecide }) => {
    const { t } = useTranslation()
    const titleId = useId()
    return (
        <Flex vertical aria-labelledby={titleId} data-testid="workbook-merge-palette" gap="small" role="group">
            <ConflictHeading
                status={decision && t(STATUSES[decision])}
                title={t('merge:workbook.palette')}
                titleId={titleId}
            />
            <Typography.Text type="secondary">{t('merge:workbook.palette_reason')}</Typography.Text>
            <ChoiceButtons
                decision={decision}
                onDecide={side => onDecide(PALETTE_CONFLICT, side)}
                testId="workbook-merge-palette"
            />
        </Flex>
    )
}

export default WorkbookMergeEditor

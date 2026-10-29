import React, { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react'
import {
    Alert,
    Button,
    Flex,
    Modal,
    Pagination,
    Popover,
    Space,
    Spin,
    Tabs,
    Tag,
    Tooltip,
    Typography,
} from 'antd'
import {
    DoubleLeftOutlined,
    DoubleRightOutlined,
    QuestionCircleOutlined,
    UndoOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { PaperTheme } from 'components/PaperTheme'
import { RawTableGrid, type CellDecoration, type HeadingLine } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'
import {
    getConflictWorkbook,
    mergeConflictWorkbook,
    PALETTE_CONFLICT,
    type ConflictCellSide,
    type ConflictSide,
} from '../../services/compare'
import { errorMessage } from '../../utils/errorMessage'
import { openConflictCompareWindow } from '../projects/compare'
import { basename } from '../projects/projectPaths'
import { useCellStyles } from '../ComparePage/ComparisonPanes.styles'
import { CellValueEditor, type EditorKind } from '../modules/CellValueEditor'
import { useStyles as useCellFieldStyles } from '../modules/TableEditor.styles'
import { ButtonColumns, Columns, ColumnTitles, ConflictHeading, TakeBaseButton, TakeButton } from './MergeColumns'
import type { Side } from './textMerge'
import type {
    ConflictCell,
    ConflictSheet,
    ConflictTable,
    ConflictTableMerge,
    ConflictTableVersion,
    ConflictWorkbook,
    MergedWorkbook,
} from './types'
import {
    cellAt,
    cellKey,
    cellsLeftOf,
    choicesOf,
    conflictsOf,
    conflictWindowOf,
    decisionOf,
    decisionsOf,
    headingsFrom,
    isByCells,
    isConflicting,
    isDecided,
    isWhole,
    outOfSightOf,
    progressOf,
    resolvedOf,
    sideOfAll,
    sideOfCell,
    textOf,
    typedOf,
    withCellsTaken,
    withCellTyped,
    withEveryCellTaken,
    type CellChoices,
    type CellDecisions,
    type RowWindow,
    type WorkbookDecisions,
} from './workbookMerge'

/** How a resolved conflict is told, by the version it is taken from. */
const STATUSES: Record<ConflictSide, string> = {
    ours: 'merge:editor.status_yours',
    theirs: 'merge:editor.status_theirs',
    base: 'merge:editor.status_base',
}

/** How many rows a table shows at least for the way to the other conflicts to be drawn under it too. */
const LONG_TABLE_ROWS = 12

/** How many cells both branches changed differently a table names above its versions. */
const NAMED_CONFLICTS = 5

/**
 * The classes a version of a table is marked with: a cell its branch changed, a cell both changed differently, and the
 * whole table when its branch changed its formatting. A table merged cell by cell marks a cell the merge took by
 * itself, a conflicting cell a version is chosen for, a cell typed by hand and the cell picked.
 */
interface CellMarks {
    changedFrame: string
    conflict: string
    restyled: string
    merged: string
    chosen: string
    edited: string
    picked: string
}

/** Tells the editor the version a conflict is taken from, or none to leave it to resolve. */
type Decide = (id: string, side: ConflictSide | undefined) => void

/** Tells the editor what is decided for a table merged cell by cell, from what was decided before. */
type ChooseCells = (id: string, choose: (choices: CellChoices) => CellChoices) => void

/** Tells a table merged cell by cell what is decided for it, from what was decided before. */
type Choose = (choose: (choices: CellChoices) => CellChoices) => void

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
 * the base version, without the changes of either branch. A sheet whose conflict lies outside its tables is taken whole the same
 * way, and the comparison of the workbook tells what each branch changed in it. Any conflicting sheet can be taken
 * whole at once.
 *
 * A table only some cells of which conflict comes merged cell by cell: the result shows it with the cells only one
 * branch changed merged, and the user chooses the version of each conflicting cell or of the whole table, or types the
 * value of any cell in the cell itself. Taking the table whole stays possible.
 *
 * The workbook is merged on the server once every conflict is resolved. A merge still under way when the window
 * closes is dropped.
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
    const [cellDecisions, setCellDecisions] = useState<CellDecisions>(earlier?.cellDecisions ?? {})
    const [error, setError] = useState<string | null>(null)
    const [applyError, setApplyError] = useState<string | null>(null)
    const [applying, setApplying] = useState(false)
    // The sheet shown, and the table shown on each sheet of tables, from 0.
    const [picked, setPicked] = useState<string>()
    const [shownTables, setShownTables] = useState<Record<string, number>>({})
    const open = useRef(true)
    // The marks of the changed cells are read once for every table the editor draws. They are kept by their class
    // names, which stay the same while the theme does, so a table not decided again is not drawn again.
    const { styles: { changedFrame, conflict, restyled, merged, chosen, edited, picked: pickedMark } } = useCellStyles()
    const marks = useMemo<CellMarks>(
        () => ({ changedFrame, conflict, restyled, merged, chosen, edited, picked: pickedMark }),
        [changedFrame, conflict, restyled, merged, chosen, edited, pickedMark])

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

    const progress = useMemo(() => workbook && progressOf(workbook, decisions, cellDecisions),
        [workbook, decisions, cellDecisions])
    const resolved = !!progress && progress.resolved === progress.count
    // A sheet the merge takes by itself leaves nothing to look at or decide, so only the conflicts are gone through.
    const sheets = useMemo(() => workbook?.sheets.filter(isConflicting) ?? [], [workbook])
    // The editor opens on the first conflicting sheet, and each sheet on its first conflicting table.
    const sheet = sheets.find(item => item.name === picked) ?? sheets[0]
    const tableOf = (item: ConflictSheet) => Math.min(shownTables[item.name] ?? 0, item.tables.length - 1)
    const shownTable = sheet && !isWhole(sheet) ? sheet.tables[tableOf(sheet)] : undefined
    const compare = useCallback(() => openConflictCompareWindow(projectId, filePath), [projectId, filePath])
    const showTable = useCallback((name: string, index: number) => {
        setShownTables(current => ({ ...current, [name]: index }))
    }, [])

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

    // One function for every table merged cell by cell, so a table redraws only when its own choices change.
    const chooseCells = useCallback<ChooseCells>((id, choose) => {
        setApplyError(null)
        setCellDecisions(current => ({ ...current, [id]: choose(choicesOf(current, id)) }))
    }, [])

    const apply = async () => {
        if (!workbook || !resolved) {
            return
        }
        setApplying(true)
        setApplyError(null)
        try {
            const { whole, byCells } = decisionsOf(workbook, decisions, cellDecisions)
            const file = await mergeConflictWorkbook(projectId, filePath, whole, byCells)
            // Every decision is kept, the ones a sheet taken whole stands for included, for Edit to show them again.
            if (open.current) {
                onApply({ file, decisions, cellDecisions, workbook })
            }
        } catch (mergeError: unknown) {
            setApplyError(errorMessage(mergeError) || t('merge:workbook.apply_failed'))
        } finally {
            setApplying(false)
        }
    }

    // Whether a table of the workbook is merged cell by cell, which the result lets the reader edit.
    const editable = !!shownTable && isByCells(shownTable, decisions)

    return (
        <Modal
            centered
            destroyOnHidden
            open
            onCancel={onCancel}
            // The three columns take the width of the window. The tables scroll inside the dialog, under the titles
            // of the columns, which keeps its buttons within the window.
            // A wide table scrolls in its own column, so the dialog scrolls down alone: the gutters of the columns
            // reach a little past its edges, which would give it a scroll across that moves nothing.
            styles={{ body: { maxHeight: '72vh', overflowY: 'auto', overflowX: 'hidden' } }}
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
                    <Space size={4}>
                        {t('merge:workbook.title', { name: basename(filePath) })}
                        <Popover
                            placement="bottomLeft"
                            title={t('merge:workbook.legend')}
                            trigger="click"
                            content={(
                                <Legend
                                    testId="workbook-merge-legend"
                                    rows={[[
                                        { key: 'conflict', frame: marks.conflict, label: t('merge:workbook.legend_conflict') },
                                        { key: 'merged', frame: marks.merged, label: t('merge:workbook.legend_merged') },
                                        { key: 'chosen', frame: marks.chosen, label: t('merge:workbook.legend_chosen') },
                                        { key: 'edited', frame: marks.edited, label: t('merge:workbook.legend_edited') },
                                    ], [
                                        { key: 'changed', frame: marks.changedFrame, label: t('merge:workbook.legend_changed') },
                                        { key: 'restyled', frame: marks.restyled, label: t('merge:workbook.legend_restyled') },
                                    ]]}
                                />
                            )}
                        >
                            <Button
                                aria-label={t('merge:workbook.legend')}
                                data-testid="workbook-merge-legend-open"
                                icon={<QuestionCircleOutlined />}
                                size="small"
                                type="text"
                            />
                        </Popover>
                    </Space>
                    <ColumnTitles
                        versionLabels={versionLabels}
                        resultHint={editable
                            ? <Typography.Text type="secondary">{t('merge:workbook.edit_hint')}</Typography.Text>
                            : undefined}
                    />
                </Flex>
            )}
        >
            {error && <Alert showIcon data-testid="workbook-merge-error" title={error} type="error" />}
            {!error && !workbook && <Spin description={t('merge:workbook.loading')} />}
            {workbook && (
                <Flex vertical gap="small">
                    {/* The palette belongs to the whole workbook, so it stands above the sheets. */}
                    {workbook.palette && (
                        <PalettePart decision={decisionOf(decisions, PALETTE_CONFLICT)} onDecide={decide} />
                    )}
                    <Tabs
                        activeKey={sheet?.name ?? ''}
                        onChange={setPicked}
                        items={sheets.map(item => ({
                            key: item.name,
                            label: <SheetLabel cellDecisions={cellDecisions} decisions={decisions} sheet={item} />,
                            children: isWhole(item)
                                ? (
                                    <WholeSheetPart
                                        decision={decisionOf(decisions, item.name)}
                                        onCompare={compare}
                                        onDecide={decide}
                                        sheet={item}
                                    />
                                )
                                : (
                                    <SheetTables
                                        cellDecisions={cellDecisions}
                                        decisions={decisions}
                                        index={tableOf(item)}
                                        marks={marks}
                                        onChooseCells={chooseCells}
                                        onDecide={decide}
                                        onGo={showTable}
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

/** Draws the way through the tables of a sheet where a part of the window asks for it, under a test id. */
type Navigation = (testId: string) => React.ReactNode

interface SheetLabelProps {
    sheet: ConflictSheet
    decisions: WorkbookDecisions
    cellDecisions: CellDecisions
}

/** The name of a conflicting sheet on its tab, with how many of its conflicts are left. */
const SheetLabel: React.FC<SheetLabelProps> = ({ sheet, decisions, cellDecisions }) => {
    const { t } = useTranslation()
    const left = conflictsOf(sheet).length - resolvedOf(sheet, decisions, cellDecisions)
    return (
        <Space size={4}>
            {sheet.name}
            {left === 0
                ? <Tag color="success">{t('merge:workbook.resolved')}</Tag>
                : <Tag color="warning">{t('merge:workbook.left', { count: left })}</Tag>}
        </Space>
    )
}

interface SheetTablesProps {
    sheet: ConflictSheet
    /** The table shown, from 0. */
    index: number
    /** Shows a table of a sheet, by the name of the sheet and the place of the table, from 0. */
    onGo: (sheet: string, index: number) => void
    decisions: WorkbookDecisions
    cellDecisions: CellDecisions
    onDecide: Decide
    onChooseCells: ChooseCells
    marks: CellMarks
}

/** The conflicting tables of a sheet, one at a time, with the way to the others when the sheet has more than one. */
const SheetTables: React.FC<SheetTablesProps> = ({
    sheet, index, onGo, decisions, cellDecisions, onDecide, onChooseCells, marks,
}) => {
    const table = sheet.tables[index]
    const count = sheet.tables.length
    // The same way through for as long as the same table is shown, so the table is not drawn again for it.
    const navigation = useCallback<Navigation>(testId => (count > 1
        ? <StepNavigation count={count} index={index} onGo={next => onGo(sheet.name, next)} testId={testId} />
        : null), [count, index, onGo, sheet.name])
    return table && (
        <TablePart
            // A table shown again starts as it would the first time, with the rows around its cells.
            key={table.id}
            choices={choicesOf(cellDecisions, table.id)}
            decision={decisionOf(decisions, table.id)}
            marks={marks}
            navigation={navigation}
            number={index + 1}
            onChooseCells={onChooseCells}
            onDecide={onDecide}
            table={table}
        />
    )
}

interface StepNavigationProps {
    /** The conflict shown, from 0. */
    index: number
    count: number
    onGo: (index: number) => void
    testId: string
}

/** The way through the tables of a sheet, one at a time: which one is shown, of how many, and the arrows. */
const StepNavigation: React.FC<StepNavigationProps> = ({ index, count, onGo, testId }) => (
    <span data-testid={testId}>
        <Pagination
            current={index + 1}
            onChange={page => onGo(page - 1)}
            pageSize={1}
            showSizeChanger={false}
            simple={{ readOnly: true }}
            size="small"
            total={count}
        />
    </span>
)

interface ChoiceButtonsProps {
    decision: ConflictSide | undefined
    /** What taking the base version takes; none for a conflict that is taken from your or their version only. */
    hint?: string | undefined
    /** What a button tells in place of taking its version, such as removing what the version has not got. */
    labels?: Partial<Record<ConflictCellSide, string>> | undefined
    onDecide: (side: ConflictSide) => void
    testId: string
}

/** The buttons that take a conflict from a version, each in the column of the version they take. */
const ChoiceButtons: React.FC<ChoiceButtonsProps> = props => (
    <ButtonColumns
        ours={<ChoiceButton {...props} side="ours" />}
        result={<ChoiceButton {...props} side="base" />}
        theirs={<ChoiceButton {...props} side="theirs" />}
    />
)

/** The button that takes a conflict from one version; none for the base version of a conflict that offers none. */
const ChoiceButton: React.FC<ChoiceButtonsProps & { side: ConflictSide }> = ({
    side, decision, hint, labels, onDecide, testId,
}) => {
    const { t } = useTranslation()
    if (side === 'base') {
        return hint && (
            <TakeBaseButton
                disabled={decision === 'base'}
                hint={hint}
                onTake={() => onDecide('base')}
                testId={`${testId}-take-base`}
            />
        )
    }
    return (
        <TakeButton
            disabled={decision === side}
            label={labels?.[side] ?? t(side === 'ours' ? 'merge:editor.take_yours' : 'merge:editor.take_theirs')}
            onTake={() => onDecide(side)}
            side={side}
            testId={`${testId}-take-${side}`}
        />
    )
}

/** What the buttons of the versions without a sheet or a table tell: that taking them removes it. */
const removalsOf = (
    held: { ours?: unknown; theirs?: unknown },
    removal: string
): Partial<Record<ConflictCellSide, string>> => ({
    ...(held.ours ? {} : { ours: removal }),
    ...(held.theirs ? {} : { theirs: removal }),
})

interface WholeSheetPartProps {
    sheet: ConflictSheet
    decision: ConflictSide | undefined
    onCompare: () => void
    onDecide: Decide
}

/**
 * A sheet resolved only as a whole, however many tables it holds: why, and the buttons that take it from a version,
 * which remove it when the version has no such sheet. What each branch changed in it is read in the comparison of the
 * workbook.
 */
const WholeSheetPart: React.FC<WholeSheetPartProps> = ({ sheet, decision, onCompare, onDecide }) => {
    const { t } = useTranslation()
    const titleId = useId()
    return (
        <Flex vertical aria-labelledby={titleId} data-testid="workbook-merge-sheet" gap="small" role="group">
            <ConflictHeading
                status={decision && t(STATUSES[decision])}
                title={t('merge:workbook.sheet', { name: sheet.name })}
                titleId={titleId}
                extra={(
                    <Button data-testid="workbook-merge-sheet-compare" onClick={onCompare} size="small" type="link">
                        {t('merge:workbook.compare_versions')}
                    </Button>
                )}
            />
            {sheet.reason && (
                <Typography.Text type="secondary">{t(`merge:workbook.sheet_reason.${sheet.reason}`)}</Typography.Text>
            )}
            <ChoiceButtons
                decision={decision}
                hint={t('merge:workbook.base_sheet_hint')}
                labels={removalsOf(sheet, t('merge:workbook.remove_sheet'))}
                onDecide={side => onDecide(sheet.name, side)}
                testId="workbook-merge-sheet"
            />
        </Flex>
    )
}

interface TablePartProps {
    table: ConflictTable
    /** The place of the table among the conflicting tables of its sheet, from 1. */
    number: number
    navigation: Navigation
    decision: ConflictSide | undefined
    /** What is decided for the table merged cell by cell. */
    choices: CellChoices
    onDecide: Decide
    onChooseCells: ChooseCells
    marks: CellMarks
}

/**
 * A conflicting table: why its changes conflict, the cells both branches changed differently, each version of it beside
 * the result, and the buttons that take it from a version. Until it is resolved, the result shows the base version.
 *
 * A table that comes merged cell by cell shows the merged table as the result, until it is taken whole: the cells only
 * one branch changed are merged, and each conflicting cell is decided in the result. A table taken whole can be merged
 * cell by cell again, with the choices made before.
 *
 * A table with such cells shows the rows around them, the same rows in every version, until the whole table is asked
 * for.
 */
const TablePart = React.memo(function TablePart({
    table,
    number,
    navigation,
    decision,
    choices,
    onDecide,
    onChooseCells,
    marks,
}: TablePartProps) {
    const { t } = useTranslation()
    const titleId = useId()
    const [whole, setWhole] = useState(false)
    const around = useMemo(() => conflictWindowOf(table), [table])
    const shown = whole ? undefined : around
    const testId = `workbook-merge-table-${number}`
    // A table that comes merged is merged cell by cell until it is taken whole.
    const merged = decision ? undefined : table.merged
    const left = merged && cellsLeftOf(table, choices)
    const status = statusOf(decision, left)
    const choose: Choose = chosen => onChooseCells(table.id, chosen)
    const take = (side: ConflictCellSide) => merged && {
        side,
        conflicts: merged.conflicts,
        choices,
        onTake: (cell: ConflictCell) => choose(current => withCellsTaken(current, [cell], side)),
        testId,
    }
    const wholeTable: ChoiceButtonsProps = {
        decision,
        hint: t('merge:workbook.base_table_hint'),
        labels: removalsOf(table, t('merge:workbook.remove_table')),
        onDecide: side => onDecide(table.id, side),
        testId,
    }
    // A table merged cell by cell with more than one conflicting cell also takes a version for all of them at once.
    const allConflicts: ChoiceButtonsProps | undefined = merged && merged.conflicts.length > 1 ? {
        decision: sideOfAll(choices, merged.conflicts),
        labels: { ours: t('merge:workbook.all_conflicts_yours'), theirs: t('merge:workbook.all_conflicts_theirs') },
        onDecide: side => {
            if (side !== 'base') {
                choose(current => withEveryCellTaken(current, merged.conflicts, side))
            }
        },
        testId: `${testId}-all`,
    } : undefined
    // The conflicting cells are red until the table is taken, then marked chosen in the version taken and the result.
    const conflictFrameOf = (side: ConflictSide) => {
        if (!decision) {
            return marks.conflict
        }
        return side === decision || side === 'base' ? marks.chosen : undefined
    }
    const buttonsOf = (side: ConflictSide) => (
        <>
            <ChoiceButton {...wholeTable} side={side} />
            {allConflicts && <ChoiceButton {...allConflicts} side={side} />}
        </>
    )
    return (
        <Flex vertical aria-labelledby={titleId} data-testid={testId} gap="small" role="group">
            <ConflictHeading
                lead={navigation('workbook-merge-steps')}
                pending={left === undefined ? undefined : t('merge:workbook.conflicts_left', { count: left })}
                status={status && t(status)}
                title={table.name}
                titleId={titleId}
                extra={decision && table.merged && (
                    <Tooltip title={t('merge:workbook.undo_whole_hint')}>
                        <Button
                            data-testid={`${testId}-undo`}
                            icon={<UndoOutlined />}
                            onClick={() => onDecide(table.id, undefined)}
                            size="small"
                            type="link"
                        >
                            {t('merge:workbook.undo_whole')}
                        </Button>
                    </Tooltip>
                )}
            />
            <TableChoices choices={choices} merged={merged} onChoose={choose} table={table} testId={testId} />
            {around && <RowsSwitch onSwitch={() => setWhole(!whole)} shown={shown} table={table} testId={testId} />}
            <Columns
                ours={(
                    <TableColumn buttons={buttonsOf('ours')} justify="end">
                        <VersionGrid
                            conflictFrame={conflictFrameOf('ours')}
                            marks={marks}
                            shownRows={shown}
                            take={take('ours')}
                            version={table.ours}
                        />
                    </TableColumn>
                )}
                result={(
                    <TableColumn buttons={buttonsOf('base')} justify="center">
                        {merged
                            ? (
                                <MergedPart
                                    choices={choices}
                                    marks={marks}
                                    merged={merged}
                                    onChoose={choose}
                                    shownRows={shown}
                                    testId={testId}
                                />
                            )
                            : (
                                <VersionGrid
                                    absent={t('merge:workbook.table_left_out')}
                                    conflictFrame={conflictFrameOf('base')}
                                    marks={marks}
                                    shownRows={shown}
                                    version={decision ? table[decision] : table.base}
                                />
                            )}
                    </TableColumn>
                )}
                theirs={(
                    <TableColumn buttons={buttonsOf('theirs')} justify="start">
                        <VersionGrid
                            conflictFrame={conflictFrameOf('theirs')}
                            marks={marks}
                            shownRows={shown}
                            take={take('theirs')}
                            version={table.theirs}
                        />
                    </TableColumn>
                )}
            />
            {/* A table this long is gone past from under it as well, without scrolling back up. */}
            {rowsShownOf(table, shown) > LONG_TABLE_ROWS && (
                <Flex justify="center">{navigation('workbook-merge-steps-bottom')}</Flex>
            )}
        </Flex>
    )
})

/**
 * What the heading of a table tells of how it is resolved: the version it is taken from, or that each of its
 * conflicting cells is decided; none while it is not resolved.
 *
 * @param left how many conflicting cells of a table merged cell by cell are left, or none for a table taken whole
 */
const statusOf = (decision: ConflictSide | undefined, left: number | undefined): string | undefined => {
    if (decision) {
        return STATUSES[decision]
    }
    return left === 0 ? 'merge:workbook.status_all_resolved' : undefined
}

/** How many rows the versions of a table show: the rows around its conflicts, or the longest version whole. */
const rowsShownOf = (table: ConflictTable, shown: RowWindow | undefined): number => (shown
    ? shown.to - shown.from + 1
    : Math.max(...[table.ours, table.base, table.theirs].map(version => version?.cells.length ?? 0)))

interface TableChoicesProps {
    table: ConflictTable
    /** The table merged cell by cell, or none for a table taken whole. */
    merged: ConflictTableMerge | undefined
    choices: CellChoices
    onChoose: Choose
    testId: string
}

/**
 * What tells how a conflicting table differs, above its versions. A table taken whole tells why it conflicts and names
 * the cells both branches changed differently. A table merged cell by cell names the conflicting cells past the rows
 * shown.
 */
const TableChoices: React.FC<TableChoicesProps> = ({ table, merged, choices, onChoose, testId }) => {
    const { t } = useTranslation()
    const formattingBoth = table.ours?.formattingChanged && table.theirs?.formattingChanged
    if (merged) {
        return (
            <>
                {formattingBoth && <FormattingBoth testId={testId} />}
                {/* A conflicting cell the rows of the result leave out is named, so it can still be decided. */}
                {outOfSightOf(merged).length > 0 && (
                    <MergedConflictCells choices={choices} merged={merged} onChoose={onChoose} testId={testId} />
                )}
            </>
        )
    }
    return (
        <>
            <Typography.Text type="secondary">{t(`merge:workbook.table_reason.${table.reason}`)}</Typography.Text>
            {formattingBoth && <FormattingBoth testId={testId} />}
            <ConflictCells table={table} />
        </>
    )
}

interface TableColumnProps {
    /** The buttons that take the table from the version of the column. */
    buttons: React.ReactNode
    /** Where the buttons stand over the table: next to the result they point to, or in the middle of the result. */
    justify: 'end' | 'center' | 'start'
    children: React.ReactNode
}

/** A version of a table under the buttons that take it, as wide as the table, so the buttons line up with its edges. */
const TableColumn: React.FC<TableColumnProps> = ({ buttons, justify, children }) => (
    <Flex vertical gap="small" style={{ width: 'fit-content', maxWidth: '100%' }}>
        <Flex gap="small" justify={justify}>{buttons}</Flex>
        {children}
    </Flex>
)

/** Tells that both branches changed the formatting of a table. */
const FormattingBoth: React.FC<{ testId: string }> = ({ testId }) => {
    const { t } = useTranslation()
    return (
        <Typography.Text data-testid={`${testId}-formatting`} type="warning">
            {t('merge:workbook.formatting_both')}
        </Typography.Text>
    )
}

interface RowsSwitchProps {
    table: ConflictTable
    /** The rows shown around the conflicts, or none while the whole table is shown. */
    shown: RowWindow | undefined
    onSwitch: () => void
    testId: string
}

/** Which rows of a table are shown, and the button that shows every row, or the rows around the conflicts again. */
const RowsSwitch: React.FC<RowsSwitchProps> = ({ table, shown, onSwitch, testId }) => {
    const { t } = useTranslation()
    return (
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
            <Button data-testid={`${testId}-whole`} onClick={onSwitch} size="small" type="link">
                {shown ? t('merge:workbook.show_whole_table') : t('merge:workbook.show_conflict_rows')}
            </Button>
        </Flex>
    )
}

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

interface MergedConflictCellsProps {
    merged: ConflictTableMerge
    choices: CellChoices
    onChoose: Choose
    testId: string
}

/**
 * The cells both branches changed differently past the rows of a table merged cell by cell that are shown, each with
 * what your and their version hold in it and how it is decided. The name of a cell offers its two versions.
 */
const MergedConflictCells: React.FC<MergedConflictCellsProps> = ({ merged, choices, onChoose, testId }) => {
    const { t } = useTranslation()
    // The cell whose versions are offered, by its name.
    const [offered, setOffered] = useState<string>()
    return (
        <Flex vertical data-testid={`${testId}-conflict-cells`} gap={2}>
            <Typography.Text strong>{t('merge:workbook.conflict_cells')}</Typography.Text>
            {outOfSightOf(merged).map(cell => {
                const side = sideOfCell(choices, cell.row, cell.column)
                const typed = typedOf(choices, cell.row, cell.column)
                return (
                    <Flex key={cell.cell} wrap align="center" gap="small">
                        <Popover
                            onOpenChange={open => setOffered(open ? cell.cell : undefined)}
                            open={offered === cell.cell}
                            trigger="click"
                            content={(
                                <CellChoice
                                    cells={[cell]}
                                    choices={choices}
                                    onChoose={onChoose}
                                    onDone={() => setOffered(undefined)}
                                    testId={`${testId}-${cell.cell}`}
                                />
                            )}
                        >
                            <Button size="small" type="link">{cell.cell}</Button>
                        </Popover>
                        <Typography.Text>
                            {t('merge:workbook.cell_versions', {
                                ours: textOf(cell.ours),
                                theirs: textOf(cell.theirs),
                            })}
                        </Typography.Text>
                        {typed !== undefined && <Tag color="processing">{t('merge:editor.status_edited')}</Tag>}
                        {typed === undefined && side && <Tag color="success">{t(STATUSES[side])}</Tag>}
                    </Flex>
                )
            })}
        </Flex>
    )
}

/**
 * The arrow beside a conflicting cell of a version, pointing to the result: it takes the value of the cell into the
 * result.
 */
const TakeArrow: React.FC<{ cell: ConflictCell; take: SideTake }> = ({ cell, take }) => {
    const { t } = useTranslation()
    const label = t(take.side === 'ours' ? 'merge:workbook.take_cell_yours' : 'merge:workbook.take_cell_theirs',
        { cell: cell.cell })
    return (
        <Button
            aria-label={label}
            color="primary"
            data-testid={`${take.testId}-${cell.cell}-take-${take.side}`}
            icon={take.side === 'ours' ? <DoubleRightOutlined /> : <DoubleLeftOutlined />}
            onClick={() => take.onTake(cell)}
            size="small"
            // The arrow keeps a little room from the value it stands beside.
            style={take.side === 'ours' ? { marginInlineStart: 4 } : { marginInlineEnd: 4 }}
            title={label}
            variant="text"
        />
    )
}

/** A sign of a legend and what it tells. */
interface LegendEntry {
    key: string
    /** The frame of the cells the entry keys, which its swatch wears, so the two cannot drift apart. */
    frame?: string | undefined
    /** What stands for the entry where it keys no frame, such as a letter and a number of the headings. */
    sign?: React.ReactNode
    label: React.ReactNode
}

/**
 * What the frames and the signs of the tables mean, one under another, each shown as the tables show it beside a few
 * words. The entries come in groups, each group a kind of its own, set apart by a wider gap.
 */
const Legend: React.FC<{ rows: LegendEntry[][]; testId: string }> = ({ rows, testId }) => (
    <Flex vertical data-testid={testId} gap="middle">
        {rows.map(entries => (
            <Flex key={entries[0]?.key} vertical gap="small">
                {entries.map(entry => (
                    <Space key={entry.key} size="small">
                        {entry.frame !== undefined && (
                            // The margin leaves room for a frame drawn outside the swatch, such as the dashed one.
                            <span
                                aria-hidden
                                className={entry.frame}
                                style={{ display: 'block', width: 14, height: 14, margin: 2 }}
                            />
                        )}
                        {entry.sign}
                        <Typography.Text>{entry.label}</Typography.Text>
                    </Space>
                ))}
            </Flex>
        ))}
    </Flex>
)

interface CellChoiceProps {
    /** The conflicting cells offered: a cell, or those of a row or a column. */
    cells: ConflictCell[]
    choices: CellChoices
    onChoose: Choose
    /** Closes the choice once a version is taken. */
    onDone: () => void
    testId: string
}

/**
 * The two versions conflicting cells take their values from, a cell, a row or a column of them, the one they take now
 * marked. Taking one replaces what was chosen or typed for each of them before.
 */
const CellChoice: React.FC<CellChoiceProps> = ({ cells, choices, onChoose, onDone, testId }) => {
    const { t } = useTranslation()
    const side = sideOfAll(choices, cells)
    const take = (taken: ConflictCellSide) => () => {
        onDone()
        onChoose(current => withCellsTaken(current, cells, taken))
    }
    return (
        <Space data-testid={testId} size="small">
            <Button
                data-testid={`${testId}-ours`}
                onClick={take('ours')}
                size="small"
                type={side === 'ours' ? 'primary' : 'default'}
            >
                {t('merge:editor.take_yours')}
            </Button>
            <Button
                data-testid={`${testId}-theirs`}
                onClick={take('theirs')}
                size="small"
                type={side === 'theirs' ? 'primary' : 'default'}
            >
                {t('merge:editor.take_theirs')}
            </Button>
        </Space>
    )
}

interface MergedPartProps {
    merged: ConflictTableMerge
    choices: CellChoices
    onChoose: Choose
    marks: CellMarks
    /** The rows shown, or none to show every row. */
    shownRows: RowWindow | undefined
    testId: string
}

/** Where a cell or a heading lies over the table, in pixels from the top left corner of the table. */
interface Box {
    left: number
    top: number
    width: number
    height: number
}

/**
 * The conflicting cells whose versions are offered: one cell, or those of a row or a column. The popover stands at
 * the cell or at the heading, and the key names the cell picked.
 */
interface Offer {
    cells: ConflictCell[]
    box: Box
    key?: string | undefined
    testId: string
}

/**
 * The result of a table merged cell by cell: the merged table, decided in its own cells, under the letters of its
 * columns and beside the numbers of its rows on the sheet. A click on a conflicting cell offers your and their version
 * of it beside the cell; a click on the heading of a row or a column with conflicting cells offers them for all its
 * conflicting cells. A click on any other cell, or a double click on a conflicting one, opens the cell to type its
 * value.
 *
 * A conflicting cell shows the version chosen for it, or the base version while none is; a cell typed by hand shows
 * what was typed. A cell one branch alone changed is framed as the version it comes from frames it.
 *
 * The versions are offered over the cell rather than in it: a cell that changed what it holds between the two clicks
 * of a double click is given no double click by the browser, and it would lose the note shown over it.
 */
const MergedPart: React.FC<MergedPartProps> = ({ merged, choices, onChoose, marks, shownRows, testId }) => {
    const { t } = useTranslation()
    const table = useRef<HTMLDivElement>(null)
    // The conflicting cells whose versions are offered, and the cell being typed into, by its key.
    const [offer, setOffer] = useState<Offer>()
    const [open, setOpen] = useState<string>()
    const conflicts = useMemo(() => new Map(merged.conflicts.map(cell => [cellKey(cell.row, cell.column), cell])),
        [merged])
    const changed = useMemo(() => new Set(merged.changedCells), [merged])
    const offset = shownRows?.from ?? 0
    const rows = useMemo(() => (shownRows ? merged.cells.slice(shownRows.from, shownRows.to + 1) : merged.cells)
        .map((line, place) => line.map((cell, column) => resultCellOf(cell,
            conflicts.get(cellKey(place + offset, column)), choices, place + offset, column))),
    [merged, conflicts, choices, shownRows, offset])
    /** Where an element lies over the table. */
    const boxOf = (element: Element): Box | undefined => {
        const from = table.current?.getBoundingClientRect()
        const at = element.getBoundingClientRect()
        return from && { left: at.left - from.left, top: at.top - from.top, width: at.width, height: at.height }
    }
    // A click on a conflicting cell offers its versions; any other cell, and the field being typed into, keeps it.
    const pick = (row: number, column: number) => {
        const key = cellKey(row + offset, column)
        const conflict = conflicts.get(key)
        if (key === open || !conflict) {
            return
        }
        const cell = table.current?.querySelector(`[data-cell="${conflict.cell}"]`)
        const box = cell && boxOf(cell)
        if (box) {
            setOffer({ cells: [conflict], box, key, testId: `${testId}-cell` })
        }
    }
    const openCell = (row: number, column: number) => {
        setOffer(undefined)
        setOpen(cellKey(row + offset, column))
    }
    // The rows and the columns with conflicting cells, which their headings decide at once.
    const picks = useMemo(() => ({
        rows: new Set(merged.conflicts.map(cell => cell.row - offset)),
        columns: new Set(merged.conflicts.map(cell => cell.column)),
    }), [merged, offset])
    const pickLine = (line: HeadingLine, heading: HTMLElement) => {
        const box = boxOf(heading)
        const cells = merged.conflicts.filter(cell => 'row' in line
            ? cell.row === line.row + offset
            : cell.column === line.column)
        if (box) {
            setOffer({ cells, box, testId: `${testId}-${'row' in line ? 'row' : 'column'}` })
        }
    }
    /**
     * The field a cell is typed into. It keeps what is typed when it differs from what the cell shows, and for a
     * conflicting cell left undecided even when it does not, so that the base value it shows can be kept.
     */
    const fieldOf = (cell: RawTableCell, key: string, typed: boolean, undecided: boolean): CellDecoration => {
        const shown = textOf(cell)
        return {
            content: (
                <OpenMergedCell
                    from={shown}
                    onCancel={() => setOpen(undefined)}
                    onCommit={value => {
                        setOpen(undefined)
                        if (value !== shown || undecided) {
                            onChoose(current => withCellTyped(current, key, value))
                        }
                    }}
                    onUntype={typed
                        ? () => {
                            setOpen(undefined)
                            onChoose(current => withCellTyped(current, key, undefined))
                        }
                        : undefined}
                />
            ),
        }
    }
    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        if (cell.covered) {
            return undefined
        }
        const key = cellKey(row + offset, column)
        const conflict = conflicts.get(key)
        const typed = typedOf(choices, row + offset, column) !== undefined
        if (key === open) {
            return fieldOf(cell, key, typed, !!conflict && !isDecided(choices, conflict))
        }
        const frame = frameOf(marks, {
            typed,
            conflict: !!conflict,
            chosen: !!conflict && !!sideOfCell(choices, row + offset, column),
            merged: !!cell.cell && changed.has(cell.cell),
        })
        const classes = [frame, key === offer?.key && marks.picked].filter(Boolean)
        return classes.length === 0 ? undefined : { className: classes.join(' ') }
    }
    return (
        <Flex vertical data-testid={`${testId}-merged`} gap={4}>
            <Flex style={{ overflowX: 'auto' }}>
                <div ref={table} style={{ position: 'relative' }}>
                    <PaperTheme>
                        <RawTableGrid
                            decorate={decorate}
                            headings={{ from: headingsFrom(merged.range, offset), picks, onPick: pickLine }}
                            onOpenCell={openCell}
                            onPickCell={pick}
                            rows={rows}
                        />
                    </PaperTheme>
                    {offer && (
                        <Popover
                            open
                            onOpenChange={shown => !shown && setOffer(undefined)}
                            placement="bottom"
                            trigger="click"
                            content={(
                                <CellChoice
                                    cells={offer.cells}
                                    choices={choices}
                                    onChoose={onChoose}
                                    onDone={() => setOffer(undefined)}
                                    testId={offer.testId}
                                />
                            )}
                        >
                            {/* Clicks go through to the cell under it, which a double click opens to type into. */}
                            <span style={{ position: 'absolute', pointerEvents: 'none', ...offer.box }} />
                        </Popover>
                    )}
                </div>
            </Flex>
            {!shownRows && merged.rows > merged.cells.length && (
                <Typography.Text type="secondary">
                    {t('merge:workbook.rows_shown', { shown: merged.cells.length, count: merged.rows })}
                </Typography.Text>
            )}
        </Flex>
    )
}

interface OpenMergedCellProps {
    /** What the cell shows as it is opened, which the typing starts from. */
    from: string
    /** Keeps what was typed and closes the cell. */
    onCommit: (value: string) => void
    /** Closes the cell, leaving it as it was. */
    onCancel: () => void
    /** Gives the value typed for the cell up, or none when no value is typed for it. */
    onUntype: (() => void) | undefined
}

/**
 * A cell of a merged table being typed into, as a cell of the table editor is: Enter keeps what was typed, Escape
 * leaves the cell as it was. A cell typed into before gives its value up from the end of its field.
 *
 * What is being typed lives here, so a keystroke redraws this cell alone.
 */
const OpenMergedCell: React.FC<OpenMergedCellProps> = ({ from, onCommit, onCancel, onUntype }) => {
    const { t } = useTranslation()
    const { styles } = useCellFieldStyles(true)
    const [draft, setDraft] = useState(from)
    // The way of writing the reader switched to, from one line to several.
    const [switched, setSwitched] = useState<EditorKind>()
    const kind = switched ?? (draft.includes('\n') ? 'multiline' : 'text')
    return (
        <CellValueEditor
            asked={undefined}
            className={styles.input}
            kind={kind}
            onCancel={onCancel}
            onChange={setDraft}
            onCommit={value => onCommit(value ?? draft)}
            onSwitch={setSwitched}
            value={draft}
            suffix={onUntype && (
                <Button
                    aria-label={t('merge:workbook.untype_value')}
                    data-testid="workbook-merge-untype"
                    icon={<UndoOutlined />}
                    onClick={onUntype}
                    // The field keeps the focus, so losing it does not close the cell before the value is given up.
                    onMouseDown={event => event.preventDefault()}
                    size="small"
                    title={t('merge:workbook.untype_value')}
                    type="text"
                />
            )}
        />
    )
}

/**
 * What a cell of a table merged cell by cell shows: the value typed for it, or the version chosen for a conflicting
 * cell, or the merged cell itself.
 */
const resultCellOf = (
    cell: RawTableCell,
    conflict: ConflictCell | undefined,
    choices: CellChoices,
    row: number,
    column: number
): RawTableCell => {
    const side = conflict && sideOfCell(choices, row, column)
    const shown = conflict && side ? conflict[side] : cell
    const typed = typedOf(choices, row, column)
    if (typed === undefined) {
        return shown
    }
    const { formula: _formula, runs: _runs, ...rest } = shown
    return { ...rest, value: typed }
}

/** The version a side of a table merged cell by cell gives its conflicting cells, taken by the arrows beside them. */
interface SideTake {
    side: ConflictCellSide
    conflicts: ConflictCell[]
    choices: CellChoices
    onTake: (cell: ConflictCell) => void
    testId: string
}

interface VersionGridProps {
    version: ConflictTableVersion | undefined
    /** What a version without the table tells, or none to leave its place empty. */
    absent?: string | undefined
    marks: CellMarks
    /** The rows shown, or none to show every row. */
    shownRows: RowWindow | undefined
    /** The frame of the cells both branches changed differently, or none to leave them unframed. */
    conflictFrame: string | undefined
    /** What the arrows of a side of a table merged cell by cell take, or none for a table taken whole. */
    take?: SideTake | undefined
}

/**
 * A version of a table in its own formatting: the cells both branches changed differently framed as the conflict
 * stands, the other cells its branch changed framed in the colour of a change. A version whose formatting its branch changed is framed
 * whole and says so. A version without the table says so too.
 */
const VersionGrid: React.FC<VersionGridProps> = ({ version, absent, marks, shownRows, conflictFrame, take }) => {
    const { t } = useTranslation()
    const changed = useMemo(() => new Set(version?.changedCells ?? []), [version])
    const conflicting = useMemo(() => new Set(version?.conflictCells ?? []), [version])
    // The rows shown stay the same while the choices change, so the grid keeps its lines.
    const rows = useMemo(() => version && (shownRows
        ? version.cells.slice(shownRows.from, shownRows.to + 1)
        : version.cells), [version, shownRows])
    // The conflicting cells of a table merged cell by cell, by where they lie in this version.
    const takes = useMemo(() => new Map((take?.conflicts ?? []).map(cell => [
        take?.side === 'theirs' ? cell.theirCell : cell.ourCell,
        cell,
    ] as const)), [take?.conflicts, take?.side])
    if (!version || !rows) {
        return absent === undefined ? null : <Typography.Text italic type="secondary">{absent}</Typography.Text>
    }
    const decorate = (cell: RawTableCell): CellDecoration | undefined => {
        if (!cell.cell) {
            return undefined
        }
        const conflict = takes.get(cell.cell)
        if (take && conflict) {
            return takeDecorationOf(conflict, take, marks)
        }
        if (conflicting.has(cell.cell)) {
            return conflictFrame === undefined ? undefined : { className: conflictFrame }
        }
        if (!changed.has(cell.cell)) {
            return undefined
        }
        // A change of a table merged cell by cell is merged by itself; one of a table taken whole is what a choice takes.
        return { className: take ? marks.merged : marks.changedFrame }
    }
    return (
        <Flex vertical gap={4}>
            {version.formattingChanged && (
                <Tag color="warning" data-testid="workbook-merge-formatting-changed">
                    {t('merge:workbook.formatting_changed')}
                </Tag>
            )}
            {/* A table wider than its column scrolls on its own, as the versions of a comparison do. */}
            <Flex className={version.formattingChanged ? marks.restyled : undefined} style={{ overflowX: 'auto' }}>
                <PaperTheme>
                    <RawTableGrid
                        decorate={decorate}
                        headings={{ from: headingsFrom(version.range, shownRows?.from ?? 0) }}
                        rows={rows}
                    />
                </PaperTheme>
            </Flex>
            {!shownRows && version.rows > version.cells.length && (
                <Typography.Text type="secondary">
                    {t('merge:workbook.rows_shown', { shown: version.cells.length, count: version.rows })}
                </Typography.Text>
            )}
        </Flex>
    )
}

/**
 * How a conflicting cell of a version of a table merged cell by cell is drawn. It is red until it is decided, then
 * yellow in the version chosen and plain in the other one, or in both when a value is typed for it instead. The
 * version chosen has nothing left to take, so only the other one keeps its arrow.
 */
const takeDecorationOf = (conflict: ConflictCell, take: SideTake, marks: CellMarks): CellDecoration => {
    if (sideOfAll(take.choices, [conflict]) === take.side) {
        return { className: marks.chosen }
    }
    const className = isDecided(take.choices, conflict) ? undefined : marks.conflict
    const arrow = <TakeArrow cell={conflict} take={take} />
    return take.side === 'ours' ? { className, after: arrow } : { className, before: arrow }
}

/** What is known of a cell of a table merged cell by cell, which its frame tells. */
interface CellState {
    typed: boolean
    conflict: boolean
    chosen: boolean
    merged: boolean
}

/**
 * The one frame of a cell of a table merged cell by cell: blue for a value typed, yellow for a conflict a version is
 * chosen for, red for a conflict still to resolve, green for a change the merge took by itself, none otherwise. A cell
 * wears one frame only, so the first that applies is the one seen.
 */
const frameOf = (marks: CellMarks, state: CellState): string | undefined => {
    if (state.typed) {
        return marks.edited
    }
    if (state.conflict) {
        return state.chosen ? marks.chosen : marks.conflict
    }
    return state.merged ? marks.merged : undefined
}

/** The palette of an .xls workbook both branches changed differently, taken from your or their version. */
interface PalettePartProps {
    decision: ConflictSide | undefined
    onDecide: Decide
}

const PalettePart: React.FC<PalettePartProps> = ({ decision, onDecide }) => {
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

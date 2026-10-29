import React, { lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Divider, Flex, Modal, Space, Spin, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { getConflictText } from '../../services/compare'
import { errorMessage } from '../../utils/errorMessage'
import { basename } from '../projects/projectPaths'
import { ButtonColumns, ColumnTitles, Columns, ConflictHeading, TakeBaseButton, TakeButton } from './MergeColumns'
import type {
    ConflictChoice,
    ConflictDecision,
    ConflictedTextBlock,
    ConflictTextBlock,
    MergedText,
    MergedTextBlock,
} from './types'
import {
    holdsSide,
    lineBreakOf,
    lineCount,
    linesOf,
    mergedTextOf,
    shownLines,
    takeSide,
    withoutLastLineBreak,
    writtenLines,
    type Side,
} from './textMerge'

// CodeMirror is large, so it is loaded once the editor opens, as the other screens that show a file load it.
const CodeEditor = lazy(() => import('../projects/CodeEditor').then(module => ({ default: module.CodeEditor })))

/** How many lines no branch changed are shown whole; longer runs show their ends around a link to the rest. */
const SHOWN_LINES = 8
const END_LINES = 3

/** What each column shows of a part of the file: your lines, the lines of the result, their lines. */
interface ColumnTexts {
    ours: string
    result: string
    theirs: string
}

/** The numbers the first lines of a part carry in each column. */
interface FirstLines {
    firstOurs: number
    firstResult: number
    firstTheirs: number
}

/** A part of the file as the editor draws it: its block, its place among the conflicts, and where its lines start. */
interface Part extends FirstLines {
    block: ConflictTextBlock
    /** The place of the part in the file, from 0. The parts stay as they are while the file is merged. */
    place: number
    /** The place of a conflict among the conflicts of the file, from 0; -1 for lines merged by themselves. */
    conflict: number
}

/** The lines each column shows of a part: an unresolved conflict shows the base lines as its result. */
const columnsOf = (block: ConflictTextBlock, decision?: ConflictDecision | null): ColumnTexts =>
    block.kind === 'merged'
        ? { ours: block.ours ?? block.text, result: block.text, theirs: block.theirs ?? block.text }
        : { ours: block.ours, result: decision?.lines ?? block.base, theirs: block.theirs }

/** The parts of a file, each with its place among the conflicts and the numbers its lines start at. */
const partsOf = (blocks: ConflictTextBlock[], decisions: (ConflictDecision | null)[]): Part[] => {
    let conflicts = 0
    let next: FirstLines = { firstOurs: 1, firstResult: 1, firstTheirs: 1 }
    return blocks.map((block, place) => {
        const conflict = block.kind === 'conflict' ? conflicts++ : -1
        const columns = columnsOf(block, decisions[conflict])
        const first = next
        next = {
            firstOurs: first.firstOurs + lineCount(columns.ours),
            firstResult: first.firstResult + lineCount(columns.result),
            firstTheirs: first.firstTheirs + lineCount(columns.theirs),
        }
        return { block, place, conflict, ...first }
    })
}

interface TextMergeEditorProps {
    projectId: string
    /** The conflicted file, by its path. */
    filePath: string
    /** The file as it was merged here before: the editor starts from its parts and the decisions made for them. */
    merged?: MergedText | undefined
    /** What names your and their version beside the titles of their columns, such as their branches. */
    versionLabels?: Partial<Record<Side, React.ReactNode>> | undefined
    /** Takes the merged file once every conflict is resolved. */
    onApply: (merged: MergedText) => void
    onCancel: () => void
}

/**
 * Merges a conflicted text file in place, laid out as Git merge tools lay it out: your version on the left, the
 * result in the middle and their version on the right.
 *
 * The three columns show the file part by part, each part at the same height in every column, its lines numbered as
 * in the version they belong to. Lines no branch changed are shown alike in every column; lines one branch changed
 * are taken from it by the merge itself.
 *
 * A conflict shows the base lines in the result until it is resolved. The user takes your or their lines, takes the
 * other version after them, takes the base lines, or edits the result. The file is merged once every conflict is
 * resolved, with the line breaks it is written with.
 */
export const TextMergeEditor: React.FC<TextMergeEditorProps> = ({
    projectId,
    filePath,
    merged: earlier,
    versionLabels,
    onApply,
    onCancel,
}) => {
    const { t } = useTranslation()
    // A file merged here before is shown as it was merged: the versions of a conflict do not change.
    const [blocks, setBlocks] = useState<ConflictTextBlock[] | null>(earlier?.blocks ?? null)
    const [error, setError] = useState<string | null>(null)
    // How each conflict is resolved, in their order; null for one still to be resolved.
    const [decisions, setDecisions] = useState<(ConflictDecision | null)[]>(earlier?.decisions ?? [])

    useEffect(() => {
        if (earlier) {
            return undefined
        }
        let cancelled = false
        getConflictText(projectId, filePath)
            .then(answer => {
                if (!cancelled) {
                    setBlocks(answer.blocks)
                    setDecisions(answer.blocks.filter(block => block.kind === 'conflict').map(() => null))
                }
            })
            .catch((readError: unknown) => {
                if (!cancelled) {
                    setError(errorMessage(readError) || t('merge:editor.load_failed'))
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, filePath, earlier, t])

    const parts = useMemo(() => blocks && partsOf(blocks, decisions), [blocks, decisions])
    const lineBreak = useMemo(() => blocks ? lineBreakOf(blocks) : '\n', [blocks])
    const resolved = decisions.filter(Boolean).length

    // One function for every conflict, so a conflict redraws only when its own decision changes.
    const decide = useCallback((index: number, decision: ConflictDecision) => {
        setDecisions(current => current.map((old, at) => at === index ? decision : old))
    }, [])

    const apply = () => {
        if (blocks && resolved === decisions.length) {
            const taken = decisions as ConflictDecision[]
            onApply({ text: mergedTextOf(blocks, taken, lineBreak), decisions: taken, blocks })
        }
    }

    return (
        <Modal
            centered
            destroyOnHidden
            open
            onCancel={onCancel}
            // Three columns of code take the width of the window. The parts of the file scroll inside the dialog,
            // under the titles of the columns, which keeps its buttons within the window.
            styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
            width="96vw"
            footer={(
                <Flex align="center" gap="small" justify="space-between">
                    <Typography.Text data-testid="text-merge-progress" type="secondary">
                        {blocks && t('merge:editor.progress', { resolved, count: decisions.length })}
                    </Typography.Text>
                    <Space>
                        <Button onClick={onCancel}>{t('merge:buttons.cancel')}</Button>
                        <Button
                            data-testid="text-merge-apply"
                            disabled={!blocks || resolved < decisions.length}
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
                    {t('merge:editor.title', { name: basename(filePath) })}
                    <ColumnTitles versionLabels={versionLabels} />
                </Flex>
            )}
        >
            {error && <Alert showIcon data-testid="text-merge-error" title={error} type="error" />}
            {!error && !blocks && <Spin description={t('merge:editor.loading')} />}
            {parts && (
                <Suspense fallback={<Spin />}>
                    <Flex vertical data-testid="text-merge-blocks" gap="small">
                        {parts.map(({ block, place, conflict, ...first }) => {
                            if (block.kind === 'conflict') {
                                return (
                                    <ConflictPart
                                        key={place}
                                        block={block}
                                        count={decisions.length}
                                        decision={decisions[conflict] ?? null}
                                        index={conflict}
                                        lineBreak={lineBreak}
                                        onDecide={decide}
                                        path={filePath}
                                        {...first}
                                    />
                                )
                            }
                            return block.ours === undefined
                                ? <UnchangedPart key={place} path={filePath} text={block.text} {...first} />
                                : <ChangedPart key={place} block={block} path={filePath} {...first} />
                        })}
                    </Flex>
                </Suspense>
            )}
        </Modal>
    )
}

/** Lines of a version as a part of the file, numbered as in the whole version; a version with none says so. */
const Lines = React.memo(function Lines({ text, first, path }: { text: string; first: number; path: string }) {
    const { t } = useTranslation()
    return text
        ? <CodeEditor readOnly firstLine={first} path={path} value={withoutLastLineBreak(text)} />
        : <Typography.Text italic type="secondary">{t('merge:editor.no_lines')}</Typography.Text>
})

/** The lines of a part in every column, each numbered as in its version, from the line some lines into the part. */
const LinesRow: React.FC<FirstLines & { texts: ColumnTexts; path: string; skipped?: number }> = ({
    texts,
    path,
    skipped = 0,
    firstOurs,
    firstResult,
    firstTheirs,
}) => (
    <Columns
        ours={<Lines first={firstOurs + skipped} path={path} text={texts.ours} />}
        result={<Lines first={firstResult + skipped} path={path} text={texts.result} />}
        theirs={<Lines first={firstTheirs + skipped} path={path} text={texts.theirs} />}
    />
)

/** Lines no branch changed, alike in every column; a long run of them shows its ends and opens the rest on request. */
const UnchangedPart = React.memo(function UnchangedPart({
    text,
    path,
    ...first
}: FirstLines & { text: string; path: string }) {
    const { t } = useTranslation()
    const [open, setOpen] = useState(false)
    const lines = useMemo(() => text.split(/(?<=\n)/), [text])
    const row = (shown: string, skipped: number) => (
        <LinesRow path={path} skipped={skipped} texts={{ ours: shown, result: shown, theirs: shown }} {...first} />
    )
    if (open || lines.length <= SHOWN_LINES) {
        return row(text, 0)
    }
    const hidden = lines.length - 2 * END_LINES
    return (
        <Flex vertical gap="small">
            {row(lines.slice(0, END_LINES).join(''), 0)}
            <Flex justify="center">
                <Button onClick={() => setOpen(true)} size="small" type="link">
                    {t('merge:editor.unchanged_lines', { count: hidden })}
                </Button>
            </Flex>
            {row(lines.slice(-END_LINES).join(''), lines.length - END_LINES)}
        </Flex>
    )
})

/** Lines a branch changed, or both changed alike, which the merge took by itself, under where they come from. */
const ChangedPart = React.memo(function ChangedPart({
    block,
    path,
    ...first
}: FirstLines & { block: MergedTextBlock; path: string }) {
    const { t } = useTranslation()
    let source = 'merge:editor.changed_alike'
    if (block.ours === block.base) {
        source = 'merge:editor.taken_from_theirs'
    } else if (block.theirs === block.base) {
        source = 'merge:editor.taken_from_yours'
    }
    return (
        <Flex vertical gap={4}>
            <Columns result={<Typography.Text type="secondary">{t(source)}</Typography.Text>} />
            <LinesRow path={path} texts={columnsOf(block)} {...first} />
        </Flex>
    )
})

/** How a resolved conflict is told, by the choice that resolved it. */
const STATUSES: Record<ConflictChoice, string> = {
    'ours': 'merge:editor.status_yours',
    'theirs': 'merge:editor.status_theirs',
    'ours-theirs': 'merge:editor.status_yours_then_theirs',
    'theirs-ours': 'merge:editor.status_theirs_then_yours',
    'base': 'merge:editor.status_base',
    'edited': 'merge:editor.status_edited',
}

/** What a button that takes a version says: it takes the version, or adds it after the other version taken. */
const TAKE_LABELS: Record<Side, { take: string; add: string }> = {
    ours: { take: 'merge:editor.take_yours', add: 'merge:editor.add_yours' },
    theirs: { take: 'merge:editor.take_theirs', add: 'merge:editor.add_theirs' },
}

/** The button that takes the lines of a version into the result, or adds them after the other version taken. */
const TakeLinesButton: React.FC<{ side: Side; choice: ConflictChoice | undefined; onTake: (side: Side) => void }> = ({
    side,
    choice,
    onTake,
}) => {
    const { t } = useTranslation()
    const other: Side = side === 'ours' ? 'theirs' : 'ours'
    return (
        <TakeButton
            disabled={holdsSide(choice, side)}
            label={t(choice === other ? TAKE_LABELS[side].add : TAKE_LABELS[side].take)}
            onTake={() => onTake(side)}
            side={side}
            testId={`text-merge-take-${side}`}
        />
    )
}

interface ConflictPartProps extends FirstLines {
    block: ConflictedTextBlock
    path: string
    /** The place of the conflict among the conflicts of the file, from 0. */
    index: number
    count: number
    decision: ConflictDecision | null
    lineBreak: string
    onDecide: (index: number, decision: ConflictDecision) => void
}

/** A conflict of the file: your and their lines beside the result, and the buttons that resolve it. */
const ConflictPart = React.memo(function ConflictPart({
    block,
    path,
    index,
    count,
    decision,
    lineBreak,
    onDecide,
    firstOurs,
    firstResult,
    firstTheirs,
}: ConflictPartProps) {
    const { t } = useTranslation()
    const number = index + 1
    const choice = decision?.choice
    const columns = columnsOf(block, decision)

    const take = useCallback((side: Side) => {
        const taken = takeSide(choice, side)
        onDecide(index, { choice: taken, lines: linesOf(block, taken, lineBreak) })
    }, [block, choice, index, lineBreak, onDecide])

    // One function for every keystroke, so the editor of the result is not set up anew while the user types.
    const write = useCallback((value: string) => onDecide(index, {
        choice: 'edited',
        lines: writtenLines(value, block, lineBreak),
    }), [block, index, lineBreak, onDecide])

    return (
        <Flex vertical data-testid={`text-merge-conflict-${number}`} gap="small">
            <ConflictHeading
                status={choice && t(STATUSES[choice])}
                title={t('merge:editor.conflict', { number, count })}
            />
            <ButtonColumns
                ours={<TakeLinesButton choice={choice} onTake={take} side="ours" />}
                theirs={<TakeLinesButton choice={choice} onTake={take} side="theirs" />}
                result={(
                    <TakeBaseButton
                        disabled={choice === 'base'}
                        hint={t('merge:editor.base_hint')}
                        onTake={() => onDecide(index, { choice: 'base', lines: block.base })}
                        testId="text-merge-take-base"
                    />
                )}
            />
            <Columns
                ours={<Lines first={firstOurs} path={path} text={columns.ours} />}
                theirs={<Lines first={firstTheirs} path={path} text={columns.theirs} />}
                result={(
                    <div data-testid={`text-merge-result-${number}`}>
                        <CodeEditor
                            firstLine={firstResult}
                            onChange={write}
                            path={path}
                            placeholder={t('merge:editor.no_lines')}
                            value={shownLines(columns.result, block)}
                        />
                    </div>
                )}
            />
            {/* The line under the conflict tells its lines from the lines that follow it. */}
            <Divider size="small" />
        </Flex>
    )
})

export default TextMergeEditor

import React, { memo, useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Skeleton } from 'antd'
import { CloseOutlined, FileExcelOutlined, RightOutlined, SearchOutlined } from '@ant-design/icons'
import type { ModuleTable, RawTableCell, RawTableView } from 'types/tables'
import { RawTableGrid } from '../../components/RawTableGrid'
import { onPaper, rolesOf, type TableRoles } from '../../components/tableRoles'
import { type CellSearchScope, searchTables } from '../../services/modules'
import { readTableWindow } from '../../services/keptReads'
import { isApiHttpError } from '../../services/apiCall'
import { useExcelFormatting } from '../../hooks/useExcelFormatting'
import { errorMessage } from '../../utils/errorMessage'
import { versionLabel } from '../../utils/tableVersion'
import { useDateFormats } from '../../utils/dateFormat'
import { matchedParts, termsPattern } from '../../utils/matchedParts'
import { useActionBandStyles } from './actionBand.styles'
import { HiddenByFilterNote } from './HiddenByFilterNote'
import { OTHER_KIND } from './tableGrouping'
import { tableIcon } from './tableIcons'
import { useCellStyles, useStyles } from './ModuleSearchResults.styles'

/** The status Jetty answers an address too long for it with. */
const URI_TOO_LONG = 414

/** What a search answered: the tables found, or why it found none. */
type Answer = { tables: ModuleTable[] } | { failure: string }

/**
 * The whole line a table is written with: its type, what it answers with, and what it is called.
 *
 * <p>A table is told from the next by its header, and the header starts with the type — {@code Rules Double
 * AccidentPremium()}, {@code Datatype Driver} — so the type is read where it is written. A table that carries no
 * signature of its own, Environment among them, is named after its type once. A version of a table is told from the
 * others by its dimensions after the header, as every label of a version names them.
 */
const writtenLine = (table: ModuleTable, dateFormats: ReturnType<typeof useDateFormats>): string => {
    const header = table.signature ? [table.returnType, table.signature].filter(Boolean).join(' ') : table.name
    const line = header === table.kind ? header : `${table.kind} ${header}`
    return versionLabel(line, table.dimensions, dateFormats)
}

/**
 * The text with every place it holds what was looked for marked, whatever the case of the letters. A mark is keyed by
 * where it starts in the text, which no other mark of the text shares.
 */
const marked = (line: string, pattern: RegExp | undefined, className: string): React.ReactNode => {
    let at = 0
    return matchedParts(line, pattern).map(part => {
        const from = at
        at += part.text.length
        return part.match ? <mark key={from} className={className}>{part.text}</mark> : part.text
    })
}

/** Whether a cell holds what was looked for, whatever the case of its letters, as the server matched it. */
const holds = (cell: RawTableCell, wanted: string): boolean =>
    cell.value !== undefined && cell.value !== null && String(cell.value).toLowerCase().includes(wanted)

/** The project and the module the search was made through, which a table found names no other of. */
interface Place {
    project: string
    module: string
}

/**
 * The group a table found is listed under: its module in a search of the project, its project and module in a search
 * reaching the projects it depends on.
 */
const groupOf = (table: ModuleTable, scope: CellSearchScope, place: Place): string =>
    (scope === 'project'
        ? table.module ?? place.module
        : `${table.project ?? place.project} / ${table.module ?? place.module}`)

/** Where a table sits: its sheet and range, as Excel names them. */
const positionOf = (table: ModuleTable): string | undefined =>
    (table.sheet === undefined || table.pos === undefined ? table.pos : `${table.sheet}!${table.pos}`)

/** Where the body of a table opened under its line has got to. */
type Body =
    | { state: 'reading' }
    | { state: 'read', table: RawTableView, roles: TableRoles | undefined }
    | { state: 'failed', message: string }

interface PreviewProps {
    body: Body
    /** What was looked for, in small letters, which the cells holding it are marked by. */
    wanted: string
    /** The styles of the results, read once by them: a preview reads none of its own. */
    styles: ReturnType<typeof useStyles>['styles']
    /** The class a cell holding what was looked for is marked with, on the paper the table is drawn on. */
    foundCell: string
    testId: string
}

/** A table opened under its line, a window of its rows deep, the cells holding what was looked for marked. */
const Preview = ({ body, wanted, styles, foundCell, testId }: PreviewProps) => {
    const { t } = useTranslation('repository')
    const roles = body.state === 'read' ? body.roles : undefined
    const decorate = useCallback(
        (cell: RawTableCell) => (holds(cell, wanted) ? { className: foundCell, painted: true } : undefined),
        [foundCell, wanted]
    )
    if (body.state === 'reading') {
        return <Skeleton active className={styles.preview} paragraph={{ rows: 3 }} title={false} />
    }
    if (body.state === 'failed') {
        return <Alert showIcon className={styles.preview} description={body.message} type="error" />
    }
    const { table } = body
    const total = table.totalRows ?? table.source.length
    return (
        <div className={styles.preview}>
            <RawTableGrid decorate={decorate} roles={roles} rows={table.source} testId={testId} />
            {table.source.length < total && (
                <span className={styles.part}>
                    {t('browser.module.found_part', { shown: table.source.length, total })}
                </span>
            )}
        </div>
    )
}

interface ModuleSearchResultsProps {
    projectId: string
    /** The name of the project, which a search of it is said to look through. */
    projectName: string
    /** The module the search is made through: its tables are answered first. */
    moduleName: string
    /** How far the search reaches: the whole project, or the projects it depends on too. */
    scope: CellSearchScope
    /** What is looked for in the cells, as the reader sent it with Enter. */
    text: string
    /** Whether the type filter of the tree lists the free-form tables, the kind Other. */
    showOther: boolean
    /** Asks for the free-form tables to be listed, as the type filter of the tree does. */
    onShowOther: (shown: boolean) => void
    /** Opens a table found in the editor, in place of the results. */
    onOpen: (table: ModuleTable) => void
    /** Puts the results away, back to the table they took the place of. */
    onClose: () => void
}

/**
 * What a search of every cell of the project found, in place of the table, where the reader reads tables.
 *
 * <p>The search looks for the text in every cell of every table of the project, the header included, whatever the
 * case of its letters, as the server matches it; of the projects it depends on too, as the scope says. In a search of
 * the project, the band says that Enter pressed again reaches the projects it depends on. The tables found are listed
 * in the order the server answers them — the module first, then the rest of the project, then the rest — by their
 * module, or by their project and module in a search reaching the projects it depends on. A line opens its table in
 * the editor, where it is written; the arrow before it opens the table under it, a window of its rows deep, the cells
 * holding the text marked. A table is read only once it is asked for: a search can find hundreds.
 *
 * <p>The search follows the type filter of the tree: while the filter hides the free-form tables, they are left out
 * of the results too, counted, and shown on a click.
 */
export const ModuleSearchResults = memo(({
    projectId,
    projectName,
    moduleName,
    scope,
    text,
    showOther,
    onShowOther,
    onOpen,
    onClose,
}: ModuleSearchResultsProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { styles: band } = useActionBandStyles()
    // The mark of a cell lies on the paper its table is drawn on: the theme's, or the workbook's.
    const { styles: onTheme } = useCellStyles(false)
    const { styles: onWorkbook } = useCellStyles(true)
    const excelFormatting = useExcelFormatting()
    const dateFormats = useDateFormats()
    const [answer, setAnswer] = useState<Answer | null>(null)
    const [bodies, setBodies] = useState<Record<string, Body>>({})

    // A search of the same module for another text, or of another module, starts afresh: what the last one found and
    // opened belongs to it.
    useEffect(() => {
        let current = true
        setAnswer(null)
        setBodies({})
        searchTables(projectId, { module: moduleName, scope, text, includeOther: true, sort: 'position' })
            .then(tables => current && setAnswer({ tables }))
            .catch((error: unknown) => current && setAnswer({
                // An address too long for the server is refused with no reason of its own: say what to change.
                failure: isApiHttpError(error) && error.status === URI_TOO_LONG
                    ? t('browser.module.found_too_long')
                    : errorMessage(error),
            }))
        return () => {
            current = false
        }
    }, [projectId, moduleName, scope, text, t])

    const found = answer !== null && 'tables' in answer ? answer.tables : null
    const listed = useMemo(() => {
        const all = found ?? []
        return showOther ? all : all.filter(table => table.kind !== OTHER_KIND)
    }, [found, showOther])
    const hidden = (found?.length ?? 0) - listed.length
    // Grouped in the order the server answers them: a group stands where its first table does.
    const groups = useMemo(() => {
        const place = { project: projectName, module: moduleName }
        return [...Map.groupBy(listed, table => groupOf(table, scope, place))]
    }, [listed, moduleName, projectName, scope])
    const wanted = text.toLowerCase()
    const pattern = useMemo(() => termsPattern([text]), [text])

    /** Where a table found is written: the project and the module its body is read through. */
    const at = (table: ModuleTable) => ({ project: table.projectId ?? projectId, module: table.module ?? moduleName })
    const keyOf = (table: ModuleTable) => {
        const { project, module } = at(table)
        return `${project}/${module}/${table.id}`
    }

    /** Opens the body of a table under its line, or folds it away when it is open already. */
    const toggle = (table: ModuleTable) => {
        const key = keyOf(table)
        if (bodies[key] !== undefined) {
            setBodies(({ [key]: _folded, ...rest }) => rest)
            return
        }
        setBodies(open => ({ ...open, [key]: { state: 'reading' } }))
        // Read as the editor reads it, so opening the table right after shows it at once.
        const { project, module } = at(table)
        readTableWindow(project, table.id, module)
            .then(read => setBodies(open => (open[key] === undefined ? open : {
                ...open,
                [key]: { state: 'read', table: read, roles: rolesOf(read) },
            })))
            .catch((error: unknown) => setBodies(open => (open[key] === undefined ? open : {
                ...open,
                [key]: { state: 'failed', message: errorMessage(error) },
            })))
    }

    const line = (table: ModuleTable) => {
        const body = bodies[keyOf(table)]
        const position = positionOf(table)
        // The mark of a cell lies on the paper the table is drawn on.
        const paper = onPaper(body?.state === 'read' ? body.roles : undefined, excelFormatting)
        const foundCell = (paper ? onWorkbook : onTheme).foundCell
        return (
            <li key={keyOf(table)} data-testid={`module-found-${table.id}`}>
                <div className={styles.line}>
                    <Button
                        aria-expanded={body !== undefined}
                        aria-label={t(body === undefined ? 'browser.module.found_show' : 'browser.module.found_hide')}
                        className={styles.twisty}
                        data-testid={`module-found-toggle-${table.id}`}
                        icon={<RightOutlined />}
                        onClick={() => toggle(table)}
                        size="small"
                        type="text"
                    />
                    <Button
                        className={styles.open}
                        data-testid={`module-found-open-${table.id}`}
                        onClick={() => onOpen(table)}
                        type="text"
                    >
                        {tableIcon(table.kind)}
                        <span className={styles.signature}>
                            {marked(writtenLine(table, dateFormats), pattern, styles.found)}
                        </span>
                        {position && <span className={styles.position}>{position}</span>}
                    </Button>
                </div>
                {body !== undefined && (
                    <Preview
                        body={body}
                        foundCell={foundCell}
                        styles={styles}
                        testId={`module-found-grid-${table.id}`}
                        wanted={wanted}
                    />
                )}
            </li>
        )
    }

    const results = () => {
        if (answer === null) {
            return (
                <>
                    {/* The search waits for the project, and the projects it depends on, to be compiled. */}
                    <span className={styles.part}>{t('browser.module.found_waiting')}</span>
                    <Skeleton active className={styles.preview} paragraph={{ rows: 6 }} title={false} />
                </>
            )
        }
        if ('failure' in answer) {
            return (
                <Alert
                    showIcon
                    className={styles.preview}
                    data-testid="module-found-failed"
                    description={answer.failure}
                    title={t('browser.module.found_failed')}
                    type="error"
                />
            )
        }
        return (
            <>
                {hidden > 0 && (
                    <HiddenByFilterNote
                        className={styles.sheet}
                        count={hidden}
                        onShow={() => onShowOther(true)}
                        testId="module-found-hidden"
                    />
                )}
                {listed.length === 0
                    ? <Empty data-testid="module-found-none" description={t('browser.module.found_none')} />
                    : groups.map(([group, tables]) => (
                        <section key={group} aria-label={group || undefined}>
                            {group && (
                                <div className={styles.sheet}>
                                    <FileExcelOutlined />
                                    {group}
                                    <span className={styles.count}>{tables.length}</span>
                                </div>
                            )}
                            <ul className={styles.list}>{tables.map(line)}</ul>
                        </section>
                    ))}
            </>
        )
    }

    return (
        <>
            <div className={band.bar} data-testid="module-found">
                <span className={styles.summary}>
                    <SearchOutlined />
                    <span className={styles.text} data-testid="module-found-summary">
                        {found === null
                            ? t('browser.module.found_searching', { text })
                            : t('browser.module.found', { count: listed.length, text })}
                    </span>
                    <span className={styles.scope}>
                        {t(`browser.module.found_scope_${scope}`, { project: projectName })}
                    </span>
                    {/* Enter pressed again on the text the project was searched for reaches the projects it depends on. */}
                    {scope === 'project' && (
                        <span className={styles.hint} data-testid="module-found-wider">
                            <kbd className={styles.key}>↵</kbd>
                            {t('browser.module.found_wider_all')}
                        </span>
                    )}
                </span>
                <Button data-testid="module-found-close" icon={<CloseOutlined />} onClick={onClose} type="text">
                    {t('browser.module.found_close')}
                </Button>
            </div>
            <div className={styles.canvas}>{results()}</div>
        </>
    )
})

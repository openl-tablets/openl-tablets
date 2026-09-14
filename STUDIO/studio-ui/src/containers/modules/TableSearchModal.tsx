import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Input, Modal, Select, Space, Spin } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable, ProjectProperty, RawTableView } from 'types/tables'
import { RawTableGrid } from '../../components/RawTableGrid'
import {
    getRawTable,
    searchTables,
    TABLE_PAGE_ROWS,
    type TableSearchCriteria,
    type TableSearchScope,
} from '../../services/modules'
import { getProjectProperties } from '../../services/projects'
import { errorMessage } from '../../utils/errorMessage'
import { initialPropertyValue, PropertyValueInput } from '../tableModals/PropertyValueInput'
import { toPropertyGroups } from '../tableModals/shared'
import { tableIcon } from './tableIcons'

/** The families of table the search can narrow to, as the Tables API names them. */
const KINDS = [
    'Rules', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'TBasic', 'Column Match', 'Method', 'Run',
    'Constants', 'Conditions', 'Actions', 'Returns', 'Environment', 'Properties', 'Other',
]

/**
 * One property the search narrows by: the property picked, and the value a table must carry for it.
 *
 * <p>The value is whatever the property's own editor produces — a date, a flag, one or several values of an
 * enumeration — and crosses to the server as the text that property is written with.
 */
interface PropertyFilter {
    /** Identifies the row while it is being filled in, since a property is only picked later. */
    id: number
    name: string
    value: string | number | boolean | null
}

/** The next row's id: a row keyed by its place in the list would carry its neighbour's state when one goes. */
let nextFilterId = 0

/**
 * Where the body of one result has got to: on its way, read, or refused.
 *
 * <p>Each says what it is rather than being told apart by its shape — a failure carries a message, and a
 * message is a string like any other.
 */
type ResultBody =
    | { state: 'reading' }
    | { state: 'read', table: RawTableView }
    | { state: 'failed', message: string }

const useStyles = createStyles(({ css, token }) => ({
    form: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: ${token.margin}px;
    `,
    field: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXXS}px;
    `,
    label: css`
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextSecondary};
    `,
    /** The properties take the width of the form, since each is a row of its own. */
    wide: css`
        grid-column: 1 / -1;
    `,
    propertyRow: css`
        display: flex;
        gap: ${token.marginXS}px;
        margin-bottom: ${token.marginXXS}px;
    `,
    results: css`
        margin-top: ${token.margin}px;
    `,
    /** A result reads as a row of the tree does: the picture of its family, then its name. */
    name: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
    `,
    /** What was found reads as a list: one entry under the next, each opening as far as it is asked to. */
    found: css`
        display: flex;
        flex-direction: column;
        gap: ${token.margin}px;
        max-height: 50vh;
        overflow: auto;
    `,
    entry: css`
        display: flex;
        flex-direction: column;
        gap: ${token.marginXXS}px;
        padding-bottom: ${token.marginXS}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /** The signature is the table's own line, read as the editor writes it. */
    signature: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        font-family: ${token.fontFamilyCode};
        font-size: ${token.fontSizeSM}px;
        word-break: break-word;
    `,
    /** Where the table is written, said quietly under its signature. */
    where: css`
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
    `,
    /** A body is as wide as its values need; the entry scrolls it rather than squeezing it. */
    body: css`
        overflow: auto;
    `,
}))

interface TableSearchModalProps {
    open: boolean
    projectId: string
    /** The module the editor has open: what the search is made through, and what it covers by default. */
    moduleName: string
    /** What the reader had typed in the rail, which the search starts from. */
    initialName?: string
    onClose: () => void
    /** Opens one of the results, wherever it is written. */
    onOpen: (table: ModuleTable) => void
}

type TableSearchFormProps = Omit<TableSearchModalProps, 'open' | 'onClose'>

/**
 * The extended table search, as the Editor's own search offered it: how wide to look, which families of table,
 * what the header says, what is written in the cells, and which properties a table must carry.
 *
 * <p>The rail's own box searches the names of the tables already on screen. This asks the server, so it reaches
 * the modules the tree does not show — the rest of the project, and the projects it depends on — and the results
 * say where each table lives, so one can be opened where it is written.
 *
 * <p>The window holds the search rather than being it: closed, the window destroys what is inside, so the
 * search opens as it starts — whatever was asked of it before, and whichever module the reader is in by then.
 */
export const TableSearchModal = ({ open, onClose, ...asked }: TableSearchModalProps) => {
    const { t } = useTranslation('repository')
    // Each opening is a search of its own, and is built as one: what the last was asked, and what it found,
    // belong to it rather than to this one.
    const [opening, setOpening] = useState(0)
    useEffect(() => {
        if (open) {
            setOpening(count => count + 1)
        }
    }, [open])
    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={onClose}
            open={open}
            title={t('browser.module.search_extended')}
            width={900}
        >
            <TableSearchForm key={opening} {...asked} />
        </Modal>
    )
}

/** What is asked, and what it found: the form the window opens on, fresh each time it is opened. */
const TableSearchForm = ({
    projectId,
    moduleName,
    initialName = '',
    onOpen,
}: TableSearchFormProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [scope, setScope] = useState<TableSearchScope>('module')
    const [name, setName] = useState(initialName)
    const [header, setHeader] = useState('')
    const [text, setText] = useState('')
    const [kinds, setKinds] = useState<string[]>([])
    const [filters, setFilters] = useState<PropertyFilter[]>([])
    const [properties, setProperties] = useState<ProjectProperty[]>([])
    const [results, setResults] = useState<ModuleTable[] | null>(null)
    const [searching, setSearching] = useState(false)
    const [failure, setFailure] = useState<string | null>(null)
    /** The body of a result, once the reader asked for it: what was read, or why it could not be. */
    const [bodies, setBodies] = useState<Record<string, ResultBody>>({})

    // The names a property can be narrowed by are the ones the engine knows, so the list is read rather than
    // written here: a property added to the dictionary appears without a change to this screen.
    useEffect(() => {
        getProjectProperties(projectId).then(setProperties).catch(() => setProperties([]))
    }, [projectId])

    // Grouped the way the copy dialog groups them — Info, Business Dimension, Version, Dev — so a property is
    // looked for where the Table Details panel lists it.
    const propertyOptions = useMemo(() => toPropertyGroups(properties), [properties])
    const definitionOf = (name: string) => properties.find(property => property.name === name.trim())

    const search = useCallback(() => {
        setSearching(true)
        setFailure(null)
        const criteria: TableSearchCriteria = {
            module: moduleName,
            scope,
            name: name.trim() || undefined,
            header: header.trim() || undefined,
            text: text.trim() || undefined,
            kinds,
            properties: Object.fromEntries(filters
                .filter(filter => filter.name && String(filter.value ?? '').trim())
                .map(filter => [filter.name, String(filter.value).trim()])),
        }
        searchTables(projectId, criteria)
            .then(found => setResults(found))
            .catch((error: unknown) => {
                setResults([])
                setFailure(errorMessage(error))
            })
            .finally(() => setSearching(false))
    }, [projectId, moduleName, scope, name, header, text, kinds, filters])

    /** Where one result is written, which is the project and module its body is read through. */
    const at = (table: ModuleTable) => ({
        projectId: table.projectId ?? projectId,
        module: table.module ?? moduleName,
    })

    const keyOf = (table: ModuleTable) => `${at(table).projectId}/${at(table).module}/${table.id}`

    /**
     * Reads the body of one result, and folds it away when it is already on screen.
     *
     * <p>A search can answer with hundreds of tables, and drawing every body would read every one of them from
     * the workbook; the reader asks for the ones they want to see. A tall table is read a window deep, as the
     * editor reads it.
     */
    const toggleBody = (table: ModuleTable) => {
        const key = keyOf(table)
        const shown = bodies[key]
        if (shown !== undefined) {
            setBodies(({ [key]: _dropped, ...rest }) => rest)
            return
        }
        setBodies(previous => ({ ...previous, [key]: { state: 'reading' } }))
        const { projectId: where, module } = at(table)
        getRawTable(where, table.id, { module, maxRows: TABLE_PAGE_ROWS })
            .then(read => setBodies(previous => ({ ...previous, [key]: { state: 'read', table: read } })))
            .catch((error: unknown) => setBodies(previous => ({
                ...previous,
                [key]: { state: 'failed', message: errorMessage(error) },
            })))
    }

    /**
     * The whole line a table is written with: its type, what it answers with, and what it is called.
     *
     * <p>A table is told from the next by its header, and the header starts with the type — {@code Rules Double
     * AccidentPremium()}, {@code Datatype Driver}, {@code Test DetermineDriverPremium DriverPremiumTest} — so
     * the type is read where it is written rather than said again under the table.
     *
     * <p>A table that carries no signature of its own is named after its type, Environment among them, and
     * that name is written once.
     */
    const written = (table: ModuleTable) => {
        const line = table.signature
            ? [table.returnType, table.signature].filter(Boolean).join(' ')
            : table.displayName ?? table.name
        return line === table.kind ? line : `${table.kind} ${line}`
    }

    /** One result: what can be done with it, the header it reads by, and where it is written. */
    const entry = (table: ModuleTable) => {
        const key = keyOf(table)
        const body = bodies[key]
        const read = body?.state === 'read' ? body.table : null
        const rows = read?.source ?? null
        const total = read ? read.totalRows ?? read.source.length : 0
        return (
            <div key={key} className={styles.entry} data-testid={`table-search-result-${table.id}`}>
                <Space size="small">
                    <Button
                        data-testid={`table-search-open-${table.id}`}
                        onClick={() => onOpen(table)}
                        size="small"
                        type="link"
                    >
                        {t('browser.module.search_view_table')}
                    </Button>
                    <Button
                        data-testid={`table-search-body-${table.id}`}
                        loading={body?.state === 'reading'}
                        onClick={() => toggleBody(table)}
                        size="small"
                        type="link"
                    >
                        {t(body === undefined || body.state === 'reading'
                            ? 'browser.module.search_show_body'
                            : 'browser.module.search_hide_body')}
                    </Button>
                </Space>
                <span className={styles.signature}>
                    {tableIcon(table.kind)}
                    {written(table)}
                </span>
                <span className={styles.where}>
                    {[table.project, table.module ?? moduleName].filter(Boolean).join(' · ')}
                </span>
                {body?.state === 'failed' && (
                    <Alert showIcon description={body.message} type="error" />
                )}
                {rows && (
                    <div className={styles.body}>
                        <RawTableGrid rows={rows} testId={`table-search-grid-${table.id}`} />
                        {rows.length < total && (
                            <span className={styles.where}>
                                {t('browser.module.search_body_part', { shown: rows.length, total })}
                            </span>
                        )}
                    </div>
                )}
            </div>
        )
    }

    return (
        <>
            <div className={styles.form} data-testid="table-search-form">
                <label className={styles.field}>
                    <span className={styles.label}>{t('browser.module.search_scope')}</span>
                    <Select
                        data-testid="table-search-scope"
                        onChange={setScope}
                        value={scope}
                        options={[
                            { value: 'module', label: t('browser.module.search_scope_module') },
                            { value: 'project', label: t('browser.module.search_scope_project') },
                            { value: 'all', label: t('browser.module.search_scope_all') },
                        ]}
                    />
                </label>
                <label className={styles.field}>
                    <span className={styles.label}>{t('browser.module.search_kind')}</span>
                    <Select
                        allowClear
                        data-testid="table-search-kind"
                        mode="multiple"
                        onChange={setKinds}
                        options={KINDS.map(kind => ({ value: kind, label: kind }))}
                        placeholder={t('browser.module.search_kind_any')}
                        value={kinds}
                    />
                </label>
                <label className={styles.field}>
                    <span className={styles.label}>{t('browser.module.search_name')}</span>
                    <Input data-testid="table-search-name" onChange={e => setName(e.target.value)} value={name} />
                </label>
                <label className={styles.field}>
                    <span className={styles.label}>{t('browser.module.search_header')}</span>
                    <Input data-testid="table-search-header" onChange={e => setHeader(e.target.value)} value={header} />
                </label>
                <label className={cx(styles.field, styles.wide)}>
                    <span className={styles.label}>{t('browser.module.search_text')}</span>
                    <Input data-testid="table-search-text" onChange={e => setText(e.target.value)} value={text} />
                </label>
                <div className={cx(styles.field, styles.wide)}>
                    <span className={styles.label}>{t('browser.module.search_properties')}</span>
                    {filters.map((filter, index) => (
                        <div key={filter.id} className={styles.propertyRow}>
                            <Select
                                allowClear
                                data-testid={`table-search-property-${index}`}
                                options={propertyOptions}
                                placeholder={t('browser.module.search_property_name')}
                                showSearch={{ optionFilterProp: 'label' }}
                                style={{ width: 260 }}
                                value={filter.name === '' ? null : filter.name}
                                onChange={(picked: string) => setFilters(rows => rows.map((row, at) => at === index
                                    // The value starts from what the property stands for, so a flag or an
                                    // enumeration opens on something a table can actually carry.
                                    ? { ...row, name: picked ?? '', value: initialPropertyValue(definitionOf(picked ?? '')) }
                                    : row))}
                            />
                            <PropertyValueInput
                                data-testid={`table-search-property-value-${index}`}
                                definition={definitionOf(filter.name)}
                                placeholder={t('browser.module.search_property_value')}
                                value={filter.value}
                                onChange={value => setFilters(rows => rows.map((row, at) =>
                                    at === index ? { ...row, value } : row))}
                            />
                            <Button
                                aria-label={t('browser.module.search_property_remove')}
                                icon={<DeleteOutlined />}
                                onClick={() => setFilters(rows => rows.filter((_, at) => at !== index))}
                                type="text"
                            />
                        </div>
                    ))}
                    <Button
                        data-testid="table-search-property-add"
                        icon={<PlusOutlined />}
                        onClick={() => setFilters(rows => [...rows, { id: nextFilterId++, name: '', value: '' }])}
                        size="small"
                    >
                        {t('browser.module.search_property_add')}
                    </Button>
                </div>
            </div>
            <Space className={styles.results}>
                <Button data-testid="table-search-run" loading={searching} onClick={search} type="primary">
                    {t('browser.module.search')}
                </Button>
                {scope !== 'module' && (
                    <span>{t('browser.module.search_wait')}</span>
                )}
            </Space>
            {failure !== null && (
                <Alert showIcon className={styles.results} description={failure} type="error" />
            )}
            {searching && results === null && <Spin className={styles.results} />}
            {results !== null && (
                <div className={styles.results} data-testid="table-search-results">
                    {results.length === 0 ? (
                        <Empty description={t('browser.module.search_no_match')} />
                    ) : (
                        <div className={styles.found}>{results.map(entry)}</div>
                    )}
                </div>
            )}
        </>
    )
}

export default TableSearchModal

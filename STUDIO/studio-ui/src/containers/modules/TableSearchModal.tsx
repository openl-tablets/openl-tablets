import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Input, Modal, Select, Space, Spin, Table } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable, ProjectProperty } from 'types/tables'
import { searchTables, type TableSearchCriteria, type TableSearchScope } from '../../services/modules'
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
    name: string
    value: string | number | boolean | null
}

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

/**
 * The extended table search, as the Editor's own search offered it: how wide to look, which families of table,
 * what the header says, what is written in the cells, and which properties a table must carry.
 *
 * <p>The rail's own box searches the names of the tables already on screen. This asks the server, so it reaches
 * the modules the tree does not show — the rest of the project, and the projects it depends on — and the results
 * say where each table lives, so one can be opened where it is written.
 */
export const TableSearchModal = ({
    open,
    projectId,
    moduleName,
    initialName = '',
    onClose,
    onOpen,
}: TableSearchModalProps) => {
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

    // The names a property can be narrowed by are the ones the engine knows, so the list is read rather than
    // written here — a property added to the dictionary appears without a change to this screen.
    useEffect(() => {
        if (!open) {
            return
        }
        setName(initialName)
        getProjectProperties(projectId).then(setProperties).catch(() => setProperties([]))
    }, [open, projectId, initialName])

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

    const columns = [
        {
            title: t('browser.module.search_result_name'),
            dataIndex: 'name',
            render: (_: unknown, table: ModuleTable) => (
                <Button className={styles.name} onClick={() => onOpen(table)} type="link">
                    {tableIcon(table.kind)}
                    {table.displayName ?? table.name}
                </Button>
            ),
        },
        { title: t('browser.module.search_result_kind'), dataIndex: 'kind', width: 140 },
        {
            title: t('browser.module.search_result_where'),
            width: 260,
            render: (_: unknown, table: ModuleTable) => [table.project, table.module ?? moduleName]
                .filter(Boolean)
                .join(' · '),
        },
    ]

    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={onClose}
            open={open}
            title={t('browser.module.search_extended')}
            width={900}
        >
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
                        <div key={index} className={styles.propertyRow}>
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
                                    ? { name: picked ?? '', value: initialPropertyValue(definitionOf(picked ?? '')) }
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
                        onClick={() => setFilters(rows => [...rows, { name: '', value: '' }])}
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
                        <Table
                            columns={columns as never}
                            dataSource={results}
                            pagination={{ pageSize: 10, hideOnSinglePage: true }}
                            rowKey={table => `${table.module ?? moduleName}/${table.id}`}
                            size="small"
                        />
                    )}
                </div>
            )}
        </Modal>
    )
}

export default TableSearchModal

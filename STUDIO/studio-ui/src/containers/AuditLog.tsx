import React, { useContext, useEffect, useMemo, useState } from 'react'
import {
    App,
    Button,
    DatePicker,
    Descriptions,
    Flex,
    Input,
    Table,
    type TableColumnsType,
    Tag,
    theme,
    Tooltip,
    Typography,
} from 'antd'
import type { FilterDropdownProps, FilterValue } from 'antd/es/table/interface'
import { ClearOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import dayjs, { type Dayjs } from 'dayjs'
import { SystemContext } from '../contexts'
import {
    AUDIT_ACTIONS,
    AUDIT_AUTHENTICATIONS,
    AUDIT_OUTCOMES,
    type AuditAction,
    type AuditAuthentication,
    type AuditEvent,
    type AuditOutcome,
    type AuditQuery,
    exportAuditEvents,
    getAuditEvents,
} from '../services/audit'
import { formatDateTime } from '../utils/dateFormat'
import { saveFile } from '../utils/download'
import { tablePagination } from '../utils/tablePagination'

const PAGE_SIZE = 50

const OUTCOME_COLORS: Record<AuditOutcome, string> = {
    success: 'success',
    failure: 'error',
    denied: 'warning',
}

/** The filters of the columns, by the key of the column, as the table hands them over. */
type ColumnFilters = Record<string, FilterValue | null>

/** A period the events are filtered by: its start and its end, either one left open. */
type Period = [Dayjs | null, Dayjs | null] | null

/** A value of a filter as text, or nothing for an empty one. */
const textOf = (value?: React.Key | boolean) => (value ? String(value) : undefined)

/** Whether a value of the filter of the actions is an action rather than the category that holds it. */
const isAction = (value: React.Key | boolean): value is AuditAction => !Object.hasOwn(AUDIT_ACTIONS, String(value))

/** A moment of a period at the start of its second, moved by the given number of seconds, as text. */
const secondOf = (value: React.Key | boolean | undefined, seconds: number) =>
    value ? dayjs(String(value)).startOf('second').add(seconds, 'second').toISOString() : undefined

/**
 * The criteria the filters of the columns set. The period is held as its start and its end, either one empty.
 *
 * The period takes in the whole seconds its start and its end name, as the list shows the times to the second. The
 * criteria end at the start of the next second, since their end is not part of the period.
 */
const toQuery = ({
    time,
    actor,
    authentication,
    action,
    outcome,
    target,
    subject,
    clientAddress,
}: ColumnFilters): AuditQuery => ({
    from: secondOf(time?.[0], 0),
    to: secondOf(time?.[1], 1),
    actor: textOf(actor?.[0]),
    authentication: AUDIT_AUTHENTICATIONS.find(code => code === authentication?.[0]),
    actions: action?.filter(isAction),
    outcome: AUDIT_OUTCOMES.find(code => code === outcome?.[0]),
    target: textOf(target?.[0]),
    subject: textOf(subject?.[0]),
    clientAddress: textOf(clientAddress?.[0]),
})

/** The panel of a filter of the table, padded as the panels of Ant Design are. */
const FilterPanel = ({ children }: React.PropsWithChildren) => {
    const { token } = theme.useToken()
    return <div style={{ padding: token.paddingXS }}>{children}</div>
}

/** A filter by the text a column contains: a search applies the text, and clearing the text removes the filter. */
const TextFilter = ({ selectedKeys, setSelectedKeys, confirm }: FilterDropdownProps) => {
    const keysOf = (text: string) => (text.trim() ? [text] : [])
    return (
        <FilterPanel>
            <Input.Search
                allowClear
                onChange={event => setSelectedKeys(keysOf(event.target.value))}
                value={textOf(selectedKeys[0]) ?? ''}
                onSearch={text => {
                    setSelectedKeys(keysOf(text))
                    confirm()
                }}
            />
        </FilterPanel>
    )
}

/** A filter by the period of the events: picking a period applies it, and clearing the period removes the filter. */
const PeriodFilter = ({ selectedKeys, setSelectedKeys, confirm }: FilterDropdownProps) => {
    const dateOf = (key?: React.Key) => (key ? dayjs(String(key)) : null)
    const keyOf = (date: Dayjs | null) => date?.toISOString() ?? ''
    const select = (period: Period) => setSelectedKeys(period?.some(Boolean) ? period.map(keyOf) : [])
    return (
        <FilterPanel>
            <DatePicker.RangePicker
                showTime
                allowEmpty={[true, true]}
                onCalendarChange={select}
                onOpenChange={open => !open && confirm()}
                value={selectedKeys.length ? [dateOf(selectedKeys[0]), dateOf(selectedKeys[1])] : null}
                onChange={period => {
                    select(period)
                    confirm()
                }}
            />
        </FilterPanel>
    )
}

/** A value of the details that reads as a code rather than as text. */
const CODE = /^[A-Za-z]+$/

/** The keys of the details whose values are codes, by the dictionary of the audit log that names them in words. */
const CODED_DETAILS: Record<string, string> = {
    from: 'roles',
    to: 'roles',
    type: 'repository_types',
    origin: 'origins',
    action: 'actions',
    authentication: 'authentications',
    category: 'categories',
    format: 'formats',
    outcome: 'outcomes',
    reason: 'reasons',
}

/** A code as a dictionary of the audit log names it, or the code itself when the dictionary does not. */
const inWordsOf = (t: TFunction) => (dictionary: string, code: string) => (
    t(`audit:${dictionary}.${code}`, { defaultValue: code })
)

/**
 * A value of the details in words: a code as its dictionary names it, a flag as yes or no, a list item by item,
 * and anything else as it is written.
 */
const detailText = (t: TFunction, key: string, value: unknown): string => {
    const dictionary = CODED_DETAILS[key]
    const word = (code: string) => (dictionary && CODE.test(code) ? inWordsOf(t)(dictionary, code) : code)
    if (typeof value === 'string') {
        return word(value)
    }
    if (typeof value === 'boolean') {
        return t(value ? 'audit:yes' : 'audit:no')
    }
    if (Array.isArray(value) && value.every(item => typeof item === 'string')) {
        return value.map(word).join(', ')
    }
    return JSON.stringify(value)
}

/** What a change did to a value: added one where there was none, removed it, or changed it to another. */
type ChangeKind = 'added' | 'removed' | 'changed'

/** Each kind of change as git marks the lines of a diff, on a tag of its colour. */
const CHANGE_MARKS: Record<ChangeKind, { mark: string, color: string }> = {
    added: { mark: '+', color: 'success' },
    removed: { mark: '-', color: 'error' },
    changed: { mark: '~', color: 'warning' },
}

/**
 * A tag keeps its text on one line, and none of its properties changes that. A change shows its values in full, so its
 * text wraps inside the tag rather than running past the page, a long word included.
 */
const WRAPPED_TEXT: React.CSSProperties = { whiteSpace: 'normal', overflowWrap: 'anywhere' }

/** A value a change added, removed or changed, marked as git marks a line of a diff; pointing at it shows the hint. */
const ChangeTag = ({ kind, text, hint }: { kind: ChangeKind, text: string, hint: string }) => (
    <Tooltip title={hint}>
        <Tag color={CHANGE_MARKS[kind].color} style={WRAPPED_TEXT}>{`${CHANGE_MARKS[kind].mark} ${text}`}</Tag>
    </Tooltip>
)

/** A name a change added or removed, such as a privilege of a group. */
interface NameChange {
    name: string
    kind: ChangeKind
}

/** Orders changes by the names they changed. */
const byName = (one: NameChange, other: NameChange) => one.name.localeCompare(other.name)

/** Whether a value of the details lists names. */
const isNames = (value: unknown): value is string[] => (
    Array.isArray(value) && value.every(item => typeof item === 'string')
)

/** Whether a detail lists the names a change added or removed: its key is the kind of the change. */
const isListedChange = (detail: [string, unknown]): detail is [ChangeKind, string[]] => (
    Object.hasOwn(CHANGE_MARKS, detail[0]) && isNames(detail[1])
)

/** The names the details list as added or removed, in the order of the names. */
const nameChangesOf = (details: Record<string, unknown>): NameChange[] => Object.entries(details)
    .filter(isListedChange)
    .flatMap(([kind, names]) => names.map(name => ({ name, kind })))
    .sort(byName)

/** The names a change added or removed, a line each, naming the change when pointed at. */
const NameChanges = ({ changes }: { changes: NameChange[] }) => {
    const { t } = useTranslation()
    return (
        <Flex vertical align="flex-start" gap={4}>
            {changes.map(({ name, kind }) => (
                <ChangeTag key={`${kind} ${name}`} hint={t(`audit:changes.${kind}`)} kind={kind} text={name} />
            ))}
        </Flex>
    )
}

/** The values a field had before a change and after it, either one missing. */
interface FieldChange {
    from?: unknown
    to?: unknown
}

/** Whether a value of the details names the fields a change made, each with its values before and after. */
const isFieldChanges = (value: unknown): value is Record<string, FieldChange> => (
    typeof value === 'object' && value !== null && !Array.isArray(value)
    && Object.values(value).every(change => typeof change === 'object' && change !== null
        && ('from' in change || 'to' in change))
)

/** Whether a field had a value before a change or after it. */
const hasValue = (value: unknown) => value !== undefined && value !== null

/** What a change did to a field: added a value where there was none, removed it, or changed it to another. */
const fieldChangeKind = ({ from, to }: FieldChange): ChangeKind => {
    if (hasValue(from) && hasValue(to)) {
        return 'changed'
    }
    return hasValue(to) ? 'added' : 'removed'
}

/**
 * The fields a change made, a line each, marked as git marks the lines of a diff. A value changed reads as the value
 * before the change, an arrow and the value after; a value added or removed reads as that value alone.
 */
const FieldChanges = ({ changes }: { changes: Record<string, FieldChange> }) => {
    const { t } = useTranslation()
    return (
        <Flex vertical align="flex-start" gap={4}>
            {Object.entries(changes).map(([field, change]) => {
                const name = t(`audit:detail_names.${field}`, { defaultValue: field })
                const values = [change.from, change.to].filter(hasValue).map(value => detailText(t, field, value))
                const kind = fieldChangeKind(change)
                return (
                    <ChangeTag
                        key={field}
                        hint={t(`audit:changes.${kind}`)}
                        kind={kind}
                        text={`${name}: ${values.join(' → ')}`}
                    />
                )
            })}
        </Flex>
    )
}

/** A detail of an event: what it holds under its name. */
interface Detail {
    key: string
    label: string
    children: React.ReactNode
}

/**
 * The details of an event, each under its name. The names a change added and removed are shown together, under
 * the name of the changes, where the first of their lists was.
 */
const EventDetails = ({ details }: { details: Record<string, unknown> }) => {
    const { t } = useTranslation()
    const label = (key: string) => t(`audit:detail_names.${key}`, { defaultValue: key })
    const entries = Object.entries(details)
    const firstList = entries.findIndex(isListedChange)
    const changes = <NameChanges changes={nameChangesOf(details)} />
    const items = entries.flatMap(([key, value], index): Detail[] => {
        if (isListedChange([key, value])) {
            return index === firstList ? [{ key: 'changes', label: label('changed'), children: changes }] : []
        }
        return [{
            key,
            label: label(key),
            children: key === 'changed' && isFieldChanges(value)
                ? <FieldChanges changes={value} />
                : detailText(t, key, value),
        }]
    })
    return <Descriptions column={1} items={items} size="small" title={t('common:details')} />
}

/** The panels the table draws: the details of an event and the filters of the columns. */
const renderDetails = (event: AuditEvent) => <EventDetails details={event.details ?? {}} />
const renderTextFilter = (props: FilterDropdownProps) => <TextFilter {...props} />
const renderPeriodFilter = (props: FilterDropdownProps) => <PeriodFilter {...props} />

/**
 * The audit log, for administrators: the events the filters of the columns match, the newest first, a page at a
 * time, and an export of all of them as a CSV file.
 */
export const AuditLog: React.FC = () => {
    const { t } = useTranslation()
    const { notification } = App.useApp()
    const { isAuditLogEnabled } = useContext(SystemContext)
    const [filters, setFilters] = useState<ColumnFilters>({})
    const query = useMemo(() => toQuery(filters), [filters])
    const [page, setPage] = useState(1)
    const [pageSize, setPageSize] = useState(PAGE_SIZE)
    const [events, setEvents] = useState<AuditEvent[]>([])
    const [total, setTotal] = useState(0)
    const [totalLimited, setTotalLimited] = useState(false)
    const [loading, setLoading] = useState(true)
    const [exporting, setExporting] = useState(false)

    useEffect(() => {
        if (!isAuditLogEnabled) {
            return undefined
        }
        let current = true
        setLoading(true)
        getAuditEvents(query, page - 1, pageSize)
            .then(result => {
                if (current) {
                    setEvents(result.content ?? [])
                    setTotal(result.total ?? 0)
                    setTotalLimited(result.totalLimited ?? false)
                }
            })
            .catch(() => {
                if (current) {
                    notification.error({ title: t('audit:load_failed') })
                }
            })
            .finally(() => {
                if (current) {
                    setLoading(false)
                }
            })
        return () => {
            current = false
        }
    }, [isAuditLogEnabled, query, page, pageSize, notification, t])

    if (!isAuditLogEnabled) {
        return null
    }

    const clearFilters = () => {
        setFilters({})
        setPage(1)
    }

    const exportCsv = async () => {
        setExporting(true)
        try {
            saveFile(await exportAuditEvents(query), 'audit-log.csv')
        } catch {
            notification.error({ title: t('audit:export_failed') })
        } finally {
            setExporting(false)
        }
    }

    const inWords = inWordsOf(t)

    /** The codes a column is filtered by, named in words. */
    const filterItems = (dictionary: string, codes: readonly string[]) => codes.map(value => ({
        value,
        text: inWords(dictionary, value),
    }))

    /** The key of a column and the value of its filter. */
    const filtered = (key: string) => ({ key, filteredValue: filters[key] ?? null })

    /** A column filtered by the text it contains. */
    const searched = (key: string) => ({
        ...filtered(key),
        filterDropdown: renderTextFilter,
        filterIcon: <SearchOutlined />,
    })

    const named = (type?: string, name?: string) => (name ? (
        <Flex wrap gap={4}>
            {type && <Typography.Text type="secondary">{inWords('types', type)}</Typography.Text>}
            <span>{name}</span>
        </Flex>
    ) : null)

    const columns: TableColumnsType<AuditEvent> = [
        {
            title: t('audit:id'),
            dataIndex: 'id',
            key: 'id',
            width: 80,
        },
        {
            title: t('audit:time'),
            dataIndex: 'time',
            // The time of an event, to the second, on one line.
            width: 230,
            ...filtered('time'),
            filterDropdown: renderPeriodFilter,
            render: (time: string) => formatDateTime(time),
        },
        {
            title: t('audit:actor'),
            dataIndex: 'actor',
            ...searched('actor'),
            render: (actor?: string) => actor ?? <Typography.Text type="secondary">—</Typography.Text>,
        },
        {
            title: t('audit:authentication'),
            dataIndex: 'authentication',
            ...filtered('authentication'),
            filters: filterItems('authentications', AUDIT_AUTHENTICATIONS),
            filterMultiple: false,
            render: (authentication?: AuditAuthentication) => (
                authentication && inWords('authentications', authentication)
            ),
        },
        {
            title: t('audit:action'),
            dataIndex: 'action',
            ...filtered('action'),
            filters: Object.entries(AUDIT_ACTIONS).map(([category, actions]) => ({
                value: category,
                text: inWords('categories', category),
                children: filterItems('actions', actions),
            })),
            filterMode: 'tree',
            filterSearch: true,
            render: (action: AuditAction) => inWords('actions', action),
        },
        {
            title: t('audit:outcome'),
            dataIndex: 'outcome',
            ...filtered('outcome'),
            filters: filterItems('outcomes', AUDIT_OUTCOMES),
            filterMultiple: false,
            render: (outcome: AuditOutcome) => (
                <Tag color={OUTCOME_COLORS[outcome]}>{inWords('outcomes', outcome)}</Tag>
            ),
        },
        {
            title: t('audit:target'),
            ...searched('target'),
            render: (_, event) => named(event.targetType, event.target),
        },
        {
            title: t('audit:subject'),
            ...searched('subject'),
            render: (_, event) => named(event.subjectType, event.subject),
        },
        {
            title: t('audit:client_address'),
            dataIndex: 'clientAddress',
            ...searched('clientAddress'),
        },
    ]

    return (
        <>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('audit:title')}
            </Typography.Title>
            <Typography.Paragraph type="secondary">
                {t('audit:description')}
            </Typography.Paragraph>
            <Flex vertical gap="small">
                <Flex gap="small" justify="end">
                    {Object.values(filters).some(value => value?.length) && (
                        <Button icon={<ClearOutlined />} onClick={clearFilters}>
                            {t('audit:clear_filters')}
                        </Button>
                    )}
                    <Button icon={<DownloadOutlined />} loading={exporting} onClick={() => void exportCsv()}>
                        {t('audit:export')}
                    </Button>
                </Flex>
                <Table
                    columns={columns}
                    dataSource={events}
                    loading={loading}
                    locale={{ emptyText: t('audit:no_events') }}
                    onChange={(_, nextFilters) => setFilters(nextFilters)}
                    rowKey="id"
                    size="small"
                    expandable={{
                        rowExpandable: event => Object.keys(event.details ?? {}).length > 0,
                        expandedRowRender: renderDetails,
                    }}
                    pagination={{
                        ...tablePagination(t),
                        // A total the server stopped counting at tells only that there are more events.
                        ...(totalLimited && {
                            showTotal: (shown: number, range: [number, number]) => t('audit:showing_rows_of_more',
                                { from: range[0], to: range[1], total: shown }),
                        }),
                        current: page,
                        pageSize,
                        total,
                        onChange: (nextPage, nextSize) => {
                            setPage(nextSize === pageSize ? nextPage : 1)
                            setPageSize(nextSize)
                        },
                    }}
                />
            </Flex>
        </>
    )
}

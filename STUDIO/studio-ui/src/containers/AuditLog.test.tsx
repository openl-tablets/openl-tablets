import React from 'react'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import type { FilterDropdownProps, FilterValue } from 'antd/es/table/interface'
import dayjs, { type Dayjs } from 'dayjs'
import type { MockedFunction } from 'vitest'
import { AuditLog } from './AuditLog'
import { SystemContext } from '../contexts'
import * as audit from '../services/audit'
import { saveFile } from '../utils/download'

vi.mock('../services/audit', async importOriginal => ({
    ...await importOriginal<typeof import('../services/audit')>(),
    getAuditEvents: vi.fn(),
    exportAuditEvents: vi.fn(),
}))

vi.mock('../utils/download', () => ({ saveFile: vi.fn() }))

vi.mock('../utils/dateFormat', () => ({ formatDateTime: (value: string) => `at ${value}` }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// Ant Design Table hangs act() in jsdom (see AGENTS.md) — mock antd with plain HTML equivalents.
vi.mock('antd', async () => {
    const DatePicker = {
        RangePicker: ({ onCalendarChange, onChange, onOpenChange }: {
            onCalendarChange: (period: [Dayjs | null, Dayjs | null] | null) => void
            onChange: (period: [Dayjs | null, Dayjs | null] | null) => void
            onOpenChange: (open: boolean) => void
        }) => (
            <>
                <button onClick={() => onCalendarChange([dayjs('2026-10-01T00:00:00Z'), null])} type="button">
                    pick the start
                </button>
                <button onClick={() => onOpenChange(false)} type="button">close the calendar</button>
                <button
                    onClick={() => onChange([dayjs('2026-10-01T00:00:00.456Z'), dayjs('2026-10-15T00:00:00.789Z')])}
                    type="button"
                >
                    pick period
                </button>
                <button onClick={() => onChange(null)} type="button">clear period</button>
            </>
        ),
    }
    const Input = {
        Search: ({ onChange, onSearch, value }: {
            onChange: (event: React.ChangeEvent<HTMLInputElement>) => void
            onSearch: (text: string) => void
            value: string
        }) => (
            <input
                onChange={onChange}
                onKeyDown={event => event.key === 'Enter' && onSearch(event.currentTarget.value)}
                value={value}
            />
        ),
    }
    const Typography = {
        Title: ({ children }: { children?: React.ReactNode }) => <h4>{children}</h4>,
        Paragraph: ({ children }: { children?: React.ReactNode }) => <p>{children}</p>,
        Text: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
    }
    interface FilterItem {
        value: string
        text: React.ReactNode
        children?: FilterItem[]
    }
    interface Column {
        key: string
        dataIndex?: string
        render?: (value: unknown, row: Record<string, unknown>) => React.ReactNode
        filteredValue?: FilterValue | null
        filters?: FilterItem[]
        filterDropdown?: (props: FilterDropdownProps) => React.ReactNode
    }
    /** The panel of a column filter, which keeps the keys it is given at once, as Ant Design does. */
    const FilterDropdown = ({ column, onFilter }: {
        column: Column
        onFilter: (value: FilterValue | null) => void
    }) => {
        const keys = React.useRef<React.Key[]>(column.filteredValue?.map(String) ?? [])
        const [, rerender] = React.useState(0)
        return column.filterDropdown?.({
            prefixCls: 'ant-dropdown',
            selectedKeys: keys.current,
            setSelectedKeys: next => {
                keys.current = next
                rerender(count => count + 1)
            },
            confirm: () => onFilter(keys.current.length ? keys.current : null),
            close: () => undefined,
            visible: true,
        })
    }
    const Table = ({ columns, dataSource, expandable, loading, locale, onChange, pagination }: {
        columns: Column[]
        dataSource: Record<string, unknown>[]
        expandable: {
            rowExpandable: (row: Record<string, unknown>) => boolean
            expandedRowRender: (row: Record<string, unknown>) => React.ReactNode
        }
        loading?: boolean
        locale: { emptyText: React.ReactNode }
        onChange: (pagination: unknown, filters: Record<string, FilterValue | null>) => void
        pagination: {
            current: number
            pageSize: number
            total: number
            showTotal: (total: number, range: [number, number]) => React.ReactNode
            onChange: (page: number, size: number) => void
        }
    }) => {
        /** Applies the filter of a column as Ant Design does: with the filters of the other columns, from page 1. */
        const filter = (key: string, value: FilterValue | null) => {
            pagination.onChange(1, pagination.pageSize)
            onChange(pagination, Object.fromEntries(columns.map(column => [
                column.key,
                column.key === key ? value : column.filteredValue ?? null,
            ])))
        }
        /** A category picks itself and all of its actions, as a checked parent of a tree does. */
        const keysOf = (item: FilterItem) => [item.value, ...(item.children ?? []).map(child => child.value)]
        return (
            <div>
                {loading && <span data-testid="spin" />}
                {dataSource.length === 0 && locale.emptyText}
                {columns.map(column => (
                    <div key={column.key} data-testid={`filter-${column.key}`}>
                        <FilterDropdown column={column} onFilter={value => filter(column.key, value)} />
                        {column.filters?.flatMap(item => [item, ...(item.children ?? [])]).map(item => (
                            <button key={item.value} onClick={() => filter(column.key, keysOf(item))} type="button">
                                {item.text}
                            </button>
                        ))}
                    </div>
                ))}
                <table>
                    <tbody>
                        {dataSource.map(row => (
                            <React.Fragment key={String(row['id'])}>
                                <tr>
                                    {columns.map(column => {
                                        const value = column.dataIndex ? row[column.dataIndex] : undefined
                                        return (
                                            <td key={column.key}>
                                                {column.render ? column.render(value, row) : String(value ?? '')}
                                            </td>
                                        )
                                    })}
                                </tr>
                                {expandable.rowExpandable(row) && (
                                    <tr>
                                        <td>{expandable.expandedRowRender(row)}</td>
                                    </tr>
                                )}
                            </React.Fragment>
                        ))}
                    </tbody>
                </table>
                <span data-testid="rows">{pagination.showTotal(pagination.total, [1, pagination.pageSize])}</span>
                <button onClick={() => pagination.onChange(pagination.current + 1, pagination.pageSize)} type="button">
                    next page
                </button>
                <button onClick={() => pagination.onChange(pagination.current + 1, 100)} type="button">
                    100 per page
                </button>
            </div>
        )
    }
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        Button: ({ children, onClick }: { children?: React.ReactNode, onClick?: () => void }) => (
            <button onClick={onClick} type="button">{children}</button>
        ),
        DatePicker,
        Descriptions: ({ items, title }: {
            items: { key: string, label: React.ReactNode, children: React.ReactNode }[]
            title?: React.ReactNode
        }) => (
            <dl>
                {title}
                {items.map(item => (
                    <React.Fragment key={item.key}>
                        <dt>{item.label}</dt>
                        <dd>{item.children}</dd>
                    </React.Fragment>
                ))}
            </dl>
        ),
        Flex: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
        Input,
        notification: { error: vi.fn() },
        Table,
        Tag: ({ children, color, style }: {
            children?: React.ReactNode,
            color?: string,
            style?: React.CSSProperties,
        }) => <span data-color={color} style={style}>{children}</span>,
        theme: { useToken: () => ({ token: { paddingXS: 8 } }) },
        Tooltip: ({ children, title }: { children?: React.ReactNode, title?: string }) => (
            <span data-title={title}>{children}</span>
        ),
        Typography,
    })
})

const mockGetEvents = audit.getAuditEvents as MockedFunction<typeof audit.getAuditEvents>
const mockExport = audit.exportAuditEvents as MockedFunction<typeof audit.exportAuditEvents>
const mockSaveFile = saveFile as MockedFunction<typeof saveFile>

const failedSignIn: audit.AuditEvent = {
    id: 2,
    time: '2026-10-19T10:15:30Z',
    actor: 'jdoe',
    authentication: 'form',
    clientAddress: '10.0.0.7',
    category: 'authentication',
    action: 'signIn',
    outcome: 'failure',
    details: { reason: 'badCredentials' },
}

const purge: audit.AuditEvent = {
    id: 1,
    time: '2026-10-19T10:00:00Z',
    category: 'audit',
    action: 'auditLogPurged',
    outcome: 'success',
    targetType: 'project',
    target: 'design:/rules/Sales',
    details: { removed: 3 },
}

const renderAuditLog = (enabled = true) => render(
    <SystemContext.Provider value={{ isAuditLogEnabled: enabled } as React.ContextType<typeof SystemContext>}>
        <AuditLog />
    </SystemContext.Provider>
)

const loaded = () => waitFor(() => expect(screen.queryByTestId('spin')).not.toBeInTheDocument())

/** Expects a tag of the given colour that holds the given text. */
const expectTag = (text: string, color: string) => expect(screen.getByText(text)).toHaveAttribute('data-color', color)

/** Expects the text to name the given change when it is pointed at. */
const expectHint = (text: string, hint: string) => expect(screen.getByText(text).closest('[data-title]'))
    .toHaveAttribute('data-title', hint)

/** The texts of the tags that mark changes, in the order they are shown. */
const changeTags = () => screen.getAllByText(/^[+~-] /).map(tag => tag.textContent)

/** Types a text into the search of the filter of a column and starts the search. */
const search = (column: string, text: string) => userEvent.type(
    within(screen.getByTestId(`filter-${column}`)).getByRole('textbox'), `${text}{Enter}`)

/** Picks an item of the filter of a column. */
const pick = (column: string, item: string) => userEvent.click(
    within(screen.getByTestId(`filter-${column}`)).getByRole('button', { name: item }))

const lastQuery = (query: audit.AuditQuery) => waitFor(() => expect(mockGetEvents).toHaveBeenLastCalledWith(query,
    0, 50))

describe('AuditLog', () => {
    beforeEach(() => {
        mockGetEvents.mockResolvedValue({ content: [failedSignIn, purge], pageNumber: 0, pageSize: 50, total: 2 })
    })

    it('renders nothing when the audit log is not kept', () => {
        const { container } = renderAuditLog(false)

        expect(container).toBeEmptyDOMElement()
        expect(mockGetEvents).not.toHaveBeenCalled()
    })

    it('shows the newest events with their details', async () => {
        renderAuditLog()
        await loaded()

        expect(mockGetEvents).toHaveBeenCalledWith({}, 0, 50)
        const rows = within(screen.getByRole('table'))
        expect(rows.getByText('2')).toBeInTheDocument()
        expect(rows.getByText('at 2026-10-19T10:15:30Z')).toBeInTheDocument()
        expect(rows.getByText('jdoe')).toBeInTheDocument()
        expect(rows.getByText('audit:authentications.form')).toBeInTheDocument()
        expect(rows.getByText('audit:actions.signIn')).toBeInTheDocument()
        expect(rows.getByText('audit:outcomes.failure')).toBeInTheDocument()
        expect(rows.getByText('audit:detail_names.reason')).toBeInTheDocument()
        expect(rows.getByText('audit:reasons.badCredentials')).toBeInTheDocument()
        expect(rows.getByText('—')).toBeInTheDocument()
        expect(rows.getByText('audit:types.project')).toBeInTheDocument()
        expect(rows.getByText('design:/rules/Sales')).toBeInTheDocument()
        expect(rows.getByText('3')).toBeInTheDocument()
    })

    it('shows each field a change made as a diff: changed from before to after, added or removed', async () => {
        const update: audit.AuditEvent = {
            id: 4,
            time: '2026-10-19T10:25:00Z',
            actor: 'admin',
            category: 'user',
            action: 'userUpdated',
            outcome: 'success',
            targetType: 'user',
            target: 'jwilliams',
            details: {
                changed: {
                    displayName: { from: 'John Williams', to: 'John Williams Jr.' },
                    firstName: { from: 'John' },
                    lastName: { to: 'Williams' },
                    emailVerified: { from: false, to: true },
                },
            },
        }
        mockGetEvents.mockResolvedValue({ content: [update], pageNumber: 0, pageSize: 50, total: 1 })

        renderAuditLog()
        await loaded()

        expect(screen.getByText('audit:detail_names.changed')).toBeInTheDocument()
        expect(changeTags()).toEqual([
            '~ audit:detail_names.displayName: John Williams → John Williams Jr.',
            '- audit:detail_names.firstName: John',
            '+ audit:detail_names.lastName: Williams',
            '~ audit:detail_names.emailVerified: audit:no → audit:yes',
        ])
        expectTag('~ audit:detail_names.displayName: John Williams → John Williams Jr.', 'warning')
        expectTag('- audit:detail_names.firstName: John', 'error')
        expectTag('+ audit:detail_names.lastName: Williams', 'success')
        expectHint('~ audit:detail_names.displayName: John Williams → John Williams Jr.', 'audit:changes.changed')
        expectHint('- audit:detail_names.firstName: John', 'audit:changes.removed')
        expectHint('+ audit:detail_names.lastName: Williams', 'audit:changes.added')
        // A long value wraps inside its tag rather than running past the page.
        expect(screen.getByText('~ audit:detail_names.displayName: John Williams → John Williams Jr.'))
            .toHaveStyle({ whiteSpace: 'normal', overflowWrap: 'anywhere' })
    })

    it('shows the names a change added and removed together, in the order of the names', async () => {
        const privileges: audit.AuditEvent = {
            id: 7,
            time: '2026-10-19T10:40:00Z',
            actor: 'admin',
            category: 'group',
            action: 'groupPrivilegesChanged',
            outcome: 'success',
            targetType: 'group',
            target: 'Analysts',
            details: { added: ['VIEW_PROJECTS', 'DEPLOY_PROJECTS'], removed: ['EDIT_PROJECTS']},
        }
        mockGetEvents.mockResolvedValue({ content: [privileges], pageNumber: 0, pageSize: 50, total: 1 })

        renderAuditLog()
        await loaded()

        expect(screen.getAllByText('audit:detail_names.changed')).toHaveLength(1)
        expect(screen.queryByText('audit:detail_names.added')).not.toBeInTheDocument()
        expect(screen.queryByText('audit:detail_names.removed')).not.toBeInTheDocument()
        expectTag('- EDIT_PROJECTS', 'error')
        expectHint('- EDIT_PROJECTS', 'audit:changes.removed')
        expect(changeTags()).toEqual(['+ DEPLOY_PROJECTS', '- EDIT_PROJECTS', '+ VIEW_PROJECTS'])
    })

    it('shows the settings one save changed as a diff and marks each in the object, a secret one without values',
        async () => {
            const settings: audit.AuditEvent = {
                id: 5,
                time: '2026-10-19T10:30:00Z',
                actor: 'admin',
                category: 'settings',
                action: 'settingsChanged',
                outcome: 'success',
                targetType: 'settings',
                target: 'mail.password, mail.url, mail.username, project.history.count, repository.design.comment',
                details: {
                    changed: {
                        'mail.url': { from: 'smtp://old.example.com', to: 'smtp://mail.example.com' },
                        'mail.password': { secret: true },
                        'mail.username': { to: 'bot' },
                        'project.history.count': { from: '100' },
                        'repository.design.comment-template.use-custom-comments': { to: 'true' },
                    },
                },
            }
            mockGetEvents.mockResolvedValue({ content: [settings], pageNumber: 0, pageSize: 50, total: 1 })

            renderAuditLog()
            await loaded()

            expect(changeTags()).toEqual([
                '~ mail.password',
                '~ mail.url',
                '+ mail.username',
                '- project.history.count',
                '+ repository.design.comment-templ…',
                '~ audit:detail_names.mail.url: smtp://old.example.com → smtp://mail.example.com',
                '~ audit:detail_names.mail.password: audit:secret_changed',
                '+ audit:detail_names.mail.username: bot',
                '- audit:detail_names.project.history.count: 100',
                '+ audit:detail_names.repository.design.comment-template.use-custom-comments: true',
            ])
            expectTag('~ audit:detail_names.mail.password: audit:secret_changed', 'warning')
            expectHint('~ mail.password', 'audit:changes.changed: mail.password')
            // A name cut short to fit the column stays on one line; the details wrap their values.
            expect(screen.getByText('~ mail.password')).not.toHaveStyle({ overflowWrap: 'anywhere' })
            expect(screen.getByText('~ audit:detail_names.mail.password: audit:secret_changed'))
                .toHaveStyle({ overflowWrap: 'anywhere' })
            expectHint('+ repository.design.comment-templ…',
                'audit:changes.added: repository.design.comment-template.use-custom-comments')
            expect(screen.queryByText('mail.password, mail.url, mail.username, project.history.count'))
                .not.toBeInTheDocument()
        })

    it('names the first ten settings in the object and counts the others, which the details list', async () => {
        const names = Array.from({ length: 12 }, (_, index) => `setting.${String(index + 1).padStart(2, '0')}`)
        const settings: audit.AuditEvent = {
            id: 8,
            time: '2026-10-19T10:45:00Z',
            actor: 'admin',
            category: 'settings',
            action: 'settingsChanged',
            outcome: 'success',
            targetType: 'settings',
            target: names.join(', '),
            details: { changed: Object.fromEntries(names.map(name => [name, { from: '1', to: '2' }])) },
        }
        mockGetEvents.mockResolvedValue({ content: [settings], pageNumber: 0, pageSize: 50, total: 1 })

        renderAuditLog()
        await loaded()

        expect(screen.getByText('~ setting.10')).toBeInTheDocument()
        expect(screen.queryByText('~ setting.11')).not.toBeInTheDocument()
        expect(screen.getByText('~ audit:detail_names.setting.12: 1 → 2')).toBeInTheDocument()
        expect(screen.getByText('+2')).toBeInTheDocument()
        expect(changeTags()).toHaveLength(22)
    })

    it('shows the settings the environment changed by their names only, each marked as its change', async () => {
        const environment: audit.AuditEvent = {
            id: 6,
            time: '2026-10-19T10:35:00Z',
            category: 'settings',
            action: 'settingsChanged',
            outcome: 'success',
            targetType: 'settings',
            target: 'cors.allowed.origins, mail.url, user.mode',
            details: {
                origin: 'environment',
                added: ['user.mode'],
                changed: ['mail.url'],
                removed: ['cors.allowed.origins'],
            },
        }
        mockGetEvents.mockResolvedValue({ content: [environment], pageNumber: 0, pageSize: 50, total: 1 })

        renderAuditLog()
        await loaded()

        expect(screen.getByText('audit:origins.environment')).toBeInTheDocument()
        expect(screen.getAllByText('audit:detail_names.changed')).toHaveLength(1)
        const marks = ['- cors.allowed.origins', '~ mail.url', '+ user.mode']
        expect(changeTags()).toEqual([...marks, ...marks])
        screen.getAllByText('~ mail.url')
            .forEach(tag => expect(tag.closest('[data-color]')).toHaveAttribute('data-color', 'warning'))
        expect(screen.getByText('audit:environment_values_not_recorded')).toBeInTheDocument()
        expect(screen.getAllByText('~ mail.url').at(-1)?.closest('[data-title]'))
            .toHaveAttribute('data-title', 'audit:changes.changed')
    })

    it('names the codes of the details in words and leaves free text as it is', async () => {
        const read: audit.AuditEvent = {
            id: 3,
            time: '2026-10-19T10:20:00Z',
            actor: 'admin',
            category: 'audit',
            action: 'auditLogRead',
            outcome: 'success',
            details: { format: 'csv', action: ['signIn', 'signOut'], actor: 'jdoe', unpaged: true },
        }
        mockGetEvents.mockResolvedValue({ content: [read], pageNumber: 0, pageSize: 50, total: 1 })

        renderAuditLog()
        await loaded()

        expect(screen.getByText('audit:formats.csv')).toBeInTheDocument()
        expect(screen.getByText('audit:actions.signIn, audit:actions.signOut')).toBeInTheDocument()
        expect(screen.getByText('audit:detail_names.actor')).toBeInTheDocument()
        expect(screen.getAllByText('jdoe')).toHaveLength(1)
        expect(screen.getByText('audit:yes')).toBeInTheDocument()
    })

    it('filters by the text a column contains, from the first page', async () => {
        renderAuditLog()
        await loaded()
        await userEvent.click(screen.getByText('next page'))
        await waitFor(() => expect(mockGetEvents).toHaveBeenLastCalledWith({}, 1, 50))

        await search('actor', 'jdoe')
        await lastQuery({ actor: 'jdoe' })
        await search('target', 'Sales')
        await lastQuery({ actor: 'jdoe', target: 'Sales' })
        await search('clientAddress', '10.0.0')

        await lastQuery({ actor: 'jdoe', target: 'Sales', clientAddress: '10.0.0' })
    })

    it('drops the filter of a column once its text is cleared', async () => {
        renderAuditLog()
        await loaded()
        await search('subject', 'Analysts')
        await lastQuery({ subject: 'Analysts' })

        await userEvent.clear(within(screen.getByTestId('filter-subject')).getByRole('textbox'))
        await search('subject', '')

        await lastQuery({})
    })

    it('filters by every action of a category, or by one action', async () => {
        renderAuditLog()
        await loaded()

        await pick('action', 'audit:categories.authentication')
        await lastQuery({ actions: ['signIn', 'signOut', 'sessionStarted', 'sessionEnded']})
        await pick('action', 'audit:actions.auditLogRead')

        await lastQuery({ actions: ['auditLogRead']})
    })

    it('filters by the starts, restarts and stops of OpenL Studio, and names a session a restart ended', async () => {
        const ended: audit.AuditEvent = {
            id: 4,
            time: '2026-10-19T10:25:00Z',
            actor: 'jdoe',
            authentication: 'form',
            category: 'authentication',
            action: 'sessionEnded',
            outcome: 'success',
            details: { reason: 'restarted' },
        }
        mockGetEvents.mockResolvedValue({ content: [ended], pageNumber: 0, pageSize: 50, total: 1 })
        renderAuditLog()
        await loaded()

        const rows = within(screen.getByRole('table'))
        expect(rows.getByText('audit:actions.sessionEnded')).toBeInTheDocument()
        expect(rows.getByText('audit:reasons.restarted')).toBeInTheDocument()
        await pick('action', 'audit:categories.application')
        await lastQuery({ actions: ['applicationStarted', 'applicationRestarted', 'applicationStopped']})
    })

    it('filters by the outcome and by how the users signed in', async () => {
        renderAuditLog()
        await loaded()

        await pick('outcome', 'audit:outcomes.failure')
        await lastQuery({ outcome: 'failure' })
        await pick('authentication', 'audit:authentications.token')

        await lastQuery({ outcome: 'failure', authentication: 'token' })
    })

    it('filters by a period of whole seconds, and drops the filter once the period is cleared', async () => {
        renderAuditLog()
        await loaded()

        await userEvent.click(screen.getByText('pick period'))
        await lastQuery({ from: '2026-10-01T00:00:00.000Z', to: '2026-10-15T00:00:01.000Z' })
        await userEvent.click(screen.getByText('clear period'))

        await lastQuery({})
    })

    it('filters by a period open at its end once the calendar closes', async () => {
        renderAuditLog()
        await loaded()

        await userEvent.click(screen.getByText('pick the start'))
        expect(mockGetEvents).toHaveBeenCalledTimes(1)
        await userEvent.click(screen.getByText('close the calendar'))

        await lastQuery({ from: '2026-10-01T00:00:00.000Z' })
    })

    it('tells that more events match when the server stopped counting them', async () => {
        mockGetEvents.mockResolvedValue({ content: [], pageNumber: 0, pageSize: 50, total: 10000, totalLimited: true })
        renderAuditLog()

        expect(await screen.findByText('audit:showing_rows_of_more')).toBeInTheDocument()
    })

    it('shows the total as it is while the server counted every event', async () => {
        renderAuditLog()
        await loaded()

        expect(screen.getByTestId('rows')).toHaveTextContent('common:table.showing_rows')
    })

    it('starts from the first page when the page size changes', async () => {
        renderAuditLog()
        await loaded()

        await userEvent.click(screen.getByText('100 per page'))

        await waitFor(() => expect(mockGetEvents).toHaveBeenLastCalledWith({}, 0, 100))
    })

    it('clears the filters with a button shown only while a filter is set', async () => {
        renderAuditLog()
        await loaded()
        expect(screen.queryByText('audit:clear_filters')).not.toBeInTheDocument()
        await search('actor', 'jdoe')
        await pick('outcome', 'audit:outcomes.denied')
        await lastQuery({ actor: 'jdoe', outcome: 'denied' })

        await userEvent.click(screen.getByText('audit:clear_filters'))

        await lastQuery({})
        expect(screen.queryByText('audit:clear_filters')).not.toBeInTheDocument()
    })

    it('exports the events the filters match as a CSV file', async () => {
        const csv = new Blob(['time,actor'])
        mockExport.mockResolvedValue(csv)
        renderAuditLog()
        await loaded()
        await pick('outcome', 'audit:outcomes.denied')
        await lastQuery({ outcome: 'denied' })

        await userEvent.click(screen.getByText('audit:export'))

        await waitFor(() => expect(mockSaveFile).toHaveBeenCalledWith(csv, 'audit-log.csv'))
        expect(mockExport).toHaveBeenCalledWith({ outcome: 'denied' })
    })

    it('reports an export that failed', async () => {
        mockExport.mockRejectedValue(new Error('Forbidden'))
        renderAuditLog()
        await loaded()

        await userEvent.click(screen.getByText('audit:export'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'audit:export_failed' }))
        expect(mockSaveFile).not.toHaveBeenCalled()
    })

    it('reports a log that failed to load', async () => {
        mockGetEvents.mockRejectedValue(new Error('Forbidden'))
        renderAuditLog()
        await loaded()

        expect(notification.error).toHaveBeenCalledWith({ title: 'audit:load_failed' })
        expect(screen.getByText('audit:no_events')).toBeInTheDocument()
    })
})

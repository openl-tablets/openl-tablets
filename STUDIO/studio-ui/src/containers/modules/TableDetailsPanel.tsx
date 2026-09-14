import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Descriptions, Empty, Select, Skeleton, Space, Tooltip } from 'antd'
import {
    ArrowUpOutlined,
    DeleteOutlined,
    EditOutlined,
    LeftOutlined,
    ProfileOutlined,
    RightOutlined,
} from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import type { ProjectProperty } from 'types/tables'
import { getTableDetails, type PropertyInheritance, type TableDetails } from '../../services/modules'
import { getProjectProperties } from '../../services/projects'
import { updateTableProperties } from '../../services/tables'
import { readJson, writeJson } from '../../utils/localStore'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { initialPropertyValue, PropertyValueInput } from '../tableModals/PropertyValueInput'
import { toPropertyGroups } from '../tableModals/shared'

/** The name takes a fixed share of the panel, so a value is not squeezed into a column of its own. */
const LABEL_WIDTH = 130

/** Whether the panel stands open, kept so a reader who folds it away keeps it folded. */
const STORAGE_KEY = 'openl.module.tableDetails'
/** The width it was last dragged to, kept for the next table the reader opens. */
const WIDTH_STORAGE_KEY = 'openl.module.tableDetails.width'

const WIDTH = { min: 220, max: 720, fallback: 320 }

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        position: relative;
        display: flex;
        flex: none;
        flex-direction: column;
        min-height: 0;
        border-left: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    /** Folded away, the panel keeps only the handle that opens it again. */
    folded: css`
        width: auto;
    `,
    header: css`
        display: flex;
        flex: none;
        align-items: center;
        gap: ${token.marginXXS}px;
        padding: ${token.paddingXXS}px ${token.paddingXXS}px ${token.paddingXXS}px ${token.paddingSM}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    title: css`
        flex: 1;
        overflow: hidden;
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextSecondary};
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    body: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: ${token.paddingSM}px;
    `,
    group: css`
        margin-bottom: ${token.marginSM}px;
    `,
    /** An inherited value is not the table's own, and reads as the old Editor greyed it. */
    inherited: css`
        color: ${token.colorTextTertiary};
    `,
    value: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: ${token.marginXXS}px;
    `,
    source: css`
        flex: none;
        min-width: 0;
    `,
    /** What is being written stands at the foot of the panel, where the old Editor kept it. */
    actions: css`
        display: flex;
        flex: none;
        justify-content: flex-end;
        gap: ${token.marginXXS}px;
        padding: ${token.paddingXS}px ${token.paddingSM}px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    /** The property to add is picked under the ones the table already carries. */
    add: css`
        width: 100%;
        margin-top: ${token.marginXS}px;
    `,
}))

/** One property as the panel draws it: what the table says about it, or what the reader is writing. */
interface PropertyRow {
    name: string
    displayName: string
    value: string
    inheritedFrom?: PropertyInheritance
    inheritedTableId?: string
}

/** What the reader has written but not saved: a value for each property they touched, {@code null} to take away. */
type PropertyDraft = Record<string, string | null>

interface TableDetailsPanelProps {
    projectId: string
    moduleName: string
    /** The table on screen; nothing is read while none is picked. */
    tableId: string | null
    /** Opens the properties table an inherited value comes from. */
    onOpenTable: (tableId: string) => void
    /** Whether the reader may edit the project; the properties are written only then. */
    canWrite?: boolean
    /** The table after its properties were written — under a new id when it had to be moved to grow. */
    onSaved?: ((tableId: string) => void) | undefined
}

/**
 * What the table on screen says about itself besides its cells, in a panel down its right-hand side — where the
 * old Editor kept its Table Details.
 *
 * The properties are read whole: those the table declares and those it inherits from the properties table of its
 * module or its category, each saying which it is. That is what the panel is for — with the table's header
 * hidden, an inherited value appears nowhere else on screen, and the properties table it comes from is a click
 * away.
 */
export const TableDetailsPanel = ({
    projectId,
    moduleName,
    tableId,
    onOpenTable,
    canWrite = false,
    onSaved,
}: TableDetailsPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const theme = useTheme()
    const [open, setOpen] = useState(() => readJson(STORAGE_KEY, true, (value): value is boolean =>
        typeof value === 'boolean'))
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'left', WIDTH)
    const [details, setDetails] = useState<TableDetails | null>(null)
    const [loading, setLoading] = useState(false)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    // Only what the reader touched: a value they wrote, or nothing at all for a property they took away.
    const [draft, setDraft] = useState<PropertyDraft>({})
    // The dictionary says how each property is written — a date, a flag, one or several values of an
    // enumeration — so it is read the first time a reader writes anything.
    const [dictionary, setDictionary] = useState<ProjectProperty[]>([])

    // The whole panel reads at the size of the table beside it — the group headings included, which stand out
    // by their weight rather than by being larger than the name of the table they describe.
    const cellStyles = useMemo(() => ({
        label: { width: LABEL_WIDTH, fontSize: theme.fontSizeSM },
        content: { fontSize: theme.fontSizeSM },
        title: { fontSize: theme.fontSizeSM, lineHeight: theme.lineHeightSM },
    }), [theme])

    useEffect(() => {
        writeJson(STORAGE_KEY, open)
    }, [open])

    useEffect(() => {
        if (!open || tableId === null) {
            setDetails(null)
            return
        }
        let dropped = false
        setLoading(true)
        getTableDetails(projectId, tableId, moduleName)
            .then(read => {
                if (!dropped) {
                    setDetails(read)
                }
            })
            .catch(() => {
                if (!dropped) {
                    setDetails(null)
                }
            })
            .finally(() => {
                if (!dropped) {
                    setLoading(false)
                }
            })
        return () => {
            dropped = true
        }
    }, [projectId, tableId, moduleName, open])

    // What is being written belongs to the table it was written on: another table is read afresh.
    useEffect(() => {
        setEditing(false)
        setDraft({})
    }, [tableId])

    useEffect(() => {
        if (!editing || dictionary.length > 0) {
            return
        }
        getProjectProperties(projectId).then(setDictionary).catch(() => setDictionary([]))
    }, [editing, projectId, dictionary.length])

    const definitionOf = useCallback(
        (name: string) => dictionary.find(property => property.name === name),
        [dictionary]
    )

    /** The properties on screen, group by group: what the table says, with what the reader wrote over it. */
    const groups = useMemo(() => {
        const shown = (details?.groups ?? []).map(group => ({
            name: group.name,
            properties: group.properties
                .filter(property => draft[property.name] !== null)
                .map((property): PropertyRow => ({
                    ...property,
                    value: property.name in draft ? draft[property.name] ?? '' : property.value,
                })),
        })).filter(group => group.properties.length > 0)
        // A property the reader added stands in the group the dictionary gives it, with the ones already there.
        const known = new Set((details?.groups ?? []).flatMap(group => group.properties.map(one => one.name)))
        for (const name of Object.keys(draft)) {
            const definition = definitionOf(name)
            if (known.has(name) || draft[name] === null || !definition) {
                continue
            }
            const row: PropertyRow = {
                name,
                displayName: definition.displayName,
                value: draft[name] ?? '',
            }
            const group = shown.find(candidate => candidate.name === definition.group)
            if (group) {
                group.properties.push(row)
            } else {
                shown.push({ name: definition.group, properties: [row]})
            }
        }
        return shown
    }, [details, draft, definitionOf])

    /** The properties the table may still be given, less the ones the reader has already added. */
    const offered = useMemo(
        () => toPropertyGroups(dictionary.filter(property =>
            (details?.available ?? []).includes(property.name) && !(property.name in draft))),
        [dictionary, details, draft]
    )

    const save = async () => {
        if (tableId === null) {
            return
        }
        // Only what the reader touched is sent: the table keeps every property this panel was not asked about.
        const written = Object.entries(draft).map(([name, value]) => ({ name, value }))
        setSaving(true)
        const table = await updateTableProperties(projectId, tableId, written, moduleName)
        setSaving(false)
        if (table !== null) {
            setEditing(false)
            setDraft({})
            onSaved?.(table)
        }
    }

    const cancel = () => {
        setEditing(false)
        setDraft({})
    }

    /** One property as it is written: the editor its own type asks for, and the way to take it away. */
    const written = (property: PropertyRow) => (
        <div className={styles.value} data-testid={`table-details-${property.name}`}>
            <PropertyValueInput
                aria-label={property.displayName}
                data-testid={`table-details-input-${property.name}`}
                definition={definitionOf(property.name)}
                onChange={value => setDraft(current => ({ ...current, [property.name]: String(value) }))}
                placeholder={property.displayName}
                value={property.value}
            />
            <Tooltip title={t('browser.module.details_remove')}>
                <Button
                    aria-label={t('browser.module.details_remove')}
                    className={styles.source}
                    data-testid={`table-details-remove-${property.name}`}
                    icon={<DeleteOutlined />}
                    onClick={() => setDraft(current => ({ ...current, [property.name]: null }))}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </div>
    )

    const toggle = (
        <Tooltip title={t(open ? 'browser.module.details_hide' : 'browser.module.details_show')}>
            <Button
                aria-label={t(open ? 'browser.module.details_hide' : 'browser.module.details_show')}
                data-testid="table-details-toggle"
                icon={open ? <RightOutlined /> : <LeftOutlined />}
                onClick={() => setOpen(current => !current)}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    if (!open) {
        return (
            <aside className={cx(styles.panel, styles.folded)} data-testid="table-details">
                <div className={styles.header}>
                    <ProfileOutlined className={styles.inherited} />
                    {toggle}
                </div>
            </aside>
        )
    }

    return (
        <aside className={styles.panel} data-testid="table-details" style={{ width }}>
            <ResizeHandle edge="left" onPointerDown={startResize} testId="table-details-resizer" />
            <div className={styles.header}>
                <span className={styles.title} title={details?.name}>
                    {/* A properties table carries no name of its own, and the panel keeps its own instead. */}
                    {details?.name || t('browser.module.details')}
                </span>
                {canWrite && details?.canEditProperties && !editing && (
                    <Tooltip title={t('browser.module.details_edit')}>
                        <Button
                            aria-label={t('browser.module.details_edit')}
                            data-testid="table-details-edit"
                            icon={<EditOutlined />}
                            onClick={() => setEditing(true)}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                )}
                {toggle}
            </div>
            <div className={styles.body} data-testid="table-details-body">
                {loading && <Skeleton active title data-testid="table-details-loading" paragraph={{ rows: 4 }} />}
                {!loading && (details === null || groups.length === 0) && !editing && (
                    <Empty
                        data-testid="table-details-empty"
                        description={t('browser.module.details_none')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                )}
                {!loading && groups.map(group => (
                    <Descriptions
                        key={group.name}
                        bordered
                        className={styles.group}
                        column={1}
                        data-testid={`table-details-group-${group.name}`}
                        size="small"
                        styles={cellStyles}
                        title={group.name}
                        items={group.properties.map(property => ({
                            key: property.name,
                            label: (
                                <span className={property.inheritedFrom ? styles.inherited : undefined}>
                                    {property.displayName}
                                </span>
                            ),
                            children: editing
                                ? written(property)
                                : (
                                    <div className={styles.value} data-testid={`table-details-${property.name}`}>
                                        <span className={property.inheritedFrom ? styles.inherited : undefined}>
                                            {property.value}
                                        </span>
                                        {property.inheritedFrom && (
                                            <Tooltip title={inheritedTitle(property.inheritedFrom, t)}>
                                                <Button
                                                    aria-label={inheritedTitle(property.inheritedFrom, t)}
                                                    className={styles.source}
                                                    data-testid={`table-details-source-${property.name}`}
                                                    disabled={!property.inheritedTableId}
                                                    icon={<ArrowUpOutlined />}
                                                    size="small"
                                                    type="text"
                                                    onClick={() => property.inheritedTableId
                                                        && onOpenTable(property.inheritedTableId)}
                                                />
                                            </Tooltip>
                                        )}
                                    </div>
                                ),
                        }))}
                    />
                ))}
                {editing && (
                    <Select
                        showSearch
                        className={styles.add}
                        data-testid="table-details-add"
                        options={offered}
                        placeholder={t('browser.module.details_add')}
                        size="small"
                        value={null}
                        onChange={(name: string) => setDraft(current => ({
                            ...current,
                            [name]: String(initialPropertyValue(definitionOf(name))),
                        }))}
                    />
                )}
            </div>
            {editing && (
                <div className={styles.actions}>
                    <Space size="small">
                        <Button data-testid="table-details-cancel" disabled={saving} onClick={cancel} size="small">
                            {t('browser.module.details_cancel')}
                        </Button>
                        <Button
                            data-testid="table-details-save"
                            disabled={Object.keys(draft).length === 0}
                            loading={saving}
                            onClick={save}
                            size="small"
                            type="primary"
                        >
                            {t('browser.module.details_save')}
                        </Button>
                    </Space>
                </div>
            )}
        </aside>
    )
}

/** Says which properties table a value was inherited from, so the reader knows what the arrow opens. */
const inheritedTitle = (level: PropertyInheritance, t: (key: string) => string): string =>
    t(`browser.module.details_inherited_${level}`)

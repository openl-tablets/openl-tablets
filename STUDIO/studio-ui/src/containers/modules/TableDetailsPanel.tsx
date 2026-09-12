import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Descriptions, Empty, Skeleton, Tooltip } from 'antd'
import { ArrowUpOutlined, LeftOutlined, ProfileOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import { getTableDetails, type PropertyInheritance, type TableDetails } from '../../services/modules'
import { readJson, writeJson } from '../../utils/localStore'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'

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
}))

interface TableDetailsPanelProps {
    projectId: string
    moduleName: string
    /** The table on screen; nothing is read while none is picked. */
    tableId: string | null
    /** Opens the properties table an inherited value comes from. */
    onOpenTable: (tableId: string) => void
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
export const TableDetailsPanel = ({ projectId, moduleName, tableId, onOpenTable }: TableDetailsPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const theme = useTheme()
    const [open, setOpen] = useState(() => readJson(STORAGE_KEY, true, (value): value is boolean =>
        typeof value === 'boolean'))
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'left', WIDTH)
    const [details, setDetails] = useState<TableDetails | null>(null)
    const [loading, setLoading] = useState(false)

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
                {toggle}
            </div>
            <div className={styles.body} data-testid="table-details-body">
                {loading && <Skeleton active title data-testid="table-details-loading" paragraph={{ rows: 4 }} />}
                {!loading && (details === null || details.groups.length === 0) && (
                    <Empty
                        data-testid="table-details-empty"
                        description={t('browser.module.details_none')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                )}
                {!loading && details?.groups.map(group => (
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
                            children: (
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
            </div>
        </aside>
    )
}

/** Says which properties table a value was inherited from, so the reader knows what the arrow opens. */
const inheritedTitle = (level: PropertyInheritance, t: (key: string) => string): string =>
    t(`browser.module.details_inherited_${level}`)

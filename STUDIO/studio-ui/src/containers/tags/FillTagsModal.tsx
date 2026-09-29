import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Alert, Checkbox, Empty, Modal, Skeleton, Table, Tag as AntTag, Tooltip, Typography } from 'antd'
import type { ModalProps } from 'antd'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { ArrowRightOutlined } from '@ant-design/icons'
import { apiCall } from '../../services'
import { errorMessage } from '../../utils/errorMessage'

/** What filling does with one derived tag value, as the backend reports it. */
type TagFillState = 'assign' | 'create' | 'rejected' | 'keep'

interface TagFillItem {
    type: string
    current?: string
    derived: string
    state: TagFillState
}

/** Why a project cannot take its tags now, as the backend reports it. */
type TagFillBlockerReason = 'locked' | 'lockedByYou' | 'branchProtected' | 'noPermission' | 'olderRevision'
    | 'archive'

interface TagFillBlocker {
    reason: TagFillBlockerReason
    /** Who holds the lock of a locked project; absent when that is unknown. */
    lockedBy?: string
    /** The protected branch of the project. */
    branch?: string
}

export interface TagFillPreview {
    projectName: string
    modifiable: boolean
    /** Why the project cannot be changed now; absent when it can. */
    blocker?: TagFillBlocker
    tags: TagFillItem[]
}

/** What filling did to one project it was asked for, as the backend reports it. */
type TagFillOutcome = 'updated' | 'notModifiable' | 'nothingToAssign' | 'failed'

export interface TagFillResult {
    projectName: string
    outcome: TagFillOutcome
    /** The tag values the project got, by tag type; absent when it was left alone. */
    tags?: Record<string, string>
    /** The missing values the project could not get, by tag type. */
    rejected?: Record<string, string>
    /** Why the project could not be changed, when it was not. */
    blocker?: TagFillBlocker
}

const useStyles = createStyles(({ css, token }) => ({
    row: css`
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 6px;
    `,
    /** The tag type the value belongs to. */
    type: css`
        color: ${token.colorTextTertiary};
    `,
    /** White — the value is configured and will be assigned. */
    assign: css`
        background: ${token.colorBgContainer};
        border-color: ${token.colorBorder};
        color: ${token.colorText};
    `,
    /** Green — the value will be created for its extensible tag type and assigned. */
    create: css`
        background: ${token.colorSuccessBg};
        border-color: ${token.colorSuccessBorder};
        color: ${token.colorSuccessText};
    `,
    /** Red — the value is not configured and its tag type does not take new values. */
    rejected: css`
        background: ${token.colorErrorBg};
        border-color: ${token.colorErrorBorder};
        color: ${token.colorErrorText};
    `,
    /** Grey — the project already carries the value, nothing changes. */
    keep: css`
        background: ${token.colorFillTertiary};
        border-color: ${token.colorBorderSecondary};
        color: ${token.colorTextTertiary};
    `,
    /** A project name stays on one line, however long the result next to it is. */
    project: css`
        white-space: nowrap;
    `,
    /** The values a project got or could not get, above why it was left alone. */
    result: css`
        display: flex;
        flex-direction: column;
        gap: 4px;
    `,
}))

/** Once the projects are filled, the window only closes: the footer keeps just the OK button, which reads Close. */
const closeOnlyFooter: ModalProps['footer'] = (_, { OkBtn }) => <OkBtn />

interface FillTagsModalProps {
    open: boolean
    onClose: () => void
    /** Called once the projects are filled, since filling may have created new tag values. */
    onFilled: () => void
}

/**
 * The projects whose name matches a project name template and that miss a tag it derives, with what
 * filling would do to each tag — assign a configured value, create it for an extensible tag type, or
 * leave the project as it is. The user picks the projects to fill, and then sees what filling did to
 * each of them: the tag values it got and the ones it could not get, or why it was left alone. A project
 * that cannot be picked says why, and so does a project left alone, with what to do about it.
 */
export const FillTagsModal = ({ open, onClose, onFilled }: FillTagsModalProps) => {
    const { t } = useTranslation('tags')
    const { styles } = useStyles()
    const [previews, setPreviews] = useState<TagFillPreview[] | null>(null)
    const [selected, setSelected] = useState<string[]>([])
    const [filling, setFilling] = useState(false)
    const [results, setResults] = useState<TagFillResult[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    // Every opening of the window is a session of its own. An answer that arrives once the window was closed, or
    // opened again, belongs to a session that is over, so it does not change what the window shows.
    const session = useRef(0)

    useEffect(() => {
        if (!open) {
            return
        }
        session.current += 1
        const current = session.current
        setPreviews(null)
        setSelected([])
        setFilling(false)
        setResults(null)
        setError(null)
        apiCall('/admin/tag-config/fill/preview', { method: 'GET' }, { throwError: true })
            .then((result: unknown) => {
                if (session.current !== current) {
                    return
                }
                const rows = (result ?? []) as TagFillPreview[]
                setPreviews(rows)
                setSelected(rows.filter(row => row.modifiable).map(row => row.projectName))
            })
            .catch(e => {
                if (session.current === current) {
                    setPreviews([])
                    setError(errorMessage(e))
                }
            })
        return () => {
            session.current += 1
        }
    }, [open])

    const fill = async () => {
        const current = session.current
        setFilling(true)
        setError(null)
        try {
            const filled = await apiCall('/admin/tag-config/fill', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(selected),
            }, { throwError: true }) as TagFillResult[] | undefined
            // The projects are filled whether the window still shows them or not, so the catalog is read again.
            onFilled()
            if (session.current === current) {
                setResults(filled ?? [])
            }
        } catch (e) {
            if (session.current === current) {
                setError(errorMessage(e))
            }
        } finally {
            if (session.current === current) {
                setFilling(false)
            }
        }
    }

    /** Why a project cannot be changed now, and what to do about it. */
    const blockerText = (blocker?: TagFillBlocker) => blocker
        ? t(`fill_blocker.${blocker.reason}`, {
            lockedBy: blocker.lockedBy ?? t('fill_blocker.another_user'),
            branch: blocker.branch,
        })
        : t('fill_blocker.unknown')

    /** Why filling left the project alone, and what to do about it. */
    const leftAloneText = (row: TagFillResult) => t('fill_result.left_alone', {
        reason: row.outcome === 'notModifiable' ? blockerText(row.blocker) : t(`fill_result.reason.${row.outcome}`),
    })

    /** A tag type, followed by what the project carries, gets or cannot get for it. */
    const typed = (type: string, values: ReactNode) => (
        <span key={type} className={styles.row}>
            <Typography.Text className={styles.type}>{type}:</Typography.Text>
            {values}
        </span>
    )

    // Hoisted out of the cell renderer, so the update chain never nests beyond what reads clearly.
    const toggleSelected = (projectName: string, checked: boolean) => setSelected(prev => checked
        ? [...prev, projectName]
        : prev.filter(name => name !== projectName))

    const previewColumns = [
        {
            title: t('fill_preview.project_column'),
            dataIndex: 'projectName',
            key: 'projectName',
            render: (projectName: string, row: TagFillPreview) => (
                <Checkbox
                    checked={selected.includes(projectName)}
                    data-testid={`fill-project-${projectName}`}
                    disabled={!row.modifiable}
                    onChange={event => toggleSelected(projectName, event.target.checked)}
                >
                    <Tooltip title={row.modifiable ? undefined : blockerText(row.blocker)}>
                        {projectName}
                    </Tooltip>
                </Checkbox>
            ),
        },
        {
            title: t('fill_preview.tags_column'),
            dataIndex: 'tags',
            key: 'tags',
            render: (tags: TagFillItem[], row: TagFillPreview) => (
                <div className={styles.row}>
                    {tags.map(tag => typed(tag.type, (
                        <>
                            {tag.current && (
                                <AntTag className={styles.keep} data-testid={`fill-current-${row.projectName}-${tag.type}`}>
                                    {tag.current}
                                </AntTag>
                            )}
                            {tag.state !== 'keep' && tag.current && <ArrowRightOutlined />}
                            {tag.state !== 'keep' && (
                                <Tooltip title={t(`fill_preview.state.${tag.state}`)}>
                                    <AntTag className={styles[tag.state]} data-testid={`fill-derived-${row.projectName}-${tag.type}`}>
                                        {tag.derived}
                                    </AntTag>
                                </Tooltip>
                            )}
                        </>
                    )))}
                </div>
            ),
        },
    ]

    const resultColumns = [
        {
            title: t('fill_preview.project_column'),
            dataIndex: 'projectName',
            key: 'projectName',
            render: (projectName: string) => <span className={styles.project}>{projectName}</span>,
        },
        {
            title: t('fill_result.result_column'),
            dataIndex: 'outcome',
            key: 'outcome',
            render: (outcome: TagFillOutcome, row: TagFillResult) => (
                <div className={styles.result}>
                    {(row.tags || row.rejected) && (
                        <div className={styles.row}>
                            {Object.entries(row.tags ?? {}).map(([type, value]) => typed(type, (
                                <AntTag data-testid={`fill-result-${row.projectName}-${type}`}>{value}</AntTag>
                            )))}
                            {Object.entries(row.rejected ?? {}).map(([type, value]) => typed(type, (
                                <Tooltip title={t('fill_result.not_assigned')}>
                                    <AntTag className={styles.rejected} data-testid={`fill-rejected-${row.projectName}-${type}`}>
                                        {value}
                                    </AntTag>
                                </Tooltip>
                            )))}
                        </div>
                    )}
                    {outcome !== 'updated' && (
                        <Typography.Text
                            data-testid={`fill-result-${row.projectName}`}
                            type={outcome === 'failed' ? 'danger' : 'warning'}
                        >
                            {leftAloneText(row)}
                        </Typography.Text>
                    )}
                </div>
            ),
        },
    ]

    const filled = results !== null
    const updated = results?.filter(result => result.outcome === 'updated').length ?? 0
    const resultView = results?.length === 0
        ? <Empty data-testid="fill-result-empty" description={t('fill_preview.nothing_to_fill')} />
        : (
            <>
                <Typography.Paragraph data-testid="fill-result-summary" type="secondary">
                    {t('fill_result.summary', { updated, skipped: (results?.length ?? 0) - updated })}
                </Typography.Paragraph>
                <Table
                    columns={resultColumns}
                    data-testid="fill-result-table"
                    dataSource={results ?? []}
                    pagination={false}
                    rowKey="projectName"
                    size="small"
                />
            </>
        )
    const previewView = (
        <>
            {previews === null && <Skeleton active paragraph={{ rows: 4 }} title={false} />}
            {error && <Alert showIcon data-testid="fill-error" title={error} type="error" />}
            {previews?.length === 0 && !error && (
                <Empty data-testid="fill-empty" description={t('fill_preview.nothing_to_fill')} />
            )}
            {previews && previews.length > 0 && (
                <>
                    <Typography.Paragraph type="secondary">{t('fill_preview.legend')}</Typography.Paragraph>
                    <Table
                        columns={previewColumns}
                        data-testid="fill-preview-table"
                        dataSource={previews}
                        pagination={false}
                        rowKey="projectName"
                        size="small"
                    />
                </>
            )}
        </>
    )

    // Once the projects are filled, the window shows what happened to each of them and only closes.
    return (
        <Modal
            destroyOnHidden
            footer={filled ? closeOnlyFooter : undefined}
            okButtonProps={{ disabled: !filled && selected.length === 0, loading: filling }}
            okText={filled ? t('fill_result.close') : t('fill_preview.apply')}
            onCancel={onClose}
            onOk={filled ? onClose : fill}
            open={open}
            title={filled ? t('fill_result.title') : t('fill_preview.title')}
            width={800}
        >
            {filled ? resultView : previewView}
        </Modal>
    )
}

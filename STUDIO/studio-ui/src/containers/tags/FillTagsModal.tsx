import { useEffect, useState } from 'react'
import { Alert, Checkbox, Empty, Modal, Skeleton, Table, Tag as AntTag, Tooltip, Typography } from 'antd'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { ArrowRightOutlined } from '@ant-design/icons'
import { apiCall } from '../../services'
import { errorMessage } from '../../utils/errorMessage'

/** What filling does with one derived tag value, as the backend reports it. */
export type TagFillState = 'assign' | 'create' | 'rejected' | 'keep'

export interface TagFillItem {
    type: string
    current?: string
    derived: string
    state: TagFillState
}

export interface TagFillPreview {
    projectName: string
    modifiable: boolean
    tags: TagFillItem[]
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
}))

interface FillTagsModalProps {
    open: boolean
    onClose: () => void
    onFilled: (updated: number) => void
}

/**
 * The projects whose name matches a project name template and that miss a tag it derives, with what
 * filling would do to each tag — assign a configured value, create it for an extensible tag type, or
 * leave the project as it is. The user picks the projects to fill.
 */
export const FillTagsModal = ({ open, onClose, onFilled }: FillTagsModalProps) => {
    const { t } = useTranslation('tags')
    const { styles } = useStyles()
    const [previews, setPreviews] = useState<TagFillPreview[] | null>(null)
    const [selected, setSelected] = useState<string[]>([])
    const [filling, setFilling] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!open) {
            return
        }
        setPreviews(null)
        setSelected([])
        setError(null)
        apiCall('/admin/tag-config/fill/preview', { method: 'GET' }, { throwError: true })
            .then((result: unknown) => {
                const rows = (result ?? []) as TagFillPreview[]
                setPreviews(rows)
                setSelected(rows.filter(row => row.modifiable).map(row => row.projectName))
            })
            .catch(e => {
                setPreviews([])
                setError(errorMessage(e))
            })
    }, [open])

    const fill = async () => {
        setFilling(true)
        setError(null)
        try {
            const result = await apiCall('/admin/tag-config/fill', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(selected),
            }, { throwError: true }) as { updated?: number } | undefined
            onFilled(result?.updated ?? 0)
            onClose()
        } catch (e) {
            setError(errorMessage(e))
        } finally {
            setFilling(false)
        }
    }

    // Hoisted out of the cell renderer, so the update chain never nests beyond what reads clearly.
    const toggleSelected = (projectName: string, checked: boolean) => setSelected(prev => checked
        ? [...prev, projectName]
        : prev.filter(name => name !== projectName))

    const columns = [
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
                    <Tooltip title={row.modifiable ? undefined : t('fill_preview.not_modifiable')}>
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
                    {tags.map(tag => (
                        <span key={tag.type} className={styles.row}>
                            <Typography.Text className={styles.type}>{tag.type}:</Typography.Text>
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
                        </span>
                    ))}
                </div>
            ),
        },
    ]

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ disabled: selected.length === 0, loading: filling }}
            okText={t('fill_preview.apply')}
            onCancel={onClose}
            onOk={fill}
            open={open}
            title={t('fill_preview.title')}
            width={800}
        >
            {previews === null && <Skeleton active paragraph={{ rows: 4 }} title={false} />}
            {error && <Alert showIcon data-testid="fill-error" title={error} type="error" />}
            {previews?.length === 0 && !error && (
                <Empty data-testid="fill-empty" description={t('fill_preview.nothing_to_fill')} />
            )}
            {previews && previews.length > 0 && (
                <>
                    <Typography.Paragraph type="secondary">{t('fill_preview.legend')}</Typography.Paragraph>
                    <Table
                        columns={columns}
                        data-testid="fill-preview-table"
                        dataSource={previews}
                        pagination={false}
                        rowKey="projectName"
                        size="small"
                    />
                </>
            )}
        </Modal>
    )
}

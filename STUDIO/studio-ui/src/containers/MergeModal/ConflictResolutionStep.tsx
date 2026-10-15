import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { App, Alert, Button, Descriptions, Input, Radio, Space, Spin, Table, Tooltip, Typography, Upload } from 'antd'
import {
    CheckCircleOutlined,
    DeleteOutlined,
    DownloadOutlined,
    EyeOutlined,
    FileOutlined,
    FolderOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall, CONFIG } from '../../services'
import { formatDateTime } from '../../utils/dateFormat'
import { saveFile } from '../../utils/download'
import { isWorkbookPath } from '../../utils/workbooks'
import { basename } from '../projects/projectPaths'
import {
    BranchInfo,
    ConflictDetails,
    ConflictFileState,
    ConflictGroup,
    ConflictResolutionStrategy,
    FileResolution,
    FileSide,
    MergedText,
    MergedWorkbook,
    MergeSummary,
    ResolveConflictsResponse,
    RevisionInfo,
} from './types'
import { MergeBranchLabel } from './MergeBranchLabel'
import { TextMergeEditor } from './TextMergeEditor'
import { WorkbookMergeEditor } from './WorkbookMergeEditor'
import { useStyles } from './ConflictResolutionStep.styles'

const { TextArea } = Input

/**
 * The resolution of a file merged in the dialog, a text file line by line or a workbook table by table; it is sent as
 * a merged file.
 */
const MERGE_IN_PLACE = 'MERGE' satisfies FileResolution

/** The file a resolution sends: the one uploaded, the text of a file merged line by line, or a merged workbook. */
const fileOf = (res: ConflictFileState): File | undefined => {
    if (res.resolution === MERGE_IN_PLACE && res.merged) {
        return new File([res.merged.text], basename(res.filePath), { type: 'text/plain' })
    }
    if (res.resolution === MERGE_IN_PLACE && res.mergedWorkbook) {
        return new File([res.mergedWorkbook.file], basename(res.filePath), { type: res.mergedWorkbook.file.type })
    }
    return res.resolution === 'CUSTOM' ? res.customFile : undefined
}

interface ConflictResolutionStepProps {
    projectId: string
    conflictGroups: ConflictGroup[]
    /** The branches of the project, so a branch carries its marks here as it does everywhere else. */
    branches: BranchInfo[]
    /** Called once the conflicts are resolved, with what the merge changed when the server tells it. */
    onResolveSuccess: (summary?: MergeSummary) => void
    onCancel: () => void
    onCompare: (filePath: string) => void
}

export const ConflictResolutionStep: React.FC<ConflictResolutionStepProps> = ({
    projectId,
    conflictGroups: initialConflictGroups,
    branches,
    onResolveSuccess,
    onCancel,
    onCompare,
}) => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { styles } = useStyles()

    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Conflict details from API
    const [conflictDetails, setConflictDetails] = useState<ConflictDetails | null>(null)

    // User-entered merge message and original default
    const [mergeMessage, setMergeMessage] = useState('')
    const [defaultMessage, setDefaultMessage] = useState('')

    // Resolution state for each file
    const [resolutions, setResolutions] = useState<Record<string, ConflictFileState>>({})
    // The text file being merged line by line, if any
    const [editing, setEditing] = useState<string | null>(null)

    const loadConflictDetails = useCallback(async () => {
        setIsLoading(true)
        setError(null)

        try {
            // API returns ConflictDetails with revision info and default message
            const details: ConflictDetails = await apiCall(
                `/projects/${projectId}/merge/conflicts`,
                { method: 'GET' },
                true
            )

            setConflictDetails(details)
            setMergeMessage(details.defaultMessage || '')
            setDefaultMessage(details.defaultMessage || '')

            // Initialize resolutions for all files
            const initialResolutions: Record<string, ConflictFileState> = {}
            details.conflictGroups.forEach(group => {
                group.files.forEach(filePath => {
                    initialResolutions[filePath] = {
                        filePath,
                        resolution: null,
                    }
                })
            })
            setResolutions(initialResolutions)
        } catch (err: any) {
            setError(err?.message || t('merge:errors.load_failed'))
        } finally {
            setIsLoading(false)
        }
    }, [projectId, t])

    // Load conflict details on mount or when projectId changes
    useEffect(() => {
        void loadConflictDetails()
    }, [loadConflictDetails])

    // Flatten all files for table
    const allFiles = useMemo(() => {
        const groups = conflictDetails?.conflictGroups || initialConflictGroups
        return groups.flatMap(group =>
            group.files.map(filePath => ({
                key: filePath,
                filePath,
                fileName: filePath.split('/').pop() || filePath,
                projectName: group.projectName,
            }))
        )
    }, [conflictDetails, initialConflictGroups])

    // Check if all conflicts are resolved
    const allResolved = useMemo(() => {
        return allFiles.every(file => {
            const res = resolutions[file.filePath]
            if (!res?.resolution) return false
            if (res.resolution === 'CUSTOM' && !res.customFile) return false
            return true
        })
    }, [allFiles, resolutions])

    const handleResolutionChange = (filePath: string, strategy: ConflictResolutionStrategy) => {
        setResolutions(prev => ({
            ...prev,
            [filePath]: {
                ...prev[filePath],
                filePath,
                resolution: strategy,
                customFile: strategy === 'CUSTOM' ? prev[filePath]?.customFile : undefined,
                merged: undefined,
                mergedWorkbook: undefined,
            },
        }))
    }

    const handleFileUpload = (filePath: string, file: File) => {
        setResolutions(prev => ({
            ...prev,
            [filePath]: {
                ...prev[filePath],
                filePath,
                resolution: 'CUSTOM',
                customFile: file,
                merged: undefined,
                mergedWorkbook: undefined,
            },
        }))
        return false // Prevent default upload behavior
    }

    /** Takes a text file merged line by line as its resolution. */
    const handleMerged = (filePath: string, merged: MergedText) => {
        setResolutions(prev => ({ ...prev, [filePath]: { filePath, resolution: MERGE_IN_PLACE, merged } }))
        setEditing(null)
    }

    /** Takes a workbook merged table by table as its resolution. */
    const handleMergedWorkbook = (filePath: string, mergedWorkbook: MergedWorkbook) => {
        setResolutions(prev => ({ ...prev, [filePath]: { filePath, resolution: MERGE_IN_PLACE, mergedWorkbook } }))
        setEditing(null)
    }

    const handleDownload = (filePath: string, side: FileSide) => {
        const url = `${CONFIG.API_ROOT}/projects/${projectId}/merge/conflicts/files?file=${encodeURIComponent(filePath)}&side=${side}`
        window.open(url, '_blank')
    }

    const renderDownloadAction = (
        filePath: string,
        side: FileSide,
        exists: boolean | undefined,
        downloadLabel: string,
        deletedLabel: string
    ) => {
        if (exists === undefined) return null

        return exists ? (
            <Button
                icon={<DownloadOutlined />}
                onClick={() => handleDownload(filePath, side)}
                size="small"
                type="link"
            >
                {downloadLabel}
            </Button>
        ) : (
            <Typography.Text
                className={styles.deletedFileStatus}
                type="secondary"
            >
                <DeleteOutlined />
                {deletedLabel}
            </Typography.Text>
        )
    }

    const handleSave = async () => {
        if (!allResolved) {
            notification.warning({
                title: t('merge:errors.all_conflicts_required'),
            })
            return
        }

        setIsSaving(true)
        setError(null)

        try {
            // Build FormData for multipart request
            const formData = new FormData()

            // Add resolutions as indexed form fields
            const resolutionEntries = Object.values(resolutions)
            resolutionEntries.forEach((res, index) => {
                formData.append(`resolutions[${index}].filePath`, res.filePath)
                // A file merged line by line is sent as a merged file.
                formData.append(`resolutions[${index}].strategy`,
                    res.resolution === MERGE_IN_PLACE ? 'CUSTOM' : res.resolution!)
            })

            // Add merge message only if user modified it from default
            if (mergeMessage && mergeMessage !== defaultMessage) {
                formData.append('message', mergeMessage)
            }

            // Add custom files
            resolutionEntries.forEach((res, index) => {
                const file = fileOf(res)
                if (file) {
                    formData.append(`resolutions[${index}].file`, file)
                }
            })

            const response = await apiCall(
                `/projects/${projectId}/merge/conflicts/resolve`,
                {
                    method: 'POST',
                    body: formData,
                },
                true
            ) as ResolveConflictsResponse | undefined

            onResolveSuccess(response?.summary)
        } catch (err: any) {
            setError(err?.message || t('merge:errors.resolve_failed'))
        } finally {
            setIsSaving(false)
        }
    }

    const handleCancel = async () => {
        try {
            await apiCall(
                `/projects/${projectId}/merge/conflicts`,
                { method: 'DELETE' },
                true
            )
            notification.info({
                title: t('merge:notifications.merge_cancelled'),
                description: t('merge:notifications.merge_cancelled_description'),
            })
        } catch (_err) {
            // Ignore cancel errors
        }
        onCancel()
    }

    /** The branch a version comes from, marked as the dialog marks it; nothing for a version of no branch. */
    const branchLabelOf = (revision: RevisionInfo | undefined, testId?: string) => revision?.branch && (
        <MergeBranchLabel truncate branches={branches} name={revision.branch} testId={testId} />
    )

    /** What names your and their version in a merge window: their branches. */
    const versionLabels = {
        ours: branchLabelOf(conflictDetails?.oursRevision),
        theirs: branchLabelOf(conflictDetails?.theirsRevision),
    }

    const renderRevisionInfo = (label: string, revision: RevisionInfo | undefined) => {
        if (!revision) return null

        return (
            <Descriptions.Item label={label}>
                {revision.exists ? (
                    <Tooltip title={t('merge:revisions.commit', { commit: revision.commit })}>
                        <Space orientation="vertical" size={0}>
                            {branchLabelOf(revision, `conflict-branch-${revision.branch}`)}
                            {revision.author && (
                                <Typography.Text type="secondary">
                                    {t('merge:revisions.by', { author: revision.author })}
                                </Typography.Text>
                            )}
                            {revision.modifiedAt && (
                                <Typography.Text type="secondary">
                                    {t('merge:revisions.at', { date: formatDateTime(revision.modifiedAt) ?? '' })}
                                </Typography.Text>
                            )}
                        </Space>
                    </Tooltip>
                ) : (
                    <Typography.Text type="secondary">
                        {t('merge:revisions.not_exists')}
                    </Typography.Text>
                )}
            </Descriptions.Item>
        )
    }

    const columns = [
        {
            title: t('merge:conflicts.file_column'),
            dataIndex: 'fileName',
            key: 'fileName',
            render: (fileName: string, record: { filePath: string }) => (
                <Tooltip title={record.filePath}>
                    <Space>
                        <FileOutlined />
                        <Typography.Text>{fileName}</Typography.Text>
                    </Space>
                </Tooltip>
            ),
        },
        {
            title: t('merge:conflicts.compare_column'),
            key: 'compare',
            width: 200,
            render: (_: any, record: { filePath: string }) => {
                const availability = conflictDetails?.fileAvailability[record.filePath]
                return (
                    <Space orientation="vertical" size="small">
                        <Button
                            icon={<EyeOutlined />}
                            onClick={() => onCompare(record.filePath)}
                            size="small"
                            type="link"
                        >
                            {t('merge:compare.title')}
                        </Button>
                        {renderDownloadAction(
                            record.filePath,
                            'ours',
                            availability?.ours,
                            t('merge:compare.download_yours'),
                            t('merge:compare.deleted_yours')
                        )}
                        {renderDownloadAction(
                            record.filePath,
                            'base',
                            availability?.base,
                            t('merge:compare.download_base'),
                            t('merge:compare.deleted_base')
                        )}
                        {renderDownloadAction(
                            record.filePath,
                            'theirs',
                            availability?.theirs,
                            t('merge:compare.download_theirs'),
                            t('merge:compare.deleted_theirs')
                        )}
                    </Space>
                )
            },
        },
        {
            title: t('merge:conflicts.resolution_column'),
            key: 'resolution',
            width: 350,
            render: (_: any, record: { filePath: string }) => {
                const res = resolutions[record.filePath]
                const isResolved = res?.resolution && (res.resolution !== 'CUSTOM' || res.customFile)
                // A text file both versions hold is merged line by line; the server tells text from a binary file. A
                // workbook is merged table by table against its base version, so every version has to hold it.
                const availability = conflictDetails?.fileAvailability[record.filePath]
                const workbook = isWorkbookPath(record.filePath)
                const mergeable = !!availability?.ours && !!availability.theirs
                    && (workbook ? availability.base : availability.text)

                return (
                    <Space orientation="vertical" size="small">
                        <Radio.Group
                            value={res?.resolution}
                            onChange={e => e.target.value === MERGE_IN_PLACE
                                ? setEditing(record.filePath)
                                : handleResolutionChange(record.filePath, e.target.value)}
                        >
                            <Space orientation="vertical">
                                <Radio value="OURS">{t('merge:resolution.use_yours')}</Radio>
                                <Radio value="THEIRS">{t('merge:resolution.use_theirs')}</Radio>
                                <Radio value="BASE">{t('merge:resolution.use_base')}</Radio>
                                {mergeable && (
                                    <Radio data-testid="merge-in-place" value={MERGE_IN_PLACE}>
                                        {t(workbook ? 'merge:resolution.merge_tables' : 'merge:resolution.merge_in_place')}
                                    </Radio>
                                )}
                                <Radio value="CUSTOM">
                                    <Space>
                                        {t('merge:resolution.upload_custom')}
                                        {res?.resolution === 'CUSTOM' && (
                                            <Upload
                                                beforeUpload={file => handleFileUpload(record.filePath, file)}
                                                maxCount={1}
                                                showUploadList={false}
                                            >
                                                <Button icon={<UploadOutlined />} size="small">
                                                    {t('merge:upload.select_file')}
                                                </Button>
                                            </Upload>
                                        )}
                                    </Space>
                                </Radio>
                            </Space>
                        </Radio.Group>
                        {res?.resolution === MERGE_IN_PLACE && (
                            <Space size="small">
                                <Typography.Text type="success">
                                    <CheckCircleOutlined />{' '}
                                    {t(workbook ? 'merge:resolution.merged_tables' : 'merge:resolution.merged_in_place')}
                                </Typography.Text>
                                <Button
                                    data-testid="merge-in-place-edit"
                                    onClick={() => setEditing(record.filePath)}
                                    size="small"
                                    type="link"
                                >
                                    {t('merge:resolution.edit_merge')}
                                </Button>
                                {res.mergedWorkbook && (
                                    <Button
                                        data-testid="merge-in-place-download"
                                        icon={<DownloadOutlined />}
                                        onClick={() => saveFile(res.mergedWorkbook!.file, basename(record.filePath))}
                                        size="small"
                                        type="link"
                                    >
                                        {t('merge:resolution.download_merged')}
                                    </Button>
                                )}
                            </Space>
                        )}
                        {res?.customFile && (
                            <Typography.Text type="success">
                                <CheckCircleOutlined /> {res.customFile.name}
                            </Typography.Text>
                        )}
                        {isResolved && res?.resolution !== 'CUSTOM' && res?.resolution !== MERGE_IN_PLACE && (
                            <Typography.Text type="success">
                                <CheckCircleOutlined /> {t('merge:resolution.resolved')}
                            </Typography.Text>
                        )}
                    </Space>
                )
            },
        },
    ]

    if (isLoading) {
        return (
            <div style={{ textAlign: 'center', padding: 48 }}>
                <Spin size="large" />
            </div>
        )
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '70vh' }}>
            {error && (
                <Alert
                    showIcon
                    style={{ marginBottom: 16 }}
                    title={error}
                    type="error"
                />
            )}
            {/* Revision Information - only show if available */}
            {(conflictDetails?.oursRevision || conflictDetails?.theirsRevision || conflictDetails?.baseRevision) && (
                <Descriptions bordered column={3} size="small" style={{ marginBottom: 16 }}>
                    {/* The versions stand as merge tools lay them out: yours, the base, theirs. */}
                    {renderRevisionInfo(t('merge:revisions.yours'), conflictDetails?.oursRevision)}
                    {renderRevisionInfo(t('merge:revisions.base'), conflictDetails?.baseRevision)}
                    {renderRevisionInfo(t('merge:revisions.theirs'), conflictDetails?.theirsRevision)}
                </Descriptions>
            )}
            {/* Merge Message */}
            <div style={{ marginBottom: 16 }}>
                <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
                    {t('merge:conflicts.message_label')}
                </Typography.Text>
                <TextArea
                    autoSize={{ minRows: 2, maxRows: 4 }}
                    onChange={e => setMergeMessage(e.target.value)}
                    placeholder={t('merge:conflicts.message_placeholder')}
                    value={mergeMessage}
                />
            </div>
            {/* Conflict Files grouped by Project - Scrollable */}
            <div style={{ flex: 1, overflow: 'auto', marginBottom: 16, minHeight: 200 }}>
                <Typography.Text strong className={styles.conflictsHeading}>
                    {t('merge:conflicts.description')}
                </Typography.Text>
                {(conflictDetails?.conflictGroups || initialConflictGroups).map(group => {
                    const groupFiles = allFiles.filter(f => f.projectName === group.projectName)
                    return (
                        <div key={group.projectName} style={{ marginBottom: 16 }}>
                            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
                                <FolderOutlined style={{ marginRight: 8 }} />
                                {group.projectName}
                            </Typography.Text>
                            <Table
                                columns={columns}
                                dataSource={groupFiles}
                                pagination={false}
                                size="small"
                            />
                        </div>
                    )
                })}
            </div>
            {/* Actions - Fixed at bottom */}
            <Space className={styles.actions}>
                <Button
                    disabled={isSaving}
                    onClick={handleCancel}
                >
                    {t('merge:buttons.cancel')}
                </Button>
                <Button
                    disabled={!allResolved || isSaving}
                    loading={isSaving}
                    onClick={handleSave}
                    type="primary"
                >
                    {t('merge:buttons.resolve')}
                </Button>
            </Space>
            {editing && !isWorkbookPath(editing) && (
                <TextMergeEditor
                    filePath={editing}
                    merged={resolutions[editing]?.merged}
                    onApply={merged => handleMerged(editing, merged)}
                    onCancel={() => setEditing(null)}
                    projectId={projectId}
                    versionLabels={versionLabels}
                />
            )}
            {editing && isWorkbookPath(editing) && (
                <WorkbookMergeEditor
                    filePath={editing}
                    merged={resolutions[editing]?.mergedWorkbook}
                    onApply={merged => handleMergedWorkbook(editing, merged)}
                    onCancel={() => setEditing(null)}
                    projectId={projectId}
                    versionLabels={versionLabels}
                />
            )}
        </div>
    )
}

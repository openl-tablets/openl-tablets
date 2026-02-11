import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
    Alert,
    Button,
    Descriptions,
    Input,
    notification,
    Radio,
    Space,
    Spin,
    Table,
    Tooltip,
    Typography,
    Upload,
} from 'antd'
import {
    CheckCircleOutlined,
    DownloadOutlined,
    EyeOutlined,
    FileOutlined,
    FolderOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall, CONFIG } from '../../services'
import {
    ConflictDetails,
    ConflictFileState,
    ConflictGroup,
    ConflictResolutionStrategy,
    FileSide,
    RevisionInfo,
} from './types'

const { TextArea } = Input

interface ConflictResolutionStepProps {
    projectId: string
    conflictGroups: ConflictGroup[]
    onResolveSuccess: () => void
    onCancel: () => void
    onCompare: (filePath: string) => void
}

export const ConflictResolutionStep: React.FC<ConflictResolutionStepProps> = ({
    projectId,
    conflictGroups: initialConflictGroups,
    onResolveSuccess,
    onCancel,
    onCompare,
}) => {
    const { t } = useTranslation()

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
        loadConflictDetails()
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
                // Clear custom file if switching away from CUSTOM
                customFile: strategy === 'CUSTOM' ? prev[filePath]?.customFile : undefined,
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
            },
        }))
        return false // Prevent default upload behavior
    }

    const handleDownload = async (filePath: string, side: FileSide) => {
        const url = `${CONFIG.CONTEXT}/web/projects/${projectId}/merge/conflicts/files?file=${encodeURIComponent(filePath)}&side=${side}`
        window.open(url, '_blank')
    }

    const handleSave = async () => {
        if (!allResolved) {
            notification.warning({
                message: t('merge:errors.all_conflicts_required'),
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
                formData.append(`resolutions[${index}].strategy`, res.resolution!)
            })

            // Add merge message only if user modified it from default
            if (mergeMessage && mergeMessage !== defaultMessage) {
                formData.append('message', mergeMessage)
            }

            // Add custom files
            resolutionEntries.forEach((res, index) => {
                if (res.resolution === 'CUSTOM' && res.customFile) {
                    formData.append(`resolutions[${index}].file`, res.customFile)
                }
            })

            await apiCall(
                `/projects/${projectId}/merge/conflicts/resolve`,
                {
                    method: 'POST',
                    body: formData,
                },
                true
            )

            notification.success({
                message: t('merge:notifications.resolve_success'),
                description: t('merge:notifications.resolve_success_description'),
            })

            onResolveSuccess()
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
                message: t('merge:notifications.merge_cancelled'),
                description: t('merge:notifications.merge_cancelled_description'),
            })
        } catch (_err) {
            // Ignore cancel errors
        }
        onCancel()
    }

    const formatDateTime = (isoDate: string | null): string => {
        if (!isoDate) return ''
        try {
            return new Date(isoDate).toLocaleString()
        } catch {
            return isoDate
        }
    }

    const renderRevisionInfo = (label: string, revision: RevisionInfo | undefined) => {
        if (!revision) return null

        return (
            <Descriptions.Item label={label}>
                {revision.exists ? (
                    <Tooltip title={t('merge:revisions.commit', { commit: revision.commit })}>
                        <Space direction="vertical" size={0}>
                            <Typography.Text strong>{revision.branch}</Typography.Text>
                            {revision.author && (
                                <Typography.Text type="secondary">
                                    {t('merge:revisions.by', { author: revision.author })}
                                </Typography.Text>
                            )}
                            {revision.modifiedAt && (
                                <Typography.Text type="secondary">
                                    {t('merge:revisions.at', { date: formatDateTime(revision.modifiedAt) })}
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
            render: (_: any, record: { filePath: string }) => (
                <Space direction="vertical" size="small">
                    <Button
                        icon={<EyeOutlined />}
                        onClick={() => onCompare(record.filePath)}
                        size="small"
                        type="link"
                    >
                        {t('merge:compare.title')}
                    </Button>
                    <Button
                        icon={<DownloadOutlined />}
                        onClick={() => handleDownload(record.filePath, 'OURS')}
                        size="small"
                        type="link"
                    >
                        {t('merge:compare.download_yours')}
                    </Button>
                    <Button
                        icon={<DownloadOutlined />}
                        onClick={() => handleDownload(record.filePath, 'THEIRS')}
                        size="small"
                        type="link"
                    >
                        {t('merge:compare.download_theirs')}
                    </Button>
                    <Button
                        icon={<DownloadOutlined />}
                        onClick={() => handleDownload(record.filePath, 'BASE')}
                        size="small"
                        type="link"
                    >
                        {t('merge:compare.download_base')}
                    </Button>
                </Space>
            ),
        },
        {
            title: t('merge:conflicts.resolution_column'),
            key: 'resolution',
            width: 350,
            render: (_: any, record: { filePath: string }) => {
                const res = resolutions[record.filePath]
                const isResolved = res?.resolution && (res.resolution !== 'CUSTOM' || res.customFile)

                return (
                    <Space direction="vertical" size="small">
                        <Radio.Group
                            onChange={e => handleResolutionChange(record.filePath, e.target.value)}
                            value={res?.resolution}
                        >
                            <Space direction="vertical">
                                <Radio value="OURS">{t('merge:resolution.use_yours')}</Radio>
                                <Radio value="THEIRS">{t('merge:resolution.use_theirs')}</Radio>
                                <Radio value="BASE">{t('merge:resolution.use_base')}</Radio>
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
                        {res?.customFile && (
                            <Typography.Text type="success">
                                <CheckCircleOutlined /> {res.customFile.name}
                            </Typography.Text>
                        )}
                        {isResolved && !res?.customFile && res?.resolution !== 'CUSTOM' && (
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
                    {renderRevisionInfo(t('merge:revisions.yours'), conflictDetails?.oursRevision)}
                    {renderRevisionInfo(t('merge:revisions.theirs'), conflictDetails?.theirsRevision)}
                    {renderRevisionInfo(t('merge:revisions.base'), conflictDetails?.baseRevision)}
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
                <Typography.Text strong style={{ display: 'block', marginBottom: 8, position: 'sticky', top: 0, background: '#fff', zIndex: 1, paddingBottom: 8 }}>
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
            <Space style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: 8, borderTop: '1px solid #f0f0f0' }}>
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
        </div>
    )
}

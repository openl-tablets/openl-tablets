import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Empty, Modal, notification, Select, Skeleton } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import type { ProjectRevision } from '../../services/repositories'
import {
    listProjectRevisionExcelFiles,
    openProjectRevisionFileCompare,
} from './projectRevisionCompare'

interface ProjectRevisionCompareModalProps {
    open: boolean
    projectId: string
    fromRevision: ProjectRevision | null
    toRevision: ProjectRevision | null
    onClose: () => void
}

export const ProjectRevisionCompareModal = ({
    open,
    projectId,
    fromRevision,
    toRevision,
    onClose,
}: ProjectRevisionCompareModalProps) => {
    const { t } = useTranslation('repository')
    const [files, setFiles] = useState<string[] | 'loading' | 'error'>('loading')
    const [selectedPath, setSelectedPath] = useState<string | null>(null)
    const [comparing, setComparing] = useState(false)

    useEffect(() => {
        if (!open || !fromRevision || !toRevision) {
            return
        }
        let cancelled = false
        setFiles('loading')
        setSelectedPath(null)
        listProjectRevisionExcelFiles(projectId, fromRevision.revisionNo, toRevision.revisionNo)
            .then(paths => {
                if (cancelled) {
                    return
                }
                setFiles(paths)
                setSelectedPath(paths[0] ?? null)
            })
            .catch(() => {
                if (!cancelled) {
                    setFiles('error')
                }
            })
        return () => {
            cancelled = true
        }
    }, [open, projectId, fromRevision, toRevision])

    const compare = async () => {
        if (!selectedPath || !fromRevision || !toRevision) {
            return
        }
        setComparing(true)
        try {
            await openProjectRevisionFileCompare(projectId, selectedPath, fromRevision.revisionNo, toRevision.revisionNo)
            onClose()
        } catch (error) {
            notification.error({
                title: t('browser.history.compare_failed'),
                description: errorMessage(error),
            })
        } finally {
            setComparing(false)
        }
    }

    const options = Array.isArray(files)
        ? files.map(path => ({ value: path, label: path }))
        : []

    return (
        <Modal
            destroyOnHidden
            cancelText={t('common:btn.cancel')}
            confirmLoading={comparing}
            okText={t('browser.history.compare')}
            onCancel={onClose}
            onOk={compare}
            open={open}
            title={t('browser.history.compare')}
            okButtonProps={{
                disabled: !selectedPath,
                'data-testid': 'revision-compare-submit',
            }}
        >
            {files === 'loading' && <Skeleton active paragraph={{ rows: 4 }} />}
            {files === 'error' && (
                <Alert showIcon data-testid="revision-compare-error" title={t('browser.history.compare_failed')} type="error" />
            )}
            {Array.isArray(files) && files.length === 0 && (
                <Empty
                    data-testid="revision-compare-empty"
                    description={t('browser.history.compare_no_files')}
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
            )}
            {Array.isArray(files) && files.length > 0 && (
                <Select
                    data-testid="revision-compare-file"
                    onChange={setSelectedPath}
                    options={options}
                    style={{ width: '100%' }}
                    value={selectedPath}
                />
            )}
        </Modal>
    )
}

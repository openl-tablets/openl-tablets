import React from 'react'
import { Alert, Button, Collapse, Space, Tag, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { ChangedFileList } from '../projects/ChangedFileList'
import { openFileRevisionsCompareWindow } from '../projects/compare'
import { toProjectRelativePath } from '../projects/fileChanges'
import { ProjectHeader } from './MergeScopeTabs'
import type { FileChange, MergeSummary, ProjectChanges, SheetChange } from './types'

interface MergeSummaryStepProps {
    summary: MergeSummary
    /** Whether the merge was completed by resolving its conflicts. */
    resolved: boolean
    onClose: () => void
}

/**
 * What a merge changed in the target branch, grouped by project, each file by its path from the root of the
 * repository.
 *
 * A workbook Sync merged by itself lists the sheets it took from each branch. A file the merge modified can be
 * compared before and after the merge, in the window every comparison opens in.
 */
export const MergeSummaryStep: React.FC<MergeSummaryStepProps> = ({ summary, resolved, onClose }) => {
    const { t } = useTranslation()
    const branches = { source: summary.sourceBranch, target: summary.targetBranch }
    return (
        <Space orientation="vertical" size="middle" style={{ width: '100%', paddingTop: 8 }}>
            <Alert
                showIcon
                data-testid="merge-summary-result"
                title={resolved ? t('merge:summary.resolved', branches) : t('merge:summary.merged', branches)}
                type="success"
            />
            {summary.projects.length === 0 ? (
                <Typography.Text type="secondary">
                    {summary.hiddenProjects > 0
                        ? t('merge:summary.hidden_only', { count: summary.hiddenProjects })
                        : t('merge:summary.nothing')}
                </Typography.Text>
            ) : (
                // A merge of many projects scrolls here, so the dialog and its button stay in the window.
                <div data-testid="merge-summary-scroll" style={{ maxHeight: '55vh', overflowY: 'auto' }}>
                    <Collapse
                        data-testid="merge-summary-projects"
                        defaultActiveKey={summary.projects.filter(project => project.current).map(keyOf)}
                        size="small"
                        items={summary.projects.map(project => ({
                            key: keyOf(project),
                            label: <ProjectHeader changedFiles={project.files.length} project={project} />,
                            children: <FileList project={project} summary={summary} />,
                        }))}
                    />
                </div>
            )}
            {summary.projects.length > 0 && summary.hiddenProjects > 0 && (
                <Typography.Text data-testid="merge-summary-hidden" type="secondary">
                    {t('merge:scope.hidden', { count: summary.hiddenProjects })}
                </Typography.Text>
            )}
            <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button data-testid="merge-summary-close" onClick={onClose} type="primary">
                    {t('merge:summary.close')}
                </Button>
            </Space>
        </Space>
    )
}

const keyOf = (project: ProjectChanges): string => project.path ?? ''

const FileList: React.FC<{ project: ProjectChanges, summary: MergeSummary }> = ({ project, summary }) => {
    const { t } = useTranslation()
    const { id, path } = project
    const renderSheets = (file: FileChange) => file.sheets && file.sheets.length > 0 && (
        <Tooltip title={t('merge:summary.sheets_hint')}>
            <Space wrap data-testid="merge-summary-sheets" size={4}>
                {file.sheets.map(sheet => (
                    <Tag key={sheet.name}>{sheetLabel(sheet, summary, t)}</Tag>
                ))}
            </Space>
        </Tooltip>
    )
    return (
        <ChangedFileList
            details={renderSheets}
            files={project.files}
            testId="merge-summary"
            // A file is compared through the project that holds it, at its two revisions on the branch the merge
            // changed: a workbook element by element, any other file line by line.
            compare={id && path
                ? {
                    hint: t('merge:summary.compare_hint'),
                    open: file => openFileRevisionsCompareWindow(id, toProjectRelativePath(file.path, path),
                        summary.targetBranch, summary.before, summary.after),
                }
                : undefined}
        />
    )
}

const sheetLabel = (
    sheet: SheetChange,
    summary: MergeSummary,
    t: TFunction
): string => {
    if (sheet.takenFrom === 'both') {
        return t('merge:summary.sheet_both', { sheet: sheet.name })
    }
    const branch = sheet.takenFrom === 'source' ? summary.sourceBranch : summary.targetBranch
    return t('merge:summary.sheet_from', { sheet: sheet.name, branch })
}

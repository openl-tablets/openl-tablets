import React from 'react'
import { Alert, Button, Collapse, Flex, Listy, Space, Tag, Tooltip, Typography } from 'antd'
import { DiffOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { FileChangeIcon } from '../projects/FileChangeIcon'
import { openFileRevisionsCompareWindow } from '../projects/compare'
import { FilePath, LIST_HEIGHT, ProjectHeader } from './MergeScopeTabs'
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
    // A file is compared through the project that holds it, at its two revisions on the branch the merge changed:
    // a workbook element by element, any other file line by line. A file added or deleted has one revision only.
    const compare = (file: FileChange) => {
        if (project.id && project.path) {
            openFileRevisionsCompareWindow(project.id, file.path.slice(project.path.length + 1), summary.targetBranch,
                summary.before, summary.after)
        }
    }
    const comparable = (file: FileChange) => !!project.id && file.type === 'modified'
    return (
        <Listy
            virtual
            height={LIST_HEIGHT}
            items={project.files}
            rowKey={file => file.path}
            itemRender={file => (
                <Flex vertical data-testid="merge-summary-file" gap={2} style={{ paddingBlock: 4 }}>
                    <Flex align="center" gap="small">
                        <FileChangeIcon title={t(`repository:browser.files.change.${file.type}`)} type={file.type} />
                        <FilePath path={file.path} />
                        {comparable(file) && (
                            <Tooltip title={t('merge:summary.compare_hint')}>
                                <Button
                                    data-testid="merge-summary-compare"
                                    icon={<DiffOutlined />}
                                    onClick={() => compare(file)}
                                    size="small"
                                    type="link"
                                >
                                    {t('merge:summary.compare')}
                                </Button>
                            </Tooltip>
                        )}
                    </Flex>
                    {file.sheets && file.sheets.length > 0 && (
                        <Tooltip title={t('merge:summary.sheets_hint')}>
                            <Space wrap data-testid="merge-summary-sheets" size={4}>
                                {file.sheets.map(sheet => (
                                    <Tag key={sheet.name}>{sheetLabel(sheet, summary, t)}</Tag>
                                ))}
                            </Space>
                        </Tooltip>
                    )}
                </Flex>
            )}
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

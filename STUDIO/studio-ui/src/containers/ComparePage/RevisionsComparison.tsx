import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Breadcrumb, Button, Card, Empty, Flex, Skeleton, Tooltip, Typography } from 'antd'
import { ArrowLeftOutlined, ArrowRightOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons'
import { getProject, type ProjectRevision } from 'services/repositories'
import { errorHandler } from 'utils/errorHandling'
import { formatDateTime, useDateFormats } from 'utils/dateFormat'
import type { ProjectFileChange } from 'services/projectStatus'
import { FileChangeIcon } from '../projects/FileChangeIcon'
import { CHANGED_FILES_PAGE_SIZE, ChangedFilePages, type LoadedChanges } from '../projects/RevisionChanges'
import { basename, parentFolder } from '../projects/projectPaths'
import { authorName, shortRevision } from '../projects/revisions'
import { useStyles } from './ComparePage.styles'

/** Two revisions of a project the window compares, as its address names them. */
export interface ComparedRevisions {
    projectId: string
    /** The branch holding the revisions, or nothing in a repository without branches. */
    branch: string | undefined
    /** The earlier revision. */
    base: string
    /** The later revision. */
    revision: string
}

/** The two revisions the address names, or null when the window was opened for something else. */
export const comparedRevisionsOf = (params: URLSearchParams): ComparedRevisions | null => {
    const projectId = params.get('projectId')
    const base = params.get('base')
    const revision = params.get('revision')
    return projectId && base && revision
        ? { projectId, branch: params.get('branch') ?? undefined, base, revision }
        : null
}

/**
 * The name of the project the revisions belong to, read once for the window; nothing until it is read, or when it
 * cannot be read.
 */
export const useProjectName = (projectId: string): string | null => {
    const [name, setName] = useState<string | null>(null)
    useEffect(() => {
        let cancelled = false
        getProject(projectId)
            .then(project => { if (!cancelled) setName(project.name) })
            .catch((error: unknown) => {
                // The window names the revisions without the project then; the reason is kept for support.
                const failure = error instanceof Error ? error : new Error(String(error))
                errorHandler.logError(failure, { message: `Cannot read the name of project ${projectId}: ${failure.message}` })
                if (!cancelled) setName(null)
            })
        return () => { cancelled = true }
    }, [projectId])
    return name
}

/** The page of the list a file is on. */
export const pageOf = (index: number): number => Math.floor(index / CHANGED_FILES_PAGE_SIZE) + 1

/** What the window knows of a revision: undefined while the changes are read, null when they cannot be. */
type RevisionDetails = ProjectRevision | null | undefined

interface RevisionCardProps {
    /** Which of the two the revision is. */
    label: string
    /** The number of the revision, as the address names it. */
    number: string
    details: RevisionDetails
    testId: string
}

/** One of the two revisions compared, as the history shows it: why it was made, who made it, when, and its number. */
const RevisionCard: React.FC<RevisionCardProps> = ({ label, number, details, testId }) => {
    // The date is written in the formats of the reader, and again the moment the reader changes them.
    useDateFormats()
    const subject = details?.fullComment.split(/\r?\n/)[0]
    return (
        <Card data-testid={testId} size="small" style={{ flex: '1 1 0', minWidth: 0 }}>
            <Flex vertical gap={2}>
                <Typography.Text type="secondary">{label}</Typography.Text>
                {details === undefined && <Skeleton active paragraph={{ rows: 1 }} title={false} />}
                {subject && <Typography.Text strong ellipsis={{ tooltip: details?.fullComment }}>{subject}</Typography.Text>}
                {details !== undefined && (
                    <Flex wrap gap="small">
                        {details && <Typography.Text type="secondary">{authorName(details)}</Typography.Text>}
                        {details && (
                            <Typography.Text type="secondary">{formatDateTime(details.createdAt) ?? details.createdAt}</Typography.Text>
                        )}
                        <Typography.Text type="secondary">{shortRevision(number)}</Typography.Text>
                    </Flex>
                )}
            </Flex>
        </Card>
    )
}

/** The two revisions compared, as a short pair whose numbers tell why each was made when pointed at. */
const RevisionPair: React.FC<{ revisions: ComparedRevisions, changes: LoadedChanges }> = ({ revisions, changes }) => (
    <Flex align="center" data-testid="compare-revisions" gap="small">
        <Tooltip title={changes?.base?.fullComment}>
            <Typography.Text type="secondary">{shortRevision(revisions.base)}</Typography.Text>
        </Tooltip>
        <ArrowRightOutlined />
        <Tooltip title={changes?.revision.fullComment}>
            <Typography.Text type="secondary">{shortRevision(revisions.revision)}</Typography.Text>
        </Tooltip>
    </Flex>
)

/** Where a compared file stands among the changed files, and the way to its neighbours. */
interface FileSteps {
    /** The place of the file among the files the list shows, from 0; -1 when the list does not show it. */
    index: number
    total: number
    onStep: (by: -1 | 1) => void
}

interface RevisionsHeadProps {
    revisions: ComparedRevisions
    projectName: string | null
    changes: LoadedChanges
    /** The file compared. */
    file: ProjectFileChange
    onBack: () => void
    steps: FileSteps
}

/**
 * What the window compares while a file is: the way back to the changed files, the project and the file, its
 * neighbours among the changed files, and the two revisions.
 */
export const RevisionsHead: React.FC<RevisionsHeadProps> = ({ revisions, projectName, changes, file, onBack, steps }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const folder = parentFolder(file.path)
    const folders = folder ? folder.split('/') : []
    const known = steps.index >= 0
    return (
        <div className={styles.conflictHead} data-testid="compare-revisions-head">
            <Tooltip title={t('back_to_changed_files')}>
                <Button
                    aria-label={t('back_to_changed_files')}
                    data-testid="compare-revisions-back"
                    icon={<ArrowLeftOutlined />}
                    onClick={onBack}
                    type="text"
                />
            </Tooltip>
            <Breadcrumb
                style={{ flex: 1, minWidth: 0 }}
                items={[
                    { title: <Typography.Link onClick={onBack}>{projectName ?? t('changed_files_title')}</Typography.Link> },
                    ...folders.map(folder => ({ title: folder })),
                    {
                        title: (
                            <Flex align="center" data-testid="compare-revisions-file" gap="small">
                                <FileChangeIcon title={t(`repository:browser.files.change.${file.type}`)} type={file.type} />
                                <Typography.Text strong>{basename(file.path)}</Typography.Text>
                            </Flex>
                        ),
                    },
                ]}
            />
            {known && (
                <Flex align="center" gap="small">
                    <Tooltip title={t('previous_file')}>
                        <Button
                            aria-label={t('previous_file')}
                            data-testid="compare-revisions-previous"
                            disabled={steps.index === 0}
                            icon={<LeftOutlined />}
                            onClick={() => steps.onStep(-1)}
                            size="small"
                        />
                    </Tooltip>
                    <Typography.Text type="secondary">
                        {t('file_position', { index: steps.index + 1, total: steps.total })}
                    </Typography.Text>
                    <Tooltip title={t('next_file')}>
                        <Button
                            aria-label={t('next_file')}
                            data-testid="compare-revisions-next"
                            disabled={steps.index === steps.total - 1}
                            icon={<RightOutlined />}
                            onClick={() => steps.onStep(1)}
                            size="small"
                        />
                    </Tooltip>
                </Flex>
            )}
            <RevisionPair changes={changes} revisions={revisions} />
        </div>
    )
}

interface RevisionsComparisonProps {
    revisions: ComparedRevisions
    projectName: string | null
    changes: LoadedChanges
    page: number
    onPage: (page: number) => void
    /** The file compared last, which the list marks so the reader sees where they were. */
    selected: string | null
    /** Compares one of the changed files in the window. */
    onCompare: (file: ProjectFileChange) => void
}

/**
 * The files changed between two revisions of a project, the first step of comparing them: each file is compared in
 * the same window, a workbook element by element and any other file line by line.
 *
 * The head shows the two revisions side by side, each with why it was made, who made it and when, as the changes
 * tell them. The files are listed a page at a time, as the files a revision changed are listed in the history.
 */
export const RevisionsComparison: React.FC<RevisionsComparisonProps> = ({
    revisions,
    projectName,
    changes,
    page,
    onPage,
    selected,
    onCompare,
}) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()

    const body = () => {
        if (changes === undefined) {
            return <Skeleton active paragraph={{ rows: 4 }} title={false} />
        }
        if (changes === null) {
            return <Empty description={t('repository:browser.history.changes_between_error')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        if (changes.files.length === 0) {
            return <Empty description={t('repository:browser.history.no_changes_between')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return (
            <Flex vertical data-testid={`revision-changes-${revisions.revision}`} gap="small">
                <Typography.Text strong>{t('changed_files', { count: changes.files.length })}</Typography.Text>
                <ChangedFilePages
                    files={changes.files}
                    onPage={onPage}
                    page={page}
                    selected={selected}
                    // An added or a deleted file is compared too: the side that does not hold it is empty.
                    compare={{
                        everyFile: true,
                        hint: t('repository:browser.history.compare_between_hint'),
                        open: onCompare,
                    }}
                />
            </Flex>
        )
    }

    return (
        <div className={styles.page}>
            <Flex vertical className={styles.revisionsHead} data-testid="compare-revisions-summary" gap="middle">
                <Typography.Title level={5} style={{ margin: 0 }}>
                    {projectName ? t('revisions_title', { name: projectName }) : t('changed_files_title')}
                </Typography.Title>
                <Flex align="center" gap="middle">
                    <RevisionCard
                        // The failed read tells nothing of either revision, which keep their numbers.
                        details={changes === undefined ? undefined : changes?.base ?? null}
                        label={t('earlier_revision')}
                        number={revisions.base}
                        testId="compare-revisions-base"
                    />
                    <ArrowRightOutlined />
                    <RevisionCard
                        details={changes === undefined ? undefined : changes?.revision ?? null}
                        label={t('later_revision')}
                        number={revisions.revision}
                        testId="compare-revisions-revision"
                    />
                </Flex>
            </Flex>
            <div className={styles.changedFiles}>{body()}</div>
        </div>
    )
}

import React from 'react'
import { Alert, Flex, Listy, Space, Tabs, Tag, Tooltip, Typography } from 'antd'
import { CheckOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { ProjectFileChange, ProjectFileChangeType } from '../../services/projectStatus'
import { ChangedFileList, ChangesHeader, LIST_HEIGHT } from '../projects/ChangedFileList'
import { ConflictTag } from '../projects/ConflictTag'
import { openRevisionsCompareWindow } from '../projects/compare'
import { toProjectRelativePath } from '../projects/fileChanges'
import type { ChangedProject, CheckMergeResult, MergeScope, ProjectScope } from './types'

interface MergeScopeTabsProps {
    /** The check of the merge of the selected branch into the current one. */
    receive: CheckMergeResult | null
    /** The check of the merge of the current branch into the selected one. */
    send: CheckMergeResult | null
}

/** How many projects a merge changes, those the user may not read included; the files outside projects are none. */
const projectCount = (scope: MergeScope | undefined): number =>
    scope ? scope.projects.filter(project => project.name).length + scope.hiddenProjects : 0

/**
 * Whether what a direction brings in is known: nothing when the branches are up to date, the scope otherwise. A
 * check that failed, or a scope the server could not tell, leaves it unknown.
 */
const isKnown = (result: CheckMergeResult | null): boolean => result?.status === 'up-to-date' || !!result?.scope

/**
 * What each direction of a Sync would bring in, before any merge starts: one tab for receiving their updates and
 * one for sending yours.
 *
 * A branch holds every project, so a merge brings in the changes of other projects too. The current project is
 * listed first and the others are marked as other projects; the files both branches changed are named under
 * their project, each marked as a conflict or as merged by Sync itself. Sync tells beforehand whether it would stop
 * at conflicts: it tries the merge without merging anything. A file of a project is compared as a pull request
 * compares it, from the revision both branches parted at.
 */
export const MergeScopeTabs: React.FC<MergeScopeTabsProps> = ({ receive, send }) => {
    const { t } = useTranslation()
    // Your branch is the one the dialog was opened on: the target of a receive, the source of a send.
    const tab = (key: string, label: string, result: CheckMergeResult | null, yoursEarlier: boolean) => ({
        key,
        label: isKnown(result) ? t('merge:scope.tab', { label, count: projectCount(result?.scope) }) : label,
        children: <ScopeList result={result} yoursEarlier={yoursEarlier} />,
    })
    return (
        <Tabs
            data-testid="merge-scope"
            size="small"
            items={[
                tab('receive', t('merge:actions.receive'), receive, true),
                tab('send', t('merge:actions.send'), send, false),
            ]}
        />
    )
}

const ScopeList: React.FC<{ result: CheckMergeResult | null; yoursEarlier: boolean }> = ({ result, yoursEarlier }) => {
    const { t } = useTranslation()
    if (!isKnown(result)) {
        return (
            <Typography.Text data-testid="merge-scope-unknown" type="secondary">
                {t('merge:scope.unknown')}
            </Typography.Text>
        )
    }
    const scope = result?.scope
    if (!result || !scope || (scope.projects.length === 0 && scope.hiddenProjects === 0)) {
        return <Typography.Text type="secondary">{t('merge:scope.nothing')}</Typography.Text>
    }
    const reachesOthers = scope.hiddenProjects > 0 || scope.projects.some(project => project.name && !project.current)
    const renderProject = (project: ProjectScope) => (
        <Flex vertical data-testid="merge-scope-project" gap={2} style={{ paddingBlock: 4 }}>
            <ProjectHeader changedFiles={project.changedFiles} project={project} />
            <ChangedInBoth project={project} result={result} yoursEarlier={yoursEarlier} />
        </Flex>
    )
    return (
        <Space orientation="vertical" size="small" style={{ width: '100%' }}>
            <ConflictsNote scope={scope} />
            {reachesOthers && (
                <Alert showIcon data-testid="merge-scope-whole-branch" title={t('merge:scope.whole_branch')} type="info" />
            )}
            {scope.projects.length > 0 && (
                <Listy
                    virtual
                    height={LIST_HEIGHT}
                    itemRender={renderProject}
                    items={scope.projects}
                    rowKey={project => project.path ?? ''}
                />
            )}
            {scope.hiddenProjects > 0 && (
                // With no project to list, the count is a sentence of its own rather than the end of the list.
                <Typography.Text data-testid="merge-scope-hidden" type="secondary">
                    {scope.projects.length > 0
                        ? t('merge:scope.hidden', { count: scope.hiddenProjects })
                        : t('merge:scope.hidden_only', { count: scope.hiddenProjects })}
                </Typography.Text>
            )}
        </Space>
    )
}

/**
 * Whether Sync would stop at conflicts. Only conflicts in the projects the user may read are known, so a merge that
 * reaches a project the user may not read is never told free of them.
 */
const ConflictsNote: React.FC<{ scope: MergeScope }> = ({ scope }) => {
    const { t } = useTranslation()
    const count = scope.projects.reduce((sum, project) => sum + (project.conflicts?.length ?? 0), 0)
    if (count > 0) {
        return (
            <Alert
                showIcon
                data-testid="merge-scope-conflicts"
                title={t('merge:scope.conflicts', { count })}
                type="warning"
            />
        )
    }
    return scope.hiddenProjects === 0
        ? <Alert showIcon data-testid="merge-scope-no-conflicts" title={t('merge:scope.no_conflicts')} type="success" />
        : null
}

interface ChangedInBothProps {
    project: ProjectScope
    /** The check the project was told by: which branch it merges into which. */
    result: CheckMergeResult
    /** Whether your branch is the one merged into, as on receive: it then stands first in a comparison. */
    yoursEarlier: boolean
}

/**
 * The files of a project both branches changed, each marked as a possible conflict or as merged by Sync itself.
 *
 * A file of a project is compared in the comparison window the way the option merges: what the branch merged in
 * changed since both branches parted, as a pull request shows it. A workbook that may conflict shows both versions,
 * each compared with the revision both parted at, the version the merge goes into first. The files outside any
 * project are not compared.
 */
const ChangedInBoth: React.FC<ChangedInBothProps> = ({ project, result, yoursEarlier }) => {
    const { t } = useTranslation()
    const files = project.changedInBoth ?? []
    if (files.length === 0) {
        return null
    }
    const conflicts = new Set(project.conflicts)
    const { id, path } = project
    return (
        <ChangedFileList
            files={files.map((file): ProjectFileChange => ({ path: file, type: 'modified' }))}
            renderMarks={file => (conflicts.has(file.path) ? <ConflictTag /> : <MergesByItselfTag />)}
            testId="merge-scope-changed-in-both"
            virtual={false}
            compare={id && path
                ? {
                    hint: t('merge:scope.compare_hint'),
                    open: file => openRevisionsCompareWindow(id, result.sourceBranch, result.targetBranch,
                        result.sourceBranch, toProjectRelativePath(file.path, path), yoursEarlier),
                }
                : undefined}
        />
    )
}

/** Marks a file both branches changed in different parts, which Sync combines by itself. */
const MergesByItselfTag: React.FC = () => {
    const { t } = useTranslation()
    return (
        <Tooltip title={t('merge:scope.merges_by_itself_hint')}>
            <Tag color="success" data-testid="file-merges-by-itself" icon={<CheckOutlined />}>
                {t('merge:scope.merges_by_itself')}
            </Tag>
        </Tooltip>
    )
}

interface ProjectHeaderProps {
    project: ChangedProject
    changedFiles: number
}

/**
 * The head of a project in what a merge changes: its name, whether it is the current project, whether the merge
 * adds or deletes it as a whole, and how many of its files change. The files outside any project are named so.
 */
export const ProjectHeader: React.FC<ProjectHeaderProps> = ({ project, changedFiles }) => {
    const { t } = useTranslation()
    return (
        <ChangesHeader
            count={t('merge:scope.changed_files', { count: changedFiles })}
            name={project.name ?? t('merge:scope.outside')}
        >
            {project.name && (
                <Tag color={project.current ? 'blue' : 'default'}>
                    {project.current ? t('merge:scope.current') : t('merge:scope.other')}
                </Tag>
            )}
            <ProjectStatusTag status={project.status} />
        </ChangesHeader>
    )
}

/** Marks a project the merge adds or deletes as a whole; a project it only changes needs no mark. */
const ProjectStatusTag: React.FC<{ status: ProjectFileChangeType | undefined }> = ({ status }) => {
    const { t } = useTranslation()
    if (status === 'added') {
        return <Tag color="success">{t('repository:browser.files.change.added')}</Tag>
    }
    if (status === 'deleted') {
        return <Tag color="error">{t('repository:browser.files.change.deleted')}</Tag>
    }
    return null
}

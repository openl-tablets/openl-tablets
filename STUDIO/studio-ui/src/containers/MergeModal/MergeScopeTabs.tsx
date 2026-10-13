import React from 'react'
import { Alert, Flex, Listy, Space, Tabs, Tag, Tooltip, Typography } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { ProjectFileChangeType } from '../../services/projectStatus'
import { ChangesHeader, FilePath, LIST_HEIGHT } from '../projects/ChangedFileList'
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
 * their project.
 */
export const MergeScopeTabs: React.FC<MergeScopeTabsProps> = ({ receive, send }) => {
    const { t } = useTranslation()
    const tab = (key: string, label: string, result: CheckMergeResult | null) => ({
        key,
        label: isKnown(result) ? t('merge:scope.tab', { label, count: projectCount(result?.scope) }) : label,
        children: <ScopeList result={result} />,
    })
    return (
        <Tabs
            data-testid="merge-scope"
            size="small"
            items={[
                tab('receive', t('merge:actions.receive'), receive),
                tab('send', t('merge:actions.send'), send),
            ]}
        />
    )
}

const ScopeList: React.FC<{ result: CheckMergeResult | null }> = ({ result }) => {
    const { t } = useTranslation()
    if (!isKnown(result)) {
        return (
            <Typography.Text data-testid="merge-scope-unknown" type="secondary">
                {t('merge:scope.unknown')}
            </Typography.Text>
        )
    }
    const scope = result?.scope
    if (!scope || (scope.projects.length === 0 && scope.hiddenProjects === 0)) {
        return <Typography.Text type="secondary">{t('merge:scope.nothing')}</Typography.Text>
    }
    const reachesOthers = scope.hiddenProjects > 0 || scope.projects.some(project => project.name && !project.current)
    const renderProject = (project: ProjectScope) => (
        <Flex vertical data-testid="merge-scope-project" gap={2} style={{ paddingBlock: 4 }}>
            <ProjectHeader changedFiles={project.changedFiles} project={project} />
            {project.changedInBoth?.map(file => (
                <Flex key={file} align="center" data-testid="merge-scope-changed-in-both" gap="small">
                    <FilePath path={file} />
                    <Tooltip title={t('merge:scope.changed_in_both_hint')}>
                        <Tag color="warning" icon={<WarningOutlined />}>
                            {t('merge:scope.changed_in_both')}
                        </Tag>
                    </Tooltip>
                </Flex>
            ))}
        </Flex>
    )
    return (
        <Space orientation="vertical" size="small" style={{ width: '100%' }}>
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

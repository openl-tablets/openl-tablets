import React, { useMemo } from 'react'
import { Link, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { WorkspaceHeader } from '../../components/WorkspaceHeader'
import type { ModuleInfo } from '../../services/modules'
import type { Project } from '../../types/projects'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { CompilationMark } from '../modules/ModuleCompilationView'
import type { ConfirmBefore } from '../modules/useConfirmBefore'
import type { ModuleCompilation } from '../modules/useModuleCompilation'
import { ReadOnlyTag } from '../maven/mavenProject'
import { BranchSwitcher } from './BranchSwitcher'
import { CrumbSwitcher } from './CrumbSwitcher'
import { switchedRoute, switcherItems } from './moduleSwitcher'
import { ProjectSwitcher } from './ProjectSwitcher'

interface ProjectScreenHeaderProps {
    project: Project
    /** The branch the project is read on; null for a repository without branches. */
    branch: string | null
    /** Whether the project is being opened: the trail switches nothing meanwhile. */
    opening: boolean
    /** Asks before another branch is read over what is not saved. */
    confirmDiscard: ConfirmBefore
    /** Reads the screen again on the branch switched to. */
    onBranchSwitched: () => void
    /** The modules of the project, or null while they are read. */
    modules: ModuleInfo[] | null
    /** What the title names: the module on screen, or the messages. */
    title: string
    /** The item of the title among the modules and the messages of the project. */
    selectedKey: string
    /** Whether another module cannot be opened yet. */
    switcherDisabled?: boolean | undefined
    compilation: ModuleCompilation
    /** Whether nothing can be compiled: a project nobody opened. */
    compilationDisabled: boolean
    /** Compiles the project again and reads the screen afresh. */
    onRefresh: () => void
    /** What the screen does to the project, at the end of the header. */
    actions: React.ReactNode
    testId: string
}

/**
 * The header of a screen of an opened project: the module page and the screen of the messages wear the same one.
 *
 * <p>The trail leads to the projects, to another project and to another branch of this one, as a project holds what
 * it shows. It ends in the name of what is on screen, which opens another module of the project, or its messages; the
 * mark after it tells what was compiled and compiles it again. A project that can only be read, such as
 * one downloaded from the Maven repository, is marked so after that name. The actions of the screen end the line of
 * the trail.
 */
export const ProjectScreenHeader = ({
    project,
    branch,
    opening,
    confirmDiscard,
    onBranchSwitched,
    modules,
    title,
    selectedKey,
    switcherDisabled,
    compilation,
    compilationDisabled,
    onRefresh,
    actions,
    testId,
}: ProjectScreenHeaderProps) => {
    const { t } = useTranslation('repository')
    const navigate = useNavigate()
    const hasBranches = supportsBranches({ features: project.repositoryInfo?.features }) && branch !== null
    // The modules of the project, and after them its messages, which they all share.
    const localizationLabel = t('localization:title')
    const items = useMemo(() => switcherItems(modules, localizationLabel), [modules, localizationLabel])
    // The trail goes down from the projects as a project holds what it shows: the project, the branch it is read on,
    // then the module of that branch on screen.
    const crumbs = [
        <Link key="projects" to="/projects">{t('home.title')}</Link>,
        // Which of its modules to read is the reader's to say, on the project's own screen.
        <ProjectSwitcher
            key="project"
            currentName={project.name}
            currentProjectId={project.id}
            disabled={opening}
            onSelect={picked => void navigate(`/projects/${picked}`)}
            testId="crumb-project"
        />,
        hasBranches && (
            <BranchSwitcher
                key="branch"
                beforeSwitch={confirmDiscard}
                currentBranch={branch}
                currentBranchDefault={project.branchDefault}
                currentBranchProtected={project.branchProtected}
                data-testid="crumb-branch"
                disabled={opening}
                onSwitched={onBranchSwitched}
                projectId={project.id}
                tone="secondary"
            />
        ),
    ]

    return (
        <WorkspaceHeader
            actions={actions}
            crumbs={crumbs}
            testId={testId}
            title={(
                <CrumbSwitcher
                    current={title}
                    disabled={switcherDisabled}
                    emptyText={t('browser.module.module_no_match')}
                    items={items}
                    loading={modules === null}
                    searchPlaceholder={t('browser.module.module_filter')}
                    selectedKey={selectedKey}
                    testId="module-switcher"
                    onSelect={picked => {
                        if (picked !== selectedKey) {
                            void navigate(switchedRoute(project.id, picked))
                        }
                    }}
                />
            )}
            titleAfter={(
                <>
                    {project.readOnly && <ReadOnlyTag projectId={project.id} />}
                    <CompilationMark compilation={compilation} disabled={compilationDisabled} onRefresh={onRefresh} />
                </>
            )}
        />
    )
}

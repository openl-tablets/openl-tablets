import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Checkbox, Modal } from 'antd'
import { useTranslation } from 'react-i18next'
import { ProjectStatus } from '../../constants/project'
import { useGlobalEvents } from '../../hooks'
import { DependencyList } from './DependencyList'
import type { OpenDependenciesOptions } from './openProjectDialog'
import type { ProjectDependency } from '../../types/projects'

/**
 * Detail passed with the {@code openProjectModal} event by whoever opens the dialog.
 *
 * The dependencies are read before the dialog opens, so a project that declares none never asks anything.
 */
export interface OpenProjectModalDetail {
    projectName: string
    /** The repository of the project; only its own branches mean anything to it. */
    repository?: string
    /** The branch the project is opened on. A repository without branches reports none. */
    branch?: string
    dependencies: ProjectDependency[]
    /** Runs the opening with the answers the user gave. */
    onConfirm: (options: OpenDependenciesOptions) => void
}

/**
 * The dependencies its own repository keeps on another branch than the project.
 *
 * A resolved dependency of another repository is never one of them: nothing keeps the branches of two
 * repositories in step, so its branch name means nothing here.
 */
const onOtherBranch = (
    dependencies: ProjectDependency[],
    repository?: string,
    branch?: string
): ProjectDependency[] => dependencies.filter(dependency => !dependency.missing
    && dependency.repository === repository && !!branch && !!dependency.branch && dependency.branch !== branch)

/** Moving a dependency with unsaved changes would lose them, so it keeps its branch. */
const keepsBranch = (dependency: ProjectDependency): boolean => dependency.status === ProjectStatus.Editing

/**
 * Whether the move can stop short of a transitive dependency. It moves only what a dependency on the branch of the
 * project declares, so it never reaches below a dependency of another repository or one that keeps its branch.
 * The list does not say which dependency declares which, so then any transitive one may stay off the branch.
 */
const stopsShort = (dependencies: ProjectDependency[], otherBranch: ProjectDependency[], repository?: string) =>
    dependencies.some(dependency => !dependency.missing && dependency.repository !== repository)
    || otherBranch.some(keepsBranch)

/**
 * Asks whether to open the dependencies of a project along with it, and whether to move the ones on another
 * branch of its repository onto the branch of the project.
 *
 * The warning names every dependency the project is compiled against off its branch: the ones nothing
 * resolves, and the ones that stay on another branch. An unresolved dependency carries its declared name
 * alone, so it is reported whatever repository was meant.
 *
 * @example to open this modal, dispatch a custom event:
 * globalThis.dispatchEvent(new CustomEvent('openProjectModal', {detail: {...}}))
 */
export const OpenProjectModal: React.FC = () => {
    const { t } = useTranslation('repository')
    const { detail } = useGlobalEvents<OpenProjectModalDetail>('openProjectModal')

    const [openDependencies, setOpenDependencies] = useState(true)
    const [switchDependencies, setSwitchDependencies] = useState(true)

    // Opening the dependencies on the branch of the project is what it needs to compile, so it stays the default.
    useEffect(() => {
        setOpenDependencies(true)
        setSwitchDependencies(true)
    }, [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openProjectModal', { detail: null }))
    }, [])

    const moving = openDependencies && switchDependencies

    const handleConfirm = useCallback(() => {
        detail?.onConfirm({ openDependencies, switchDependencies: moving })
        handleClose()
    }, [detail, openDependencies, moving, handleClose])

    const dependencies = detail?.dependencies ?? []
    const otherBranch = onOtherBranch(dependencies, detail?.repository, detail?.branch)
    const canMove = otherBranch.some(dependency => !keepsBranch(dependency))
    const mayStay = stopsShort(dependencies, otherBranch, detail?.repository)
    const strayDependencies = [
        ...dependencies.filter(dependency => dependency.missing),
        ...(moving
            ? otherBranch.filter(dependency => keepsBranch(dependency) || (mayStay && !!dependency.transitive))
            : otherBranch),
    ]

    return (
        <Modal
            destroyOnHidden
            cancelText={t('common:btn.cancel')}
            okText={t('browser.open_project.confirm_button')}
            onCancel={handleClose}
            onOk={handleConfirm}
            open={!!detail?.dependencies?.length}
            title={t('browser.open_project.title', { name: detail?.projectName })}
        >
            <Checkbox
                checked={openDependencies}
                data-testid="open-project-dependencies"
                onChange={event => setOpenDependencies(event.target.checked)}
            >
                {t('browser.open_project.open_dependencies')}
            </Checkbox>
            {canMove && (
                <Checkbox
                    checked={moving}
                    data-testid="open-project-switch-dependencies"
                    disabled={!openDependencies}
                    onChange={event => setSwitchDependencies(event.target.checked)}
                    style={{ display: 'flex', marginTop: 8 }}
                >
                    {t('browser.open_project.switch_dependencies', { branch: detail?.branch })}
                </Checkbox>
            )}
            {strayDependencies.length > 0 && (
                <Alert
                    showIcon
                    data-testid="open-project-branch-warning"
                    description={<DependencyList deps={strayDependencies} />}
                    style={{ marginTop: 12 }}
                    title={t('browser.open_project.other_branch_warning', { branch: detail?.branch })}
                    type="warning"
                />
            )}
        </Modal>
    )
}

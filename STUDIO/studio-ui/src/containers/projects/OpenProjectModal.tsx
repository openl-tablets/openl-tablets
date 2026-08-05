import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Checkbox, Modal } from 'antd'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from '../../hooks'
import { DependencyList } from './DependencyList'
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
    /** Runs the opening with the answer the user gave. */
    onConfirm: (openDependencies: boolean) => void
}

/**
 * The dependencies the branch of the project does not hold: the ones nothing resolves, and the ones its own
 * repository keeps on another branch. Opening the project takes them as they are, which is what the warning
 * is about.
 *
 * An unresolved dependency carries its declared name alone, so it is reported whatever repository was meant.
 * A resolved one of another repository is not: nothing keeps the branches of two repositories in step, so its
 * branch name means nothing here.
 */
const outsideBranch = (
    dependencies: ProjectDependency[],
    repository?: string,
    branch?: string
): ProjectDependency[] => dependencies.filter(dependency => dependency.missing
    || (dependency.repository === repository && !!branch && !!dependency.branch && dependency.branch !== branch))

/**
 * Asks whether to open the dependencies of a project along with it, and warns about the ones the branch
 * of the project does not hold.
 *
 * @example to open this modal, dispatch a custom event:
 * globalThis.dispatchEvent(new CustomEvent('openProjectModal', {detail: {...}}))
 */
export const OpenProjectModal: React.FC = () => {
    const { t } = useTranslation('repository')
    const { detail } = useGlobalEvents<OpenProjectModalDetail>('openProjectModal')

    const [openDependencies, setOpenDependencies] = useState(true)

    // Opening the dependencies is what the project needs to compile, so it stays the default.
    useEffect(() => setOpenDependencies(true), [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openProjectModal', { detail: null }))
    }, [])

    const handleConfirm = useCallback(() => {
        detail?.onConfirm(openDependencies)
        handleClose()
    }, [detail, openDependencies, handleClose])

    const strayDependencies = outsideBranch(detail?.dependencies ?? [], detail?.repository, detail?.branch)

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

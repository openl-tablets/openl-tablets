import type { Project } from '../../types/projects'

/** Every action a project offers, wherever it is offered from. */
export type ActionId = 'save' | 'open' | 'close' | 'deploy' | 'compare' | 'copy' | 'openRevision' | 'sync'
    | 'deleteBranch' | 'export' | 'delete' | 'unlock'

/** The actions a project in the list offers; unlocking belongs to the project's own screen. */
export type RowActionId = Exclude<ActionId, 'unlock'>

interface ProjectActionMeta {
    labelKey: string
    /** The server-computed capabilities that gate this action; any one of them is enough. */
    caps: Array<keyof NonNullable<Project['capabilities']>>
    /** Destroys the project itself, unlike deleting a branch of it — shown in red, confirmed first. */
    danger?: boolean
}

/**
 * What each action is called and who may perform it — the one place both the project screen and the list
 * rows read it from, so an action can never be offered on one surface and hidden on the other.
 *
 * Access always comes from the server-computed capabilities (ACL ∧ project state); the UI never re-derives
 * it from raw permissions.
 */
export const PROJECT_ACTIONS: Record<ActionId, ProjectActionMeta> = {
    save: { labelKey: 'browser.save', caps: ['canSave']},
    open: { labelKey: 'browser.open', caps: ['canOpen']},
    close: { labelKey: 'browser.close', caps: ['canClose']},
    // Copy covers both branching the project and copying it into a new one; the two are granted separately.
    copy: { labelKey: 'browser.copy', caps: ['canCopy', 'canManageBranches']},
    // The server decides: the main branch, a protected branch without the bypass right, and the last branch
    // holding the project are all refused when the deletion runs. The last of these would delete the project
    // itself, which the delete permission — not branch management — guards.
    deleteBranch: { labelKey: 'browser.delete_branch_action', caps: ['canDeleteBranch']},
    // Reading the project is enough to open any of its revisions.
    openRevision: { labelKey: 'browser.open_revision', caps: ['canViewHistory']},
    sync: { labelKey: 'browser.sync', caps: ['canManageBranches']},
    deploy: { labelKey: 'browser.deploy', caps: ['canDeploy']},
    compare: { labelKey: 'browser.compare', caps: ['canCompare']},
    export: { labelKey: 'browser.export', caps: ['canExport']},
    delete: { labelKey: 'browser.delete', caps: ['canDelete'], danger: true },
    unlock: { labelKey: 'browser.unlock', caps: ['canUnlock'], danger: true },
}

/** Whether the current user may perform the action on this project right now. */
export const isActionAvailable = (project: Project, id: ActionId): boolean => {
    const action = PROJECT_ACTIONS[id]
    return action.caps.some(cap => !!project.capabilities?.[cap])
}

/** The given actions, in the given order, keeping only those the project currently offers. */
export const availableActions = <T extends ActionId>(project: Project, ids: readonly T[]): T[] =>
    ids.filter(id => isActionAvailable(project, id))
